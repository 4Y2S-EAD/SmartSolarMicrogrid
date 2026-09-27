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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import android.graphics.Color
import kotlin.math.ceil
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
    private var routeLine: Polyline? = null
    private var drawnRoute: DrivingRoute? = null
    private var framedRoute: DrivingRoute? = null
    private var threeDimensional = false
    private var normalCamera: CameraPosition? = null
    private var routeFitPoints: List<LatLng> = emptyList()
    private var routeFitAttempts = 0
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
        threeDimensional = savedInstanceState?.getBoolean("threeDimensional") ?: threeDimensional
        @Suppress("DEPRECATION")
        normalCamera = savedInstanceState?.getParcelable<CameraPosition>("normalCamera") ?: normalCamera
        view.findViewById<SwitchMaterial>(R.id.m4ThreeDimensional).apply {
            isChecked = threeDimensional
            setOnCheckedChangeListener { _, checked -> setThreeDimensional(checked) }
        }
        view.findViewById<View>(R.id.m4RouteRetry).setOnClickListener { model.requestRoute() }
        view.findViewById<View>(R.id.m4StartDriving).setOnClickListener {
            val destination = navigationDestination(model.state.value) ?: return@setOnClickListener
            if (!StationNavigation.open(destination) { startActivity(it) }) {
                android.widget.Toast.makeText(requireContext(), R.string.m4_navigation_unavailable,
                    android.widget.Toast.LENGTH_LONG).show()
            }
        }
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
        routeLine = null
        drawnRoute = null
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
                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        shouldFrame = false
                        routeFitPoints = emptyList()
                    }
                }
                map.setOnCameraIdleListener { ensureRouteFits() }
                map.isBuildingsEnabled = true
                enableLocationLayer()
                savedCamera?.let { map.moveCamera(CameraUpdateFactory.newCameraPosition(it)); shouldFrame = false }
                if (!model.state.value.loading) renderMarkers(model.state.value.stations)
                renderRoute(model.state.value)
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
        renderRoute(state)
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
        routeLine = null
        drawnRoute = null
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
                if (threeDimensional) {
                    normalCamera = map.cameraPosition
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder(map.cameraPosition).tilt(45f).build()))
                }
                shouldFrame = false
            }
        }
    }

    private fun renderRoute(state: StationMapState) {
        // Route metrics never reuse the station API's straight-line distance.
        val root = view ?: return
        root.findViewById<View>(R.id.m4StartDriving).isVisible = navigationDestination(state) != null
        root.findViewById<View>(R.id.m4RoutePanel).isVisible =
            state.routeLoading || state.route != null || state.routeError != null
        root.findViewById<View>(R.id.m4RouteRetry).isVisible =
            state.routeError != null && state.routeError != "SIGN_IN_REQUIRED"
        val summary = root.findViewById<TextView>(R.id.m4RouteSummary)
        val route = state.route
        summary.text = when {
            state.routeLoading -> getString(R.string.m4_route_loading)
            route != null -> {
                val distance = if (route.distanceMeters < 1000) getString(R.string.m4_route_meters, route.distanceMeters)
                    else getString(R.string.m4_route_km, route.distanceMeters / 1000.0)
                val minutes = ceil(route.durationSeconds / 60).toInt()
                val duration = if (minutes < 60) getString(R.string.m4_route_minutes, minutes)
                    else getString(R.string.m4_route_hours, minutes / 60, minutes % 60)
                getString(R.string.m4_route_summary, route.stationName, distance, duration)
            }
            state.routeError == "ROUTING_NOT_CONFIGURED" -> getString(R.string.m4_route_setup)
            state.routeError == "NO_DRIVING_ROUTE" -> getString(R.string.m4_route_none)
            state.routeError == "SIGN_IN_REQUIRED" -> getString(R.string.m4_route_sign_in)
            state.routeError != null -> getString(R.string.m4_route_unavailable)
            else -> ""
        }
        if (route == null) {
            routeLine?.remove()
            routeLine = null
            drawnRoute = null
            framedRoute = null
            routeFitPoints = emptyList()
            return
        }
        val map = googleMap ?: return
        if (drawnRoute != route) {
            val points = RoutePolyline.decode(route.encodedPolyline).map { LatLng(it.latitude, it.longitude) }
            routeLine?.remove()
            routeLine = map.addPolyline(PolylineOptions().addAll(points)
                .color(Color.rgb(33, 101, 245)).width(5f * resources.displayMetrics.density)
                .geodesic(false).zIndex(2f))
            drawnRoute = route
        }
        if (framedRoute != route) {
            shouldFrame = false
            root.findViewById<View>(R.id.m4MapContainer).post {
                if (view != null && googleMap === map && model.state.value.route == route) fitRoute(route)
            }
        }
    }

    private fun navigationDestination(state: StationMapState): RoutePoint? {
        // Resolve the current nearest API station at tap time; never navigate using a stale search result.
        if (!state.nearby || state.loading || state.error) return null
        val station = state.stations.firstOrNull()?.takeIf { it.distanceKm != null } ?: return null
        val destination = state.route?.takeIf { it.stationId == station.stationId }?.destination
            ?: station.location?.let { location ->
                val latitude = location.latitude ?: return null
                val longitude = location.longitude ?: return null
                RoutePoint(latitude, longitude)
            } ?: return null
        return destination.takeIf { it.latitude.isFinite() && it.longitude.isFinite() &&
            it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
    }

    private fun fitRoute(route: DrivingRoute) {
        // Include the exact device origin, station destination and every returned road vertex.
        val map = googleMap ?: return
        val container = view?.findViewById<View>(R.id.m4MapContainer) ?: return
        if (container.width == 0 || container.height == 0) return
        val points = RoutePolyline.decode(route.encodedPolyline).map { LatLng(it.latitude, it.longitude) } +
            listOf(LatLng(route.origin.latitude, route.origin.longitude),
                LatLng(route.destination.latitude, route.destination.longitude))
        val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
        val padding = minOf((40 * resources.displayMetrics.density).toInt(), container.height / 4, container.width / 4)
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, container.width, container.height, padding))
        normalCamera = CameraPosition.Builder(map.cameraPosition).tilt(0f).bearing(0f).build()
        if (threeDimensional) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(map.cameraPosition)
                .tilt(45f).zoom((map.cameraPosition.zoom - 0.5f).coerceAtLeast(map.minZoomLevel)).build()))
        }
        framedRoute = route
        routeFitPoints = points
        routeFitAttempts = 0
        ensureRouteFits()
    }

    private fun ensureRouteFits() {
        // Tilt changes the visible footprint; zoom out if any route endpoint/vertex would be clipped.
        val map = googleMap ?: return
        val container = view?.findViewById<View>(R.id.m4MapContainer) ?: return
        if (routeFitPoints.isEmpty()) return
        val margin = 16 * resources.displayMetrics.density
        val fits = routeFitPoints.all {
            val pixel = map.projection.toScreenLocation(it)
            pixel.x >= margin && pixel.x <= container.width - margin &&
                pixel.y >= margin && pixel.y <= container.height - margin
        }
        if (fits || routeFitAttempts >= 8 || map.cameraPosition.zoom <= map.minZoomLevel) {
            routeFitPoints = emptyList()
        } else {
            routeFitAttempts++
            map.moveCamera(CameraUpdateFactory.zoomTo((map.cameraPosition.zoom - 0.5f).coerceAtLeast(map.minZoomLevel)))
        }
    }

    private fun setThreeDimensional(enabled: Boolean) {
        // Tilt the same Google Map; restore the normal viewport when the control is disabled.
        threeDimensional = enabled
        val map = googleMap ?: return
        if (enabled) {
            normalCamera = map.cameraPosition
            val route = model.state.value.route
            if (route != null) fitRoute(route)
            else map.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder(map.cameraPosition).tilt(45f).build()))
        } else {
            routeFitPoints = emptyList()
            val position = normalCamera ?: map.cameraPosition
            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder(position).tilt(0f).bearing(0f).build()))
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
                map.animateCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder(map.cameraPosition).target(LatLng(lat, lng))
                        .zoom(maxOf(map.cameraPosition.zoom, 12f)).tilt(if (threeDimensional) 45f else 0f).build()))
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
        outState.putBoolean("threeDimensional", threeDimensional)
        outState.putParcelable("normalCamera", normalCamera)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        // Release map/view references; the retained ViewModel owns only data, never a view or Activity.
        savedCamera = googleMap?.cameraPosition ?: savedCamera
        googleMap = null
        routeLine = null
        drawnRoute = null
        routeFitPoints = emptyList()
        renderedStations = null
        mapWait?.cancel()
        locationRequest?.cancel()
        view?.findViewById<RecyclerView>(R.id.m4Stations)?.adapter = null
        super.onDestroyView()
    }
}
