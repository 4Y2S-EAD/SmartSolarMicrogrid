package com.smartsolar.microgrid.member2.activities

import androidx.annotation.DrawableRes
import com.smartsolar.microgrid.R

/** Local presentation artwork only; station records remain owned by the API. */
object StationImages {
    private val images = intArrayOf(
        R.drawable.station1, R.drawable.station2, R.drawable.station3, R.drawable.station4
    )

    @DrawableRes
    fun forStation(stationId: String): Int {
        // ID-based selection survives sorting, refreshes, recycling and opening details.
        return images[Math.floorMod(stationId.hashCode(), images.size)]
    }
}
