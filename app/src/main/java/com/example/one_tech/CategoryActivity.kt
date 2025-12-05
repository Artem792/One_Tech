package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CategoryActivity : AppCompatActivity() {

    private var isAdminMode = false
    private var categoryName = ""
    private lateinit var productsAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var addProductButton: TextView
    private lateinit var aiAssistantButton: TextView
    private lateinit var bottomNavigation: LinearLayout
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Получаем данные из Intent
        categoryName = intent.getStringExtra("category_name") ?: "Категория"
        isAdminMode = intent.getBooleanExtra("admin_mode", false)

        // Инициализация Views
        initViews()

        // Настраиваем обработчик кнопки "Назад"
        setupBackPressedHandler()

        // Устанавливаем заголовок категории
        setupCategoryTitle(categoryName)
        setupBackButton()
        setupFilterButton()

        // Настраиваем RecyclerView
        setupRecyclerView()

        // Настраиваем интерфейс в зависимости от режима
        if (isAdminMode) {
            setupAdminMode()
        } else {
            setupNormalUserMode()
        }

        // Загружаем товары
        loadProducts()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.productsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)
        addProductButton = findViewById(R.id.addProductButton)
        aiAssistantButton = findViewById(R.id.aiAssistantButton)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdminMode) {
                    // Если это режим админа - возвращаемся в админ каталог
                    val intent = Intent(this@CategoryActivity, CatalogActivity::class.java)
                    intent.putExtra("admin_mode", true)
                    startActivity(intent)
                    finish()
                } else {
                    // Для обычных пользователей - стандартное поведение
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupAdminMode() {
        // Скрываем нижнюю навигацию
        bottomNavigation.visibility = View.GONE

        // Скрываем кнопку ИИ-помощника и ПОКАЗЫВАЕМ кнопку добавления
        aiAssistantButton.visibility = View.GONE
        addProductButton.visibility = View.VISIBLE

        // Настраиваем обработчик кнопки добавления
        addProductButton.setOnClickListener {
            openAddProductActivity()
        }
    }

    private fun setupNormalUserMode() {
        setupClickListeners()
        setupAiAssistantButton()

        // Скрываем кнопку добавления для обычных пользователей
        addProductButton.visibility = View.GONE
        aiAssistantButton.visibility = View.VISIBLE
    }

    private fun setupCategoryTitle(categoryName: String) {
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = categoryName
    }

    private fun setupBackButton() {
        val backButton = findViewById<TextView>(R.id.backButton)
        backButton.setOnClickListener {
            if (isAdminMode) {
                // Если это режим админа - возвращаемся в админ каталог
                val intent = Intent(this, CatalogActivity::class.java)
                intent.putExtra("admin_mode", true)
                startActivity(intent)
                finish()
            } else {
                finish() // Возврат назад к каталогу для обычных пользователей
            }
        }
    }

    private fun setupFilterButton() {
        val filterButton = findViewById<TextView>(R.id.filterButton)
        filterButton.setOnClickListener {
            // Здесь можно добавить логику для фильтров
            Toast.makeText(this, "Фильтры - в разработке", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Обработчики для нижней навигации (только для обычных пользователей)
        findViewById<LinearLayout>(R.id.navCatalog)?.setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navCart)?.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupAiAssistantButton() {
        aiAssistantButton.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductAdapter(emptyList(),
            onItemClick = { product ->
                if (isAdminMode) {
                    // Редактирование товара для админа
                    openEditProductActivity(product)
                } else {
                    // Просмотр товара для пользователя
                    openProductDetailsActivity(product)
                }
            },
            onAddToCartClick = { product ->
                addToCart(product)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productsAdapter
    }

    private fun loadProducts() {
        showLoading(true)

        db.collection("products")
            .whereEqualTo("category", categoryName)
            .get()
            .addOnSuccessListener { documents ->
                val productsList = mutableListOf<Product>()
                for (document in documents) {
                    try {
                        // Firebase автоматически заполнит поле id благодаря @DocumentId
                        val product = document.toObject(Product::class.java)
                        productsList.add(product)
                    } catch (e: Exception) {
                        println("Ошибка парсинга товара ${document.id}: ${e.message}")
                    }
                }

                // Сортируем по дате создания (новые сначала)
                val sortedProducts = productsList.sortedByDescending { it.createdAt }
                productsAdapter.updateProducts(sortedProducts)
                showLoading(false)

                if (sortedProducts.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Ошибка загрузки товаров: ${exception.message}", Toast.LENGTH_LONG).show()
                showEmptyState(true)
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
        }
    }

    private fun openAddProductActivity() {
        try {
            val intent = Intent(this, AddProductActivity::class.java).apply {
                putExtra("category_name", categoryName)
                putExtra("admin_mode", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка открытия формы добавления: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun openEditProductActivity(product: Product) {
        // TODO: Создать активность для редактирования товара
        Toast.makeText(this, "Редактирование: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun openProductDetailsActivity(product: Product) {
        val intent = Intent(this, ProductDetailsActivity::class.java)
        intent.putExtra("product_id", product.id)
        startActivity(intent)
    }

    private fun addToCart(product: Product) {
        if (isAdminMode) {
            Toast.makeText(this, "Администраторы не могут добавлять товары в корзину", Toast.LENGTH_SHORT).show()
            return
        }

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

    // Обновляем список при возврате на экран
    override fun onResume() {
        super.onResume()
        // Обновляем список товаров при возврате на экран
        loadProducts()
    }
}