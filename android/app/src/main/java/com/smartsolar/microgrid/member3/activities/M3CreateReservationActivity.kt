package com.smartsolar.microgrid.member3.activities

import android.app.DatePickerDialog
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
import com.smartsolar.microgrid.network.models.AvailableTimeSlot
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
import java.util.Locale

class M3CreateReservationActivity : AppCompatActivity() {

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
    private lateinit var pbTimeSlotsLoading: ProgressBar
    private lateinit var tvNoTimeSlotsNotice: TextView
    private lateinit var spTimeSlot: Spinner
    private lateinit var layoutProcessingStatus: LinearLayout
    private lateinit var tvStatusText: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSubmit: MaterialButton

    // State
    private val stationsList = mutableListOf<Station>()
    private val slotsList = mutableListOf<BookingSlot>()
    private val availableTimeSlotsList = mutableListOf<AvailableTimeSlot>()
    private var selectedStationId: String = ""
    private var selectedSlotId: String = ""
    private var startTime: String = ""
    private var endTime: String = ""
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
        pbTimeSlotsLoading = findViewById(R.id.pbCreateTimeSlotsLoading)
        tvNoTimeSlotsNotice = findViewById(R.id.tvCreateNoTimeSlotsNotice)
        spTimeSlot = findViewById(R.id.spCreateTimeSlot)
        layoutProcessingStatus = findViewById(R.id.layoutProcessingStatus)
        tvStatusText = findViewById(R.id.tvCreateStatusText)
        btnCancel = findViewById(R.id.btnCancelCreate)
        btnSubmit = findViewById(R.id.btnSubmitCreateReservation)

        tvSelectedDate.text = dateDisplayFormat.format(bookingCalendar.time)

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

        // Date selection restricted within 7 days
        btnPickDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    bookingCalendar.set(Calendar.YEAR, year)
                    bookingCalendar.set(Calendar.MONTH, month)
                    bookingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    tvSelectedDate.text = dateDisplayFormat.format(bookingCalendar.time)
                    tvError.visibility = View.GONE
                    // Refresh available time slots for the new date
                    loadAvailableTimeSlots()
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

                        val stationDisplayList = stationsList.map { "${it.stationName} (${it.capacityKwh} kWh Capacity)" }
                        val adapter = ArrayAdapter(this@M3CreateReservationActivity, android.R.layout.simple_spinner_dropdown_item, stationDisplayList)
                        spStation.adapter = adapter

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

                        selectedStationId = stationsList[0].stationId
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
        pbSlotsLoading.visibility = View.VISIBLE
        layoutSlotList.removeAllViews()
        tvNoSlotsNotice.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.apiService.getStationSlots(stationId)
                withContext(Dispatchers.Main) {
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
                            selectedSlotId = slotsList[0].slotId
                            renderSlotCards()
                            loadAvailableTimeSlots()
                        } else {
                            selectedSlotId = ""
                            renderSlotCards()
                            spTimeSlot.adapter = null
                            tvNoTimeSlotsNotice.visibility = View.VISIBLE
                            btnSubmit.isEnabled = false
                        }
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
                if (selectedSlotId != slot.slotId) {
                    selectedSlotId = slot.slotId
                    tvError.visibility = View.GONE
                    renderSlotCards()
                    loadAvailableTimeSlots()
                }
            }

            layoutSlotList.addView(itemView)
        }
    }

    // Fetches available 2-hour time slots for selected slot and date
    private fun loadAvailableTimeSlots() {
        if (selectedSlotId.isBlank()) return

        val dateStr = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            bookingCalendar.get(Calendar.YEAR),
            bookingCalendar.get(Calendar.MONTH) + 1,
            bookingCalendar.get(Calendar.DAY_OF_MONTH)
        )

        pbTimeSlotsLoading.visibility = View.VISIBLE
        tvNoTimeSlotsNotice.visibility = View.GONE
        spTimeSlot.adapter = null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.apiService.getAvailableTimeSlots(selectedSlotId, dateStr)
                withContext(Dispatchers.Main) {
                    pbTimeSlotsLoading.visibility = View.GONE
                    if (resp.isSuccessful && resp.body() != null) {
                        availableTimeSlotsList.clear()
                        availableTimeSlotsList.addAll(resp.body()!!)

                        if (availableTimeSlotsList.isNotEmpty()) {
                            tvNoTimeSlotsNotice.visibility = View.GONE
                            btnSubmit.isEnabled = !isSubmitting

                            val slotLabels = availableTimeSlotsList.map { it.label }
                            val adapter = ArrayAdapter(
                                this@M3CreateReservationActivity,
                                android.R.layout.simple_spinner_dropdown_item,
                                slotLabels
                            )
                            spTimeSlot.adapter = adapter

                            startTime = availableTimeSlotsList[0].startTime
                            endTime = availableTimeSlotsList[0].endTime

                            spTimeSlot.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    val chosen = availableTimeSlotsList[position]
                                    startTime = chosen.startTime
                                    endTime = chosen.endTime
                                    tvError.visibility = View.GONE
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                            }
                        } else {
                            tvNoTimeSlotsNotice.visibility = View.VISIBLE
                            btnSubmit.isEnabled = false
                            startTime = ""
                            endTime = ""
                        }
                    } else {
                        tvNoTimeSlotsNotice.visibility = View.VISIBLE
                        btnSubmit.isEnabled = false
                        startTime = ""
                        endTime = ""
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbTimeSlotsLoading.visibility = View.GONE
                    tvNoTimeSlotsNotice.visibility = View.VISIBLE
                    btnSubmit.isEnabled = false
                    startTime = ""
                    endTime = ""
                }
            }
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

        if (startTime.isBlank() || endTime.isBlank()) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Please select an available 2-hour time slot."
            return
        }

        // Validate 7-day rule
        val maxAllowedCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
        if (bookingCalendar.after(maxAllowedCal)) {
            tvError.visibility = View.VISIBLE
            tvError.text = "Reservations can only be scheduled within 7 days from today."
            return
        }

        val isoDate = String.format(
            Locale.US,
            "%04d-%02d-%02dT00:00:00.000Z",
            bookingCalendar.get(Calendar.YEAR),
            bookingCalendar.get(Calendar.MONTH) + 1,
            bookingCalendar.get(Calendar.DAY_OF_MONTH)
        )

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

                    withContext(Dispatchers.Main) {
                        tvStatusText.text = "Generating and signing QR token..."
                    }

                    try {
                        ApiClient.apiService.generateQrCode(newReservationId)
                    } catch (_: Exception) {
                        // QR code generation handled on demand if needed
                    }

                    withContext(Dispatchers.Main) {
                        layoutProcessingStatus.visibility = View.GONE
                        Toast.makeText(
                            this@M3CreateReservationActivity,
                            "Energy reservation created successfully!",
                            Toast.LENGTH_LONG
                        ).show()

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
