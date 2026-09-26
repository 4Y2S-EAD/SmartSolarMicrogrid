/* Member 4 | Nearby Stations | Render server-returned summaries and emit station selections. */
package com.smartsolar.microgrid.member4.maps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import com.smartsolar.microgrid.member2.activities.StationImages
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R

class StationMapAdapter(private val select: (MapStation) -> Unit) :
    ListAdapter<MapStation, StationMapAdapter.Holder>(object : DiffUtil.ItemCallback<MapStation>() {
        override fun areItemsTheSame(old: MapStation, new: MapStation): Boolean {
            // Use the server ID rather than a name or row position for selection identity.
            return old.stationId == new.stationId
        }
        override fun areContentsTheSame(old: MapStation, new: MapStation): Boolean {
            // Refresh presentation only when the returned station changes.
            return old == new
        }
    }) {
    private var nearestId: String? = null

    fun highlightNearest(stationId: String?) {
        // Rebind both old and new highlights, including when recycled cards return to all-stations mode.
        if (nearestId == stationId) return
        val previous = nearestId
        nearestId = stationId
        currentList.forEachIndexed { index, station ->
            if (station.stationId == previous || station.stationId == stationId) notifyItemChanged(index)
        }
    }

    class Holder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        // Inflate the shared station card for either mobile role.
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.m4_station_card, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        // Format authoritative values without computing availability or distance on the client.
        val station = getItem(position)
        val view = holder.view
        view.findViewById<ImageView>(R.id.stationPresentationImage)
            .setImageResource(StationImages.forStation(station.stationId))
        val context = view.context
        val unknown = context.getString(R.string.m4_unknown)
        val nearest = station.stationId == nearestId
        view.findViewById<TextView>(R.id.m4NearestBadge).isVisible = nearest
        (view as com.google.android.material.card.MaterialCardView).apply {
            strokeWidth = if (nearest) (2 * resources.displayMetrics.density).toInt() else 0
            strokeColor = ContextCompat.getColor(context, R.color.emerald_600)
        }
        view.findViewById<TextView>(R.id.m4StationName).text = station.stationName
        view.findViewById<TextView>(R.id.m4StationStatus).apply {
            text = context.getString(R.string.m4_status, station.status)
            setTextColor(ContextCompat.getColor(context,
                if (station.status.equals("Active", true)) R.color.emerald_600 else R.color.gray_500))
        }
        view.findViewById<TextView>(R.id.m4StationCapacity).text = context.getString(
            R.string.m4_capacity, station.capacityKwh?.toString() ?: unknown)
        view.findViewById<TextView>(R.id.m4StationSlots).text = context.getString(
            R.string.m4_slots, station.availableSlotCount?.toString() ?: unknown,
            station.batterySlotCount?.toString() ?: unknown)
        view.findViewById<TextView>(R.id.m4StationDistance).apply {
            isVisible = station.distanceKm != null || station.location == null
            text = if (station.distanceKm != null) context.getString(R.string.m4_distance, station.distanceKm)
                else context.getString(R.string.m4_coordinates_missing)
        }
        view.setOnClickListener { select(station) }
    }
}
