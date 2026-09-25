package com.smartsolar.microgrid.member2.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.ApiClient
import com.smartsolar.microgrid.network.models.BookingSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class M2StationDetailsActivity : AppCompatActivity() {

    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var pbSlotsLoading: ProgressBar
    private lateinit var adapter: M2TimeSlotsAdapter
    private var selectedSlot: BookingSlot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m2_activity_station_details)

        // Get passed data from M2StationsActivity
        val stationId = intent.getStringExtra("STATION_ID") ?: return
        val stationName = intent.getStringExtra("STATION_NAME") ?: "Unknown Station"
        val capacity = intent.getDoubleExtra("CAPACITY", 0.0)
        val availableSlots = intent.getIntExtra("AVAILABLE_SLOTS", 0)

        // Bind UI Elements
        val tvDetailStationName: TextView = findViewById(R.id.tvDetailStationName)
        val tvDetailStatusBadge: TextView = findViewById(R.id.tvDetailStatusBadge)
        val tvDetailCapacity: TextView = findViewById(R.id.tvDetailCapacity)
        val tvDetailSlots: TextView = findViewById(R.id.tvDetailSlots)
        val btnBackDetails: ImageView = findViewById(R.id.btnBackDetails)
        val btnBookStation: Button = findViewById(R.id.btnBookStation)

        rvTimeSlots = findViewById(R.id.rvTimeSlots)
        pbSlotsLoading = findViewById(R.id.pbSlotsLoading)

        // Set UI values
        tvDetailStationName.text = stationName
        tvDetailCapacity.text = "$capacity kW/h"
        tvDetailSlots.text = availableSlots.toString()

        if (availableSlots > 0) {
            tvDetailStatusBadge.text = "Available"
        } else {
            tvDetailStatusBadge.text = "Full"
        }

        // Setup RecyclerView for Grid layout (2 columns)
        rvTimeSlots.layoutManager = GridLayoutManager(this, 2)
        adapter = M2TimeSlotsAdapter(emptyList()) { slot ->
            selectedSlot = slot
            btnBookStation.isEnabled = true
        }
        rvTimeSlots.adapter = adapter
        
        btnBookStation.isEnabled = false // Disable until a slot is selected

        // Back button logic
        btnBackDetails.setOnClickListener {
            finish()
        }

        // Book button logic
        btnBookStation.setOnClickListener {
            selectedSlot?.let {
                Toast.makeText(this, "Booked Slot: ${it.startTime}", Toast.LENGTH_LONG).show()
                // TODO: Add actual booking API call here
            } ?: run {
                Toast.makeText(this, "Please select a time slot first", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch slots from API
        fetchStationSlots(stationId)
    }

    private fun fetchStationSlots(stationId: String) {
        pbSlotsLoading.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getStationSlots(stationId)
                
                withContext(Dispatchers.Main) {
                    pbSlotsLoading.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        adapter.updateData(response.body()!!)
                    } else {
                        Toast.makeText(this@M2StationDetailsActivity, "Failed to load slots", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbSlotsLoading.visibility = View.GONE
                    Toast.makeText(this@M2StationDetailsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
