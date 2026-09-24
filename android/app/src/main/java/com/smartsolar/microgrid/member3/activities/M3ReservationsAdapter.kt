package com.smartsolar.microgrid.member3.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.models.ReservationSummaryItem
import java.text.SimpleDateFormat
import java.util.Locale

class M3ReservationsAdapter(
    private var reservations: List<ReservationSummaryItem>,
    private val onItemClick: ((ReservationSummaryItem) -> Unit)? = null
) : RecyclerView.Adapter<M3ReservationsAdapter.ReservationViewHolder>() {

    class ReservationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStationName: TextView = view.findViewById(R.id.tvStationName)
        val tvSlotNumber: TextView = view.findViewById(R.id.tvSlotNumber)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvBookingDate: TextView = view.findViewById(R.id.tvBookingDate)
        val tvTimeSlot: TextView = view.findViewById(R.id.tvTimeSlot)
        val tvCancellationReason: TextView = view.findViewById(R.id.tvCancellationReason)
        val tvReservationId: TextView = view.findViewById(R.id.tvReservationId)
        val btnQrCode: MaterialButton = view.findViewById(R.id.btnQrCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.m3_item_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        val item = reservations[position]

        holder.tvStationName.text = item.stationName
        holder.tvSlotNumber.text = "Slot No : ${item.slotNumber}"
        holder.tvBookingDate.text = formatDate(item.bookingDate)
        holder.tvTimeSlot.text = "${item.startTime} - ${item.endTime}"
        
        val shortId = if (item.reservationId.length > 8) {
            item.reservationId.takeLast(8).uppercase()
        } else {
            item.reservationId.uppercase()
        }
        holder.tvReservationId.text = "Ref: #$shortId"

        // Status Badge Styling
        val statusUpper = item.status.uppercase()
        holder.tvStatusBadge.text = item.status
        when {
            statusUpper.contains("APPROV") -> {
                holder.tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            }
            statusUpper.contains("PEND") -> {
                holder.tvStatusBadge.setTextColor(Color.parseColor("#B45309"))
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            }
            statusUpper.contains("COMPLET") -> {
                holder.tvStatusBadge.setTextColor(Color.parseColor("#4338CA"))
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF2FF"))
            }
            statusUpper.contains("CANCEL") -> {
                holder.tvStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            }
            else -> {
                holder.tvStatusBadge.setTextColor(Color.parseColor("#4B5563"))
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F3F4F6"))
            }
        }

        // Cancellation Reason
        if (!item.cancellationReason.isNullOrBlank()) {
            holder.tvCancellationReason.visibility = View.VISIBLE
            holder.tvCancellationReason.text = "Reason: ${item.cancellationReason}"
        } else {
            holder.tvCancellationReason.visibility = View.GONE
        }

        // QR Code Button
        if (!item.qrToken.isNullOrBlank() && statusUpper.contains("APPROV")) {
            holder.btnQrCode.visibility = View.VISIBLE
            holder.btnQrCode.setOnClickListener {
                showQrTokenDialog(holder.itemView.context, item)
            }
        } else {
            holder.btnQrCode.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = reservations.size

    fun updateData(newReservations: List<ReservationSummaryItem>) {
        this.reservations = newReservations
        notifyDataSetChanged()
    }

    private fun formatDate(rawDate: String): String {
        return try {
            if (rawDate.contains("T")) {
                rawDate.substringBefore("T")
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }

    private fun showQrTokenDialog(context: android.content.Context, item: ReservationSummaryItem) {
        AlertDialog.Builder(context)
            .setTitle("Transaction Token")
            .setMessage("Station: ${item.stationName}\nSlot: ${item.slotNumber}\nTime: ${item.startTime} - ${item.endTime}\n\nSecure QR Token:\n${item.qrToken}")
            .setPositiveButton("Close", null)
            .show()
    }
}
