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

class M2SlotGridAdapter(
    private var slots: List<BookingSlot>,
    private val onClick: (BookingSlot) -> Unit
) : RecyclerView.Adapter<M2SlotGridAdapter.SlotGridViewHolder>() {

    fun updateData(newSlots: List<BookingSlot>) {
        slots = newSlots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotGridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.m2_item_slot_grid, parent, false)
        return SlotGridViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotGridViewHolder, position: Int) {
        val slot = slots[position]
        holder.bind(slot, onClick)
    }

    override fun getItemCount() = slots.size

    class SlotGridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<MaterialCardView>(R.id.cardSlotGrid)
        private val cardIcon = view.findViewById<MaterialCardView>(R.id.cardIconGrid)
        private val tvTime = view.findViewById<TextView>(R.id.tvSlotTimeGrid)
        private val tvStatus = view.findViewById<TextView>(R.id.tvSlotStatusGrid)
        private val tvCount = view.findViewById<TextView>(R.id.tvSlotCountGrid)
        private val ivArrow = view.findViewById<ImageView>(R.id.ivArrowGrid)

        fun bind(slot: BookingSlot, onClick: (BookingSlot) -> Unit) {
            tvTime.text = "${formatTime(slot.startTime)}\n- ${formatTime(slot.endTime)}"
            tvStatus.text = slot.status
            
            val isAvailable = slot.status.equals("Available", ignoreCase = true)
            
            if (isAvailable) {
                // Available styles (Green theme)
                card.setCardBackgroundColor(Color.parseColor("#F4FBF7"))
                card.strokeColor = Color.parseColor("#A7F3D0")
                cardIcon.setCardBackgroundColor(Color.parseColor("#059669"))
                tvTime.setTextColor(Color.parseColor("#111827"))
                tvStatus.setTextColor(Color.parseColor("#059669"))
                tvCount.setTextColor(Color.parseColor("#6B7280"))
                tvCount.text = "${slot.capacityKwh} kWh slots" // Placeholder count
                ivArrow.visibility = View.VISIBLE
                ivArrow.setColorFilter(Color.parseColor("#059669"))
                
                card.setOnClickListener { onClick(slot) }
                card.isClickable = true
            } else {
                // Booked styles (Grey theme)
                card.setCardBackgroundColor(Color.parseColor("#F3F4F6"))
                card.strokeColor = Color.parseColor("#E5E7EB")
                cardIcon.setCardBackgroundColor(Color.parseColor("#9CA3AF"))
                tvTime.setTextColor(Color.parseColor("#6B7280"))
                tvStatus.setTextColor(Color.parseColor("#9CA3AF"))
                tvCount.setTextColor(Color.parseColor("#9CA3AF"))
                tvCount.text = "0 slots"
                ivArrow.visibility = View.INVISIBLE
                
                card.setOnClickListener(null)
                card.isClickable = false
            }
        }

        private fun formatTime(isoString: String): String {
            try {
                val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = parser.parse(isoString) ?: return isoString
                val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                formatter.timeZone = java.util.TimeZone.getDefault()
                return formatter.format(date)
            } catch (e: Exception) {
                try {
                    val parser2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    parser2.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val date2 = parser2.parse(isoString) ?: return isoString
                    val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                    formatter.timeZone = java.util.TimeZone.getDefault()
                    return formatter.format(date2)
                } catch (e2: Exception) {
                    return isoString
                }
            }
        }
    }
}
