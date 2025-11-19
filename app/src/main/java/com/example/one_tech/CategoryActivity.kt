package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Получаем данные из Intent
        val categoryName = intent.getStringExtra("category_name") ?: "Категория"
        val categoryIcon = intent.getStringExtra("category_icon") ?: "📦"

        // Устанавливаем заголовок категории
        setupCategoryTitle(categoryName)
        setupClickListeners()
        setupBackButton()
        setupFilterButton()
    }

    private fun setupCategoryTitle(categoryName: String) {
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = categoryName
    }

    private fun setupBackButton() {
        val backButton = findViewById<TextView>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Возврат назад к каталогу
        }
    }

    private fun setupFilterButton() {
        val filterButton = findViewById<TextView>(R.id.filterButton)
        filterButton.setOnClickListener {
            // Здесь можно добавить логику для фильтров
            // Например, показать диалог с фильтрами
        }
    }

    private fun setupClickListeners() {
        // Обработчики для нижней навигации
        findViewById<LinearLayout>(R.id.navCatalog).setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Добавляем обработку кнопки ИИ-помощника
    private fun setupAiAssistantButton() {
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton?.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }
}