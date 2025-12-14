package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import java.util.Date

class AddProductActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var categoryName = ""
    private val TAG = "AddProductActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        Log.d(TAG, "=== ЗАПУСК AddProductActivity ===")

        // Получаем название категории из интента
        categoryName = intent.getStringExtra("category_name") ?: "Категория"
        val adminMode = intent.getBooleanExtra("admin_mode", false)

        Log.d(TAG, "Категория: $categoryName, Режим админа: $adminMode")

        if (!adminMode) {
            Toast.makeText(this, "Доступ запрещен", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Проверяем аутентификацию
        if (auth.currentUser == null) {
            Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupClickListeners()
        setupDynamicFields()
    }

    private fun setupUI() {
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "Добавить товар в $categoryName"
    }

    private fun setupDynamicFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()

        Log.d(TAG, "Настройка полей для: $categoryName")

        when (categoryName) {
            "Видеокарты" -> setupGraphicsCardFields()
            "Процессоры" -> setupProcessorFields()
            "Память" -> setupMemoryFields()
            "Материнские платы" -> setupMotherboardFields()
            "Накопители" -> setupStorageFields()
            "Блоки питания" -> setupPowerSupplyFields()
            "Корпуса" -> setupCaseFields()
            "Охлаждение" -> setupCoolingFields()
            "Готовые ПК" -> setupReadyPCFields()
            else -> setupDefaultFields()
        }
    }

    private fun setupGraphicsCardFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Объем видеопамяти (GB)", "memory")
        addSpecField(dynamicLayout, "Тип памяти", "memoryType")
        addSpecField(dynamicLayout, "Частота GPU (MHz)", "gpuClock")
        addSpecField(dynamicLayout, "Частота памяти (MHz)", "memoryClock")
        addSpecField(dynamicLayout, "Разъемы", "connectors")
        addSpecField(dynamicLayout, "Разрядность шины (бит)", "busWidth")
    }

    private fun setupProcessorFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Сокет", "socket")
        addSpecField(dynamicLayout, "Количество ядер", "cores")
        addSpecField(dynamicLayout, "Количество потоков", "threads")
        addSpecField(dynamicLayout, "Базовая частота (GHz)", "frequency")
        addSpecField(dynamicLayout, "Макс. частота (GHz)", "maxFrequency")
        addSpecField(dynamicLayout, "Кэш-память (MB)", "cache")
        addSpecField(dynamicLayout, "TDP (Вт)", "tdp")
    }

    private fun setupMemoryFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Тип памяти", "memoryFormat")
        addSpecField(dynamicLayout, "Объем (GB)", "memoryCapacity")
        addSpecField(dynamicLayout, "Частота (MHz)", "memoryFrequency")
        addSpecField(dynamicLayout, "Тайминги", "timings")
        addSpecField(dynamicLayout, "Напряжение (V)", "voltage")
    }

    private fun setupMotherboardFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Сокет", "motherboardSocket")
        addSpecField(dynamicLayout, "Чипсет", "chipset")
        addSpecField(dynamicLayout, "Форм-фактор", "formFactor")
        addSpecField(dynamicLayout, "Слоты памяти", "memorySlots")
        addSpecField(dynamicLayout, "SATA порты", "sataPorts")
        addSpecField(dynamicLayout, "M.2 слоты", "m2Slots")
    }

    private fun setupStorageFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Тип накопителя", "storageType")
        addSpecField(dynamicLayout, "Объем (GB/TB)", "storageCapacity")
        addSpecField(dynamicLayout, "Скорость чтения (MB/s)", "readSpeed")
        addSpecField(dynamicLayout, "Скорость записи (MB/s)", "writeSpeed")
        addSpecField(dynamicLayout, "Интерфейс", "interfaceType")
    }

    private fun setupPowerSupplyFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Мощность (Вт)", "power")
        addSpecField(dynamicLayout, "Форм-фактор", "psuFormat")
        addSpecField(dynamicLayout, "Сертификат 80 PLUS", "efficiency")
        addSpecField(dynamicLayout, "Модульность", "modular")
    }

    private fun setupCaseFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Форм-фактор", "caseFormat")
        addSpecField(dynamicLayout, "Размеры (мм)", "dimensions")
        addSpecField(dynamicLayout, "Материал", "material")
        addSpecField(dynamicLayout, "Вентиляторы в комплекте", "fansIncluded")
    }

    private fun setupCoolingFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Тип охлаждения", "coolingType")
        addSpecField(dynamicLayout, "Размер радиатора (мм)", "radiatorSize")
        addSpecField(dynamicLayout, "Скорость вентиляторов (RPM)", "fanSpeed")
        addSpecField(dynamicLayout, "Уровень шума (дБ)", "noiseLevel")
    }

    private fun setupReadyPCFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Процессор", "processor")
        addSpecField(dynamicLayout, "Материнская плата", "motherboard")
        addSpecField(dynamicLayout, "Оперативная память", "ram")
        addSpecField(dynamicLayout, "Видеокарта", "graphics")
        addSpecField(dynamicLayout, "Накопитель", "storage")
        addSpecField(dynamicLayout, "Блок питания", "powerSupply")
        addSpecField(dynamicLayout, "Корпус", "pcCase")
        addSpecField(dynamicLayout, "Охлаждение", "cooling")
        addSpecField(dynamicLayout, "Операционная система", "os")
    }

    private fun setupDefaultFields() {
        val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)
        dynamicLayout.removeAllViews()
        addSpecField(dynamicLayout, "Основные характеристики", "spec1")
        addSpecField(dynamicLayout, "Дополнительные характеристики", "spec2")
    }

    private fun addSpecField(parent: LinearLayout, hint: String, fieldId: String) {
        val inputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            setBoxCornerRadii(12f, 12f, 12f, 12f)
            boxStrokeColor = ContextCompat.getColor(this@AddProductActivity, android.R.color.darker_gray)
            setHintTextColor(ContextCompat.getColorStateList(this@AddProductActivity, android.R.color.darker_gray))
            this.hint = hint
        }

        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColor(this@AddProductActivity, android.R.color.white))
            setBackgroundColor(0x1AFFFFFF)
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            tag = fieldId
        }

        inputLayout.addView(editText)
        parent.addView(inputLayout)
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveProduct()
        }
    }

    private fun saveProduct() {
        try {
            Log.d(TAG, "=== НАЧАЛО СОХРАНЕНИЯ ===")

            // Получаем данные
            val name = findViewById<EditText>(R.id.nameInput).text.toString().trim()
            val priceText = findViewById<EditText>(R.id.priceInput).text.toString().trim()
            val description = findViewById<EditText>(R.id.descriptionInput).text.toString().trim()
            val manufacturer = findViewById<EditText>(R.id.manufacturerInput).text.toString().trim()
            val model = findViewById<EditText>(R.id.modelInput).text.toString().trim()
            val series = findViewById<EditText>(R.id.seriesInput).text.toString().trim()
            val stockText = findViewById<EditText>(R.id.stockInput).text.toString().trim()

            // Базовая валидация
            if (name.isEmpty() || priceText.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Заполните обязательные поля: название, цена, описание",
                    Toast.LENGTH_LONG).show()
                return
            }

            val price = try {
                priceText.replace(",", ".").toDouble()
            } catch (e: Exception) {
                Toast.makeText(this, "Введите корректную цену (например: 19999.99)",
                    Toast.LENGTH_SHORT).show()
                return
            }

            val stock = try {
                if (stockText.isEmpty()) 0 else stockText.toInt()
            } catch (e: Exception) {
                0
            }

            // Проверяем пользователя
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "❌ Вы не авторизованы", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            Log.d(TAG, "Пользователь: ${currentUser.uid}, Email: ${currentUser.email}")

            // Собираем характеристики
            val specs = HashMap<String, String>()
            val dynamicLayout = findViewById<LinearLayout>(R.id.dynamicSpecsLayout)

            for (i in 0 until dynamicLayout.childCount) {
                val child = dynamicLayout.getChildAt(i)
                if (child is TextInputLayout) {
                    val editText = child.editText as? TextInputEditText
                    val fieldId = editText?.tag as? String
                    val value = editText?.text.toString().trim()

                    if (!fieldId.isNullOrEmpty() && value.isNotEmpty()) {
                        specs[fieldId] = value
                    }
                }
            }

            // Создаем объект товара (ПРОСТОЙ ВАРИАНТ)
            val product = hashMapOf<String, Any>(
                "name" to name,
                "price" to price,
                "description" to description,
                "category" to categoryName,
                "manufacturer" to manufacturer,
                "model" to model,
                "series" to series,
                "stock" to stock,
                "inStock" to (stock > 0),
                "createdAt" to FieldValue.serverTimestamp(),
                "createdBy" to currentUser.uid,
                "images" to listOf<String>(),
                "rating" to 0.0,
                "reviewCount" to 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Добавляем характеристики, если они есть
            if (specs.isNotEmpty()) {
                product["specs"] = specs
            }

            Log.d(TAG, "Сохранение товара: $name, цена: $price")

            // Блокируем кнопку
            val saveButton = findViewById<Button>(R.id.saveButton)
            saveButton.isEnabled = false
            saveButton.text = "Сохранение..."

            // Сохраняем в Firestore
            db.collection("products")
                .add(product)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "✅ Товар успешно добавлен! ID: ${documentReference.id}")

                    Toast.makeText(this, "✅ Товар успешно добавлен!",
                        Toast.LENGTH_SHORT).show()

                    // Возвращаемся обратно
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Ошибка Firestore: ${e.message}", e)

                    // Анализируем ошибку
                    val errorMsg = when {
                        e.message?.contains("PERMISSION_DENIED") == true -> {
                            "❌ Нет прав. Обновите правила Firestore!"
                        }
                        e.message?.contains("network") == true -> {
                            "❌ Ошибка сети. Проверьте интернет"
                        }
                        else -> "❌ Ошибка: ${e.localizedMessage}"
                    }

                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()

                    // Разблокируем кнопку
                    saveButton.isEnabled = true
                    saveButton.text = "Сохранить"
                }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Неожиданная ошибка", e)
            Toast.makeText(this, "❌ Ошибка: ${e.localizedMessage}",
                Toast.LENGTH_LONG).show()

            val saveButton = findViewById<Button>(R.id.saveButton)
            saveButton.isEnabled = true
            saveButton.text = "Сохранить"
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}