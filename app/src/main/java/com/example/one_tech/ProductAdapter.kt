package com.example.one_tech

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProductAdapter(
    private var products: List<Product>,
    private val isAdminMode: Boolean = false,
    private val onItemClick: (Product) -> Unit = {},
    private val onAddToCartClick: (Product) -> Unit = {},
    private val onEditClick: (Product) -> Unit = {},
    private val onDeleteClick: (Product) -> Unit = {}
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private var lastClickTime = 0L
    private val MIN_CLICK_INTERVAL = 1000L

    // Новый метод для определения layout
    override fun getItemViewType(position: Int): Int {
        return if (isAdminMode) R.layout.item_product_admin else R.layout.item_product
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Общие элементы
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val memorySpec: TextView = itemView.findViewById(R.id.memorySpec)
        val clockSpec: TextView = itemView.findViewById(R.id.clockSpec)
        val connectorsSpec: TextView = itemView.findViewById(R.id.connectorsSpec)

        // Элементы для обычного режима
        val addToCartButton: Button? = itemView.findViewById(R.id.addToCartButton)

        // Элементы для админ режима
        val editButton: TextView? = itemView.findViewById(R.id.editButton)
        val deleteButton: TextView? = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(viewType, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        holder.productName.text = product.name
        holder.productPrice.text = "${String.format("%,.0f", product.price)} ₽"

        // Устанавливаем характеристики в зависимости от категории
        when (product.category) {
            "Видеокарты" -> {
                holder.memorySpec.text = "Память: ${product.specs["memory"] ?: "Не указано"}"
                holder.clockSpec.text = "Частота: ${product.specs["gpuClock"] ?: "Не указано"}"
                holder.connectorsSpec.text = "Разъемы: ${product.specs["connectors"] ?: "Не указано"}"
            }
            "Процессоры" -> {
                holder.memorySpec.text = "Ядер: ${product.specs["cores"] ?: "Не указано"}"
                holder.clockSpec.text = "Частота: ${product.specs["frequency"] ?: "Не указано"}"
                holder.connectorsSpec.text = "Сокет: ${product.specs["socket"] ?: "Не указано"}"
            }
            "Память" -> {
                holder.memorySpec.text = "Объем: ${product.specs["memoryCapacity"] ?: "Не указано"}"
                holder.clockSpec.text = "Частота: ${product.specs["memoryFrequency"] ?: "Не указано"}"
                holder.connectorsSpec.text = "Тайминги: ${product.specs["timings"] ?: "Не указано"}"
            }
            "Материнские платы" -> {
                holder.memorySpec.text = "Сокет: ${product.specs["motherboardSocket"] ?: "Не указано"}"
                holder.clockSpec.text = "Чипсет: ${product.specs["chipset"] ?: "Не указано"}"
                holder.connectorsSpec.text = "Форм-фактор: ${product.specs["formFactor"] ?: "Не указано"}"
            }
            "Накопители" -> {
                holder.memorySpec.text = "Тип: ${product.specs["storageType"] ?: "Не указано"}"
                holder.clockSpec.text = "Объем: ${product.specs["storageCapacity"] ?: "Не указано"}"
                holder.connectorsSpec.text = "Интерфейс: ${product.specs["interfaceType"] ?: "Не указано"}"
            }
            else -> {
                holder.memorySpec.text = product.specs.values.firstOrNull() ?: "Характеристики"
                holder.clockSpec.text = product.specs.values.elementAtOrNull(1) ?: "Не указано"
                holder.connectorsSpec.text = product.specs.values.elementAtOrNull(2) ?: "Не указано"
            }
        }

        // Обработка клика на всю карточку - ВСЕГДА открывает просмотр
        holder.itemView.setOnClickListener {
            onItemClick(product)
        }

        // Режим администратора
        if (isAdminMode) {
            // Показываем кнопки редактирования/удаления
            holder.editButton?.visibility = View.VISIBLE
            holder.deleteButton?.visibility = View.VISIBLE
            holder.addToCartButton?.visibility = View.GONE

            // Обработка кнопки редактирования (иконка ✏️)
            holder.editButton?.setOnClickListener {
                onEditClick(product)
            }

            // Обработка кнопки удаления (иконка 🗑️)
            holder.deleteButton?.setOnClickListener {
                showDeleteConfirmationDialog(holder.itemView.context, product)
            }
        } else {
            // Обычный режим пользователя
            holder.editButton?.visibility = View.GONE
            holder.deleteButton?.visibility = View.GONE
            holder.addToCartButton?.visibility = View.VISIBLE

            // Обработка кнопки добавления в корзину
            holder.addToCartButton?.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime

                    if (auth.currentUser == null) {
                        Toast.makeText(holder.itemView.context,
                            "Войдите в аккаунт чтобы добавить в корзину",
                            Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    holder.addToCartButton?.isEnabled = false
                    holder.addToCartButton?.text = "ДОБАВЛЯЕМ..."

                    onAddToCartClick(product)

                    holder.addToCartButton?.postDelayed({
                        holder.addToCartButton?.isEnabled = true
                        holder.addToCartButton?.text = "В КОРЗИНУ"
                    }, MIN_CLICK_INTERVAL)
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(context: android.content.Context, product: Product) {
        AlertDialog.Builder(context)
            .setTitle("Удаление товара")
            .setMessage("Вы точно хотите удалить товар \"${product.name}\"?")
            .setPositiveButton("Удалить") { dialog, _ ->
                onDeleteClick(product)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}