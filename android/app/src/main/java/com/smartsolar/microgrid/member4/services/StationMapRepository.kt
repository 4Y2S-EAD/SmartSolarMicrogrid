/* Member 4 | Nearby Stations | Reuse Member 2's API and the server's map validation/query projection. */
package com.smartsolar.microgrid.member4.services

import com.smartsolar.microgrid.member4.maps.StationMapResponse
import com.smartsolar.microgrid.network.ApiClient
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException

class StationMapRepository {
    suspend fun load(query: String, latitude: Double?, longitude: Double?): StationMapResponse {
        // The existing API remains the canonical source in all-stations mode. The map projection
        // supplies validated nullable coordinates (missing BSON coordinates must not become zero).
        val baseline = if (query.isBlank() && latitude == null) try {
            val response = ApiClient.apiService.getStations()
            if (!response.isSuccessful) throw HttpException(response)
            requireNotNull(response.body()).associateBy { it.stationId }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A malformed legacy station must not hide the valid records in the safe map projection.
            null
        } else null
        val response = ApiClient.apiService.getMapStations(
            query.takeIf { it.isNotBlank() }, latitude, longitude, if (latitude != null) 25.0 else null
        )
        if (!response.isSuccessful) throw HttpException(response)
        val projection = requireNotNull(response.body())
        // Render the server-selected order; this is identity-based display mapping, not local filtering.
        return projection.copy(stations = projection.stations.map { station ->
            val source = baseline?.get(station.stationId)
            if (source == null) station else station.copy(
                stationName = source.stationName, status = source.status, schedule = source.schedule
            )
        })
    }
}
