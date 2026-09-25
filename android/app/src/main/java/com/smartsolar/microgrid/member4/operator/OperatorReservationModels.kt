/* Member 4 | Operator reservations | Read-only API projections; no local status/count rules. */
package com.smartsolar.microgrid.member4.operator

data class OperatorReservation(
    val reservationId: String, val prosumerNic: String, val stationId: String, val stationName: String?,
    val slotId: String, val slotNumber: Int?, val bookingDate: String, val startTime: String, val endTime: String,
    val status: String, val canApprove: Boolean, val cancellationReason: String?, val completedAt: String?
)
data class OperatorSummary(val activeCount: Long, val pendingCount: Long, val approvedCount: Long, val completedCount: Long)
data class OperatorReservationPage(val items: List<OperatorReservation>, val currentPage: Int, val pageSize: Int,
    val totalRecords: Long, val totalPages: Long, val summary: OperatorSummary, val statusOptions: List<String>)
data class OperatorReservationState(val loading: Boolean = true, val data: OperatorReservationPage? = null,
    val error: String? = null, val feedback: String? = null, val approvingId: String? = null,
    val view: String = "all", val page: Int = 1, val filters: Map<String, String> = emptyMap(),
    val statusOptions: List<String> = emptyList())
