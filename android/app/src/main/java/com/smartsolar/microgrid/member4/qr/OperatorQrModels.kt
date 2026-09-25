package com.smartsolar.microgrid.member4.qr

// Member 4: transport-only models. The API owns all verification and completion rules.
data class VerifyQrRequest(val qrData: String)
data class VerifyQrResponse(val result: String, val code: String, val message: String,
    val reservation: VerifiedReservation?)
data class VerifiedReservation(
    val reservationId: String, val prosumerNic: String, val prosumerName: String,
    val stationId: String, val stationName: String, val slotId: String, val slotNumber: Int,
    val bookingDate: String, val startTime: String, val endTime: String, val capacityKwh: Double,
    val status: String, val verifiedAt: String
)
data class CompleteTransferResponse(val message: String, val reservationId: String,
    val status: String, val operatorId: String, val verifiedAt: String, val completedAt: String)

enum class QrPhase { SCANNING, VERIFYING, VERIFIED, REJECTED, ERROR, COMPLETING, COMPLETED }
data class QrScreenState(val phase: QrPhase = QrPhase.SCANNING, val message: String = "",
    val reservation: VerifiedReservation? = null, val completion: CompleteTransferResponse? = null)
