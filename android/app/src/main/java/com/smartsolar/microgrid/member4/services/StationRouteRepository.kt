/* Member 4 | Driving routes | Reuse authenticated API transport; never embed Google routing credentials. */
package com.smartsolar.microgrid.member4.services

import com.smartsolar.microgrid.member4.maps.DrivingRoute
import com.smartsolar.microgrid.member4.maps.DrivingRouteRequest
import com.smartsolar.microgrid.member4.maps.RoutePolyline
import com.smartsolar.microgrid.network.ApiClient
import org.json.JSONObject

class RouteRequestException(val code: String) : Exception(code)

class StationRouteRepository {
    suspend fun load(request: DrivingRouteRequest): DrivingRoute {
        // Preserve structured failures without exposing raw server/provider errors in the UI.
        val response = ApiClient.apiService.getDrivingRoute(request)
        if (!response.isSuccessful) {
            val code = if (response.code() == 401) "SIGN_IN_REQUIRED" else runCatching {
                JSONObject(response.errorBody()?.string().orEmpty()).optString("code", "ROUTING_UNAVAILABLE")
            }.getOrDefault("ROUTING_UNAVAILABLE")
            throw RouteRequestException(code)
        }
        val route = requireNotNull(response.body())
        require(route.stationId == request.stationId && route.distanceMeters >= 0 &&
            route.durationSeconds.isFinite() && route.durationSeconds >= 0)
        RoutePolyline.decode(route.encodedPolyline)
        return route
    }
}
