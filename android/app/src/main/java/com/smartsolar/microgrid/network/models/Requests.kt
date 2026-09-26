package com.smartsolar.microgrid.network.models

data class RegisterRequest(
    val nic: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val address: String,
    val password: String
)

data class LoginRequest(
    val nic: String,
    val password: String
)

data class UpdateProfileRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val address: String
)

data class DeactivationRequest(
    val reason: String
)

data class UpdateReservationRequest(
    val stationId: String,
    val slotId: String,
    val bookingDate: String,
    val startTime: String,
    val endTime: String
)

data class CancelReservationRequest(
    val cancellationReason: String
)

data class CreateReservationRequest(
    val prosumerNic: String,
    val stationId: String,
    val slotId: String,
    val bookingDate: String,
    val startTime: String,
    val endTime: String
)

data class GenerateQrResponse(
    val message: String? = null,
    val reservationId: String? = null,
    val qrToken: String? = null,
    val generatedAt: String? = null
)

data class AvailableTimeSlot(
    val label: String,
    val startTime: String,
    val endTime: String
)
