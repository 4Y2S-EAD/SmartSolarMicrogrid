/* Member 4 | Operator reservations | Native list, tabs, server search and approval presentation. */
package com.smartsolar.microgrid.member4.operator

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class OperatorReservationsActivity : AppCompatActivity() {
    private val model: OperatorReservationsViewModel by viewModels()
    private lateinit var adapter: OperatorReservationAdapter
    private var statuses: List<String> = emptyList()
    private var filtersExpanded = false
    private var renderedPage: OperatorReservationPage? = null
    private val fields = mapOf("reservationId" to R.id.m4FilterId, "prosumerNic" to R.id.m4FilterNic,
        "station" to R.id.m4FilterStation, "bookingDate" to R.id.m4FilterDate)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bind navigation and inputs once; the ViewModel survives screen recreation.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m4_operator_reservations)
        filtersExpanded = savedInstanceState?.getBoolean("filtersExpanded") ?: (intent.getStringExtra("view") == "search")
        if (savedInstanceState == null) model.select(intent.getStringExtra("view") ?: "all")
        adapter = OperatorReservationAdapter(model::approve)
        findViewById<RecyclerView>(R.id.m4ReservationList).apply { layoutManager = LinearLayoutManager(this@OperatorReservationsActivity); adapter = this@OperatorReservationsActivity.adapter }
        findViewById<View>(R.id.m4OperatorBack).setOnClickListener { finish() }
        findViewById<View>(R.id.m4OperatorRefresh).setOnClickListener { model.refresh() }
        mapOf(R.id.m4TabAll to "all", R.id.m4TabPending to "pending", R.id.m4TabApproved to "approved",
            R.id.m4TabCompleted to "completed", R.id.m4TabHistory to "history").forEach { (id, target) ->
            findViewById<View>(id).setOnClickListener { filtersExpanded = false; model.select(target) }
        }
        findViewById<View>(R.id.m4TabSearch).setOnClickListener {
            filtersExpanded = !filtersExpanded
            renderSearchPanel()
        }
        fields.forEach { (key, id) -> findViewById<EditText>(id).setText(model.state.value.filters[key].orEmpty()) }
        findViewById<View>(R.id.m4FilterSubmit).setOnClickListener {
            val values = fields.mapValues { (_, id) -> findViewById<EditText>(id).text.toString().trim() }.toMutableMap()
            values["status"] = statuses.getOrNull(findViewById<Spinner>(R.id.m4FilterStatus).selectedItemPosition - 1).orEmpty()
            filtersExpanded = false
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(findViewById<View>(R.id.m4FilterSubmit).windowToken, 0)
            model.search(values)
        }
        findViewById<View>(R.id.m4FilterClear).setOnClickListener {
            fields.values.forEach { findViewById<EditText>(it).setText("") }
            findViewById<Spinner>(R.id.m4FilterStatus).setSelection(0)
            model.search(emptyMap())
        }
        findViewById<EditText>(R.id.m4FilterDate).setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> findViewById<EditText>(R.id.m4FilterDate).setText(String.format(Locale.ROOT, "%04d-%02d-%02d", y, m + 1, d)) }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        findViewById<View>(R.id.m4PreviousPage).setOnClickListener { model.page(-1) }
        findViewById<View>(R.id.m4NextPage).setOnClickListener { model.page(1) }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { model.state.collect { render(it) } } }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        // Restore expanded filter controls without changing the submitted server query.
        outState.putBoolean("filtersExpanded", filtersExpanded)
        super.onSaveInstanceState(outState)
    }
    private fun renderSearchPanel() {
        // Keep search separate from status navigation, and leave room for results after submission.
        val panel = findViewById<View>(R.id.m4OperatorFilters)
        if (filtersExpanded && !panel.isVisible && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            panel.alpha = 0f
            panel.animate().alpha(1f).setDuration(180).start()
        }
        panel.isVisible = filtersExpanded
        findViewById<MaterialButton>(R.id.m4TabSearch).apply {
            setText(if (filtersExpanded) R.string.m4_close_search_filters
                else if (model.state.value.view == "search") R.string.m4_edit_search_filters
                else R.string.m4_search_bookings_filters)
            isSelected = filtersExpanded
        }
    }
    private fun render(state: OperatorReservationState) {
        // Display server results and metadata without local filtering, status transitions or counts.
        findViewById<View>(R.id.m4OperatorLoading).isVisible = state.loading
        renderSearchPanel()
        findViewById<TextView>(R.id.m4OperatorTitle).text = when(state.view) { "pending" -> "Pending reservations"; "approved" -> "Approved reservations"; "completed" -> "Completed reservations"; "history" -> "Booking history"; "search" -> "Search bookings"; else -> "All reservations" }
        if (statuses != state.statusOptions) {
            statuses = state.statusOptions
            findViewById<Spinner>(R.id.m4FilterStatus).apply {
                adapter = ArrayAdapter(this@OperatorReservationsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("All statuses") + statuses)
                setSelection((statuses.indexOf(state.filters["status"]) + 1).coerceAtLeast(0))
            }
        }
        findViewById<TextView>(R.id.m4OperatorMessage).apply {
            text = state.error ?: if (state.loading) "Loading reservations..." else if (state.data?.items.isNullOrEmpty()) "No reservations found. Refresh or adjust your filters." else ""
            isVisible = text.isNotEmpty()
        }
        findViewById<TextView>(R.id.m4OperatorFeedback).apply { text = state.feedback; isVisible = state.feedback != null }
        adapter.setApproving(state.approvingId)
        adapter.submitList(state.data?.items ?: emptyList())
        if (state.data != null && renderedPage !== state.data && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            findViewById<RecyclerView>(R.id.m4ReservationList).apply { alpha = 0f; animate().alpha(1f).setDuration(180).start() }
        }
        renderedPage = state.data
        findViewById<TextView>(R.id.m4PageInfo).text = state.data?.let { "${it.totalRecords} results ? Page ${it.currentPage} / ${maxOf(1, it.totalPages)}" }.orEmpty()
        findViewById<MaterialButton>(R.id.m4PreviousPage).isEnabled = !state.loading && state.data != null && state.page > 1
        findViewById<MaterialButton>(R.id.m4NextPage).isEnabled = !state.loading && state.page < (state.data?.totalPages ?: 0)
    }
}
