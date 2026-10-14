package com.smartsolar.microgrid.member2.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member3.activities.M3CreateReservationActivity
import com.smartsolar.microgrid.network.ApiClient
import kotlinx.coroutines.launch

class M2ViewAllSlotsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m2_activity_all_slots)

        findViewById<ImageView>(R.id.btnBackAllSlots).setOnClickListener { finish() }

        val stationId = intent.getStringExtra(M3CreateReservationActivity.EXTRA_STATION_ID)
        if (stationId.isNullOrBlank()) {
            Toast.makeText(this, "Station unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val rvAllSlotsGrid = findViewById<RecyclerView>(R.id.rvAllSlotsGrid)
        rvAllSlotsGrid.layoutManager = GridLayoutManager(this, 2)

        val adapter = M2SlotGridAdapter(emptyList()) { slot ->
            startActivity(Intent(this, M3CreateReservationActivity::class.java).apply {
                putExtra(M3CreateReservationActivity.EXTRA_STATION_ID, stationId)
                putExtra(M3CreateReservationActivity.EXTRA_SLOT_ID, slot.slotId)
            })
        }
        rvAllSlotsGrid.adapter = adapter

        val loading = findViewById<ProgressBar>(R.id.pbAllSlotsLoading)
        loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val slotResponse = ApiClient.apiService.getStationSlots(stationId)
                if (slotResponse.isSuccessful) {
                    adapter.updateData(slotResponse.body().orEmpty())
                } else {
                    Toast.makeText(this@M2ViewAllSlotsActivity, "Failed to load slots", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@M2ViewAllSlotsActivity, "Could not load data", Toast.LENGTH_LONG).show()
            } finally {
                loading.visibility = View.GONE
            }
        }
    }
}
