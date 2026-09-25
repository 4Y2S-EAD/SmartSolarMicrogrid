/* Member 4 | Operator reservations | Render API data and server-provided approval availability. */
package com.smartsolar.microgrid.member4.operator

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R

class OperatorReservationAdapter(private val approve: (String) -> Unit) : ListAdapter<OperatorReservation, OperatorReservationAdapter.Holder>(object : DiffUtil.ItemCallback<OperatorReservation>() {
    override fun areItemsTheSame(a: OperatorReservation, b: OperatorReservation) = a.reservationId == b.reservationId
    override fun areContentsTheSame(a: OperatorReservation, b: OperatorReservation) = a == b
}) {
    private var approving: String? = null
    class Holder(val root: View) : RecyclerView.ViewHolder(root)
    fun setApproving(id: String?) {
        // Rebind visible action buttons whenever the shared approval request changes.
        if (approving != id) { approving = id; notifyItemRangeChanged(0, itemCount) }
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        // Inflate the member-owned card using the existing Material design theme.
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.m4_operator_reservation_card, parent, false))
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        // Status colors are presentation only; canApprove and every displayed value come from the API.
        val row = getItem(position)
        with(holder.root) {
            findViewById<TextView>(R.id.m4ReservationStation).text = row.stationName ?: "Station unavailable"
            findViewById<TextView>(R.id.m4ReservationId).text = "Booking ID: ${row.reservationId}"
            findViewById<TextView>(R.id.m4ReservationInfo).text = listOfNotNull(
                "Prosumer NIC: ${row.prosumerNic}", "${row.bookingDate.take(10)} | ${row.startTime} - ${row.endTime}",
                row.slotNumber?.let { "Battery slot $it" } ?: "Slot unavailable", row.cancellationReason
            ).joinToString("\n")
            findViewById<TextView>(R.id.m4ReservationStatus).apply {
                text = row.status
                val statusColor = Color.parseColor(when(row.status) { "Pending" -> "#92400E"; "Approved" -> "#1D4ED8"; "Completed" -> "#047857"; "Cancelled" -> "#BE123C"; else -> "#4B5563" })
                setTextColor(statusColor)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 24 * resources.displayMetrics.density
                    setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(statusColor, 20))
                }
                val padding = (8 * resources.displayMetrics.density).toInt()
                setPadding(padding, padding / 2, padding, padding / 2)
            }
            findViewById<MaterialButton>(R.id.m4ReservationApprove).apply {
                isVisible = row.canApprove
                isEnabled = approving == null
                text = if (approving == row.reservationId) "Approving..." else "Approve"
                setOnClickListener { approve(row.reservationId) }
            }
        }
    }
}
