package com.example.one_tech

import java.io.Serializable

// Для будущих расширений (диапазон цен, характеристики)
data class AdvancedFilterData(
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val categorySpecificFilters: Map<String, String> = emptyMap()
) : Serializable

// Пример специфичных фильтров для категорий
object CategoryFilters {
    // Видеокарты
    val GRAPHICS_CARD = listOf("memory", "memoryType", "gpuClock", "connectors")

    // Процессоры
    val PROCESSORS = listOf("socket", "cores", "frequency", "tdp")

    // Оперативная память
    val MEMORY = listOf("memoryCapacity", "memoryFrequency", "timings")

    // Накопители
    val STORAGE = listOf("storageType", "storageCapacity", "interfaceType")

    // Получить фильтры для категории
    fun getFiltersForCategory(category: String): List<String> {
        return when (category) {
            "Видеокарты" -> GRAPHICS_CARD
            "Процессоры" -> PROCESSORS
            "Память" -> MEMORY
            "Накопители" -> STORAGE
            else -> emptyList()
        }
    }
}