package com.smartsolar.microgrid.member2.activities

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.models.Station

class M2StationsAdapter(
    private var stations: List<Station>,
    private val onStationClick: (Station) -> Unit
) : RecyclerView.Adapter<M2StationsAdapter.StationViewHolder>() {

    class StationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStationName: TextView = view.findViewById(R.id.tvStationName)
        val tvCapacity: TextView = view.findViewById(R.id.tvCapacity)
        val tvAvailableSlots: TextView = view.findViewById(R.id.tvAvailableSlots)

        val ivStationImage: ImageView = view.findViewById(R.id.ivStationImage)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.m2_item_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]

        holder.tvStationName.text = station.stationName
        holder.tvCapacity.text = "Distance: 2.4 km • ${station.capacityKwh} kW/h"

        // අර ඔයා දාන පින්තූර 4 මාරුවෙන් මාරුවට සෙට් කරන කෑල්ල
        val imageResId = when (position % 4) {
            0 -> R.drawable.station1
            1 -> R.drawable.station2
            2 -> R.drawable.station3
            else -> R.drawable.station4
        }
        holder.ivStationImage.setImageResource(imageResId)

        // Status Badge Logic
        if (station.availableSlotCount <= 0) {
            holder.tvStatusBadge.text = "Full"
            holder.tvStatusBadge.setTextColor(Color.parseColor("#991B1B")) // Dark Red
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2")) // Light Red
            holder.tvAvailableSlots.text = "No slots available"
        } else if (station.availableSlotCount <= 2) {
            holder.tvStatusBadge.text = "Limited"
            holder.tvStatusBadge.setTextColor(Color.parseColor("#92400E")) // Dark Orange
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7")) // Light Orange
            holder.tvAvailableSlots.text = "${station.availableSlotCount} battery slots"
        } else {
            holder.tvStatusBadge.text = "Available"
            holder.tvStatusBadge.setTextColor(Color.parseColor("#065F46")) // Dark Green
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#D1FAE5")) // Light Green
            holder.tvAvailableSlots.text = "${station.availableSlotCount} battery slots"
        }

        // Detect clicks on the station card
        holder.itemView.setOnClickListener {
            onStationClick(station)
        }
    }

    override fun getItemCount(): Int = stations.size

    fun updateData(newStations: List<Station>) {
        stations = newStations
        notifyDataSetChanged()
    }
}
