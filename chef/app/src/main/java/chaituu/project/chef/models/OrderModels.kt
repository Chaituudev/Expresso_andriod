package chaituu.project.chef.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Order(
    val _id: String,
    val cafeId: String,
    val tableId: Table,
    val items: List<OrderItem>,
    val totalPrice: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class Table(
    val _id: String,
    val tableNumber: String
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    val menuItemId: MenuItem? = null,
    val name: String,
    val qty: Int,
    val price: Double
)

@JsonClass(generateAdapter = true)
data class MenuItem(
    val _id: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class OrdersResponse(
    val success: Boolean,
    val count: Int,
    val data: List<Order>
)

@JsonClass(generateAdapter = true)
data class OrderStatusUpdate(
    val status: String
)

@JsonClass(generateAdapter = true)
data class ApiResponse(
    val success: Boolean,
    val message: String
)
