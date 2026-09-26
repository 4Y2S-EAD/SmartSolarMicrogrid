/* Member 4 | Nearby Stations | Show the selected API station without implementing booking actions. */
package com.smartsolar.microgrid.member4.maps

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import com.smartsolar.microgrid.member2.activities.StationImages
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member2.activities.M2StationDetailsActivity

class StationDetailsBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        // Restore the selected record from arguments when Android recreates the dialog.
        return inflater.inflate(R.layout.m4_station_details_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        // Display only API data; absent fields remain explicitly unavailable.
        val station = Gson().fromJson(requireArguments().getString("station"), MapStation::class.java)
        val unknown = getString(R.string.m4_unknown)
        view.findViewById<ImageView>(R.id.stationPresentationImage)
            .setImageResource(StationImages.forStation(station.stationId))
        view.findViewById<TextView>(R.id.m4DetailName).text = station.stationName
        view.findViewById<TextView>(R.id.m4DetailBody).text = listOfNotNull(
            getString(R.string.m4_status, station.status),
            getString(R.string.m4_capacity, station.capacityKwh?.toString() ?: unknown),
            getString(R.string.m4_slots, station.availableSlotCount?.toString() ?: unknown,
                station.batterySlotCount?.toString() ?: unknown),
            station.schedule?.takeIf { it.isNotBlank() } ?: getString(R.string.m4_schedule_missing),
            station.distanceKm?.let { getString(R.string.m4_distance, it) },
            if (station.location == null) getString(R.string.m4_coordinates_missing) else null
        ).joinToString("\n")
        view.findViewById<View>(R.id.m4DetailClose).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.m4DetailOpen).apply {
            // Retain the existing Member 2 screen without inventing required numeric extras.
            isEnabled = station.capacityKwh != null && station.availableSlotCount != null
            setOnClickListener {
                startActivity(Intent(requireContext(), M2StationDetailsActivity::class.java).apply {
                    putExtra("STATION_ID", station.stationId)
                    putExtra("STATION_NAME", station.stationName)
                    putExtra("CAPACITY", station.capacityKwh!!)
                    putExtra("AVAILABLE_SLOTS", station.availableSlotCount!!)
                })
                dismiss()
            }
        }
    }

    companion object {
        fun forStation(station: MapStation): StationDetailsBottomSheet {
            // Bundle the selected response so dialog restoration never depends on a global singleton.
            return StationDetailsBottomSheet().apply {
                arguments = Bundle().apply { putString("station", Gson().toJson(station)) }
            }
        }
    }
}
