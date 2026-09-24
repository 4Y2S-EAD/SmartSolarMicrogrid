/* Member 4 | Operator dashboard | Backend metrics and reservation navigation around the existing map entry. */
package com.smartsolar.microgrid.member4.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartsolar.microgrid.R
import kotlinx.coroutines.launch

class OperatorSummaryFragment : Fragment(R.layout.m4_operator_summary) {
    private val model: OperatorReservationsViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Keep new reservation UI separate from the existing dashboard's map and account actions.
        mapOf(R.id.m4OpenAll to "all", R.id.m4OpenPending to "pending", R.id.m4OpenHistory to "history", R.id.m4OpenSearch to "search", R.id.m4OpenCompleted to "completed").forEach { (id, target) ->
            view.findViewById<View>(id).setOnClickListener { startActivity(Intent(requireContext(), OperatorReservationsActivity::class.java).putExtra("view", target)) }
        }
        view.findViewById<View>(R.id.m4SummaryRefresh).setOnClickListener { model.refresh() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    val summary = state.data?.summary
                    listOf(R.id.m4ActiveCount to summary?.activeCount, R.id.m4PendingCount to summary?.pendingCount,
                        R.id.m4ApprovedCount to summary?.approvedCount, R.id.m4CompletedCount to summary?.completedCount).forEach { (id, value) ->
                        view.findViewById<TextView>(id).text = value?.toString() ?: "--"
                    }
                    view.findViewById<TextView>(R.id.m4SummaryMessage).apply {
                        text = state.error ?: if (state.loading) "Loading reservation metrics..." else "Active bookings: Pending + Approved"
                    }
                    view.findViewById<View>(R.id.m4SummaryLoading).isVisible = state.loading
                }
            }
        }
    }
    override fun onResume() {
        // Returning from approval must show refreshed backend metrics.
        super.onResume()
        model.refresh()
    }
}
