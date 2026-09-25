package com.smartsolar.microgrid.network

import com.smartsolar.microgrid.network.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import com.smartsolar.microgrid.member4.maps.StationMapResponse

interface ApiService {
    // Member 4: send the scanned credential to the central API, then reuse existing completion.
    @POST("operator/verify-qr")
    suspend fun verifyOperatorQr(@Body request: com.smartsolar.microgrid.member4.qr.VerifyQrRequest): Response<com.smartsolar.microgrid.member4.qr.VerifyQrResponse>
    @PUT("reservations/{id}/complete")
    suspend fun completeOperatorTransfer(@Path("id") id: String): Response<com.smartsolar.microgrid.member4.qr.CompleteTransferResponse>

    // Member 4: reservation reads and the existing Member 3 approval write.
    @GET("operator/reservations")
    suspend fun getOperatorReservations(@retrofit2.http.QueryMap filters: Map<String, String>): Response<com.smartsolar.microgrid.member4.operator.OperatorReservationPage>
    @GET("operator/reservations/pending")
    suspend fun getOperatorPending(@retrofit2.http.QueryMap filters: Map<String, String>): Response<com.smartsolar.microgrid.member4.operator.OperatorReservationPage>
    @GET("operator/reservations/history")
    suspend fun getOperatorHistory(@retrofit2.http.QueryMap filters: Map<String, String>): Response<com.smartsolar.microgrid.member4.operator.OperatorReservationPage>
    @GET("operator/reservations/search")
    suspend fun searchOperatorReservations(@retrofit2.http.QueryMap filters: Map<String, String>): Response<com.smartsolar.microgrid.member4.operator.OperatorReservationPage>
    @PUT("reservations/{id}/approve")
    suspend fun approveOperatorReservation(@Path("id") id: String): Response<MessageResponse>

    @POST("member1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("member1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("member1/profile/{nic}")
    suspend fun getProfile(@Path("nic") nic: String): Response<UserProfileResponse>

    @PUT("member1/profile/{nic}")
    suspend fun updateProfile(
        @Path("nic") nic: String,
        @Body request: UpdateProfileRequest
    ): Response<MessageResponse>

    @POST("member1/profile/{nic}/deactivation-request")
    suspend fun requestDeactivation(
        @Path("nic") nic: String,
        @Body request: DeactivationRequest
    ): Response<MessageResponse>

    // ==========================================
    // MEMBER 2: MICROGRID NODE & SLOT MANAGEMENT
    // ==========================================

    // Fetches all microgrid stations from the backend (including inactive stations).
    @GET("stations")
    suspend fun getStations(): Response<List<Station>>

    // Member 4: all search, coordinate validation and nearby calculations run in the API.
    @GET("member4/maps/stations")
    suspend fun getMapStations(
        @Query("query") query: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radiusKm") radiusKm: Double? = null
    ): Response<StationMapResponse>

    // Fetches single station details by ID
    @GET("stations/{id}")
    suspend fun getStationById(@Path("id") stationId: String): Response<Station>

    // Fetches all available booking slots for a specific station
    @GET("stations/{id}/slots")
    suspend fun getStationSlots(@Path("id") stationId: String): Response<List<BookingSlot>>

    // Fetches prosumer reservation dashboard stats (Active, Pending, Completed)
    @GET("reservations/user/{nic}/dashboard")
    suspend fun getUserReservationDashboard(@Path("nic") nic: String): Response<UserReservationDashboardResponse>

    // Fetches single reservation details by ID
    @GET("reservations/{id}")
    suspend fun getReservationById(@Path("id") id: String): Response<ReservationSummaryItem>

    // Fetches paginated reservations by status for a user
    @GET("reservations/user/{nic}/approved")
    suspend fun getApprovedReservations(
        @Path("nic") nic: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<PaginatedReservationsResponse>

    @GET("reservations/user/{nic}/pending")
    suspend fun getPendingReservations(
        @Path("nic") nic: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<PaginatedReservationsResponse>

    @GET("reservations/user/{nic}/completed")
    suspend fun getCompletedReservations(
        @Path("nic") nic: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<PaginatedReservationsResponse>

    @GET("reservations/user/{nic}/cancelled")
    suspend fun getCancelledReservations(
        @Path("nic") nic: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<PaginatedReservationsResponse>

    // Updates an existing reservation schedule, station or slot
    @PUT("reservations/{id}")
    suspend fun updateReservation(
        @Path("id") id: String,
        @Body request: UpdateReservationRequest
    ): Response<ReservationSummaryItem>

    // Cancels a reservation with a cancellation reason
    @PUT("reservations/{id}/cancel")
    suspend fun cancelReservation(
        @Path("id") id: String,
        @Body request: CancelReservationRequest
    ): Response<MessageResponse>

    // Creates a new reservation for prosumer
    @POST("reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequest
    ): Response<ReservationSummaryItem>

    // Generates signed QR code for an approved or newly created reservation
    @POST("reservations/{id}/generate-qr")
    suspend fun generateQrCode(
        @Path("id") id: String
    ): Response<GenerateQrResponse>
}
