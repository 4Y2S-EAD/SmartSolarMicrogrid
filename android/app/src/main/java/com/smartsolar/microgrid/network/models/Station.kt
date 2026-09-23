package com.smartsolar.microgrid.network.models

// Represents a Microgrid Node (Hub) from the C# backend
data class Station(
    val stationId: String,          // Unique ID of the station
    val stationName: String,        // Name of the station (e.g., Colombo Hub)
    val capacityKwh: Double,        // Total energy capacity in kWh
    val batterySlotCount: Int,      // Total number of battery slots
    val availableSlotCount: Int,    // Currently available battery slots
    val status: String              // Status (e.g., Active, Inactive)
)
