package com.smartsolar.microgrid.member3.activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Spinner
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
import com.smartsolar.microgrid.network.models.BookingSlot
import com.smartsolar.microgrid.network.models.CancelReservationRequest
import com.smartsolar.microgrid.network.models.ReservationSummaryItem
import com.smartsolar.microgrid.network.models.Station
import com.smartsolar.microgrid.network.models.UpdateReservationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    // Action Controls (Update & Cancel Booking)
    private lateinit var layoutActionButtons: LinearLayout
    private lateinit var btnUpdateReservation: MaterialButton
    private lateinit var btnCancelReservation: MaterialButton
    private lateinit var tvActionNotice: TextView

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

        // Action Buttons
        layoutActionButtons = findViewById(R.id.layoutActionButtons)
        btnUpdateReservation = findViewById(R.id.btnUpdateReservation)
        btnCancelReservation = findViewById(R.id.btnCancelReservation)
        tvActionNotice = findViewById(R.id.tvActionNotice)

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

        btnCancelReservation.setOnClickListener {
            currentReservation?.let { reservation ->
                showCancelDialog(reservation)
            }
        }

        btnUpdateReservation.setOnClickListener {
            currentReservation?.let { reservation ->
                showUpdateDialog(reservation)
            }
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

                    var stationCapacity = reservation.stationCapacityKwh
                    var batterySlots = reservation.batterySlotCount
                    var availableSlots = reservation.availableSlotCount
                    var stationLat = reservation.latitude
                    var stationLng = reservation.longitude
                    var stationStatus = "Active"
                    var slotCapacity = reservation.slotCapacityKwh

                    // Fetch Station details if not fully joined
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
            val formattedLat = String.format(Locale.US, "%.4f", stationLat)
            val formattedLng = String.format(Locale.US, "%.4f", stationLng)
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

        // Action Controls Visibility (Only editable when Pending or Approved)
        val isEditable = statusUpper.contains("PEND") || statusUpper.contains("APPROV")
        if (isEditable) {
            btnUpdateReservation.visibility = View.VISIBLE
            btnCancelReservation.visibility = View.VISIBLE
            tvActionNotice.visibility = View.GONE
        } else {
            btnUpdateReservation.visibility = View.GONE
            btnCancelReservation.visibility = View.GONE
            tvActionNotice.visibility = View.VISIBLE
            tvActionNotice.text = "This reservation is ${item.status} and can no longer be updated or cancelled."
        }
    }

    private fun showCancelDialog(reservation: ReservationSummaryItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.m3_dialog_cancel_reservation, null)
        val etReason = dialogView.findViewById<EditText>(R.id.etCancelReason)
        val tvError = dialogView.findViewById<TextView>(R.id.tvCancelError)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pbCancelLoading)
        val btnDismiss = dialogView.findViewById<MaterialButton>(R.id.btnCancelDismiss)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmCancel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnDismiss.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val reason = etReason.text.toString().trim()
            if (reason.isBlank()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Please enter a cancellation reason."
                return@setOnClickListener
            }

            if (isLessThan12HoursAway(reservation.bookingDate, reservation.startTime)) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Reservations can only be cancelled at least 12 hours in advance."
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            pbLoading.visibility = View.VISIBLE
            btnConfirm.isEnabled = false
            btnDismiss.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resp = ApiClient.apiService.cancelReservation(
                        reservation.reservationId,
                        CancelReservationRequest(cancellationReason = reason)
                    )
                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        btnConfirm.isEnabled = true
                        btnDismiss.isEnabled = true

                        if (resp.isSuccessful) {
                            dialog.dismiss()
                            Toast.makeText(this@M3ReservationDetailsActivity, "Reservation cancelled successfully.", Toast.LENGTH_LONG).show()
                            fetchReservationDetails()
                        } else {
                            val errorMsg = parseErrorMessage(resp.errorBody()?.string())
                                ?: "Failed to cancel reservation (${resp.code()})"
                            tvError.visibility = View.VISIBLE
                            tvError.text = errorMsg
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        btnConfirm.isEnabled = true
                        btnDismiss.isEnabled = true
                        tvError.visibility = View.VISIBLE
                        tvError.text = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showUpdateDialog(reservation: ReservationSummaryItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.m3_dialog_update_reservation, null)
        val tvError = dialogView.findViewById<TextView>(R.id.tvUpdateError)
        val spStation = dialogView.findViewById<Spinner>(R.id.spUpdateStation)
        val pbSlots = dialogView.findViewById<ProgressBar>(R.id.pbSlotsLoading)
        val tvNoSlots = dialogView.findViewById<TextView>(R.id.tvNoSlotsNotice)
        val layoutSlots = dialogView.findViewById<LinearLayout>(R.id.layoutSlotList)
        val btnPickDate = dialogView.findViewById<MaterialCardView>(R.id.btnPickDate)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickStart = dialogView.findViewById<MaterialCardView>(R.id.btnPickStartTime)
        val tvStart = dialogView.findViewById<TextView>(R.id.tvSelectedStartTime)
        val btnPickEnd = dialogView.findViewById<MaterialCardView>(R.id.btnPickEndTime)
        val tvEnd = dialogView.findViewById<TextView>(R.id.tvSelectedEndTime)
        val pbUpdate = dialogView.findViewById<ProgressBar>(R.id.pbUpdateLoading)
        val btnDismiss = dialogView.findViewById<MaterialButton>(R.id.btnUpdateDismiss)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSaveUpdate)

        // Selected form state
        var targetStationId = reservation.stationId
        var targetSlotId = reservation.slotId
        var targetStartTime = sanitizeTime(reservation.startTime, "01:00 AM")
        var targetEndTime = sanitizeTime(reservation.endTime, "01:00 AM")

        val targetCalendar = Calendar.getInstance()
        val parsedDate = parseIsoDate(reservation.bookingDate)
        if (parsedDate != null && parsedDate.after(Date())) {
            targetCalendar.time = parsedDate
        } else {
            targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateDisplayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        tvDate.text = dateDisplayFormat.format(targetCalendar.time)
        tvStart.text = targetStartTime
        tvEnd.text = targetEndTime

        val stationsList = mutableListOf<Station>()
        val slotsList = mutableListOf<BookingSlot>()
        var isInitialStationSelection = true

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnDismiss.setOnClickListener { dialog.dismiss() }

        // Date Picker (Only today up to 7 days ahead)
        btnPickDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    targetCalendar.set(Calendar.YEAR, year)
                    targetCalendar.set(Calendar.MONTH, month)
                    targetCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    tvDate.text = dateDisplayFormat.format(targetCalendar.time)
                    tvError.visibility = View.GONE
                },
                targetCalendar.get(Calendar.YEAR),
                targetCalendar.get(Calendar.MONTH),
                targetCalendar.get(Calendar.DAY_OF_MONTH)
            )

            val minCal = Calendar.getInstance()
            datePicker.datePicker.minDate = minCal.timeInMillis
            val maxCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            datePicker.datePicker.maxDate = maxCal.timeInMillis
            datePicker.show()
        }

        // Start Time Picker (12-hour format)
        btnPickStart.setOnClickListener {
            val (hour, min) = parse12HourParts(targetStartTime)
            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val hour12 = when {
                        selectedHour == 0 -> 12
                        selectedHour > 12 -> selectedHour - 12
                        else -> selectedHour
                    }
                    targetStartTime = String.format(Locale.US, "%02d:%02d %s", hour12, selectedMinute, amPm)
                    tvStart.text = targetStartTime
                    tvError.visibility = View.GONE
                },
                hour,
                min,
                false
            ).show()
        }

        // End Time Picker (12-hour format)
        btnPickEnd.setOnClickListener {
            val (hour, min) = parse12HourParts(targetEndTime)
            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val hour12 = when {
                        selectedHour == 0 -> 12
                        selectedHour > 12 -> selectedHour - 12
                        else -> selectedHour
                    }
                    targetEndTime = String.format(Locale.US, "%02d:%02d %s", hour12, selectedMinute, amPm)
                    tvEnd.text = targetEndTime
                    tvError.visibility = View.GONE
                },
                hour,
                min,
                false
            ).show()
        }

        // Function to render selectable slot cards
        fun refreshSlotCards() {
            layoutSlots.removeAllViews()
            if (slotsList.isEmpty()) {
                tvNoSlots.visibility = View.VISIBLE
                return
            }
            tvNoSlots.visibility = View.GONE

            for (slot in slotsList) {
                val itemView = LayoutInflater.from(this).inflate(R.layout.m3_item_slot_selectable, layoutSlots, false)
                val card = itemView.findViewById<MaterialCardView>(R.id.cardSelectableSlot)
                val rb = itemView.findViewById<RadioButton>(R.id.rbSlotSelected)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvSlotTitle)
                val tvCap = itemView.findViewById<TextView>(R.id.tvSlotCapacity)
                val tvBadge = itemView.findViewById<TextView>(R.id.tvSlotStatusBadge)

                val isSelected = (slot.slotId == targetSlotId)
                rb.isChecked = isSelected

                tvTitle.text = "Slot #${slot.slotNumber}"
                tvCap.text = "Capacity: ${slot.capacityKwh} kWh"
                tvBadge.text = slot.status

                if (isSelected) {
                    card.strokeColor = Color.parseColor("#059669")
                    card.strokeWidth = 4
                    card.setCardBackgroundColor(Color.parseColor("#ECFDF5"))
                } else {
                    card.strokeColor = Color.parseColor("#E5E7EB")
                    card.strokeWidth = 2
                    card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                }

                card.setOnClickListener {
                    targetSlotId = slot.slotId
                    if (slot.startTime.isNotBlank() && slot.startTime.contains(":")) {
                        targetStartTime = slot.startTime
                        tvStart.text = targetStartTime
                    }
                    if (slot.endTime.isNotBlank() && slot.endTime.contains(":")) {
                        targetEndTime = slot.endTime
                        tvEnd.text = targetEndTime
                    }
                    tvError.visibility = View.GONE
                    refreshSlotCards()
                }

                layoutSlots.addView(itemView)
            }
        }

        // Function to load slots for selected station
        fun loadSlotsForStation(stationId: String, preselectedSlotId: String?) {
            pbSlots.visibility = View.VISIBLE
            layoutSlots.removeAllViews()
            tvNoSlots.visibility = View.GONE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resp = ApiClient.apiService.getStationSlots(stationId)
                    withContext(Dispatchers.Main) {
                        pbSlots.visibility = View.GONE
                        if (resp.isSuccessful && resp.body() != null) {
                            slotsList.clear()
                            slotsList.addAll(resp.body()!!)

                            if (preselectedSlotId != null && slotsList.any { it.slotId == preselectedSlotId }) {
                                targetSlotId = preselectedSlotId
                            } else if (slotsList.isNotEmpty()) {
                                targetSlotId = slotsList[0].slotId
                            } else {
                                targetSlotId = ""
                            }
                            refreshSlotCards()
                        } else {
                            tvNoSlots.visibility = View.VISIBLE
                            tvNoSlots.text = "Could not fetch slots for this station."
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pbSlots.visibility = View.GONE
                        tvNoSlots.visibility = View.VISIBLE
                        tvNoSlots.text = "Network error loading slots."
                    }
                }
            }
        }

        // Fetch Stations from backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.apiService.getStations()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body() != null) {
                        stationsList.clear()
                        stationsList.addAll(resp.body()!!.filter { it.status.equals("Active", ignoreCase = true) })
                        if (stationsList.isEmpty()) {
                            stationsList.addAll(resp.body()!!)
                        }

                        val stationNames = stationsList.map { it.stationName }
                        val adapter = ArrayAdapter(this@M3ReservationDetailsActivity, android.R.layout.simple_spinner_dropdown_item, stationNames)
                        spStation.adapter = adapter

                        val currentIndex = stationsList.indexOfFirst { it.stationId == targetStationId }
                        if (currentIndex >= 0) {
                            spStation.setSelection(currentIndex)
                        }

                        spStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                val selectedStation = stationsList[position]
                                if (isInitialStationSelection) {
                                    isInitialStationSelection = false
                                    loadSlotsForStation(selectedStation.stationId, reservation.slotId)
                                } else {
                                    if (targetStationId != selectedStation.stationId) {
                                        targetStationId = selectedStation.stationId
                                        targetSlotId = ""
                                        loadSlotsForStation(targetStationId, null)
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    } else {
                        tvError.visibility = View.VISIBLE
                        tvError.text = "Failed to load stations."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Failed to load stations: ${e.localizedMessage}"
                }
            }
        }

        // Save Changes button
        btnSave.setOnClickListener {
            if (targetStationId.isBlank()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Please select a station."
                return@setOnClickListener
            }

            if (targetSlotId.isBlank()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Please select a battery slot."
                return@setOnClickListener
            }

            // Check 7 day limit
            val maxAllowedCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            if (targetCalendar.after(maxAllowedCal)) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Reservation reschedule must be within the 7 day period."
                return@setOnClickListener
            }

            // Check if current reservation has at least 12 hours remaining
            if (isLessThan12HoursAway(reservation.bookingDate, reservation.startTime)) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Reservations can only be modified at least 12 hours in advance."
                return@setOnClickListener
            }

            val isoDate = String.format(
                Locale.US,
                "%04d-%02d-%02dT00:00:00.000Z",
                targetCalendar.get(Calendar.YEAR),
                targetCalendar.get(Calendar.MONTH) + 1,
                targetCalendar.get(Calendar.DAY_OF_MONTH)
            )

            tvError.visibility = View.GONE
            pbUpdate.visibility = View.VISIBLE
            btnSave.isEnabled = false
            btnDismiss.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val updateReq = UpdateReservationRequest(
                        stationId = targetStationId,
                        slotId = targetSlotId,
                        bookingDate = isoDate,
                        startTime = targetStartTime,
                        endTime = targetEndTime
                    )
                    val resp = ApiClient.apiService.updateReservation(reservation.reservationId, updateReq)
                    withContext(Dispatchers.Main) {
                        pbUpdate.visibility = View.GONE
                        btnSave.isEnabled = true
                        btnDismiss.isEnabled = true

                        if (resp.isSuccessful) {
                            dialog.dismiss()
                            Toast.makeText(this@M3ReservationDetailsActivity, "Reservation updated successfully!", Toast.LENGTH_LONG).show()
                            fetchReservationDetails()
                        } else {
                            val err = parseErrorMessage(resp.errorBody()?.string())
                                ?: "Failed to update reservation (${resp.code()})"
                            tvError.visibility = View.VISIBLE
                            tvError.text = err
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pbUpdate.visibility = View.GONE
                        btnSave.isEnabled = true
                        btnDismiss.isEnabled = true
                        tvError.visibility = View.VISIBLE
                        tvError.text = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                    }
                }
            }
        }

        dialog.show()
    }

    private fun isLessThan12HoursAway(bookingDateStr: String, startTimeStr: String): Boolean {
        return try {
            val datePart = bookingDateStr.substringBefore("T")
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val scheduledDate = sdf.parse("$datePart $startTimeStr") ?: return false
            val remainingMillis = scheduledDate.time - System.currentTimeMillis()
            remainingMillis < (12L * 60 * 60 * 1000)
        } catch (_: Exception) {
            false
        }
    }

    private fun parse12HourParts(timeStr: String): Pair<Int, Int> {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val d = sdf.parse(timeStr.trim()) ?: return Pair(1, 0)
            val cal = Calendar.getInstance().apply { time = d }
            Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } catch (_: Exception) {
            Pair(1, 0)
        }
    }

    private fun sanitizeTime(rawTime: String?, fallback: String = "01:00 AM"): String {
        if (rawTime.isNullOrBlank()) return fallback
        val trimmed = rawTime.trim()
        if (trimmed.matches(Regex("^(0?[1-9]|1[0-2]):[0-5][0-9]\\s?(AM|PM)$", RegexOption.IGNORE_CASE))) {
            return try {
                val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                val d = sdf.parse(trimmed)
                if (d != null) SimpleDateFormat("hh:mm a", Locale.US).format(d) else fallback
            } catch (_: Exception) {
                fallback
            }
        }
        return try {
            val timePart = when {
                trimmed.contains("T") -> trimmed.substringAfter("T").substringBefore(".").substringBefore("Z").trim()
                trimmed.contains(" ") && trimmed.contains(":") -> trimmed.substringAfter(" ").substringBefore(".").trim()
                else -> trimmed
            }
            val parts = timePart.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 1
                val m = parts[1].toIntOrNull() ?: 0
                val amPm = if (h < 12) "AM" else "PM"
                val h12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                String.format(Locale.US, "%02d:%02d %s", h12, m, amPm)
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun parseIsoDate(isoDateStr: String): Date? {
        return try {
            val datePart = isoDateStr.substringBefore("T")
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseErrorMessage(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null
        return try {
            val json = JSONObject(rawError)
            when {
                json.has("message") -> json.getString("message")
                json.has("Message") -> json.getString("Message")
                else -> rawError
            }
        } catch (_: Exception) {
            rawError
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
