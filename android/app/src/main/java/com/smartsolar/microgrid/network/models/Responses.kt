package com.smartsolar.microgrid.network.models

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val nic: String,
    val fullName: String,
    val email: String,
    val role: String,
    val accountStatus: String
)

data class MessageResponse(
    val message: String
)

data class UserProfileResponse(
    val nic: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val address: String?,
    val role: String,
    val accountStatus: String,
    val isApproved: Boolean
)

// Energy Reservation Dashboard Stats
data class UserReservationDashboardResponse(
    val prosumerNic: String? = null,
    val totalReservations: Long = 0,
    val pendingCount: Long = 0,
    val approvedCount: Long = 0,
    val completedCount: Long = 0,
    val cancelledCount: Long = 0
)

// Paginated Reservations Response
data class PaginatedReservationsResponse(
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val totalRecords: Long = 0,
    val totalPages: Int = 1,
    val items: List<ReservationSummaryItem> = emptyList()
)

// Reservation Summary Item
data class ReservationSummaryItem(
    val reservationId: String = "",
    val prosumerNic: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val slotId: String = "",
    val slotNumber: Int = 0,
    val bookingDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    val qrToken: String? = null,
    val operatorId: String? = null,
    val verifiedAt: String? = null,
    val completedAt: String? = null,
    val cancellationReason: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val stationCapacityKwh: Double? = null,
    val batterySlotCount: Int? = null,
    val availableSlotCount: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val slotCapacityKwh: Double? = null
)
