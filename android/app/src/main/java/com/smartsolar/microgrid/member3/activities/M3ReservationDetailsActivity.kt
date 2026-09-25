package com.smartsolar.microgrid.member3.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private var stationLatitude: Double? = null
    private var stationLongitude: Double? = null
    private var stationDisplayName: String = ""

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

    // Booking Info Card
    private lateinit var tvDetailBookingDate: TextView
    private lateinit var tvDetailTimeSlot: TextView
    private lateinit var tvDetailBookingStationName: TextView
    private lateinit var tvDetailBookingSlotNumber: TextView
    private lateinit var layoutSlotCapacity: LinearLayout
    private lateinit var tvDetailBookingSlotCapacity: TextView

    // Cancellation Card
    private lateinit var cardCancellationDetails: MaterialCardView
    private lateinit var tvDetailCancellationReason: TextView

    // QR Card
    private lateinit var cardQrDetails: MaterialCardView
    private lateinit var ivDetailQrImage: ImageView
    private lateinit var btnEnlargeQr: MaterialButton

    // Station Information & Map Card
    private lateinit var cardStationDetails: MaterialCardView
    private lateinit var tvStationInfoTitle: TextView
    private lateinit var tvDetailStationStatusBadge: TextView
    private lateinit var tvDetailStationCapacity: TextView
    private lateinit var tvDetailTotalSlots: TextView
    private lateinit var tvDetailAvailableSlots: TextView
    private lateinit var layoutStationMapContainer: LinearLayout
    private lateinit var tvDetailCoordinates: TextView
    private lateinit var wvStationMap: WebView
    private lateinit var btnOpenGoogleMaps: MaterialButton

    // Timeline Card
    private lateinit var layoutTimelineCreated: LinearLayout
    private lateinit var tvDetailCreatedAt: TextView
    private lateinit var layoutTimelineVerified: LinearLayout
    private lateinit var tvDetailVerifiedAt: TextView
    private lateinit var layoutTimelineVerifiedBy: LinearLayout
    private lateinit var tvDetailVerifiedBy: TextView
    private lateinit var layoutTimelineCompleted: LinearLayout
    private lateinit var tvDetailCompletedAt: TextView
    private lateinit var layoutTimelineUpdated: LinearLayout
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

        // Header Card
        tvDetailStationName = findViewById(R.id.tvDetailStationName)
        tvDetailSlotNumber = findViewById(R.id.tvDetailSlotNumber)
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge)

        // Booking Info Card
        tvDetailBookingDate = findViewById(R.id.tvDetailBookingDate)
        tvDetailTimeSlot = findViewById(R.id.tvDetailTimeSlot)
        tvDetailBookingStationName = findViewById(R.id.tvDetailBookingStationName)
        tvDetailBookingSlotNumber = findViewById(R.id.tvDetailBookingSlotNumber)
        layoutSlotCapacity = findViewById(R.id.layoutSlotCapacity)
        tvDetailBookingSlotCapacity = findViewById(R.id.tvDetailBookingSlotCapacity)

        // Cancellation Card
        cardCancellationDetails = findViewById(R.id.cardCancellationDetails)
        tvDetailCancellationReason = findViewById(R.id.tvDetailCancellationReason)

        // QR Card
        cardQrDetails = findViewById(R.id.cardQrDetails)
        ivDetailQrImage = findViewById(R.id.ivDetailQrImage)
        btnEnlargeQr = findViewById(R.id.btnEnlargeQr)

        // Station Information & Map Card
        cardStationDetails = findViewById(R.id.cardStationDetails)
        tvStationInfoTitle = findViewById(R.id.tvStationInfoTitle)
        tvDetailStationStatusBadge = findViewById(R.id.tvDetailStationStatusBadge)
        tvDetailStationCapacity = findViewById(R.id.tvDetailStationCapacity)
        tvDetailTotalSlots = findViewById(R.id.tvDetailTotalSlots)
        tvDetailAvailableSlots = findViewById(R.id.tvDetailAvailableSlots)
        layoutStationMapContainer = findViewById(R.id.layoutStationMapContainer)
        tvDetailCoordinates = findViewById(R.id.tvDetailCoordinates)
        wvStationMap = findViewById(R.id.wvStationMap)
        btnOpenGoogleMaps = findViewById(R.id.btnOpenGoogleMaps)

        // Timeline Card
        layoutTimelineCreated = findViewById(R.id.layoutTimelineCreated)
        tvDetailCreatedAt = findViewById(R.id.tvDetailCreatedAt)
        layoutTimelineVerified = findViewById(R.id.layoutTimelineVerified)
        tvDetailVerifiedAt = findViewById(R.id.tvDetailVerifiedAt)
        layoutTimelineVerifiedBy = findViewById(R.id.layoutTimelineVerifiedBy)
        tvDetailVerifiedBy = findViewById(R.id.tvDetailVerifiedBy)
        layoutTimelineCompleted = findViewById(R.id.layoutTimelineCompleted)
        tvDetailCompletedAt = findViewById(R.id.tvDetailCompletedAt)
        layoutTimelineUpdated = findViewById(R.id.layoutTimelineUpdated)
        tvDetailUpdatedAt = findViewById(R.id.tvDetailUpdatedAt)

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        wvStationMap.settings.javaScriptEnabled = true
        wvStationMap.settings.domStorageEnabled = true
        wvStationMap.settings.loadWithOverviewMode = true
        wvStationMap.settings.useWideViewPort = true
        wvStationMap.webViewClient = WebViewClient()
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

        btnOpenGoogleMaps.setOnClickListener {
            openLocationInGoogleMaps()
        }
    }

    private fun openLocationInGoogleMaps() {
        val lat = stationLatitude
        val lng = stationLongitude
        if (lat == null || lng == null) {
            Toast.makeText(this, "Coordinates not available for this station", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val encodedName = Uri.encode(stationDisplayName.ifBlank { "Solar Station" })
            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedName)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to browser or any map handler
                val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                startActivity(webMapIntent)
            }
        } catch (e: Exception) {
            val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
            startActivity(webMapIntent)
        }
    }

    private fun fetchReservationDetails() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getReservationById(reservationId)
                if (response.isSuccessful && response.body() != null) {
                    val reservation = response.body()!!
                    currentReservation = reservation

                    // Check if we need to fetch additional station or slot metadata
                    var stationCapacity = reservation.stationCapacityKwh
                    var batterySlots = reservation.batterySlotCount
                    var availableSlots = reservation.availableSlotCount
                    var stationLat = reservation.latitude
                    var stationLng = reservation.longitude
                    var stationStatus = "Active"
                    var slotCapacity = reservation.slotCapacityKwh

                    // Fetch Station details if not fully present
                    if (stationLat == null || stationCapacity == null) {
                        try {
                            val stationResp = ApiClient.apiService.getStationById(reservation.stationId)
                            if (stationResp.isSuccessful && stationResp.body() != null) {
                                val s = stationResp.body()!!
                                stationCapacity = s.capacityKwh
                                batterySlots = s.batterySlotCount
                                availableSlots = s.availableSlotCount
                                stationLat = s.location?.latitude
                                stationLng = s.location?.longitude
                                stationStatus = s.status
                            }
                        } catch (_: Exception) {}
                    }

                    // Fetch Slot details if slot capacity not present
                    if (slotCapacity == null && reservation.stationId.isNotBlank()) {
                        try {
                            val slotsResp = ApiClient.apiService.getStationSlots(reservation.stationId)
                            if (slotsResp.isSuccessful && slotsResp.body() != null) {
                                val matchedSlot = slotsResp.body()!!.find { it.slotId == reservation.slotId }
                                slotCapacity = matchedSlot?.capacityKwh
                            }
                        } catch (_: Exception) {}
                    }

                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        renderDetails(
                            reservation,
                            stationCapacity,
                            batterySlots,
                            availableSlots,
                            stationLat,
                            stationLng,
                            stationStatus,
                            slotCapacity
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
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

    private fun renderDetails(
        item: ReservationSummaryItem,
        stationCapacity: Double?,
        batterySlots: Int?,
        availableSlots: Int?,
        stationLat: Double?,
        stationLng: Double?,
        stationStatus: String,
        slotCapacity: Double?
    ) {
        layoutErrorDetails.visibility = View.GONE
        scrollDetails.visibility = View.VISIBLE

        stationLatitude = stationLat
        stationLongitude = stationLng
        stationDisplayName = item.stationName

        // Top Station & Status
        tvDetailStationName.text = item.stationName
        tvDetailSlotNumber.text = "Slot No. ${item.slotNumber}"

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
        tvDetailBookingStationName.text = item.stationName
        tvDetailBookingSlotNumber.text = "Slot ${item.slotNumber}"

        if (slotCapacity != null && slotCapacity > 0.0) {
            layoutSlotCapacity.visibility = View.VISIBLE
            tvDetailBookingSlotCapacity.text = "$slotCapacity kWh"
        } else {
            layoutSlotCapacity.visibility = View.GONE
        }

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

        // Station Information & Map Card
        tvStationInfoTitle.text = "${item.stationName} Station"
        tvDetailStationCapacity.text = if (stationCapacity != null && stationCapacity > 0) "$stationCapacity kWh" else "—"
        tvDetailTotalSlots.text = if (batterySlots != null && batterySlots > 0) "$batterySlots" else "—"
        tvDetailAvailableSlots.text = if (availableSlots != null) "$availableSlots" else "—"

        tvDetailStationStatusBadge.text = stationStatus
        if (stationStatus.equals("Active", ignoreCase = true)) {
            tvDetailStationStatusBadge.setTextColor(Color.parseColor("#15803D"))
            tvDetailStationStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
        } else {
            tvDetailStationStatusBadge.setTextColor(Color.parseColor("#6B7280"))
            tvDetailStationStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F3F4F6"))
        }

        // Map section
        if (stationLat != null && stationLng != null && stationLat != 0.0 && stationLng != 0.0) {
            layoutStationMapContainer.visibility = View.VISIBLE
            val formattedLat = String.format("%.4f", stationLat)
            val formattedLng = String.format("%.4f", stationLng)
            tvDetailCoordinates.text = "$formattedLat°, $formattedLng°"

            val mapHtml = buildLeafletMapHtml(stationLat, stationLng, item.stationName)
            wvStationMap.loadDataWithBaseURL("https://unpkg.com", mapHtml, "text/html", "UTF-8", null)
        } else {
            layoutStationMapContainer.visibility = View.GONE
        }

        // Timeline Section (Only show rows if date/time available)
        if (!item.createdAt.isNullOrBlank()) {
            layoutTimelineCreated.visibility = View.VISIBLE
            tvDetailCreatedAt.text = formatTimestamp(item.createdAt)
        } else {
            layoutTimelineCreated.visibility = View.GONE
        }

        if (!item.verifiedAt.isNullOrBlank()) {
            layoutTimelineVerified.visibility = View.VISIBLE
            tvDetailVerifiedAt.text = formatTimestamp(item.verifiedAt)
        } else {
            layoutTimelineVerified.visibility = View.GONE
        }

        if (!item.operatorId.isNullOrBlank()) {
            layoutTimelineVerifiedBy.visibility = View.VISIBLE
            tvDetailVerifiedBy.text = item.operatorId
        } else {
            layoutTimelineVerifiedBy.visibility = View.GONE
        }

        if (!item.completedAt.isNullOrBlank()) {
            layoutTimelineCompleted.visibility = View.VISIBLE
            tvDetailCompletedAt.text = formatTimestamp(item.completedAt)
        } else {
            layoutTimelineCompleted.visibility = View.GONE
        }

        if (!item.updatedAt.isNullOrBlank()) {
            layoutTimelineUpdated.visibility = View.VISIBLE
            tvDetailUpdatedAt.text = formatTimestamp(item.updatedAt)
        } else {
            layoutTimelineUpdated.visibility = View.GONE
        }
    }

    private fun buildLeafletMapHtml(lat: Double, lng: Double, stationTitle: String): String {
        val safeTitle = stationTitle.replace("'", "\\'").replace("\"", "\\\"")
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #F3F4F6; }
                    .leaflet-control-attribution { display: none !important; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    try {
                        var map = L.map('map', {zoomControl: true, attributionControl: false}).setView([$lat, $lng], 14);
                        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19
                        }).addTo(map);
                        
                        var marker = L.marker([$lat, $lng]).addTo(map);
                        marker.bindPopup('<b>$safeTitle</b><br/><small>$lat, $lng</small>').openPopup();
                    } catch(e) {
                        document.body.innerHTML = '<div style="padding:20px;text-align:center;color:#666;">Location: $lat, $lng</div>';
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
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
