package com.example.one_tech

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var product: Product
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var isAdminMode = false

    // Views
    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var inStockText: TextView
    private lateinit var addToCartButton: Button
    private lateinit var editButton: Button
    private lateinit var specsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // Получаем флаг режима админа из Intent
        isAdminMode = intent.getBooleanExtra("admin_mode", false)

        initViews()
        setupClickListeners()

        // Настраиваем интерфейс в зависимости от режима
        if (isAdminMode) {
            setupAdminMode()
        } else {
            setupUserMode()
        }

        // Получаем productId из Intent
        val productId = intent.getStringExtra("product_id")
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "Ошибка загрузки товара", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProductDetails(productId)
    }

    private fun initViews() {
        productImage = findViewById(R.id.productImage)
        productName = findViewById(R.id.productName)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)
        inStockText = findViewById(R.id.inStockText)
        addToCartButton = findViewById(R.id.addToCartButton)
        editButton = findViewById(R.id.editButton)
        specsContainer = findViewById(R.id.specsContainer)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupAdminMode() {
        // Для админа: показываем кнопку редактирования, скрываем кнопку корзины
        addToCartButton.visibility = View.GONE
        editButton.visibility = View.VISIBLE
        editButton.text = "РЕДАКТИРОВАТЬ ТОВАР"
    }

    private fun setupUserMode() {
        // Для пользователя: показываем кнопку корзины, скрываем кнопку редактирования
        addToCartButton.visibility = View.VISIBLE
        editButton.visibility = View.GONE
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        // Обработчик кнопки добавления в корзину (только для пользователей)
        addToCartButton.setOnClickListener {
            addToCart()
        }

        // Обработчик кнопки редактирования (только для админов)
        editButton.setOnClickListener {
            // Переход в режим редактирования
            val intent = Intent(this, EditProductActivity::class.java).apply {
                putExtra("product_id", product.id)
                putExtra("category_name", product.category)
                putExtra("admin_mode", true)
            }
            startActivity(intent)
        }
    }

    private fun loadProductDetails(productId: String) {
        showLoading(true)

        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    product = document.toObject(Product::class.java) ?: Product()
                    displayProductDetails()
                } else {
                    Toast.makeText(this, "Товар не найден", Toast.LENGTH_SHORT).show()
                    finish()
                }
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Ошибка загрузки товара: ${exception.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun displayProductDetails() {
        // Основная информация
        productName.text = product.name
        productPrice.text = "${String.format("%,.0f", product.price)} ₽"
        productDescription.text = product.description

        // Наличие
        val stockText = if (product.inStock) {
            "✅ В наличии (${product.stock} шт.)"
        } else {
            "❌ Нет в наличии"
        }
        inStockText.text = stockText

        // Загрузка изображения
        if (product.images.isNotEmpty() && product.images[0].isNotBlank()) {
            productImage.setImageResource(R.drawable.placeholder_image)
        } else {
            productImage.setImageResource(R.drawable.placeholder_image)
        }

        // Отображение всей информации
        displayAllInformation()
    }

    private fun displayAllInformation() {
        specsContainer.removeAllViews()

        // Проверяем, есть ли информация
        if (product.specs.isEmpty() &&
            product.manufacturer.isEmpty() &&
            product.model.isEmpty() &&
            product.series.isEmpty()) {

            val emptyInfo = TextView(this).apply {
                text = "Информация о товаре не указана"
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
            }
            specsContainer.addView(emptyInfo)
            return
        }

        // Добавляем заголовок "Характеристики"
        val header = TextView(this).apply {
            text = "Характеристики"
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 16.dpToPx(), 0, 16.dpToPx())
        }
        specsContainer.addView(header)

        // Сначала добавляем общую информацию
        val generalInfo = listOf(
            "Производитель" to product.manufacturer,
            "Модель" to product.model,
            "Серия" to product.series,
            "Категория" to product.category
        )

        generalInfo.forEach { (label, value) ->
            if (value.isNotBlank()) {
                addSpecRow(label, value)
            }
        }

        // Добавляем разделитель
        if (product.specs.isNotEmpty() &&
            (product.manufacturer.isNotBlank() || product.model.isNotBlank() || product.series.isNotBlank())) {

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    topMargin = 8.dpToPx()
                    bottomMargin = 8.dpToPx()
                }
                setBackgroundColor(ContextCompat.getColor(this@ProductDetailsActivity, android.R.color.darker_gray))
            }
            specsContainer.addView(divider)
        }

        // Затем добавляем технические характеристики
        product.specs.forEach { (key, value) ->
            if (value.isNotBlank()) {
                val displayName = getDisplayNameForSpec(key)
                addSpecRow(displayName, value)
            }
        }

        // Если нет ни общей информации, ни характеристик
        if (specsContainer.childCount == 1) { // только заголовок
            val emptySpecs = TextView(this).apply {
                text = "Нет информации"
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
            }
            specsContainer.addView(emptySpecs)
        }
    }

    private fun addSpecRow(label: String, value: String) {
        val specView = layoutInflater.inflate(R.layout.item_spec, specsContainer, false)

        val specName = specView.findViewById<TextView>(R.id.specName)
        val specValue = specView.findViewById<TextView>(R.id.specValue)

        specName.text = "$label:"
        specValue.text = value

        specsContainer.addView(specView)
    }

    private fun getDisplayNameForSpec(key: String): String {
        return when (key) {
            // Видеокарты
            "memory" -> "Объем видеопамяти"
            "memoryType" -> "Тип памяти"
            "gpuClock" -> "Частота GPU"
            "memoryClock" -> "Частота памяти"
            "connectors" -> "Разъемы"
            "busWidth" -> "Разрядность шины"

            // Процессоры
            "socket" -> "Сокет"
            "cores" -> "Количество ядер"
            "threads" -> "Количество потоков"
            "frequency" -> "Базовая частота"
            "maxFrequency" -> "Максимальная частота"
            "cache" -> "Кэш-память"
            "tdp" -> "TDP"

            // Память
            "memoryFormat" -> "Тип памяти"
            "memoryCapacity" -> "Объем"
            "memoryFrequency" -> "Частота"
            "timings" -> "Тайминги"
            "voltage" -> "Напряжение"

            // Материнские платы
            "motherboardSocket" -> "Сокет"
            "chipset" -> "Чипсет"
            "formFactor" -> "Форм-фактор"
            "memorySlots" -> "Слоты памяти"
            "sataPorts" -> "SATA порты"
            "m2Slots" -> "M.2 слоты"

            // Накопители
            "storageType" -> "Тип накопителя"
            "storageCapacity" -> "Объем"
            "readSpeed" -> "Скорость чтения"
            "writeSpeed" -> "Скорость записи"
            "interfaceType" -> "Интерфейс"

            // Блоки питания
            "power" -> "Мощность"
            "psuFormat" -> "Форм-фактор"
            "efficiency" -> "Сертификат 80 PLUS"
            "modular" -> "Модульность"

            // Корпуса
            "caseFormat" -> "Форм-фактор"
            "dimensions" -> "Размеры"
            "material" -> "Материал"
            "fansIncluded" -> "Вентиляторы в комплекте"

            // Охлаждение
            "coolingType" -> "Тип охлаждения"
            "radiatorSize" -> "Размер радиатора"
            "fanSpeed" -> "Скорость вентиляторов"
            "noiseLevel" -> "Уровень шума"

            // Готовые ПК
            "processor" -> "Процессор"
            "motherboard" -> "Материнская плата"
            "ram" -> "Оперативная память"
            "graphics" -> "Видеокарта"
            "storage" -> "Накопитель"
            "powerSupply" -> "Блок питания"
            "pcCase" -> "Корпус"
            "cooling" -> "Охлаждение"
            "os" -> "Операционная система"

            // Дефолтные
            "spec1" -> "Основные характеристики"
            "spec2" -> "Дополнительные характеристики"

            else -> key.replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun addToCart() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Войдите в аккаунт чтобы добавить в корзину", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, есть ли уже этот товар в корзине
        db.collection("cart")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("productId", product.id)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Товара нет в корзине - добавляем новый
                    val cartItem = hashMapOf(
                        "productId" to product.id,
                        "productName" to product.name,
                        "productPrice" to product.price,
                        "productImage" to (product.images.firstOrNull() ?: ""),
                        "quantity" to 1,
                        "category" to product.category,
                        "userId" to currentUser.uid,
                        "addedAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("cart")
                        .add(cartItem)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Товар добавлен в корзину!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Ошибка добавления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Товар уже в корзине - увеличиваем количество
                    val document = documents.documents[0]
                    val currentQuantity = document.getLong("quantity")?.toInt() ?: 1

                    db.collection("cart").document(document.id)
                        .update("quantity", currentQuantity + 1)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Количество товара увеличено!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Ошибка проверки корзины: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}