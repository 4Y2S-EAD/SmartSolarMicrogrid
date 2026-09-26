/* Member 4 | Operator reservations | Reuse shared Retrofit, bearer token and existing approval route. */
package com.smartsolar.microgrid.member4.operator

import com.smartsolar.microgrid.network.ApiClient
import org.json.JSONObject
import retrofit2.Response

class OperatorReservationRepository {
    @Suppress("SENSELESS_COMPARISON") // Gson may populate null despite Kotlin non-null declarations.
    suspend fun load(view: String, page: Int, filters: Map<String, String>): OperatorReservationPage {
        // Send criteria to the hub-scoped service; the backend restricts results to the operator's
        // assignedHubId from the JWT. No filtering or counting is done in Android.
        val query = mutableMapOf("page" to page.toString(), "pageSize" to "20")
        if (view == "search") query.putAll(filters.filterValues { it.isNotBlank() })
        val api = ApiClient.apiService
        val response = when (view) {
            "pending"   -> api.getHubPending(query)
            "approved"  -> api.getHubApproved(query)
            "completed" -> api.getHubCompleted(query)
            "history"   -> api.getHubHistory(query)
            "search"    -> api.searchHubReservations(query)
            else        -> api.getHubReservations(query)
        }
        checkResponse(response)
        val data = requireNotNull(response.body()) { "Invalid reservation response. Please retry." }
        require(data.items != null && data.summary != null && data.statusOptions != null && data.currentPage > 0) { "Invalid reservation response. Please retry." }
        require(data.items.all { it.reservationId != null && it.status != null && it.bookingDate != null }) { "Invalid reservation record. Please retry." }
        return data
    }
    suspend fun approve(id: String) {
        // Acknowledge the existing PUT only; the ViewModel retrieves the updated backend state afterward.
        checkResponse(ApiClient.apiService.approveOperatorReservation(id))
    }
    private fun checkResponse(response: Response<*>) {
        // Convert authorization, validation and service failures into messages safe to display.
        if (response.isSuccessful) return
        val message = when (response.code()) {
            401 -> "Your session expired. Please sign in again."
            403 -> "A Grid Operator account is required."
            else -> try {
                val json = JSONObject(response.errorBody()?.string().orEmpty())
                val errors = json.optJSONObject("errors")
                if (errors != null) errors.keys().asSequence().map { errors.getJSONArray(it).optString(0) }.joinToString("\n")
                else json.optString("message").ifBlank { json.optString("title").ifBlank { "Request failed. Please retry." } }
            } catch (_: Exception) { "Request failed. Check the connection and retry." }
        }
        throw IllegalStateException(message)
    }
}
