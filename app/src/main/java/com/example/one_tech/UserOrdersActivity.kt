package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class UserOrdersActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var ordersAdapter: UserOrdersAdapter
    private val TAG = "UserOrdersActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_orders)

        Log.d(TAG, "UserOrdersActivity создан")

        initViews()
        setupBackButton()
        setupRefreshButton()
        loadUserOrders()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.ordersRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)

        ordersAdapter = UserOrdersAdapter(emptyList()) { order ->
            showOrderDetails(order)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ordersAdapter
    }

    private fun setupBackButton() {
        findViewById<TextView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun setupRefreshButton() {
        findViewById<TextView>(R.id.refreshButton).setOnClickListener {
            loadUserOrders()
        }
    }

    private fun loadUserOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Пользователь не авторизован")
            finish()
            return
        }

        val userId = currentUser.uid
        showLoading(true)
        emptyStateText.visibility = View.GONE

        Log.d(TAG, "🔄 Начинаем загрузку заказов для пользователя: $userId")

        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d(TAG, "✅ Запрос успешен. Найдено документов: ${querySnapshot.size()}")

                val ordersList = mutableListOf<Order>()

                if (querySnapshot.isEmpty) {
                    Log.d(TAG, "📭 У пользователя нет заказов")
                    showEmptyState("У вас пока нет заказов")
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    try {
                        Log.d(TAG, "📄 Обработка документа: ${document.id}")

                        // Парсим заказ вручную для отладки
                        val orderId = document.id
                        val userIdFromDoc = document.getString("userId") ?: ""
                        val totalAmount = document.getDouble("totalAmount") ?: 0.0
                        val status = document.getString("status") ?: "pending"
                        val createdAt = document.getTimestamp("createdAt")

                        // Парсим товары
                        val itemsList = mutableListOf<OrderItem>()
                        val items = document.get("items") as? List<Map<String, Any>> ?: emptyList()

                        items.forEachIndexed { index, itemMap ->
                            val productId = itemMap["productId"] as? String ?: ""
                            val productName = itemMap["productName"] as? String ?: "Товар ${index + 1}"
                            val quantity = (itemMap["quantity"] as? Long ?: 1L).toInt()
                            val price = (itemMap["price"] as? Double ?: 0.0)

                            itemsList.add(OrderItem(productId, productName, quantity, price))
                        }

                        val order = Order(
                            id = orderId,
                            userId = userIdFromDoc,
                            items = itemsList,
                            totalAmount = totalAmount,
                            status = status,
                            createdAt = createdAt,
                            updatedAt = document.getTimestamp("updatedAt")
                        )

                        ordersList.add(order)
                        Log.d(TAG, "✅ Добавлен заказ: $orderId, сумма: $totalAmount, товаров: ${itemsList.size}")

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Ошибка парсинга заказа ${document.id}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                Log.d(TAG, "📊 Всего загружено заказов: ${ordersList.size}")
                ordersAdapter.updateOrders(ordersList)
                showLoading(false)

                if (ordersList.isEmpty()) {
                    showEmptyState("У вас пока нет заказов")
                } else {
                    emptyStateText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "❌ Ошибка загрузки заказов: ${exception.message}", exception)
                showEmptyState("Ошибка загрузки заказов: ${exception.localizedMessage}")
            }
    }

    private fun showOrderDetails(order: Order) {
        val detailsBuilder = StringBuilder()
        detailsBuilder.append("📦 **Детали заказа**\n\n")
        detailsBuilder.append("Заказ #${order.id.take(8)}\n")
        detailsBuilder.append("Сумма: ${String.format("%,.0f", order.totalAmount)} ₽\n\n")

        if (order.createdAt != null) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            detailsBuilder.append("Дата: ${dateFormat.format(order.createdAt.toDate())}\n\n")
        }

        detailsBuilder.append("📋 **Товары:**\n")
        order.items.forEachIndexed { index, item ->
            val itemTotal = item.quantity * item.price
            detailsBuilder.append("${index + 1}. ${item.productName}\n")
            detailsBuilder.append("   Кол-во: ${item.quantity} x ${String.format("%,.0f", item.price)} ₽ = ${String.format("%,.0f", itemTotal)} ₽\n")
        }

        detailsBuilder.append("\n📊 **Статус:** ${getStatusText(order.status)}")

        AlertDialog.Builder(this)
            .setTitle("Детали заказа")
            .setMessage(detailsBuilder.toString())
            .setPositiveButton("Закрыть") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "pending" -> "⏳ Ожидает обработки"
            "processing" -> "📦 Собирается"
            "shipping" -> "🚚 В доставке"
            "delivered" -> "✅ Доставлен"
            "cancelled" -> "❌ Отменен"
            else -> "❓ $status"
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(message: String) {
        runOnUiThread {
            emptyStateText.text = message
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
    }
}

// Адаптер для заказов пользователя
class UserOrdersAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<UserOrdersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.orderIdText)
        val orderDateText: TextView = itemView.findViewById(R.id.orderDateText)
        val orderTotalText: TextView = itemView.findViewById(R.id.orderTotalText)
        val orderStatusText: TextView = itemView.findViewById(R.id.orderStatusText)
        val itemsCountText: TextView = itemView.findViewById(R.id.itemsCountText)
        val userNameText: TextView = itemView.findViewById(R.id.userNameText)
        val deleteButton: View = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        // ID заказа
        holder.orderIdText.text = "Заказ #${order.id.take(8)}"

        // Общая сумма
        holder.orderTotalText.text = "${String.format("%,.0f", order.totalAmount)} ₽"

        // Количество товаров
        holder.itemsCountText.text = "Товаров: ${order.items.size}"

        // Дата заказа
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = if (order.createdAt != null) {
            dateFormat.format(order.createdAt.toDate())
        } else {
            "Дата не указана"
        }
        holder.orderDateText.text = dateStr

        // Статус заказа с цветом
        val (statusText, colorRes) = when (order.status) {
            "pending" -> "⏳ Ожидает" to android.R.color.holo_orange_dark
            "processing" -> "📦 Собирается" to android.R.color.holo_orange_light
            "shipping" -> "🚚 В доставке" to android.R.color.holo_blue_light
            "delivered" -> "✅ Доставлен" to android.R.color.holo_green_dark
            "cancelled" -> "❌ Отменен" to android.R.color.darker_gray
            else -> "❓ ${order.status}" to android.R.color.darker_gray
        }
        holder.orderStatusText.text = statusText
        holder.orderStatusText.setTextColor(holder.itemView.context.resources.getColor(colorRes, null))

        // Имя пользователя
        holder.userNameText.text = "Вы"

        // Скрываем кнопку удаления
        holder.deleteButton.visibility = View.GONE

        // Клик на весь элемент
        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}