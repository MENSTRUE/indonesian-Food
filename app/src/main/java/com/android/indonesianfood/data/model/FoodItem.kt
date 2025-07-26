package com.android.indonesianfood.data.model

data class FoodItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val origin: String,
    val imageUrl: String,
    val ingredients: List<String>,
    val instructions: List<String>
)