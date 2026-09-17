package com.smartsolar.microgrid.network

import com.smartsolar.microgrid.network.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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

    @POST("member1/profile/{nic}/deactivate")
    suspend fun deactivateProfile(@Path("nic") nic: String): Response<MessageResponse>
}
