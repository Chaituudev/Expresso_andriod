package chaituu.project.chef.api

import chaituu.project.chef.models.*
import retrofit2.http.*

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("chef/join-cafe")
    suspend fun joinCafeWithCode(@Body request: JoinCafeRequest): JoinCafeResponse

    @GET("chef/orders/{cafeId}")
    suspend fun getOrders(@Path("cafeId") cafeId: String): OrdersResponse

    @PATCH("chef/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: String,
        @Body statusUpdate: OrderStatusUpdate
    ): ApiResponse
}
