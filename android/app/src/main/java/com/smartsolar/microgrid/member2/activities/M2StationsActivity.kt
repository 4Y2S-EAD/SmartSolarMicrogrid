package com.smartsolar.microgrid.member2.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class M2StationsActivity : AppCompatActivity() {

    private lateinit var rvStations: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: M2StationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m2_activity_stations)

        // මේ තියෙන්නේ ඔයාගේ Error එකට හේතුව වුන අලුත් කෑලි ටික
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        rvStations = findViewById(R.id.rvStations)
        progressBar = findViewById(R.id.progressBar)

        rvStations.layoutManager = LinearLayoutManager(this)

        adapter = M2StationsAdapter(emptyList()) { selectedStation ->
            val intent = android.content.Intent(this, M2StationDetailsActivity::class.java).apply {
                putExtra("STATION_ID", selectedStation.stationId)
                putExtra("STATION_NAME", selectedStation.stationName)
                putExtra("CAPACITY", selectedStation.capacityKwh)
                putExtra("AVAILABLE_SLOTS", selectedStation.availableSlotCount)
            }
            startActivity(intent)
        }
        rvStations.adapter = adapter

        fetchStations()
    }

    private fun fetchStations() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getStations()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        adapter.updateData(response.body()!!)
                    } else {
                        Toast.makeText(this@M2StationsActivity, "Failed to load", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@M2StationsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
