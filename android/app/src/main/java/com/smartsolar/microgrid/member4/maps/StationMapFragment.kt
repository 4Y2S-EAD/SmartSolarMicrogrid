/*
 * Member 4 | Nearby Stations
 * Shared native Google Map and station list. Device permissions and rendering stay here;
 * search, distance, radius filtering and data validation remain in the central API.
 */
package com.smartsolar.microgrid.member4.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.smartsolar.microgrid.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StationMapFragment : Fragment(R.layout.m4_fragment_station_map) {
    private val model: NearbyStationsViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private var mapWait: Job? = null
    private var locationRequest: CancellationTokenSource? = null
    private var savedCamera: CameraPosition? = null
    private var renderedStations: List<MapStation>? = null
    private var renderedNearestId: String? = null
    private var shouldFrame = true
    private lateinit var adapter: StationMapAdapter

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (view != null) {
            if (hasLocationPermission()) locate() else {
                showLocationMessage(R.string.m4_location_denied)
                model.showAll()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Wire the shared screen once; no role-specific map or network implementation is needed.
        @Suppress("DEPRECATION")
        savedCamera = savedInstanceState?.getParcelable<CameraPosition>("camera") ?: savedCamera
        adapter = StationMapAdapter(::selectStation)
        view.findViewById<RecyclerView>(R.id.m4Stations).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@StationMapFragment.adapter
        }
        view.findViewById<View>(R.id.m4Back).setOnClickListener { requireActivity().finish() }
        view.findViewById<View>(R.id.m4Refresh).setOnClickListener { refresh() }
        view.findViewById<View>(R.id.m4All).setOnClickListener {
            locationRequest?.cancel()
            view.findViewById<View>(R.id.m4Near).isEnabled = true
            view.findViewById<View>(R.id.m4LocationMessage).isVisible = false
            shouldFrame = true
            model.showAll()
        }
        view.findViewById<View>(R.id.m4Near).setOnClickListener { requestLocation() }
        val search = view.findViewById<EditText>(R.id.m4Search)
        search.setText(model.state.value.query)
        view.findViewById<View>(R.id.m4SearchSubmit).setOnClickListener { submitSearch() }
        view.findViewById<View>(R.id.m4SearchClear).setOnClickListener {
            search.setText("")
            submitSearch()
        }
        search.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) { submitSearch(); true } else false
        }
        view.findViewById<View>(R.id.m4MapMessage).setOnClickListener { initializeMap() }
        initializeMap()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { render(it) }
            }
        }
    }

    private fun submitSearch() {
        // Send entered text to the API; Android does not search the station collection locally.
        val search = requireView().findViewById<EditText>(R.id.m4Search)
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(search.windowToken, 0)
        search.clearFocus()
        shouldFrame = true
        dismissDetails()
        model.search(search.text.toString())
    }

    private fun refresh() {
        // Refresh data without stealing the camera position from a user exploring the map.
        dismissDetails()
        model.refresh()
    }

    private fun initializeMap() {
        // Check SDK availability and create a lifecycle-managed child map with an independent timeout.
        if (view == null || childFragmentManager.isStateSaved) return
        savedCamera = googleMap?.cameraPosition ?: savedCamera
        googleMap = null
        renderedStations = null
        mapWait?.cancel()
        val message = requireView().findViewById<TextView>(R.id.m4MapMessage)
        message.isVisible = true
        message.setText(R.string.m4_loading_map)
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext()) != ConnectionResult.SUCCESS) {
            message.setText(R.string.m4_map_unavailable)
            return
        }
        try {
            val fragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction().replace(R.id.m4MapContainer, fragment).commitNow()
            mapWait = viewLifecycleOwner.lifecycleScope.launch {
                delay(15000)
                view?.findViewById<TextView>(R.id.m4MapMessage)?.setText(R.string.m4_map_unavailable)
            }
            fragment.getMapAsync { map ->
                if (view == null || childFragmentManager.findFragmentById(R.id.m4MapContainer) !== fragment) return@getMapAsync
                googleMap = map
                map.uiSettings.isZoomControlsEnabled = true
                map.uiSettings.isMapToolbarEnabled = false
                map.setPadding(0, 0, 0, 12)
                map.setOnMapLoadedCallback {
                    mapWait?.cancel()
                    view?.findViewById<View>(R.id.m4MapMessage)?.isVisible = false
                }
                map.setOnMarkerClickListener { marker ->
                    model.state.value.stations.firstOrNull { it.stationId == marker.tag }?.let(::selectStation)
                    true
                }
                map.setOnCameraMoveStartedListener { reason ->
                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) shouldFrame = false
                }
                enableLocationLayer()
                savedCamera?.let { map.moveCamera(CameraUpdateFactory.newCameraPosition(it)); shouldFrame = false }
                if (!model.state.value.loading) renderMarkers(model.state.value.stations)
            }
        } catch (_: Exception) {
            message.setText(R.string.m4_map_unavailable)
        }
    }

    private fun render(state: StationMapState) {
        // Keep map loading, API loading and error states independent so the list stays usable.
        val root = view ?: return
        root.findViewById<View>(R.id.m4Loading).isVisible = state.loading
        root.findViewById<TextView>(R.id.m4ResultTitle).setText(
            if (state.nearby) R.string.m4_near_me else R.string.m4_stations)
        val messages = mutableListOf<String>()
        if (state.error) messages.add(getString(R.string.m4_data_error))
        else if (state.loading) messages.add(getString(R.string.m4_loading_stations))
        else if (state.loaded && state.stations.isEmpty()) messages.add(getString(
            if (state.query.isNotEmpty() || state.nearby) R.string.m4_no_matches else R.string.m4_empty))
        if (state.unplottableCount > 0) messages.add(resources.getQuantityString(
            R.plurals.m4_unplottable, state.unplottableCount, state.unplottableCount))
        root.findViewById<TextView>(R.id.m4DataMessage).apply {
            text = messages.joinToString("\n")
            isVisible = messages.isNotEmpty()
        }
        adapter.highlightNearest(nearestStationId())
        adapter.submitList(state.stations)
        if (!state.loading) renderMarkers(state.stations)
    }

    private fun nearestStationId(): String? {
        // Nearby responses are already sorted by distance by the API; do not calculate locally.
        val state = model.state.value
        return if (state.nearby && !state.loading && !state.error)
            state.stations.firstOrNull()?.takeIf { it.distanceKm != null }?.stationId else null
    }

    private fun renderMarkers(stations: List<MapStation>) {
        // Defensively guard SDK inputs; business validation remains in the server map projection.
        val map = googleMap ?: return
        val nearestId = nearestStationId()
        if (renderedStations == stations && renderedNearestId == nearestId && !shouldFrame) return
        var nearestMarker: com.google.android.gms.maps.model.Marker? = null
        map.clear()
        val positions = mutableListOf<LatLng>()
        stations.forEach { station ->
            val latitude = station.location?.latitude
            val longitude = station.location?.longitude
            if (latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite()
                && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                val position = LatLng(latitude, longitude)
                positions.add(position)
                val isNearest = station.stationId == nearestId
                val marker = map.addMarker(com.google.android.gms.maps.model.MarkerOptions().position(position)
                    .title(if (isNearest) getString(R.string.m4_nearest_title, station.stationName) else station.stationName)
                    .snippet(if (isNearest) getString(R.string.m4_distance, station.distanceKm)
                        else getString(R.string.m4_status, station.status))
                    .zIndex(if (isNearest) 1f else 0f)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                        if (isNearest) BitmapDescriptorFactory.HUE_AZURE
                        else if (station.status.equals("Active", true)) BitmapDescriptorFactory.HUE_GREEN
                        else BitmapDescriptorFactory.HUE_ORANGE)))
                marker?.tag = station.stationId
                if (isNearest) nearestMarker = marker
            }
        }
        renderedStations = stations
        renderedNearestId = nearestId
        nearestMarker?.showInfoWindow()
        if (shouldFrame && positions.isNotEmpty()) {
            requireView().findViewById<View>(R.id.m4MapContainer).post {
                if (googleMap !== map || view == null || !shouldFrame) return@post
                val update = if (nearestMarker != null) CameraUpdateFactory.newLatLngZoom(nearestMarker!!.position, 14f)
                    else if (positions.distinct().size == 1) CameraUpdateFactory.newLatLngZoom(positions.first(), 14f)
                    else CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().apply { positions.forEach { include(it) } }.build(), 64)
                map.moveCamera(update)
                shouldFrame = false
            }
        }
    }

    private fun selectStation(station: MapStation) {
        // Resolve by server identity and present the same details for a card or marker selection.
        if (childFragmentManager.isStateSaved) return
        dismissDetails()
        val lat = station.location?.latitude
        val lng = station.location?.longitude
        if (lat != null && lng != null && lat.isFinite() && lng.isFinite()
            && lat in -90.0..90.0 && lng in -180.0..180.0) {
            shouldFrame = false
            googleMap?.let { map ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), maxOf(map.cameraPosition.zoom, 12f)))
            }
        }
        StationDetailsBottomSheet.forStation(station).show(childFragmentManager, "station-details")
    }

    private fun dismissDetails() {
        // Do not leave a selected record displayed as fresh after an explicit data refresh.
        (childFragmentManager.findFragmentByTag("station-details") as? StationDetailsBottomSheet)?.dismiss()
    }

    private fun hasLocationPermission(): Boolean {
        // Approximate foreground access is sufficient for this optional nearby feature.
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocation() {
        // Ask only after the user chooses Near me; station requests are never permission-gated.
        if (hasLocationPermission()) locate() else permissions.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationLayer() {
        // Recheck permission before touching the SDK location layer, including after settings changes.
        try { googleMap?.isMyLocationEnabled = hasLocationPermission() } catch (_: SecurityException) { }
    }

    @SuppressLint("MissingPermission")
    private fun locate() {
        // Request a bounded, fresh foreground fix and forward it to the server for nearby filtering.
        if (!hasLocationPermission() || view == null) return
        enableLocationLayer()
        locationRequest?.cancel()
        val cancellation = CancellationTokenSource()
        locationRequest = cancellation
        requireView().findViewById<View>(R.id.m4Near).isEnabled = false
        showLocationMessage(R.string.m4_locating)
        val request = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(30000).setDurationMillis(12000).build()
        try {
            LocationServices.getFusedLocationProviderClient(requireActivity())
                .getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    if (view == null || locationRequest !== cancellation || cancellation.token.isCancellationRequested) return@addOnSuccessListener
                    if (location == null) showLocationMessage(R.string.m4_location_missing)
                    else {
                        val precise = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (precise) requireView().findViewById<View>(R.id.m4LocationMessage).isVisible = false
                        else showLocationMessage(R.string.m4_location_approximate)
                        shouldFrame = true
                        dismissDetails()
                        model.useLocation(location.latitude, location.longitude)
                    }
                }.addOnFailureListener {
                    if (view != null && locationRequest === cancellation) showLocationMessage(R.string.m4_location_missing)
                }.addOnCompleteListener {
                    if (locationRequest === cancellation) view?.findViewById<View>(R.id.m4Near)?.isEnabled = true
                }
        } catch (_: Exception) {
            showLocationMessage(R.string.m4_location_missing)
            view?.findViewById<View>(R.id.m4Near)?.isEnabled = true
        }
    }

    private fun showLocationMessage(message: Int) {
        // Show a non-blocking location explanation while retaining the station map and list.
        view?.findViewById<TextView>(R.id.m4LocationMessage)?.apply { setText(message); isVisible = true }
    }

    override fun onResume() {
        // Respect permissions changed outside the app while the map was paused.
        super.onResume()
        enableLocationLayer()
        if (!hasLocationPermission() && model.state.value.nearby) model.showAll()
    }

    override fun onStop() {
        // Stop optional location work as soon as this screen is no longer visible.
        locationRequest?.cancel()
        view?.findViewById<View>(R.id.m4Near)?.isEnabled = true
        view?.findViewById<TextView>(R.id.m4LocationMessage)?.let { message ->
            if (message.text == getString(R.string.m4_locating)) message.isVisible = false
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Preserve the user's viewport across rotation and process recreation.
        outState.putParcelable("camera", googleMap?.cameraPosition ?: savedCamera)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        // Release map/view references; the retained ViewModel owns only data, never a view or Activity.
        savedCamera = googleMap?.cameraPosition ?: savedCamera
        googleMap = null
        renderedStations = null
        mapWait?.cancel()
        locationRequest?.cancel()
        view?.findViewById<RecyclerView>(R.id.m4Stations)?.adapter = null
        super.onDestroyView()
    }
}
