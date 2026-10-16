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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.m2_item_time_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]

        // Format ISO times to HH:mm and put on two lines
        holder.tvTimeRange.text = "${formatTime(slot.startTime)}\n- ${formatTime(slot.endTime)}"

        val isSelected = position == selectedPosition
        
        if (slot.status.equals("Available", ignoreCase = true)) {
            holder.tvSlotStatus.text = slot.status
            
            if (isSelected) {
                // Selected state
                holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#E6F4EA")) // Light green background
                holder.cardTimeSlot.strokeColor = Color.parseColor("#059669") // Green border
                holder.tvTimeRange.setTextColor(Color.parseColor("#111827"))
                holder.tvSlotStatus.setTextColor(Color.parseColor("#059669"))
            } else {
                // Default Available state
                holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#F9FAFB")) // Very light grey background
                holder.cardTimeSlot.strokeColor = Color.parseColor("#E5E7EB") // Light grey border
                holder.tvTimeRange.setTextColor(Color.parseColor("#111827"))
                holder.tvSlotStatus.setTextColor(Color.parseColor("#059669"))
            }

            holder.itemView.setOnClickListener {
                if (holder.adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                if (previousSelected >= 0) notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onSlotClick(slot)
            }
        } else {
            // Booked/Unavailable state
            holder.tvSlotStatus.text = "Booked" // Ensure it says Booked as per screenshot
            holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#F3F4F6")) // Light grey
            holder.cardTimeSlot.strokeColor = Color.parseColor("#E5E7EB") // Light grey border
            holder.tvTimeRange.setTextColor(Color.parseColor("#9CA3AF")) // Grey text
            holder.tvSlotStatus.setTextColor(Color.parseColor("#9CA3AF")) // Grey text
            
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
        // API clock-only values are already displayable; do not parse them as calendar dates.
        if (!isoString.contains('T')) return isoString
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.isLenient = false
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(isoString)
            
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            date?.let { formatter.format(it) } ?: isoString
        } catch (e: Exception) {
            isoString
        }
    }
}
