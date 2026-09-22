package com.smartsolar.microgrid.network.models

// Represents a battery slot inside a specific station
data class BookingSlot(
    val slotId: String,             // Unique ID of the battery slot
    val stationId: String,          // ID of the station this slot belongs to
    val slotNumber: Int,            // Slot number (e.g., 1, 2, 3)
    val startTime: String,          // Start time of the slot (ISO 8601 string)
    val endTime: String,            // End time of the slot (ISO 8601 string)
    val capacityKwh: Double,        // Energy capacity of this specific slot
    val status: String              // Status (e.g., Available, Booked)
)
