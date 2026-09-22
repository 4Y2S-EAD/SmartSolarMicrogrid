package com.smartsolar.microgrid.member2.activities

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.models.BookingSlot
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class M2TimeSlotsAdapter(
    private var slots: List<BookingSlot>,
    private val onSlotClick: (BookingSlot) -> Unit
) : RecyclerView.Adapter<M2TimeSlotsAdapter.SlotViewHolder>() {

    private var selectedPosition = -1

    class SlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardTimeSlot: MaterialCardView = view.findViewById(R.id.cardTimeSlot)
        val tvTimeRange: TextView = view.findViewById(R.id.tvTimeRange)
        val tvSlotStatus: TextView = view.findViewById(R.id.tvSlotStatus)
        val ivSlotIcon: ImageView = view.findViewById(R.id.ivSlotIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.m2_item_time_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]

        // Format ISO times to HH:mm
        holder.tvTimeRange.text = "${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}"

        val isSelected = position == selectedPosition
        
        if (slot.status.equals("Available", ignoreCase = true)) {
            holder.tvSlotStatus.text = "Available"
            
            if (isSelected) {
                // Selected state
                holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#059669")) // Dark green
                holder.tvTimeRange.setTextColor(Color.WHITE)
                holder.tvSlotStatus.setTextColor(Color.WHITE)
                holder.ivSlotIcon.setColorFilter(Color.WHITE)
            } else {
                // Default Available state
                holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#D1FAE5")) // Light green
                holder.tvTimeRange.setTextColor(Color.BLACK)
                holder.tvSlotStatus.setTextColor(Color.parseColor("#065F46"))
                holder.ivSlotIcon.setColorFilter(Color.parseColor("#059669"))
            }

            holder.itemView.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onSlotClick(slot)
            }
        } else {
            // Booked/Unavailable state
            holder.tvSlotStatus.text = "Booked"
            holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#FEE2E2")) // Light red
            holder.tvTimeRange.setTextColor(Color.parseColor("#991B1B"))
            holder.tvSlotStatus.setTextColor(Color.parseColor("#991B1B"))
            holder.ivSlotIcon.setColorFilter(Color.parseColor("#EF4444"))
            
            holder.itemView.setOnClickListener(null) // Disable click
        }
    }

    override fun getItemCount(): Int = slots.size

    fun updateData(newSlots: List<BookingSlot>) {
        slots = newSlots
        selectedPosition = -1 // Reset selection on data change
        notifyDataSetChanged()
    }

    private fun formatTime(isoString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(isoString)
            
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            date?.let { formatter.format(it) } ?: isoString
        } catch (e: Exception) {
            isoString
        }
    }
}
