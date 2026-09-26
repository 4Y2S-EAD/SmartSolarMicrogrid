/* Member 4 | Driving routes | Route values are supplied by Google through the .NET API. */
package com.smartsolar.microgrid.member4.maps

data class RoutePoint(val latitude: Double, val longitude: Double)
data class DrivingRouteRequest(val stationId: String, val latitude: Double, val longitude: Double)
data class DrivingRoute(
    val stationId: String, val stationName: String, val origin: RoutePoint, val destination: RoutePoint,
    val distanceMeters: Int, val durationSeconds: Double, val encodedPolyline: String
)

object RoutePolyline {
    fun decode(encoded: String): List<RoutePoint> {
        // Decode Google's encoded geometry; reject malformed data instead of drawing a fallback line.
        require(encoded.isNotEmpty() && encoded.length <= 1_000_000)
        var index = 0
        var latitude = 0L
        var longitude = 0L
        fun nextDelta(): Long {
            var result = 0L
            var shift = 0
            var value: Int
            do {
                require(index < encoded.length && shift <= 30)
                value = encoded[index++].code - 63
                require(value in 0..63)
                result = result or ((value and 31).toLong() shl shift)
                shift += 5
            } while (value >= 32)
            return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        }
        val points = mutableListOf<RoutePoint>()
        while (index < encoded.length) {
            latitude += nextDelta()
            longitude += nextDelta()
            val point = RoutePoint(latitude / 1e5, longitude / 1e5)
            require(point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0)
            points.add(point)
        }
        require(points.size >= 2)
        return points
    }
}
