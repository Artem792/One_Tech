package com.example.one_tech

import com.google.firebase.Timestamp

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productImage: String = "",
    val quantity: Int = 1,
    val category: String = "",
    val addedAt: Timestamp = Timestamp.now(),
    val userId: String = ""
) {
    fun getTotalPrice(): Double = productPrice * quantity
}