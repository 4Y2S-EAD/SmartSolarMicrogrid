package com.smartsolar.microgrid.member3.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.ApiClient
import com.smartsolar.microgrid.network.TokenManager
import com.smartsolar.microgrid.network.models.PaginatedReservationsResponse
import com.smartsolar.microgrid.network.models.ReservationSummaryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class M3ReservationsActivity : AppCompatActivity() {

    enum class ReservationTab(val title: String) {
        APPROVED("Approved"),
        PENDING("Pending"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled")
    }

    private lateinit var tabLayoutStatus: TabLayout
    private lateinit var rvReservations: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var tvSubtitleStats: TextView

    // Pagination
    private lateinit var layoutPagination: View
    private lateinit var btnPrevPage: MaterialButton
    private lateinit var btnNextPage: MaterialButton
    private lateinit var tvPageIndicator: TextView

    private lateinit var adapter: M3ReservationsAdapter
    private var currentTab: ReservationTab = ReservationTab.APPROVED
    private var currentPage: Int = 1
    private val pageSize: Int = 10
    private var totalPages: Int = 1
    private var totalRecords: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m3_activity_reservations)

        initViews()
        setupTabs()
        setupRecyclerView()
        setupListeners()

        loadReservations()
    }

    private fun initViews() {
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus)
        rvReservations = findViewById(R.id.rvReservations)
        progressBar = findViewById(R.id.progressBar)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        tvSubtitleStats = findViewById(R.id.tvSubtitleStats)

        layoutPagination = findViewById(R.id.layoutPagination)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
    }

    private fun setupTabs() {
        tabLayoutStatus.removeAllTabs()
        for (tab in ReservationTab.values()) {
            tabLayoutStatus.addTab(tabLayoutStatus.newTab().setText(tab.title))
        }

        tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedTab = when (tab?.position) {
                    0 -> ReservationTab.APPROVED
                    1 -> ReservationTab.PENDING
                    2 -> ReservationTab.COMPLETED
                    3 -> ReservationTab.CANCELLED
                    else -> ReservationTab.APPROVED
                }
                if (selectedTab != currentTab) {
                    currentTab = selectedTab
                    currentPage = 1
                    loadReservations()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = M3ReservationsAdapter(emptyList()) { item ->
            val intent = android.content.Intent(this, M3ReservationDetailsActivity::class.java).apply {
                putExtra(M3ReservationDetailsActivity.EXTRA_RESERVATION_ID, item.reservationId)
            }
            startActivity(intent)
        }
        rvReservations.layoutManager = LinearLayoutManager(this)
        rvReservations.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBackReservations).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnRefresh).setOnClickListener {
            loadReservations()
        }

        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadReservations()
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                loadReservations()
            }
        }
    }

    private fun loadReservations() {
        val nic = TokenManager.getNic()
        if (nic.isNullOrBlank()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<PaginatedReservationsResponse> = when (currentTab) {
                    ReservationTab.APPROVED -> ApiClient.apiService.getApprovedReservations(nic, currentPage, pageSize)
                    ReservationTab.PENDING -> ApiClient.apiService.getPendingReservations(nic, currentPage, pageSize)
                    ReservationTab.COMPLETED -> ApiClient.apiService.getCompletedReservations(nic, currentPage, pageSize)
                    ReservationTab.CANCELLED -> ApiClient.apiService.getCancelledReservations(nic, currentPage, pageSize)
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        currentPage = body.currentPage
                        totalPages = if (body.totalPages > 0) body.totalPages else 1
                        totalRecords = body.totalRecords
                        renderData(body.items)
                    } else {
                        renderError("Failed to fetch reservations: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    renderError("Network error: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    private fun renderData(items: List<ReservationSummaryItem>) {
        if (items.isEmpty()) {
            rvReservations.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            layoutPagination.visibility = View.GONE

            tvEmptyTitle.text = "No ${currentTab.title} Reservations"
            tvEmptySubtitle.text = when (currentTab) {
                ReservationTab.APPROVED -> "You do not have any approved upcoming reservations."
                ReservationTab.PENDING -> "There are no pending reservation requests waiting for approval."
                ReservationTab.COMPLETED -> "No completed power transfer transactions recorded yet."
                ReservationTab.CANCELLED -> "No cancelled reservations on record."
            }
        } else {
            layoutEmptyState.visibility = View.GONE
            rvReservations.visibility = View.VISIBLE
            layoutPagination.visibility = View.VISIBLE

            adapter.updateData(items)

            // Update Pagination UI
            tvPageIndicator.text = "Page $currentPage of $totalPages ($totalRecords total)"
            btnPrevPage.isEnabled = (currentPage > 1)
            btnNextPage.isEnabled = (currentPage < totalPages)
        }

        tvSubtitleStats.text = "$totalRecords ${currentTab.title.lowercase()} record(s) found"
    }

    private fun renderError(message: String) {
        rvReservations.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        layoutPagination.visibility = View.GONE

        tvEmptyTitle.text = "Could Not Load Data"
        tvEmptySubtitle.text = message
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            rvReservations.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
        }
    }
}
