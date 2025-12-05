package com.example.one_tech

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private var cartItems: List<CartItem>,
    private val onQuantityChange: (CartItem, Int) -> Unit = { _, _ -> },
    private val onRemoveItem: (CartItem) -> Unit = { }
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val quantityText: TextView = itemView.findViewById(R.id.quantityText)
        val totalPriceText: TextView = itemView.findViewById(R.id.totalPriceText)
        val decreaseButton: TextView = itemView.findViewById(R.id.decreaseButton)
        val increaseButton: TextView = itemView.findViewById(R.id.increaseButton)
        val removeButton: TextView = itemView.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cartItem = cartItems[position]

        holder.productName.text = cartItem.productName
        holder.productPrice.text = "${String.format("%,.0f", cartItem.productPrice)} ₽"
        holder.quantityText.text = cartItem.quantity.toString()
        holder.totalPriceText.text = "${String.format("%,.0f", cartItem.getTotalPrice())} ₽"

        // TODO: Загрузка изображения товара
        // Glide.with(holder.itemView.context).load(cartItem.productImage).into(holder.productImage)

        holder.decreaseButton.setOnClickListener {
            val newQuantity = cartItem.quantity - 1
            onQuantityChange(cartItem, newQuantity)
        }

        holder.increaseButton.setOnClickListener {
            val newQuantity = cartItem.quantity + 1
            onQuantityChange(cartItem, newQuantity)
        }

        holder.removeButton.setOnClickListener {
            onRemoveItem(cartItem)
        }
    }

    override fun getItemCount() = cartItems.size

    fun updateCartItems(newCartItems: List<CartItem>) {
        cartItems = newCartItems
        notifyDataSetChanged()
    }
}