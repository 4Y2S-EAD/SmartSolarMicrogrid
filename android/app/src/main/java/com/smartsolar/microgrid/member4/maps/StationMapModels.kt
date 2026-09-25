/* Member 4 | Nearby Stations | Server map projection and UI-only screen state. */
package com.smartsolar.microgrid.member4.maps

import com.smartsolar.microgrid.network.models.StationLocation

data class MapStation(
    val stationId: String,
    val stationName: String,
    val location: StationLocation?,
    val capacityKwh: Double?,
    val batterySlotCount: Int?,
    val availableSlotCount: Int?,
    val status: String,
    val schedule: String?,
    val distanceKm: Double? = null
)

data class StationMapResponse(val stations: List<MapStation>, val unplottableCount: Int)

data class StationMapState(
    val loading: Boolean = false,
    val stations: List<MapStation> = emptyList(),
    val unplottableCount: Int = 0,
    val error: Boolean = false,
    val loaded: Boolean = false,
    val query: String = "",
    val nearby: Boolean = false
)
