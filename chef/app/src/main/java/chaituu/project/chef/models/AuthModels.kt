package chaituu.project.chef.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val cafeId: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String,
    val user: User
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "chef"
)

@JsonClass(generateAdapter = true)
data class JoinCafeRequest(
    val code: String
)

@JsonClass(generateAdapter = true)
data class JoinCafeData(
    val cafeId: String,
    val cafeName: String,
    val user: User
)

@JsonClass(generateAdapter = true)
data class JoinCafeResponse(
    val success: Boolean,
    val message: String,
    val data: JoinCafeData
)
