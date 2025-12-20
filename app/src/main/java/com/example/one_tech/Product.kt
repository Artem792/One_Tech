package com.example.one_tech

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId val id: String = "",

    // Основная информация
    val name: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val images: List<String> = emptyList(),
    val manufacturer: String = "",
    val model: String = "",
    val series: String = "",
    val stock: Int = 0,
    val inStock: Boolean = true,

    // Общие характеристики для всех товаров
    val specs: Map<String, String> = emptyMap(),

    // Метаданные
    val createdAt: Timestamp = Timestamp.now(),
    val createdBy: String = "",
    val updatedAt: Timestamp = Timestamp.now()
) {
    // ... остальной код остается без изменений ...


    // Вспомогательные методы для получения характеристик
    fun getSpec(key: String): String = specs[key] ?: ""

    // Для видеокарт
    val memory: String get() = getSpec("memory")
    val memoryType: String get() = getSpec("memoryType")
    val gpuClock: String get() = getSpec("gpuClock")
    val memoryClock: String get() = getSpec("memoryClock")
    val connectors: String get() = getSpec("connectors")
    val busWidth: String get() = getSpec("busWidth")

    // Для процессоров
    val socket: String get() = getSpec("socket")
    val cores: String get() = getSpec("cores")
    val threads: String get() = getSpec("threads")
    val frequency: String get() = getSpec("frequency")
    val maxFrequency: String get() = getSpec("maxFrequency")
    val cache: String get() = getSpec("cache")
    val tdp: String get() = getSpec("tdp")

    // Для оперативной памяти
    val memoryCapacity: String get() = getSpec("memoryCapacity")
    val memoryFrequency: String get() = getSpec("memoryFrequency")
    val timings: String get() = getSpec("timings")
    val voltage: String get() = getSpec("voltage")
    val memoryFormat: String get() = getSpec("memoryFormat")

    // Для материнских плат
    val motherboardSocket: String get() = getSpec("motherboardSocket")
    val chipset: String get() = getSpec("chipset")
    val formFactor: String get() = getSpec("formFactor")
    val memorySlots: String get() = getSpec("memorySlots")
    val sataPorts: String get() = getSpec("sataPorts")
    val m2Slots: String get() = getSpec("m2Slots")

    // Для накопителей
    val storageType: String get() = getSpec("storageType")
    val storageCapacity: String get() = getSpec("storageCapacity")
    val readSpeed: String get() = getSpec("readSpeed")
    val writeSpeed: String get() = getSpec("writeSpeed")
    val interfaceType: String get() = getSpec("interfaceType")

    // Для блоков питания
    val power: String get() = getSpec("power")
    val psuFormat: String get() = getSpec("psuFormat")
    val efficiency: String get() = getSpec("efficiency")
    val modular: String get() = getSpec("modular")

    // Для корпусов
    val caseFormat: String get() = getSpec("caseFormat")
    val dimensions: String get() = getSpec("dimensions")
    val material: String get() = getSpec("material")
    val fansIncluded: String get() = getSpec("fansIncluded")

    // Для охлаждения
    val coolingType: String get() = getSpec("coolingType")
    val radiatorSize: String get() = getSpec("radiatorSize")
    val fanSpeed: String get() = getSpec("fanSpeed")
    val noiseLevel: String get() = getSpec("noiseLevel")

    // Для готовых ПК
    val processor: String get() = getSpec("processor")
    val motherboard: String get() = getSpec("motherboard")
    val ram: String get() = getSpec("ram")
    val graphics: String get() = getSpec("graphics")
    val storage: String get() = getSpec("storage")
    val powerSupply: String get() = getSpec("powerSupply")
    val pcCase: String get() = getSpec("pcCase")
    val cooling: String get() = getSpec("cooling")
    val os: String get() = getSpec("os")
}