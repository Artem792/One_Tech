package com.example.one_tech

import java.io.Serializable

data class FilterData(
    val sortBy: SortBy = SortBy.DEFAULT,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val manufacturer: String? = null,
    val categoryFilters: Map<String, List<String>> = emptyMap()
) : Serializable

enum class SortBy {
    DEFAULT,      // По умолчанию (новые)
    PRICE_ASC,    // Цена по возрастанию
    PRICE_DESC,   // Цена по убыванию
    NAME_ASC      // Название А-Я
}

// Класс для хранения фильтров по категориям
data class CategoryFilterOption(
    val key: String,
    val displayName: String,
    val values: List<String>
)