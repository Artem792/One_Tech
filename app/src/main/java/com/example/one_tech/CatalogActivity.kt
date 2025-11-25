package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class CatalogActivity : AppCompatActivity() {

    private var isAdminMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        // Получаем флаг режима админа
        isAdminMode = intent.getBooleanExtra("admin_mode", false)

        // Настраиваем обработчик кнопки "Назад"
        setupBackPressedHandler()

        // Настраиваем интерфейс в зависимости от режима
        if (isAdminMode) {
            setupAdminMode()
        } else {
            setupNormalUserMode()
        }

        setupCategoriesGrid()
    }

    private fun setupBackPressedHandler() {
        // Современный способ обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdminMode) {
                    // Если это режим админа - возвращаемся в админ панель
                    val intent = Intent(this@CatalogActivity, AdminActivity::class.java)
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

        // Скрываем кнопку ИИ-помощника
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton?.visibility = View.GONE

        // Меняем заголовок
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText?.text = "Управление каталогом"
    }

    private fun setupNormalUserMode() {
        setupClickListeners()
        setupAiAssistantButton()
        updateBottomNavigation()
    }

    private fun setupCategoriesGrid() {
        val categoriesGrid = findViewById<GridView>(R.id.categoriesGrid)

        val categories = listOf(
            Category(1, "Готовые ПК", "🖥️"),
            Category(2, "Процессоры", "⚡"),
            Category(3, "Видеокарты", "🎮"),
            Category(4, "Память", "🧠"),
            Category(5, "Накопители", "💾"),
            Category(6, "Блоки питания", "🔌"),
            Category(7, "Корпуса", "📦"),
            Category(8, "Охлаждение", "❄️"),
            Category(9, "Материнские платы", "🔋")
        )

        // Адаптер прямо здесь
        categoriesGrid.adapter = object : BaseAdapter() {
            override fun getCount(): Int = categories.size
            override fun getItem(position: Int): Category = categories[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@CatalogActivity)
                    .inflate(R.layout.item_category, parent, false)

                val category = categories[position]
                view.findViewById<TextView>(R.id.categoryIcon).text = category.icon
                view.findViewById<TextView>(R.id.categoryName).text = category.name

                return view
            }
        }

        categoriesGrid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val category = categories[position]
            val intent = Intent(this, CategoryActivity::class.java)
            intent.putExtra("category_name", category.name)
            intent.putExtra("category_icon", category.icon)
            intent.putExtra("admin_mode", isAdminMode)
            startActivity(intent)
        }
    }

    private fun setupClickListeners() {
        findViewById<LinearLayout>(R.id.navCatalog)?.setOnClickListener {
            // Уже на экране каталога
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

        val catalogText = navCatalog?.getChildAt(1) as? TextView
        catalogText?.setTextColor(resources.getColor(android.R.color.white, theme))
    }

    private fun resetNavigationColors() {
        val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
        val navCart = findViewById<LinearLayout>(R.id.navCart)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        val catalogText = navCatalog?.getChildAt(1) as? TextView
        val cartText = navCart?.getChildAt(1) as? TextView
        val profileText = navProfile?.getChildAt(1) as? TextView

        catalogText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        cartText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        profileText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
    }
}