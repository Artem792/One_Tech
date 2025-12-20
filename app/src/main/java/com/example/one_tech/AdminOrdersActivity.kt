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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class AdminOrdersActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var ordersAdapter: OrdersAdapter
    private val TAG = "AdminOrdersActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        initViews()
        setupBackButton()
        setupRefreshButton()
        loadOrders()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.ordersRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)

        ordersAdapter = OrdersAdapter(emptyList()) { order ->
            showOrderStatusDialog(order)
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
        val refreshButton = findViewById<TextView>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            loadOrders()
        }
    }

    private fun loadOrders() {
        showLoading(true)

        db.collection("orders")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val ordersList = mutableListOf<Order>()
                for (document in documents) {
                    try {
                        val order = document.toObject(Order::class.java).copy(id = document.id)
                        ordersList.add(order)
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка парсинга заказа: ${e.message}")
                    }
                }

                ordersAdapter.updateOrders(ordersList)
                showLoading(false)

                if (ordersList.isEmpty()) {
                    emptyStateText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Ошибка загрузки заказов: ${exception.message}")
                emptyStateText.text = "Ошибка загрузки заказов"
                emptyStateText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }

    private fun showOrderStatusDialog(order: Order) {
        val statuses = listOf("pending", "processing", "shipping", "delivered", "cancelled")
        val statusNames = mapOf(
            "pending" to "⏳ Ожидает обработки",
            "processing" to "📦 Собирается",
            "shipping" to "🚚 В доставке",
            "delivered" to "✅ Доставлен",
            "cancelled" to "❌ Отменен"
        )

        AlertDialog.Builder(this)
            .setTitle("Изменение статуса заказа")
            .setMessage("Заказ #${order.id.take(8)}\nСумма: ${String.format("%,.0f", order.totalAmount)} ₽")
            .setItems(statuses.map { statusNames[it] ?: it }.toTypedArray()) { _, which ->
                val newStatus = statuses[which]
                updateOrderStatus(order.id, newStatus)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("orders").document(orderId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Статус заказа $orderId обновлен на: $newStatus")
                Toast.makeText(this, "Статус обновлен", Toast.LENGTH_SHORT).show()
                loadOrders() // Обновляем список
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка обновления статуса: ${e.message}")
                Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}

// Адаптер для списка заказов
class OrdersAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.orderIdText)
        val orderDateText: TextView = itemView.findViewById(R.id.orderDateText)
        val orderTotalText: TextView = itemView.findViewById(R.id.orderTotalText)
        val orderStatusText: TextView = itemView.findViewById(R.id.orderStatusText)
        val itemsCountText: TextView = itemView.findViewById(R.id.itemsCountText)
        val userNameText: TextView = itemView.findViewById(R.id.userNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        holder.orderIdText.text = "Заказ #${order.id.take(8)}"
        holder.orderTotalText.text = "${String.format("%,.0f", order.totalAmount)} ₽"
        holder.itemsCountText.text = "Товаров: ${order.items.size}"

        // Форматируем дату
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = if (order.createdAt != null) {
            dateFormat.format(order.createdAt.toDate())
        } else {
            "Дата не указана"
        }
        holder.orderDateText.text = dateStr

        // Устанавливаем статус с цветом
        val (statusText, colorRes) = when (order.status) {
            "pending" -> "⏳ Ожидает" to android.R.color.holo_orange_dark
            "processing" -> "📦 Собирается" to android.R.color.holo_orange_light
            "shipping" -> "🚚 В доставке" to android.R.color.holo_blue_light
            "delivered" -> "✅ Доставлен" to android.R.color.holo_green_dark
            "cancelled" -> "❌ Отменен" to android.R.color.darker_gray
            else -> "❓ Неизвестно" to android.R.color.darker_gray
        }
        holder.orderStatusText.text = statusText
        holder.orderStatusText.setTextColor(holder.itemView.context.resources.getColor(colorRes, null))

        // Загружаем имя пользователя
        loadUserName(order.userId, holder.userNameText)

        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }
    }

    private fun loadUserName(userId: String, textView: TextView) {
        Firebase.firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("displayName") ?:
                    document.getString("username") ?:
                    "Пользователь"
                    textView.text = name
                } else {
                    textView.text = "Пользователь"
                }
            }
            .addOnFailureListener {
                textView.text = "Пользователь"
            }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}

// Обновите класс Order в отдельном файле или добавьте эти поля в существующий класс Product
data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, processing, shipping, delivered, cancelled
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
)

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0
)