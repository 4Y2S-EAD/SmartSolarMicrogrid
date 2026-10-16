package com.smartsolar.microgrid.member2.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member3.activities.M3CreateReservationActivity
import com.smartsolar.microgrid.network.ApiClient
import com.smartsolar.microgrid.network.models.BookingSlot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class M2StationDetailsActivity : AppCompatActivity() {
    private var selectedSlot: BookingSlot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m2_activity_station_details)
        findViewById<ImageView>(R.id.btnBackDetails).setOnClickListener { finish() }
        val book = findViewById<View>(R.id.btnBookStation)
        book.isEnabled = false
        val role = com.smartsolar.microgrid.network.TokenManager.getRole()
        if (role == "GridOperator") {
            book.visibility = View.GONE
        }
        val stationId = intent.getStringExtra(M3CreateReservationActivity.EXTRA_STATION_ID)
        if (stationId.isNullOrBlank()) {
            findViewById<TextView>(R.id.tvDetailStationName).text = "Station unavailable"
            return
        }

        val slots = findViewById<RecyclerView>(R.id.rvTimeSlots)
        val adapter = M2TimeSlotsAdapter(emptyList()) { selectedSlot = it }
        slots.adapter = adapter
        
        findViewById<View>(R.id.btnViewAllSlots)?.setOnClickListener {
            startActivity(Intent(this, M2ViewAllSlotsActivity::class.java).apply {
                putExtra(M3CreateReservationActivity.EXTRA_STATION_ID, stationId)
            })
        }
        val loading = findViewById<ProgressBar>(R.id.pbSlotsLoading)
        loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Refresh the selected station rather than displaying stale navigation extras.
                val response = ApiClient.apiService.getStationById(stationId)
                val station = response.body()?.takeIf { response.isSuccessful }
                if (station == null) {
                    findViewById<TextView>(R.id.tvDetailStationName).text = "Station unavailable"
                    Toast.makeText(this@M2StationDetailsActivity, "Could not load station details", Toast.LENGTH_LONG).show()
                    return@launch
                }
                findViewById<TextView>(R.id.tvDetailStationName).text = station.stationName
                findViewById<ImageView>(R.id.stationPresentationImage)
                    .setImageResource(StationImages.forStation(station.stationId))
                findViewById<TextView>(R.id.tvDetailStatusBadge).text = station.status
                findViewById<TextView>(R.id.tvDetailCapacity).text = "${station.capacityKwh} kWh"
                findViewById<TextView>(R.id.tvDetailSlots).text = "${station.availableSlotCount} / ${station.batterySlotCount}"
                val location = station.location
                if (location?.latitude != null && location.longitude != null) {
                    findViewById<TextView>(R.id.tvDetailLocation).text = "${location.latitude}, ${location.longitude}"
                    val distance = calculateDistance(6.9271, 79.8612, location.latitude, location.longitude)
                    findViewById<TextView>(R.id.tvDetailDistance).text = String.format("%.1f km", distance)
                } else {
                    findViewById<TextView>(R.id.tvDetailLocation).text = "Location unavailable"
                    findViewById<TextView>(R.id.tvDetailDistance).text = "--"
                }
                book.isEnabled = true
                book.setOnClickListener {
                    // Reuse the existing form and submission flow with real API identifiers.
                    startActivity(Intent(this@M2StationDetailsActivity, M3CreateReservationActivity::class.java).apply {
                        putExtra(M3CreateReservationActivity.EXTRA_STATION_ID, station.stationId)
                        selectedSlot?.let { putExtra(M3CreateReservationActivity.EXTRA_SLOT_ID, it.slotId) }
                    })
                }
                val slotResponse = ApiClient.apiService.getStationSlots(station.stationId)
                if (slotResponse.isSuccessful) adapter.updateData(slotResponse.body().orEmpty())
                else Toast.makeText(this@M2StationDetailsActivity, "Failed to load slots", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@M2StationDetailsActivity, "Could not load station data. Please try again.", Toast.LENGTH_LONG).show()
            } finally {
                loading.visibility = View.GONE
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
