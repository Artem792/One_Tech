package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CartActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private lateinit var cartAdapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var checkoutButton: TextView
    private lateinit var loadingLayout: LinearLayout

    private val TAG = "CartActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupClickListeners()
        setupAiAssistantButton()
        updateBottomNavigation()
        loadCartItems()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.cartRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        totalPriceText = findViewById(R.id.totalPriceText)
        checkoutButton = findViewById(R.id.checkoutButton)
        loadingLayout = findViewById(R.id.loadingLayout)

        cartAdapter = CartAdapter(emptyList(),
            onQuantityChange = { cartItem, newQuantity ->
                updateCartItemQuantity(cartItem, newQuantity)
            },
            onRemoveItem = { cartItem ->
                removeFromCart(cartItem)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = cartAdapter
    }

    private fun loadCartItems() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState(true)
            return
        }

        showLoading(true)

        Log.d(TAG, "🔄 Загрузка корзины для пользователя: ${currentUser.uid}")

        db.collection("cart")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "📊 Найдено документов в корзине: ${documents.size()}")

                val cartItems = mutableListOf<CartItem>()
                for (document in documents) {
                    try {
                        val productId = document.getString("productId") ?: ""
                        val productName = document.getString("productName") ?: ""
                        val quantity = document.getLong("quantity")?.toInt() ?: 1

                        Log.d(TAG, "📦 Товар: $productName, ID: $productId, Количество: $quantity")

                        val cartItem = CartItem(
                            id = document.id,
                            productId = productId,
                            productName = productName,
                            productPrice = document.getDouble("productPrice") ?: 0.0,
                            productImage = document.getString("productImage") ?: "",
                            quantity = quantity,
                            category = document.getString("category") ?: "",
                            userId = document.getString("userId") ?: ""
                        )
                        cartItems.add(cartItem)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Ошибка парсинга товара корзины ${document.id}: ${e.message}")
                    }
                }

                Log.d(TAG, "✅ Загружено товаров: ${cartItems.size}")

                cartAdapter.updateCartItems(cartItems)
                showLoading(false)
                updateTotalPrice(cartItems)

                if (cartItems.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "❌ Ошибка загрузки корзины: ${exception.message}")
                Toast.makeText(this, "Ошибка загрузки корзины: ${exception.message}", Toast.LENGTH_LONG).show()
                showEmptyState(true)
            }
    }

    private fun updateCartItemQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItem)
            return
        }

        db.collection("cart").document(cartItem.id)
            .update("quantity", newQuantity)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Количество обновлено: ${cartItem.productName} -> $newQuantity")
                loadCartItems() // Перезагружаем чтобы обновить общую сумму
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка обновления количества: ${e.message}")
                Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                loadCartItems() // Перезагружаем чтобы откатить изменения
            }
    }

    private fun removeFromCart(cartItem: CartItem) {
        db.collection("cart").document(cartItem.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Товар удален из корзины: ${cartItem.productName}")
                Toast.makeText(this, "Товар удален из корзины", Toast.LENGTH_SHORT).show()
                loadCartItems()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка удаления: ${e.message}")
                Toast.makeText(this, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processOrder() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Войдите в аккаунт для оформления заказа", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Получаем все товары из корзины пользователя
        db.collection("cart")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Удаляем все товары из корзины
                val batch = db.batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        showLoading(false)
                        Log.d(TAG, "✅ Заказ оформлен! Товары удалены из корзины")
                        Toast.makeText(this, "✅ Заказ успешно оформлен! Товары удалены из корзины", Toast.LENGTH_LONG).show()
                        loadCartItems() // Обновляем интерфейс
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Ошибка оформления заказа: ${e.message}")
                        Toast.makeText(this, "❌ Ошибка оформления заказа: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "❌ Ошибка получения корзины: ${e.message}")
                Toast.makeText(this, "❌ Ошибка получения корзины: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateTotalPrice(cartItems: List<CartItem>) {
        val total = cartItems.sumOf { it.productPrice * it.quantity }
        totalPriceText.text = "Итого: ${String.format("%,.0f", total)} ₽"
        Log.d(TAG, "💰 Общая сумма: $total ₽")
    }

    private fun showLoading(show: Boolean) {
        loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            totalPriceText.visibility = View.GONE
            checkoutButton.visibility = View.GONE
        } else {
            totalPriceText.visibility = View.VISIBLE
            checkoutButton.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        findViewById<LinearLayout>(R.id.navCatalog).setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            // Уже в корзине
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        checkoutButton.setOnClickListener {
            processOrder()
        }
    }

    private fun setupAiAssistantButton() {
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateBottomNavigation() {
        val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
        val navCart = findViewById<LinearLayout>(R.id.navCart)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        resetNavigationColors()

        val cartText = navCart.getChildAt(1) as TextView
        cartText.setTextColor(resources.getColor(android.R.color.white, theme))
    }

    private fun resetNavigationColors() {
        val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
        val navCart = findViewById<LinearLayout>(R.id.navCart)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        val catalogText = navCatalog.getChildAt(1) as TextView
        val cartText = navCart.getChildAt(1) as TextView
        val profileText = navProfile.getChildAt(1) as TextView

        catalogText.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        cartText.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        profileText.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
    }
}