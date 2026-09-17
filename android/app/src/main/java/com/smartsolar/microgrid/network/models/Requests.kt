package com.smartsolar.microgrid.network.models

data class RegisterRequest(
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
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
