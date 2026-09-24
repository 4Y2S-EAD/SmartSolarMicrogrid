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

    // Fetches all available booking slots for a specific station
    @GET("stations/{id}/slots")
    suspend fun getStationSlots(@Path("id") stationId: String): Response<List<BookingSlot>>

    // Fetches prosumer reservation dashboard stats (Active, Pending, Completed)
    @GET("reservations/user/{nic}/dashboard")
    suspend fun getUserReservationDashboard(@Path("nic") nic: String): Response<UserReservationDashboardResponse>
}
