package com.example.one_tech

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CatalogActivity : AppCompatActivity() {

    private var isAdminMode = false
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val SHARED_PREFS = "guest_prefs"
    private val IS_FIRST_GUEST_LOGIN = "is_first_guest_login"
    private val HAS_SHOWN_GUEST_SNACKBAR = "has_shown_guest_snackbar"

    companion object {
        private const val TAG = "CatalogActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        try {
            setContentView(R.layout.activity_catalog)
            Log.d(TAG, "setContentView successful")
        } catch (e: Exception) {
            Log.e(TAG, "Error in setContentView: ${e.message}", e)
            throw e
        }

        isAdminMode = intent.getBooleanExtra("admin_mode", false)
        setupBackPressedHandler()

        if (isAdminMode) {
            setupAdminMode()
        } else {
            setupNormalUserMode()
        }

        setupCategoriesGrid()
        checkAndShowGuestSnackbar()

        Log.d(TAG, "onCreate completed successfully")
    }

    override fun onResume() {
        super.onResume()
        checkAndShowGuestSnackbar()
    }

    private fun checkAndShowGuestSnackbar() {
        if (isAdminMode) return

        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getBoolean("isGuest") == true) {
                    val sharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
                    val hasShownSnackbar = sharedPref.getBoolean(HAS_SHOWN_GUEST_SNACKBAR, false)

                    if (!hasShownSnackbar) {
                        showGuestWelcomeSnackbar()
                        with(sharedPref.edit()) {
                            putBoolean(HAS_SHOWN_GUEST_SNACKBAR, true)
                            apply()
                        }
                    }
                }
            }
    }

    private fun showGuestWelcomeSnackbar() {
        try {
            val rootView = findViewById<LinearLayout>(R.id.include_top_bar) ?: window.decorView.rootView

            Snackbar.make(rootView, "Вы в гостевом режиме. Можете добавлять товары в корзину. Для оформления заказа зарегистрируйтесь.",
                Snackbar.LENGTH_LONG)
                .setAction("Регистрация") {
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                }
                .setActionTextColor(resources.getColor(android.R.color.holo_blue_light, theme))
                .setBackgroundTint(resources.getColor(android.R.color.background_dark, theme))
                .setTextColor(resources.getColor(android.R.color.white, theme))
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing snackbar: ${e.message}")
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAdminMode) {
                    val intent = Intent(this@CatalogActivity, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupAdminMode() {
        try {
            findViewById<LinearLayout>(R.id.bottom_navigation)?.visibility = View.GONE
            findViewById<TextView>(R.id.aiAssistantButton)?.visibility = View.GONE
            findViewById<TextView>(R.id.titleText)?.text = "Управление каталогом"
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupAdminMode: ${e.message}")
        }
    }

    private fun setupNormalUserMode() {
        try {
            setupClickListeners()
            setupAiAssistantButton()
            updateBottomNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupNormalUserMode: ${e.message}")
        }
    }

    private fun setupCategoriesGrid() {
        try {
            val categoriesGrid = findViewById<GridView>(R.id.categoriesGrid) ?: return

            val categories = listOf(
                CategoryItem(1, "Готовые ПК", "ic_pc_ready"),
                CategoryItem(2, "Процессоры", "ic_cpu"),
                CategoryItem(3, "Видеокарты", "ic_videocarta"),
                CategoryItem(4, "Память", "ic_ram"),
                CategoryItem(5, "Накопители", "ic_hard"),
                CategoryItem(6, "Блоки питания", "ic_power"),
                CategoryItem(7, "Корпуса", "ic_case"),
                CategoryItem(8, "Охлаждение", "ic_fan"),
                CategoryItem(9, "Материнские платы", "ic_plata")
            )

            categoriesGrid.adapter = object : BaseAdapter() {
                override fun getCount(): Int = categories.size
                override fun getItem(position: Int): CategoryItem = categories[position]
                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val view: View = convertView ?: LayoutInflater.from(this@CatalogActivity)
                        .inflate(R.layout.item_category, parent, false)

                    val category = categories[position]

                    try {
                        val categoryIcon = view.findViewById<ImageView>(R.id.categoryIcon)
                        val categoryName = view.findViewById<TextView>(R.id.categoryName)

                        // Получаем идентификатор ресурса
                        val iconResId = getIconResourceId(category.iconName)

                        if (iconResId != 0) {
                            // Устанавливаем картинку
                            categoryIcon.setImageResource(iconResId)
                            categoryIcon.scaleType = ImageView.ScaleType.FIT_CENTER

                            // Устанавливаем фон (серый скругленный квадрат)
                            val backgroundResId = resources.getIdentifier("gray_rounded_background", "drawable", packageName)
                            if (backgroundResId != 0) {
                                categoryIcon.setBackgroundResource(backgroundResId)
                            }

                            // Добавляем отступы внутри иконки
                            val density = resources.displayMetrics.density
                            val padding = (8 * density).toInt()
                            categoryIcon.setPadding(padding, padding, padding, padding)
                        } else {
                            // Заглушка, если иконка не найдена
                            Log.w(TAG, "Не найдена иконка: ${category.iconName}")
                            categoryIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                            categoryIcon.scaleType = ImageView.ScaleType.CENTER

                            // Фон для заглушки
                            val backgroundResId = resources.getIdentifier("gray_rounded_background", "drawable", packageName)
                            if (backgroundResId != 0) {
                                categoryIcon.setBackgroundResource(backgroundResId)
                            }
                        }

                        // Устанавливаем название категории
                        categoryName.text = category.name

                        // Проверяем, что текст виден
                        categoryName.visibility = View.VISIBLE

                    } catch (e: Exception) {
                        Log.e(TAG, "Error in getView for position $position: ${e.message}")
                    }

                    return view
                }
            }

            categoriesGrid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                try {
                    val category = categories[position]
                    val intent = Intent(this@CatalogActivity, CategoryActivity::class.java)
                    intent.putExtra("category_name", category.name)
                    intent.putExtra("admin_mode", isAdminMode)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onItemClickListener: ${e.message}")
                }
            }

            // Для отладки проверяем цвета текста
            Log.d(TAG, "Цвет текста должен быть белый (#FFFFFF)")

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupCategoriesGrid: ${e.message}", e)
        }
    }

    /**
     * Метод для получения идентификатора ресурса по имени файла
     */
    private fun getIconResourceId(iconName: String): Int {
        return try {
            // Пробуем разные расширения
            val extensions = listOf("", ".jpg", ".jpeg", ".png")

            for (extension in extensions) {
                val resourceName = if (extension.isEmpty()) iconName else "$iconName$extension"
                val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)
                if (resourceId != 0) {
                    return resourceId
                }
            }

            // Если не нашли, пробуем получить как есть
            resources.getIdentifier(iconName, "drawable", packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting resource for $iconName: ${e.message}")
            0
        }
    }

    private fun setupClickListeners() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupClickListeners: ${e.message}")
        }
    }

    private fun setupAiAssistantButton() {
        try {
            val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
            aiAssistantButton?.setOnClickListener {
                val intent = Intent(this, AiAssistantActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupAiAssistantButton: ${e.message}")
        }
    }

    private fun updateBottomNavigation() {
        try {
            val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
            val navCart = findViewById<LinearLayout>(R.id.navCart)
            val navProfile = findViewById<LinearLayout>(R.id.navProfile)

            resetNavigationColors()

            val catalogText = navCatalog?.getChildAt(1) as? TextView
            catalogText?.setTextColor(resources.getColor(android.R.color.white, theme))
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateBottomNavigation: ${e.message}")
        }
    }

    private fun resetNavigationColors() {
        try {
            val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
            val navCart = findViewById<LinearLayout>(R.id.navCart)
            val navProfile = findViewById<LinearLayout>(R.id.navProfile)

            val catalogText = navCatalog?.getChildAt(1) as? TextView
            val cartText = navCart?.getChildAt(1) as? TextView
            val profileText = navProfile?.getChildAt(1) as? TextView

            catalogText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            cartText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            profileText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        } catch (e: Exception) {
            Log.e(TAG, "Error in resetNavigationColors: ${e.message}")
        }
    }
}

// Класс для категории
data class CategoryItem(
    val id: Int,
    val name: String,
    val iconName: String
)