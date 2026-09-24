/* Member 4 | Operator dashboard | Backend reservation metrics only (no navigation duplication). */
package com.smartsolar.microgrid.member4.operator

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
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
        // Refresh button
        view.findViewById<View>(R.id.m4SummaryRefresh).setOnClickListener {
            model.refresh()
            animateMetricCards(view)
        }

        // Staggered entrance animation on first load
        animateMetricCards(view)

        // Observe reservation metrics from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    val summary = state.data?.summary

                    // Update metric values with scale animation
                    listOf(
                        R.id.m4ActiveCount to summary?.activeCount,
                        R.id.m4PendingCount to summary?.pendingCount,
                        R.id.m4ApprovedCount to summary?.approvedCount,
                        R.id.m4CompletedCount to summary?.completedCount
                    ).forEach { (id, value) ->
                        val tv = view.findViewById<TextView>(id)
                        val newText = value?.toString() ?: "--"
                        if (tv.text.toString() != newText) {
                            tv.text = newText
                            val scaleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.m4_scale_fade_in)
                            tv.startAnimation(scaleAnim)
                        }
                    }

                    // Status message
                    view.findViewById<TextView>(R.id.m4SummaryMessage).apply {
                        text = state.error
                            ?: if (state.loading) "Loading reservation metrics…"
                            else "Active bookings include Pending + Approved"
                    }

                    // Loading indicator
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

    /** Applies staggered fade-slide-up animation to each metric card. */
    private fun animateMetricCards(view: View) {
        val cardIds = listOf(R.id.m4CardActive, R.id.m4CardPending, R.id.m4CardApproved, R.id.m4CardCompleted)
        cardIds.forEachIndexed { index, id ->
            val card = view.findViewById<View>(id) ?: return@forEachIndexed
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.m4_fade_slide_up)
            anim.startOffset = (index * 120).toLong()  // stagger each card by 120ms
            card.startAnimation(anim)
        }
    }
}
