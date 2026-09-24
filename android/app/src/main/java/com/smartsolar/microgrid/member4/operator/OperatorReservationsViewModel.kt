/* Member 4 | Operator reservations | Lifecycle-safe requests and presentation state. */
package com.smartsolar.microgrid.member4.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OperatorReservationsViewModel : ViewModel() {
    private val repository = OperatorReservationRepository()
    private val mutable = MutableStateFlow(OperatorReservationState())
    val state = mutable.asStateFlow()
    private var request: Job? = null
    init { refresh() }
    fun select(view: String) {
        // Navigate to a backend-provided view, restarting pagination.
        mutable.value = mutable.value.copy(view = view, page = 1, feedback = null)
        refresh()
    }
    fun search(filters: Map<String, String>) {
        // Preserve submitted inputs across rotation; matching is exclusively server-side.
        mutable.value = mutable.value.copy(view = "search", filters = filters, page = 1)
        refresh()
    }
    fun page(delta: Int) {
        // Use the response's page metadata for navigation, not locally calculated result counts.
        val next = mutable.value.page + delta
        if (next >= 1 && next <= (mutable.value.data?.totalPages ?: 0)) {
            mutable.value = mutable.value.copy(page = next)
            refresh()
        }
    }
    fun refresh() {
        // Cancel superseded reads to prevent old requests overwriting newer tabs or filters.
        request?.cancel()
        request = viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, error = null, data = null)
            try {
                val current = mutable.value
                val data = repository.load(current.view, current.page, current.filters)
                mutable.value = mutable.value.copy(loading = false, data = data, statusOptions = data.statusOptions)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                mutable.value = mutable.value.copy(loading = false, error = error.message ?: "Unable to load reservations. Please retry.")
            }
        }
    }
    fun approve(id: String) {
        // Prevent duplicate taps and refresh all rows/counts only after the existing API succeeds.
        if (mutable.value.approvingId != null) return
        mutable.value = mutable.value.copy(approvingId = id, feedback = null)
        viewModelScope.launch {
            try {
                repository.approve(id)
                mutable.value = mutable.value.copy(feedback = "Reservation approved. Reloading latest data.")
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutable.value = mutable.value.copy(feedback = error.message ?: "Approval failed. Please retry.") }
            finally { mutable.value = mutable.value.copy(approvingId = null) }
        }
    }
}
