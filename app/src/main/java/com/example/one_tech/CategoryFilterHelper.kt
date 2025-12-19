package com.example.one_tech

object CategoryFilterHelper {

    fun getFiltersForCategory(category: String): List<CategoryFilterOption> {
        return when (category.lowercase()) {
            "видеокарты" -> getGraphicsCardFilters()
            "процессоры" -> getProcessorFilters()
            "память" -> getMemoryFilters()
            "материнские платы" -> getMotherboardFilters()
            "накопители" -> getStorageFilters()
            "блоки питания" -> getPowerSupplyFilters()
            "корпуса" -> getCaseFilters()
            "охлаждение" -> getCoolingFilters()
            "готовые пк" -> getReadyPCFilters()
            else -> emptyList()
        }
    }

    private fun getGraphicsCardFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "memory",
                displayName = "Объем видеопамяти",
                values = listOf("2 GB", "4 GB", "6 GB", "8 GB", "12 GB", "16 GB", "24 GB", "32 GB")
            ),
            CategoryFilterOption(
                key = "memoryType",
                displayName = "Тип памяти",
                values = listOf("GDDR5", "GDDR5X", "GDDR6", "GDDR6X", "GDDR7", "HBM2", "HBM3")
            ),
            CategoryFilterOption(
                key = "gpuClock",
                displayName = "Частота GPU",
                values = listOf("до 1500 MHz", "1500-1800 MHz", "1800-2100 MHz", "2100-2400 MHz", "от 2400 MHz")
            ),
            CategoryFilterOption(
                key = "memoryClock",
                displayName = "Частота памяти",
                values = listOf("до 8000 MHz", "8000-12000 MHz", "12000-16000 MHz", "16000-20000 MHz", "от 20000 MHz")
            ),
            CategoryFilterOption(
                key = "connectors",
                displayName = "Разъемы",
                values = listOf("1x HDMI", "2x HDMI", "3x HDMI", "1x DP", "2x DP", "3x DP", "4x DP", "HDMI+DP", "USB-C", "DVI")
            ),
            CategoryFilterOption(
                key = "busWidth",
                displayName = "Разрядность шины",
                values = listOf("64-bit", "128-bit", "192-bit", "256-bit", "320-bit", "384-bit", "512-bit")
            )
        )
    }

    private fun getProcessorFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "socket",
                displayName = "Сокет",
                values = listOf("LGA 1151", "LGA 1200", "LGA 1700", "AM4", "AM5", "sTRX4", "TRX40", "sWRX8")
            ),
            CategoryFilterOption(
                key = "cores",
                displayName = "Количество ядер",
                values = listOf("2", "4", "6", "8", "10", "12", "14", "16", "18", "24", "32", "64")
            ),
            CategoryFilterOption(
                key = "threads",
                displayName = "Количество потоков",
                values = listOf("4", "8", "12", "16", "20", "24", "32", "48", "64", "96", "128")
            ),
            CategoryFilterOption(
                key = "frequency",
                displayName = "Базовая частота",
                values = listOf("до 3.0 GHz", "3.0-3.5 GHz", "3.5-4.0 GHz", "4.0-4.5 GHz", "от 4.5 GHz")
            ),
            CategoryFilterOption(
                key = "maxFrequency",
                displayName = "Максимальная частота",
                values = listOf("до 4.0 GHz", "4.0-4.5 GHz", "4.5-5.0 GHz", "5.0-5.5 GHz", "от 5.5 GHz")
            ),
            CategoryFilterOption(
                key = "cache",
                displayName = "Кэш-память",
                values = listOf("до 12 MB", "12-20 MB", "20-32 MB", "32-64 MB", "от 64 MB")
            ),
            CategoryFilterOption(
                key = "tdp",
                displayName = "TDP",
                values = listOf("до 65W", "65-95W", "95-125W", "125-170W", "170-250W", "от 250W")
            )
        )
    }

    private fun getMemoryFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "memoryFormat",
                displayName = "Тип памяти",
                values = listOf("DDR3", "DDR4", "DDR5", "DDR6")
            ),
            CategoryFilterOption(
                key = "memoryCapacity",
                displayName = "Объем памяти",
                values = listOf("4 GB", "8 GB", "16 GB", "32 GB", "64 GB", "128 GB", "256 GB")
            ),
            CategoryFilterOption(
                key = "memoryFrequency",
                displayName = "Частота памяти",
                values = listOf("2133 MHz", "2400 MHz", "2666 MHz", "2933 MHz", "3200 MHz",
                    "3600 MHz", "4000 MHz", "4400 MHz", "4800 MHz", "5200 MHz",
                    "5600 MHz", "6000 MHz", "6400 MHz", "6800 MHz", "7200 MHz")
            ),
            CategoryFilterOption(
                key = "timings",
                displayName = "Тайминги",
                values = listOf("CL14", "CL16", "CL18", "CL19", "CL20", "CL22", "CL24", "CL28", "CL30", "CL32", "CL34", "CL36", "CL38", "CL40")
            ),
            CategoryFilterOption(
                key = "voltage",
                displayName = "Напряжение",
                values = listOf("1.2V", "1.35V", "1.5V")
            )
        )
    }

    private fun getMotherboardFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "motherboardSocket",
                displayName = "Сокет",
                values = listOf("LGA 1151", "LGA 1200", "LGA 1700", "AM4", "AM5", "sTRX4", "TRX40")
            ),
            CategoryFilterOption(
                key = "chipset",
                displayName = "Чипсет",
                values = listOf("Intel: H310", "Intel: B360", "Intel: B365", "Intel: H370", "Intel: Z370", "Intel: Z390",
                    "Intel: H410", "Intel: B460", "Intel: H470", "Intel: Z490",
                    "Intel: H510", "Intel: B560", "Intel: H570", "Intel: Z590",
                    "Intel: H610", "Intel: B660", "Intel: H670", "Intel: Z690", "Intel: Z790",
                    "AMD: A320", "AMD: B350", "AMD: X370",
                    "AMD: B450", "AMD: X470",
                    "AMD: A520", "AMD: B550", "AMD: X570",
                    "AMD: B650", "AMD: B650E", "AMD: X670", "AMD: X670E")
            ),
            CategoryFilterOption(
                key = "formFactor",
                displayName = "Форм-фактор",
                values = listOf("Mini-ITX", "Micro-ATX", "ATX", "E-ATX", "XL-ATX")
            ),
            CategoryFilterOption(
                key = "memorySlots",
                displayName = "Слоты памяти",
                values = listOf("2 слота", "4 слота", "8 слотов")
            ),
            CategoryFilterOption(
                key = "sataPorts",
                displayName = "SATA порты",
                values = listOf("2 порта", "4 порта", "6 портов", "8 портов")
            ),
            CategoryFilterOption(
                key = "m2Slots",
                displayName = "M.2 слоты",
                values = listOf("1 слот", "2 слота", "3 слота", "4 слота", "5 слотов")
            )
        )
    }

    private fun getStorageFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "storageType",
                displayName = "Тип накопителя",
                values = listOf("HDD 5400 RPM", "HDD 7200 RPM", "SSD SATA", "SSD M.2 SATA", "SSD M.2 NVMe", "SSD PCIe")
            ),
            CategoryFilterOption(
                key = "storageCapacity",
                displayName = "Объем",
                values = listOf("128 GB", "256 GB", "512 GB", "1 TB", "2 TB", "4 TB", "8 TB", "16 TB", "18 TB", "20 TB")
            ),
            CategoryFilterOption(
                key = "readSpeed",
                displayName = "Скорость чтения",
                values = listOf("до 200 MB/s", "200-500 MB/s", "500-1000 MB/s", "1000-3000 MB/s",
                    "3000-5000 MB/s", "5000-7000 MB/s", "7000-10000 MB/s", "от 10000 MB/s")
            ),
            CategoryFilterOption(
                key = "writeSpeed",
                displayName = "Скорость записи",
                values = listOf("до 200 MB/s", "200-500 MB/s", "500-1000 MB/s", "1000-3000 MB/s",
                    "3000-5000 MB/s", "5000-7000 MB/s", "7000-10000 MB/s", "от 10000 MB/s")
            ),
            CategoryFilterOption(
                key = "interfaceType",
                displayName = "Интерфейс",
                values = listOf("SATA II", "SATA III", "M.2 SATA", "M.2 NVMe PCIe 3.0", "M.2 NVMe PCIe 4.0", "M.2 NVMe PCIe 5.0", "U.2", "PCIe")
            )
        )
    }

    private fun getPowerSupplyFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "power",
                displayName = "Мощность",
                values = listOf("450W", "550W", "650W", "750W", "850W", "1000W", "1200W", "1500W", "1600W", "2000W")
            ),
            CategoryFilterOption(
                key = "psuFormat",
                displayName = "Форм-фактор",
                values = listOf("ATX", "SFX", "SFX-L", "TFX", "Flex ATX")
            ),
            CategoryFilterOption(
                key = "efficiency",
                displayName = "Сертификат 80 PLUS",
                values = listOf("80+", "80+ Bronze", "80+ Silver", "80+ Gold", "80+ Platinum", "80+ Titanium")
            ),
            CategoryFilterOption(
                key = "modular",
                displayName = "Модульность",
                values = listOf("Немодульный", "Полумодульный", "Полностью модульный")
            )
        )
    }

    private fun getCaseFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "caseFormat",
                displayName = "Форм-фактор",
                values = listOf("Mini-Tower", "Mid-Tower", "Full-Tower", "Super-Tower", "Mini-ITX", "Micro-ATX", "ATX", "E-ATX")
            ),
            CategoryFilterOption(
                key = "dimensions",
                displayName = "Размеры",
                values = listOf("Компактный", "Средний", "Большой", "Очень большой")
            ),
            CategoryFilterOption(
                key = "material",
                displayName = "Материал",
                values = listOf("Сталь", "Алюминий", "Закаленное стекло", "Акрил", "Комбинированный")
            ),
            CategoryFilterOption(
                key = "fansIncluded",
                displayName = "Вентиляторы в комплекте",
                values = listOf("Нет", "1 вентилятор", "2 вентилятора", "3 вентилятора", "4+ вентиляторов")
            )
        )
    }

    private fun getCoolingFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "coolingType",
                displayName = "Тип охлаждения",
                values = listOf("Воздушное", "Жидкостное AIO", "Кастомная СВО", "Пассивное", "Гибридное")
            ),
            CategoryFilterOption(
                key = "radiatorSize",
                displayName = "Размер радиатора",
                values = listOf("120 мм", "140 мм", "240 мм", "280 мм", "360 мм", "420 мм", "480 мм")
            ),
            CategoryFilterOption(
                key = "fanSpeed",
                displayName = "Скорость вентиляторов",
                values = listOf("до 1000 RPM", "1000-1500 RPM", "1500-2000 RPM", "2000-2500 RPM", "от 2500 RPM")
            ),
            CategoryFilterOption(
                key = "noiseLevel",
                displayName = "Уровень шума",
                values = listOf("до 20 дБ", "20-30 дБ", "30-40 дБ", "40-50 дБ", "от 50 дБ")
            )
        )
    }

    private fun getReadyPCFilters(): List<CategoryFilterOption> {
        return listOf(
            CategoryFilterOption(
                key = "processor",
                displayName = "Процессор",
                values = listOf("Intel Core i3", "Intel Core i5", "Intel Core i7", "Intel Core i9",
                    "AMD Ryzen 3", "AMD Ryzen 5", "AMD Ryzen 7", "AMD Ryzen 9", "AMD Threadripper")
            ),
            CategoryFilterOption(
                key = "motherboard",
                displayName = "Материнская плата",
                values = listOf("Бюджетная", "Среднего класса", "Игровая", "Профессиональная", "Премиум")
            ),
            CategoryFilterOption(
                key = "ram",
                displayName = "Оперативная память",
                values = listOf("8 GB", "16 GB", "32 GB", "64 GB", "128 GB", "256 GB")
            ),
            CategoryFilterOption(
                key = "graphics",
                displayName = "Видеокарта",
                values = listOf("Встроенная", "NVIDIA GTX 16xx", "NVIDIA RTX 3060", "NVIDIA RTX 3070",
                    "NVIDIA RTX 3080", "NVIDIA RTX 3090", "NVIDIA RTX 4070", "NVIDIA RTX 4080",
                    "NVIDIA RTX 4090", "AMD RX 6600", "AMD RX 6700", "AMD RX 6800",
                    "AMD RX 6900", "AMD RX 7800", "AMD RX 7900")
            ),
            CategoryFilterOption(
                key = "storage",
                displayName = "Накопитель",
                values = listOf("256 GB SSD", "512 GB SSD", "1 TB SSD", "2 TB SSD", "1 TB HDD + 256 GB SSD",
                    "2 TB HDD + 512 GB SSD", "4 TB HDD + 1 TB SSD", "Только SSD", "Только HDD", "Гибрид")
            ),
            CategoryFilterOption(
                key = "powerSupply",
                displayName = "Блок питания",
                values = listOf("500W", "650W", "750W", "850W", "1000W", "1200W+")
            ),
            CategoryFilterOption(
                key = "pcCase",
                displayName = "Корпус",
                values = listOf("Базовый", "Игровой", "Профессиональный", "Премиум", "Кастомный")
            ),
            CategoryFilterOption(
                key = "cooling",
                displayName = "Охлаждение",
                values = listOf("Стандартное", "Улучшенное", "Жидкостное", "Кастомное")
            ),
            CategoryFilterOption(
                key = "os",
                displayName = "Операционная система",
                values = listOf("Windows 10", "Windows 11", "Linux", "Без ОС", "Другая")
            )
        )
    }
}