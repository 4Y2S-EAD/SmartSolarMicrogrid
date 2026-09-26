/* Member 4 | Nearby Stations | Lifecycle-safe request and presentation state, without business rules. */
package com.smartsolar.microgrid.member4.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolar.microgrid.member4.services.StationMapRepository
import com.smartsolar.microgrid.member4.services.StationRouteRepository
import com.smartsolar.microgrid.member4.services.RouteRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NearbyStationsViewModel : ViewModel() {
    private val repository = StationMapRepository()
    private val routeRepository = StationRouteRepository()
    private var routeRequest: Job? = null
    private val mutableState = MutableStateFlow(StationMapState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    init { refresh() }

    fun search(query: String) {
        // Store submitted UI input and let the backend perform the actual matching.
        mutableState.value = mutableState.value.copy(query = query.trim(), loading = true)
        refresh()
    }

    fun useLocation(latitude: Double, longitude: Double) {
        // Forward device coordinates to the server without calculating distance locally.
        this.latitude = latitude
        this.longitude = longitude
        mutableState.value = mutableState.value.copy(nearby = true, loading = true,
            origin = RoutePoint(latitude, longitude))
        refresh()
    }

    fun showAll() {
        // Clear the optional location origin while preserving any submitted station search.
        latitude = null
        longitude = null
        mutableState.value = mutableState.value.copy(nearby = false, loading = true, origin = null)
        refresh()
    }

    fun refresh() {
        // Cancel superseded requests so older responses cannot overwrite newer results.
        request?.cancel()
        routeRequest?.cancel()
        mutableState.value = mutableState.value.copy(route = null, routeLoading = false, routeError = null)
        request = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = false)
            try {
                val response = repository.load(mutableState.value.query, latitude, longitude)
                mutableState.value = mutableState.value.copy(
                    loading = false, stations = response.stations,
                    unplottableCount = response.unplottableCount, loaded = true
                )
                requestRoute()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, error = true)
            }
        }
    }

    fun requestRoute() {
        // Route only to the backend-selected nearest result; never re-rank stations by road distance.
        routeRequest?.cancel()
        val current = mutableState.value
        val origin = current.origin ?: return
        val nearest = current.stations.firstOrNull() ?: return
        if (!current.nearby || current.loading || current.error || nearest.distanceKm == null ||
            nearest.location == null) return
        mutableState.value = current.copy(route = null, routeLoading = true, routeError = null)
        routeRequest = viewModelScope.launch {
            try {
                val route = routeRepository.load(DrivingRouteRequest(nearest.stationId, origin.latitude, origin.longitude))
                mutableState.value = mutableState.value.copy(route = route, routeLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(route = null, routeLoading = false,
                    routeError = (error as? RouteRequestException)?.code ?: "ROUTING_UNAVAILABLE")
            }
        }
    }
}
