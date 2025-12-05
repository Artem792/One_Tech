package com.example.one_tech

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit = {},
    private val onAddToCartClick: (Product) -> Unit = {}
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private val auth = Firebase.auth
    private var lastClickTime = 0L
    private val MIN_CLICK_INTERVAL = 1000L // 1 секунда

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val memorySpec: TextView = itemView.findViewById(R.id.memorySpec)
        val clockSpec: TextView = itemView.findViewById(R.id.clockSpec)
        val connectorsSpec: TextView = itemView.findViewById(R.id.connectorsSpec)
        val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
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
                holder.connectorsSpec.text = "Сокет: ${product.specs["socket"] ?: "Не указаno"}"
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

        holder.itemView.setOnClickListener {
            onItemClick(product)
        }

        holder.addToCartButton.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                lastClickTime = currentTime

                if (auth.currentUser == null) {
                    Toast.makeText(holder.itemView.context, "Войдите в аккаунт чтобы добавить в корзину", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Блокируем кнопку на 1 секунду
                holder.addToCartButton.isEnabled = false
                holder.addToCartButton.text = "ДОБАВЛЯЕМ..."

                // Вызываем колбэк, а вся логика будет в CategoryActivity
                onAddToCartClick(product)

                // Разблокируем кнопку через 1 секунду
                holder.addToCartButton.postDelayed({
                    holder.addToCartButton.isEnabled = true
                    holder.addToCartButton.text = "В КОРЗИНУ"
                }, MIN_CLICK_INTERVAL)
            }
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}