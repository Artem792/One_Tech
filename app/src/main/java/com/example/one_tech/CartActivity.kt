package com.example.one_tech

import android.text.Html
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.ClipboardManager
import android.content.ClipData
import java.util.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

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

        // Проверяем, является ли пользователь гостем
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val isGuest = document.getBoolean("isGuest") ?: false

                if (isGuest) {
                    // Гость не может оформлять заказы - показываем диалог
                    showGuestCannotOrderDialog(currentUser.uid)
                } else {
                    // Обычный пользователь - оформляем заказ
                    proceedWithOrder(currentUser.uid)
                }
            }
            .addOnFailureListener {
                // Если не можем проверить, считаем обычным пользователем
                proceedWithOrder(currentUser.uid)
            }
    }

    private fun showGuestCannotOrderDialog(userId: String) {
        AlertDialog.Builder(this)
            .setTitle("Оформление заказа")
            .setMessage("Гостевой режим позволяет только добавлять товары в корзину. Для оформления заказа зарегистрируйтесь.")
            .setPositiveButton("Зарегистрироваться") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Продолжить как гость") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }

    // ОСНОВНОЙ МЕТОД ОФОРМЛЕНИЯ ЗАКАЗА
    private fun proceedWithOrder(userId: String) {
        showLoading(true)

        // Получаем все товары из корзины пользователя
        db.collection("cart")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Создаем заказ
                val orderItems = mutableListOf<Map<String, Any>>()
                var totalAmount = 0.0

                for (document in documents) {
                    val item = hashMapOf<String, Any>(
                        "productId" to (document.getString("productId") ?: ""),
                        "productName" to (document.getString("productName") ?: ""),
                        "quantity" to (document.getLong("quantity") ?: 1),
                        "price" to (document.getDouble("productPrice") ?: 0.0)
                    )
                    orderItems.add(item)
                    totalAmount += (document.getDouble("productPrice") ?: 0.0) * (document.getLong("quantity")?.toInt() ?: 1)
                }

                // Создаем документ заказа
                val orderData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "items" to orderItems,
                    "totalAmount" to totalAmount,
                    "status" to "pending",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                // Сохраняем заказ
                db.collection("orders")
                    .add(orderData)
                    .addOnSuccessListener { orderDoc ->
                        Log.d(TAG, "✅ Заказ создан! ID: ${orderDoc.id}")

                        // АВТОМАТИЧЕСКАЯ ОТПРАВКА HTML-ЧЕКА
                        sendHtmlReceiptAutomatically(
                            orderId = orderDoc.id,
                            items = orderItems,
                            totalAmount = totalAmount,
                            userId = userId
                        )

                        // Удаляем все товары из корзины
                        val batch = db.batch()
                        for (document in documents) {
                            batch.delete(document.reference)
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                showLoading(false)
                                Toast.makeText(this, "✅ Заказ оформлен! Чек отправлен на ваш email.", Toast.LENGTH_LONG).show()
                                loadCartItems() // Обновляем интерфейс
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                Log.e(TAG, "❌ Ошибка очистки корзины: ${e.message}")
                                Toast.makeText(this, "❌ Ошибка очистки корзины", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Log.e(TAG, "❌ Ошибка создания заказа: ${e.message}")
                        Toast.makeText(this, "❌ Ошибка оформления заказа", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "❌ Ошибка получения корзины: ${e.message}")
                Toast.makeText(this, "❌ Ошибка получения корзины", Toast.LENGTH_LONG).show()
            }
    }

    // НОВЫЙ МЕТОД: АВТОМАТИЧЕСКАЯ ОТПРАВКА HTML-ЧЕКА
    private fun sendHtmlReceiptAutomatically(orderId: String, items: List<Map<String, Any>>, totalAmount: Double, userId: String) {
        // Получаем данные пользователя из Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val userEmail = userDoc.getString("email") ?: ""
                val userName = userDoc.getString("displayName") ?: userDoc.getString("username") ?: "Пользователь"

                if (userEmail.isEmpty()) {
                    Log.e(TAG, "❌ Email пользователя не найден")
                    return@addOnSuccessListener
                }

                // Форматируем дату
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                // Создаем HTML-чек
                val htmlReceipt = buildString {
                    append("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Чек заказа №$orderId</title>
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background: #f9f9f9; }
                            .receipt-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 20px rgba(0,0,0,0.1); }
                            .header { text-align: center; padding-bottom: 20px; border-bottom: 2px solid #4CAF50; margin-bottom: 30px; }
                            .header h1 { color: #2c3e50; font-size: 28px; margin-bottom: 10px; }
                            .header p { color: #7f8c8d; font-size: 16px; }
                            .company-info { text-align: center; margin-bottom: 20px; }
                            .company-info h2 { color: #4CAF50; font-size: 22px; }
                            .order-details { background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 30px; }
                            .detail-row { display: flex; justify-content: space-between; margin-bottom: 10px; }
                            .detail-label { font-weight: 600; color: #555; }
                            .detail-value { color: #2c3e50; }
                            .items-table { width: 100%; border-collapse: collapse; margin: 25px 0; }
                            .items-table th { background: #4CAF50; color: white; padding: 15px; text-align: left; font-weight: 600; }
                            .items-table td { padding: 15px; border-bottom: 1px solid #eee; }
                            .items-table tr:hover { background: #f5f5f5; }
                            .total-section { text-align: right; margin-top: 30px; padding-top: 20px; border-top: 2px solid #4CAF50; }
                            .total-amount { font-size: 28px; color: #e74c3c; font-weight: bold; }
                            .footer { margin-top: 40px; text-align: center; color: #95a5a6; font-size: 14px; padding-top: 20px; border-top: 1px solid #eee; }
                            .status { display: inline-block; background: #4CAF50; color: white; padding: 5px 15px; border-radius: 20px; font-size: 14px; }
                            .highlight { background: #fffde7; padding: 3px 6px; border-radius: 4px; }
                        </style>
                    </head>
                    <body>
                        <div class="receipt-container">
                            <div class="header">
                                <h1>ONE TECH STORE</h1>
                                <p>Ваш чек заказа</p>
                            </div>
                            
                            <div class="company-info">
                                <h2>Чек № <span class="highlight">$orderId</span></h2>
                                <p>Дата: $currentDate</p>
                            </div>
                            
                            <div class="order-details">
                                <div class="detail-row">
                                    <span class="detail-label">Покупатель:</span>
                                    <span class="detail-value">$userName</span>
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Email:</span>
                                    <span class="detail-value">$userEmail</span>
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Статус заказа:</span>
                                    <span class="detail-value"><span class="status">ОФОРМЛЕН</span></span>
                                </div>
                            </div>
                            
                            <table class="items-table">
                                <thead>
                                    <tr>
                                        <th>№</th>
                                        <th>Товар</th>
                                        <th>Количество</th>
                                        <th>Цена за шт.</th>
                                        <th>Сумма</th>
                                    </tr>
                                </thead>
                                <tbody>
                """)

                    // Добавляем строки с товарами
                    items.forEachIndexed { index, item ->
                        val number = index + 1
                        val productName = item["productName"] as String
                        val quantity = (item["quantity"] as Long).toInt()
                        val price = item["price"] as Double
                        val sum = quantity * price

                        append("""
                        <tr>
                            <td>$number</td>
                            <td>$productName</td>
                            <td>$quantity</td>
                            <td>${String.format("%,.0f", price)} ₽</td>
                            <td><strong>${String.format("%,.0f", sum)} ₽</strong></td>
                        </tr>
                    """)
                    }

                    append("""
                                </tbody>
                            </table>
                            
                            <div class="total-section">
                                <h3>ИТОГОВАЯ СУММА</h3>
                                <div class="total-amount">${String.format("%,.0f", totalAmount)} ₽</div>
                                <p><small>Включая все налоги и сборы</small></p>
                            </div>
                            
                            <div class="footer">
                                <p>Благодарим за покупку в One Tech Store!</p>
                                <p>Этот чек сформирован автоматически. Сохраните его для учета.</p>
                                <p>По вопросам: support@onetech.ru | +7 (999) 123-45-67</p>
                                <p>© ${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())} One Tech Store. Все права защищены.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                """)
                }

                // Автоматически отправляем email
                sendAutoEmail(
                    recipientEmail = userEmail,
                    subject = "Ваш чек заказа №$orderId от One Tech Store",
                    htmlBody = htmlReceipt
                )

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка получения данных пользователя", e)
                // Показываем уведомление
                Toast.makeText(this, "Чек не отправлен: данные пользователя не найдены", Toast.LENGTH_SHORT).show()
            }
    }

    // МЕТОД ДЛЯ АВТОМАТИЧЕСКОЙ ОТПРАВКИ EMAIL
    private fun sendAutoEmail(recipientEmail: String, subject: String, htmlBody: String) {
        try {
            // Создаем Intent для отправки email
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                // Указываем получателя (email пользователя)
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, Html.fromHtml(htmlBody))
                // Указываем, что это HTML
                putExtra(Intent.EXTRA_HTML_TEXT, htmlBody)
            }

            // Пытаемся запустить почтовое приложение
            startActivity(Intent.createChooser(emailIntent, "Отправка чека..."))

            Log.d(TAG, "✅ Email отправлен на: $recipientEmail")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка отправки email", e)

            // Если нет почтового приложения, показываем чек в WebView
            showReceiptInWebView(htmlBody, subject)
        }
    }

    // ПОКАЗ ЧЕКА В WEBVIEW (если нет почтового приложения)
    private fun showReceiptInWebView(htmlContent: String, title: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Установите почтовое приложение для отправки чека. Вот ваш чек:")
            .setPositiveButton("Скопировать чек") { dialog, _ ->
                // Копируем HTML в буфер обмена
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Чек заказа", Html.fromHtml(htmlContent).toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Чек скопирован", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Закрыть") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Создаем WebView для отображения HTML
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800 // Высота
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(webView)
        }

        dialog.setView(container)
        dialog.show()
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
        aiAssistantButton?.setOnClickListener {
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