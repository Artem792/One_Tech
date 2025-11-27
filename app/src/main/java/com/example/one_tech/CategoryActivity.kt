package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() {

    private var isAdminMode = false
    private var categoryName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Получаем данные из Intent
        categoryName = intent.getStringExtra("category_name") ?: "Категория"
        val categoryIcon = intent.getStringExtra("category_icon") ?: "📦"
        isAdminMode = intent.getBooleanExtra("admin_mode", false)

        // Настраиваем обработчик кнопки "Назад"
        setupBackPressedHandler()

        // Устанавливаем заголовок категории
        setupCategoryTitle(categoryName)
        setupBackButton()
        setupFilterButton()

        // Настраиваем интерфейс в зависимости от режима
        if (isAdminMode) {
            setupAdminMode()
        } else {
            setupNormalUserMode()
        }
    }

    private fun setupBackPressedHandler() {
        // Современный способ обработки кнопки "Назад"
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
        val bottomNavigation = findViewById<LinearLayout>(R.id.bottom_navigation)
        bottomNavigation?.visibility = View.GONE

        // Скрываем кнопку ИИ-помощника и ПОКАЗЫВАЕМ кнопку добавления
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        val addProductButton = findViewById<TextView>(R.id.addProductButton)

        aiAssistantButton?.visibility = View.GONE
        addProductButton?.visibility = View.VISIBLE

        // Настраиваем обработчик кнопки добавления
        addProductButton?.setOnClickListener {
            openAddProductActivity()
        }
    }

    private fun setupNormalUserMode() {
        setupClickListeners()
        setupAiAssistantButton()

        // Скрываем кнопку добавления для обычных пользователей
        val addProductButton = findViewById<TextView>(R.id.addProductButton)
        addProductButton?.visibility = View.GONE
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

    // Добавляем обработку кнопки ИИ-помощника (только для обычных пользователей)
    private fun setupAiAssistantButton() {
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton?.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openAddProductActivity() {
        try {
            println("DEBUG: Открываем AddProductActivity для категории: $categoryName")

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
}