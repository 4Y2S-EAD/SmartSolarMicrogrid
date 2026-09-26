package com.smartsolar.microgrid.member4.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import com.smartsolar.microgrid.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class OperatorQrViewModel : ViewModel() {
    private val mutable = MutableStateFlow(QrScreenState())
    val state = mutable.asStateFlow()

    fun verify(rawQr: String) {
        // One request per scan; keep the payload out of saved state, logs and UI.
        if (mutable.value.phase != QrPhase.SCANNING) return
        mutable.value = QrScreenState(QrPhase.VERIFYING)
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.verifyOperatorQr(VerifyQrRequest(rawQr))
                val body = response.body()
                mutable.value = if (response.isSuccessful && body?.result == "VALID" && body.reservation != null) {
                    QrScreenState(QrPhase.VERIFIED, body.message, body.reservation)
                } else {
                    QrScreenState(QrPhase.REJECTED, errorMessage(response))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: IOException) {
                mutable.value = QrScreenState(QrPhase.ERROR, "Cannot reach the server. Check your connection and scan again.")
            } catch (_: Exception) {
                mutable.value = QrScreenState(QrPhase.ERROR, "Verification could not be confirmed. Please scan again.")
            }
        }
    }

    fun complete() {
        // Only the server's verified result can supply the reservation ID for this existing API call.
        val current = mutable.value
        if (current.phase != QrPhase.VERIFIED || current.reservation == null) return
        mutable.value = current.copy(phase = QrPhase.COMPLETING)
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.completeOperatorTransfer(current.reservation.reservationId)
                val body = response.body()
                mutable.value = if (response.isSuccessful && body?.status == "Completed") {
                    current.copy(phase = QrPhase.COMPLETED, message = body.message, completion = body)
                } else {
                    QrScreenState(QrPhase.REJECTED, errorMessage(response))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                // A timeout can occur after the server committed: do not blindly retry the write.
                mutable.value = QrScreenState(QrPhase.ERROR,
                    "Completion could not be confirmed. Check Completed reservations before scanning again.")
            }
        }
    }

    fun scanAgain() {
        // Do not allow a second scan or completion while a request is in progress.
        if (mutable.value.phase !in listOf(QrPhase.VERIFYING, QrPhase.COMPLETING))
            mutable.value = QrScreenState()
    }

    private fun errorMessage(response: Response<*>): String {
        // Display safe API reasons, handling authentication and proxy errors separately.
        if (response.code() == 401) return "Your session has expired. Return to the dashboard and sign in again."
        if (response.code() == 403) return "An active Grid Operator account is required."
        if (response.code() >= 500) return "The server is unavailable. Try again shortly. If completing a transfer, check Completed reservations first."
        return try {
            JsonParser().parse(response.errorBody()?.string()).asJsonObject
                .get("message")?.asString ?: "The QR could not be verified. Please scan a reservation QR again."
        } catch (_: Exception) { "The request could not be confirmed. Please scan again." }
    }
}
