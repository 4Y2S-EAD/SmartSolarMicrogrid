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
