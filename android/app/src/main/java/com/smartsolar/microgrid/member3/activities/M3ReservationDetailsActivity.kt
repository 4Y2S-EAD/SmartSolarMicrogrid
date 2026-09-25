package com.smartsolar.microgrid.member3.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member3.utils.QrCodeHelper
import com.smartsolar.microgrid.network.ApiClient
import com.smartsolar.microgrid.network.models.ReservationSummaryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class M3ReservationDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVATION_ID = "EXTRA_RESERVATION_ID"
    }

    private var reservationId: String = ""
    private var currentReservation: ReservationSummaryItem? = null

    // Views
    private lateinit var progressBarDetails: ProgressBar
    private lateinit var layoutErrorDetails: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetryDetails: MaterialButton
    private lateinit var scrollDetails: NestedScrollView

    // Header Card
    private lateinit var tvDetailStationName: TextView
    private lateinit var tvDetailSlotNumber: TextView
    private lateinit var tvDetailStatusBadge: TextView
    private lateinit var tvDetailReservationId: TextView

    // Booking Info Card
    private lateinit var tvDetailBookingDate: TextView
    private lateinit var tvDetailTimeSlot: TextView
    private lateinit var tvDetailProsumerNic: TextView
    private lateinit var tvDetailStationId: TextView

    // Cancellation Card
    private lateinit var cardCancellationDetails: MaterialCardView
    private lateinit var tvDetailCancellationReason: TextView

    // QR Card
    private lateinit var cardQrDetails: MaterialCardView
    private lateinit var ivDetailQrImage: ImageView
    private lateinit var btnEnlargeQr: MaterialButton

    // Timeline Card
    private lateinit var tvDetailCreatedAt: TextView
    private lateinit var tvDetailVerifiedAt: TextView
    private lateinit var tvDetailCompletedAt: TextView
    private lateinit var tvDetailUpdatedAt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m3_activity_reservation_details)

        reservationId = intent.getStringExtra(EXTRA_RESERVATION_ID) ?: ""
        if (reservationId.isBlank()) {
            Toast.makeText(this, "Invalid reservation ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        fetchReservationDetails()
    }

    private fun initViews() {
        progressBarDetails = findViewById(R.id.progressBarDetails)
        layoutErrorDetails = findViewById(R.id.layoutErrorDetails)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetryDetails = findViewById(R.id.btnRetryDetails)
        scrollDetails = findViewById(R.id.scrollDetails)

        tvDetailStationName = findViewById(R.id.tvDetailStationName)
        tvDetailSlotNumber = findViewById(R.id.tvDetailSlotNumber)
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge)
        tvDetailReservationId = findViewById(R.id.tvDetailReservationId)

        tvDetailBookingDate = findViewById(R.id.tvDetailBookingDate)
        tvDetailTimeSlot = findViewById(R.id.tvDetailTimeSlot)
        tvDetailProsumerNic = findViewById(R.id.tvDetailProsumerNic)
        tvDetailStationId = findViewById(R.id.tvDetailStationId)

        cardCancellationDetails = findViewById(R.id.cardCancellationDetails)
        tvDetailCancellationReason = findViewById(R.id.tvDetailCancellationReason)

        cardQrDetails = findViewById(R.id.cardQrDetails)
        ivDetailQrImage = findViewById(R.id.ivDetailQrImage)
        btnEnlargeQr = findViewById(R.id.btnEnlargeQr)

        tvDetailCreatedAt = findViewById(R.id.tvDetailCreatedAt)
        tvDetailVerifiedAt = findViewById(R.id.tvDetailVerifiedAt)
        tvDetailCompletedAt = findViewById(R.id.tvDetailCompletedAt)
        tvDetailUpdatedAt = findViewById(R.id.tvDetailUpdatedAt)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBackDetails).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnRefreshDetails).setOnClickListener {
            fetchReservationDetails()
        }

        btnRetryDetails.setOnClickListener {
            fetchReservationDetails()
        }

        btnEnlargeQr.setOnClickListener {
            currentReservation?.let { item ->
                showQrModal(item)
            }
        }
    }

    private fun fetchReservationDetails() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getReservationById(reservationId)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        currentReservation = response.body()
                        renderDetails(response.body()!!)
                    } else {
                        showError("Failed to fetch reservation details (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Network error: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    private fun renderDetails(item: ReservationSummaryItem) {
        layoutErrorDetails.visibility = View.GONE
        scrollDetails.visibility = View.VISIBLE

        // Header info
        tvDetailStationName.text = item.stationName
        tvDetailSlotNumber.text = "Slot No. ${item.slotNumber}"
        tvDetailReservationId.text = "Reservation Reference: #${item.reservationId}"

        // Status badge styling
        val statusUpper = item.status.uppercase()
        tvDetailStatusBadge.text = item.status
        when {
            statusUpper.contains("APPROV") -> {
                tvDetailStatusBadge.setTextColor(Color.parseColor("#15803D"))
                tvDetailStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            }
            statusUpper.contains("PEND") -> {
                tvDetailStatusBadge.setTextColor(Color.parseColor("#B45309"))
                tvDetailStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            }
            statusUpper.contains("COMPLET") -> {
                tvDetailStatusBadge.setTextColor(Color.parseColor("#4338CA"))
                tvDetailStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF2FF"))
            }
            statusUpper.contains("CANCEL") -> {
                tvDetailStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
                tvDetailStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            }
            else -> {
                tvDetailStatusBadge.setTextColor(Color.parseColor("#4B5563"))
                tvDetailStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F3F4F6"))
            }
        }

        // Booking Information
        tvDetailBookingDate.text = formatTimestamp(item.bookingDate)
        tvDetailTimeSlot.text = "${item.startTime} - ${item.endTime}"
        tvDetailProsumerNic.text = item.prosumerNic
        tvDetailStationId.text = item.stationId

        // Cancellation Alert
        if (statusUpper.contains("CANCEL") || !item.cancellationReason.isNullOrBlank()) {
            cardCancellationDetails.visibility = View.VISIBLE
            tvDetailCancellationReason.text = item.cancellationReason ?: "No cancellation reason provided."
        } else {
            cardCancellationDetails.visibility = View.GONE
        }

        // QR Code Card
        if (!item.qrToken.isNullOrBlank() && statusUpper.contains("APPROV")) {
            cardQrDetails.visibility = View.VISIBLE
            val qrBitmap = QrCodeHelper.generateQrCodeBitmap(item.qrToken, 500, 500)
            if (qrBitmap != null) {
                ivDetailQrImage.setImageBitmap(qrBitmap)
            }
        } else {
            cardQrDetails.visibility = View.GONE
        }

        // Timeline & Audit
        tvDetailCreatedAt.text = formatTimestamp(item.createdAt)
        tvDetailVerifiedAt.text = if (!item.verifiedAt.isNullOrBlank()) formatTimestamp(item.verifiedAt) else "Pending operator verification"
        tvDetailCompletedAt.text = if (!item.completedAt.isNullOrBlank()) formatTimestamp(item.completedAt) else "Not completed"
        tvDetailUpdatedAt.text = if (!item.updatedAt.isNullOrBlank()) formatTimestamp(item.updatedAt) else "—"
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBarDetails.visibility = View.VISIBLE
            layoutErrorDetails.visibility = View.GONE
            scrollDetails.visibility = View.GONE
        } else {
            progressBarDetails.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        progressBarDetails.visibility = View.GONE
        scrollDetails.visibility = View.GONE
        layoutErrorDetails.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }

    private fun formatTimestamp(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "—"
        return try {
            if (rawDate.contains("T")) {
                val parts = rawDate.split("T")
                val datePart = parts[0]
                val timePart = parts[1].substringBefore(".")
                "$datePart $timePart"
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }

    private fun showQrModal(item: ReservationSummaryItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.m3_dialog_qr_code, null)
        val tvStationName = dialogView.findViewById<TextView>(R.id.tvDialogStationName)
        val tvSlotAndTime = dialogView.findViewById<TextView>(R.id.tvDialogSlotAndTime)
        val ivQrCode = dialogView.findViewById<ImageView>(R.id.ivDialogQrCode)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnDialogClose)

        tvStationName.text = item.stationName
        tvSlotAndTime.text = "Slot ${item.slotNumber} • ${item.startTime} - ${item.endTime}"

        val qrBitmap = QrCodeHelper.generateQrCodeBitmap(item.qrToken ?: "", 600, 600)
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
