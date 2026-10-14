package com.smartsolar.microgrid.member2.activities

import com.smartsolar.microgrid.member4.maps.NearbyStationsViewModel
import com.smartsolar.microgrid.member4.maps.StationMapAdapter
import com.smartsolar.microgrid.member4.maps.StationMapState
import com.smartsolar.microgrid.member4.maps.StationDetailsBottomSheet
import com.smartsolar.microgrid.member4.maps.MapStation
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import kotlinx.coroutines.launch

class M2ViewAllStationsActivity : AppCompatActivity() {

    private val model: NearbyStationsViewModel by viewModels()
    private lateinit var adapter: StationMapAdapter
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m2_activity_all_stations)

        findViewById<View>(R.id.btnBackAllStations).setOnClickListener { finish() }

        adapter = StationMapAdapter { station ->
            StationDetailsBottomSheet.forStation(station).show(supportFragmentManager, "station-details")
        }

        findViewById<RecyclerView>(R.id.rvAllStations).apply {
            layoutManager = LinearLayoutManager(this@M2ViewAllStationsActivity)
            adapter = this@M2ViewAllStationsActivity.adapter
        }

        setupGridFilter()
        setupStatusFilter()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    render(state)
                }
            }
        }
    }

    private var allStations = listOf<MapStation>()
    private var currentGrid = "All Grids"
    private var currentStatus = "All Status"

    private fun setupGridFilter() {
        val btnGridFilter = findViewById<View>(R.id.btnGridFilter)
        val tvSelectedGrid = findViewById<TextView>(R.id.tvSelectedGrid)

        btnGridFilter.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            
            // Extract unique grids from DB (base station name)
            val uniqueGrids = allStations.map { extractGridName(it.stationName) }.distinct().sorted()
            val grids = listOf("All Grids") + uniqueGrids

            grids.forEachIndexed { index, grid ->
                popup.menu.add(0, index, 0, grid)
            }
            popup.setOnMenuItemClickListener { item ->
                currentGrid = grids[item.itemId]
                tvSelectedGrid.text = currentGrid
                applyLocalFilters()
                true
            }
            popup.show()
        }
    }

    private fun setupStatusFilter() {
        val btnSortFilter = findViewById<View>(R.id.btnSortFilter)
        val tvSelectedStatus = findViewById<TextView>(R.id.tvSelectedSort) // Reusing the TextView ID
        tvSelectedStatus.text = "All Status"

        btnSortFilter.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            
            // Extract unique statuses from DB
            val uniqueStatuses = allStations.map { it.status }.distinct().sorted()
            val statuses = listOf("All Status") + uniqueStatuses

            statuses.forEachIndexed { index, status ->
                popup.menu.add(0, index, 0, status)
            }
            popup.setOnMenuItemClickListener { item ->
                currentStatus = statuses[item.itemId]
                tvSelectedStatus.text = currentStatus
                applyLocalFilters()
                true
            }
            popup.show()
        }
    }

    private fun extractGridName(name: String): String {
        return name.replace(" Solar Hub", "", ignoreCase = true)
                   .replace(" Station", "", ignoreCase = true).trim()
    }

    private fun applyLocalFilters() {
        val filtered = allStations.filter { station ->
            val matchesGrid = currentGrid == "All Grids" || extractGridName(station.stationName) == currentGrid
            val matchesStatus = currentStatus == "All Status" || station.status == currentStatus
            matchesGrid && matchesStatus
        }
        
        val countText = "${filtered.size} stations available"
        findViewById<TextView>(R.id.tvStationsCountSubtitle)?.text = countText
        adapter.submitList(filtered)
    }

    private fun render(state: StationMapState) {
        findViewById<View>(R.id.pbAllStationsLoading).isVisible = state.loading
        
        val messageView = findViewById<TextView>(R.id.tvAllStationsMessage)
        if (state.error) {
            messageView.text = getString(R.string.m4_data_error)
            messageView.isVisible = true
        } else if (state.loaded && state.stations.isEmpty()) {
            messageView.text = getString(R.string.m4_no_matches)
            messageView.isVisible = true
        } else if (!state.loading) {
            messageView.isVisible = false
        }

        if (state.loaded && allStations.isEmpty()) {
            allStations = state.stations
            applyLocalFilters()
        } else if (state.loaded) {
            // Update allStations in case of data refresh
            allStations = state.stations
            applyLocalFilters()
        }
    }
}
