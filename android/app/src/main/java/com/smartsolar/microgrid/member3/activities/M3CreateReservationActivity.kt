package com.smartsolar.microgrid.member3.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.ApiClient
import com.smartsolar.microgrid.network.TokenManager
import com.smartsolar.microgrid.network.models.BookingSlot
import com.smartsolar.microgrid.network.models.CreateReservationRequest
import com.smartsolar.microgrid.network.models.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class M3CreateReservationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STATION_ID = "STATION_ID"
        const val EXTRA_SLOT_ID = "SLOT_ID"
    }

    private var initialSlotApplied = false

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var tvError: TextView
    private lateinit var spStation: Spinner
    private lateinit var pbStationsLoading: ProgressBar
    private lateinit var pbSlotsLoading: ProgressBar
    private lateinit var tvNoSlotsNotice: TextView
    private lateinit var layoutSlotList: LinearLayout
    private lateinit var btnPickDate: MaterialCardView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvDateRuleHint: TextView
    private lateinit var btnPickStartTime: MaterialCardView
    private lateinit var tvStartTime: TextView
    private lateinit var btnPickEndTime: MaterialCardView
    private lateinit var tvEndTime: TextView
    private lateinit var layoutProcessingStatus: LinearLayout
    private lateinit var tvStatusText: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSubmit: MaterialButton

    // State
    private val stationsList = mutableListOf<Station>()
    private val slotsList = mutableListOf<BookingSlot>()
    private var selectedStationId: String = ""
    private var selectedSlotId: String = ""
    private var startTime: String = "01:00 AM"
    private var endTime: String = "01:00 AM"
    private val bookingCalendar = Calendar.getInstance().apply {
        // Default to tomorrow to satisfy advance scheduling
        add(Calendar.DAY_OF_YEAR, 1)
    }

    private val dateDisplayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var isSubmitting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m3_activity_create_reservation)

        initViews()
        setupListeners()
        loadStations()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBackCreate)
        tvError = findViewById(R.id.tvCreateError)
        spStation = findViewById(R.id.spCreateStation)
        pbStationsLoading = findViewById(R.id.pbStationsLoading)
        pbSlotsLoading = findViewById(R.id.pbCreateSlotsLoading)
        tvNoSlotsNotice = findViewById(R.id.tvCreateNoSlotsNotice)
        layoutSlotList = findViewById(R.id.layoutCreateSlotList)
        btnPickDate = findViewById(R.id.btnPickCreateDate)
        tvSelectedDate = findViewById(R.id.tvCreateSelectedDate)
        tvDateRuleHint = findViewById(R.id.tvDateRuleHint)
        btnPickStartTime = findViewById(R.id.btnPickCreateStartTime)
        tvStartTime = findViewById(R.id.tvCreateStartTime)
        btnPickEndTime = findViewById(R.id.btnPickCreateEndTime)
        tvEndTime = findViewById(R.id.tvCreateEndTime)
        layoutProcessingStatus = findViewById(R.id.layoutProcessingStatus)
        tvStatusText = findViewById(R.id.tvCreateStatusText)
        btnCancel = findViewById(R.id.btnCancelCreate)
        btnSubmit = findViewById(R.id.btnSubmitCreateReservation)

        // Set initial date/time labels
        tvSelectedDate.text = dateDisplayFormat.format(bookingCalendar.time)
        tvStartTime.text = startTime
        tvEndTime.text = endTime

        // Calculate max date for 7-day restriction hint
        val maxCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
        val maxDateStr = dateDisplayFormat.format(maxCal.time)
        tvDateRuleHint.text = "Reservations are restricted to the 7-day period ending on $maxDateStr."
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        // Date Picker (restricted to today up to 7 days ahead)
        btnPickDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    bookingCalendar.set(Calendar.YEAR, year)
                    bookingCalendar.set(Calendar.MONTH, month)
                    bookingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    tvSelectedDate.text = dateDisplayFormat.format(bookingCalendar.time)
                    tvError.visibility = View.GONE
                },
                bookingCalendar.get(Calendar.YEAR),
                bookingCalendar.get(Calendar.MONTH),
                bookingCalendar.get(Calendar.DAY_OF_MONTH)
            )

            val minCal = Calendar.getInstance()
            datePicker.datePicker.minDate = minCal.timeInMillis
            val maxCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            datePicker.datePicker.maxDate = maxCal.timeInMillis
            datePicker.show()
        }

        // Start Time Picker
        btnPickStartTime.setOnClickListener {
            val (hour, min) = parse12HourParts(startTime)
            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val hour12 = when {
                        selectedHour == 0 -> 12
                        selectedHour > 12 -> selectedHour - 12
                        else -> selectedHour
                    }
                    startTime = String.format(Locale.US, "%02d:%02d %s", hour12, selectedMinute, amPm)
                    tvStartTime.text = startTime
                    tvError.visibility = View.GONE
                },
                hour,
                min,
                false
            ).show()
        }

        // End Time Picker
        btnPickEndTime.setOnClickListener {
            val (hour, min) = parse12HourParts(endTime)
            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val hour12 = when {
                        selectedHour == 0 -> 12
                        selectedHour > 12 -> selectedHour - 12
                        else -> selectedHour
                    }
                    endTime = String.format(Locale.US, "%02d:%02d %s", hour12, selectedMinute, amPm)
                    tvEndTime.text = endTime
                    tvError.visibility = View.GONE
                },
                hour,
                min,
                false
            ).show()
        }

        btnSubmit.setOnClickListener {
            validateAndSubmitReservation()
        }
    }

    private fun loadStations() {
        pbStationsLoading.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.apiService.getStations()
                withContext(Dispatchers.Main) {
                    pbStationsLoading.visibility = View.GONE
                    if (resp.isSuccessful && resp.body() != null) {
                        stationsList.clear()
                        val activeStations = resp.body()!!.filter { it.status.equals("Active", ignoreCase = true) }
                        if (activeStations.isNotEmpty()) {
                            stationsList.addAll(activeStations)
                        } else {
                            stationsList.addAll(resp.body()!!)
                        }

                        if (stationsList.isEmpty()) {
                            tvError.visibility = View.VISIBLE
                            tvError.text = "No solar stations available at this time."
                            return@withContext
                        }

                        val requestedStationId = intent.getStringExtra(EXTRA_STATION_ID)
                        val initialPosition = if (requestedStationId.isNullOrBlank()) 0
                            else stationsList.indexOfFirst { it.stationId == requestedStationId }
                        if (initialPosition < 0) {
                            tvError.visibility = View.VISIBLE
                            tvError.text = "Selected station is no longer available. Please go back and choose another station."
                            btnSubmit.isEnabled = false
                            return@withContext
                        }

                        val stationDisplayList = stationsList.map { "${it.stationName} (${it.capacityKwh} kWh Capacity)" }
                        val adapter = ArrayAdapter(this@M3CreateReservationActivity, android.R.layout.simple_spinner_dropdown_item, stationDisplayList)
                        spStation.adapter = adapter
                        selectedStationId = stationsList[initialPosition].stationId
                        spStation.setSelection(initialPosition)

                        spStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                val selectedStation = stationsList[position]
                                if (selectedStationId != selectedStation.stationId) {
                                    selectedStationId = selectedStation.stationId
                                    selectedSlotId = ""
                                    loadSlotsForStation(selectedStationId)
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }

                        // Load the station selected on the map, or retain the normal first-station default.
                        loadSlotsForStation(selectedStationId)
                    } else {
                        tvError.visibility = View.VISIBLE
                        tvError.text = "Failed to load solar stations (${resp.code()})."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbStationsLoading.visibility = View.GONE
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Network error loading stations: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    private fun loadSlotsForStation(stationId: String) {
        selectedSlotId = ""
        btnSubmit.isEnabled = false
        pbSlotsLoading.visibility = View.VISIBLE
        layoutSlotList.removeAllViews()
        tvNoSlotsNotice.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.apiService.getStationSlots(stationId)
                withContext(Dispatchers.Main) {
                    if (selectedStationId != stationId) return@withContext
                    pbSlotsLoading.visibility = View.GONE
                    if (resp.isSuccessful && resp.body() != null) {
                        slotsList.clear()
                        val availableSlots = resp.body()!!.filter { it.status.equals("Available", ignoreCase = true) }
                        if (availableSlots.isNotEmpty()) {
                            slotsList.addAll(availableSlots)
                        } else {
                            slotsList.addAll(resp.body()!!)
                        }

                        if (slotsList.isNotEmpty()) {
                            val requestedSlot = if (!initialSlotApplied &&
                                stationId == intent.getStringExtra(EXTRA_STATION_ID)) {
                                slotsList.firstOrNull { it.slotId == intent.getStringExtra(EXTRA_SLOT_ID) }
                            } else null
                            initialSlotApplied = true
                            val initialSlot = requestedSlot ?: slotsList[0]
                            selectedSlotId = initialSlot.slotId
                            startTime = sanitizeTime(initialSlot.startTime, "01:00 AM")
                            endTime = sanitizeTime(initialSlot.endTime, "01:00 AM")
                            tvStartTime.text = startTime
                            tvEndTime.text = endTime
                        } else {
                            selectedSlotId = ""
                        }

                        renderSlotCards()
                    } else {
                        tvNoSlotsNotice.visibility = View.VISIBLE
                        tvNoSlotsNotice.text = "Could not fetch slots for this station."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbSlotsLoading.visibility = View.GONE
                    tvNoSlotsNotice.visibility = View.VISIBLE
                    tvNoSlotsNotice.text = "Network error loading slots: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    private fun renderSlotCards() {
        layoutSlotList.removeAllViews()
        if (slotsList.isEmpty()) {
            tvNoSlotsNotice.visibility = View.VISIBLE
            btnSubmit.isEnabled = false
            return
        }

        tvNoSlotsNotice.visibility = View.GONE
        btnSubmit.isEnabled = !isSubmitting

        for (slot in slotsList) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.m3_item_slot_selectable, layoutSlotList, false)
            val card = itemView.findViewById<MaterialCardView>(R.id.cardSelectableSlot)
            val rb = itemView.findViewById<RadioButton>(R.id.rbSlotSelected)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvSlotTitle)
            val tvCap = itemView.findViewById<TextView>(R.id.tvSlotCapacity)
            val tvBadge = itemView.findViewById<TextView>(R.id.tvSlotStatusBadge)

            val isSelected = (slot.slotId == selectedSlotId)
            rb.isChecked = isSelected

            tvTitle.text = "Slot #${slot.slotNumber}"
            tvCap.text = "Capacity: ${slot.capacityKwh} kWh"
            tvBadge.text = slot.status

            if (isSelected) {
                card.strokeColor = Color.parseColor("#059669")
                card.strokeWidth = 4
                card.setCardBackgroundColor(Color.parseColor("#ECFDF5"))
            } else {
                card.strokeColor = Color.parseColor("#E2E8F0")
                card.strokeWidth = 2
                card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            }

            card.setOnClickListener {
                selectedSlotId = slot.slotId
                startTime = sanitizeTime(slot.startTime, "01:00 AM")
                endTime = sanitizeTime(slot.endTime, "01:00 AM")
                tvStartTime.text = startTime
                tvEndTime.text = endTime
                tvError.visibility = View.GONE
                renderSlotCards()
            }

            layoutSlotList.addView(itemView)
        }
    }

    private fun validateAndSubmitReservation() {
        val nic = TokenManager.getNic()
        if (nic.isNullOrBlank()) {
            tvError.visibility = View.VISIBLE
            tvError.text = "User session (NIC) not found. Please log in again."
            return
        }

        if (selectedStationId.isBlank()) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Please select a solar station."
            return
        }

        if (selectedSlotId.isBlank()) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Please select an available battery slot."
            return
        }

        // 7-day restriction rule
        val maxAllowedCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
        if (bookingCalendar.after(maxAllowedCal)) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Reservations can only be scheduled within 7 days from today."
            return
        }

        // Check if scheduled time is in the future
        val scheduledMillis = getScheduledDateTimeMillis(bookingCalendar, startTime)
        if (scheduledMillis <= System.currentTimeMillis()) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Scheduled time must be in the future."
            return
        }

        // Check minimum 12 hours advance booking if scheduled for today/tomorrow
        val remainingNoticeMillis = scheduledMillis - System.currentTimeMillis()
        if (remainingNoticeMillis < (12L * 60 * 60 * 1000)) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Reservations must be booked at least 12 hours in advance."
            return
        }

        val isoDate = String.format(
            Locale.US,
            "%04d-%02d-%02dT00:00:00.000Z",
            bookingCalendar.get(Calendar.YEAR),
            bookingCalendar.get(Calendar.MONTH) + 1,
            bookingCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // Update UI state to processing
        isSubmitting = true
        tvError.visibility = View.GONE
        layoutProcessingStatus.visibility = View.VISIBLE
        tvStatusText.text = "Creating reservation record..."
        btnSubmit.isEnabled = false
        btnCancel.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val createRequest = CreateReservationRequest(
                    prosumerNic = nic,
                    stationId = selectedStationId,
                    slotId = selectedSlotId,
                    bookingDate = isoDate,
                    startTime = startTime.trim(),
                    endTime = endTime.trim()
                )

                val createResp = ApiClient.apiService.createReservation(createRequest)

                if (createResp.isSuccessful && createResp.body() != null) {
                    val createdItem = createResp.body()!!
                    val newReservationId = createdItem.reservationId

                    // Step 2: Attempt QR Code Generation
                    withContext(Dispatchers.Main) {
                        tvStatusText.text = "Generating and signing QR token..."
                    }

                    try {
                        ApiClient.apiService.generateQrCode(newReservationId)
                    } catch (_: Exception) {
                        // QR generation may also be handled on-demand by detail view
                    }

                    withContext(Dispatchers.Main) {
                        layoutProcessingStatus.visibility = View.GONE
                        Toast.makeText(
                            this@M3CreateReservationActivity,
                            "Energy reservation created successfully!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Direct to Reservation Details screen
                        val intent = Intent(this@M3CreateReservationActivity, M3ReservationDetailsActivity::class.java).apply {
                            putExtra(M3ReservationDetailsActivity.EXTRA_RESERVATION_ID, newReservationId)
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val err = parseErrorMessage(createResp.errorBody()?.string())
                        ?: "Failed to create reservation (${createResp.code()})"
                    withContext(Dispatchers.Main) {
                        isSubmitting = false
                        layoutProcessingStatus.visibility = View.GONE
                        btnSubmit.isEnabled = true
                        btnCancel.isEnabled = true
                        tvError.visibility = View.VISIBLE
                        tvError.text = err
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    layoutProcessingStatus.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    btnCancel.isEnabled = true
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    private fun getScheduledDateTimeMillis(calendar: Calendar, timeStr: String): Long {
        return try {
            val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            val d = sdf.parse("$datePart $timeStr")
            d?.time ?: calendar.timeInMillis
        } catch (_: Exception) {
            calendar.timeInMillis
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
}
