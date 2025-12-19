package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupClickListeners()
        setupAiAssistantButton()
        updateBottomNavigation()
        loadUserData()
        setupLogoutButton()
    }

    private fun setupAiAssistantButton() {
        // Если у тебя есть кнопка ИИ в топ-баре
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton?.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val isGuest = document.getBoolean("isGuest") ?: false

                        if (isGuest) {
                            // Режим гостя
                            setupGuestUI()
                        } else {
                            // Обычный пользователь
                            val username = document.getString("username") ?: "Пользователь"
                            val displayName = document.getString("displayName") ?: username

                            val userNameTextView = findViewById<TextView>(R.id.userNameText)
                            val userAvatar = findViewById<TextView>(R.id.userAvatar)

                            userNameTextView.text = displayName
                            // Устанавливаем первую букву имени или эмодзи
                            if (displayName.isNotEmpty() && displayName.first().isLetter()) {
                                userAvatar.text = displayName.first().uppercaseChar().toString()
                            } else {
                                userAvatar.text = "👤"
                            }
                        }
                    } else {
                        // Документа нет - вероятно гость
                        setupGuestUI()
                    }
                }
                .addOnFailureListener { e ->
                    setupGuestUI() // При ошибке считаем гостем
                    Log.e(TAG, "Ошибка загрузки данных: ${e.message}")
                }
        } ?: run {
            // Нет пользователя в auth - показываем гостевой UI
            setupGuestUI()
        }
    }

    private fun setupGuestUI() {
        runOnUiThread {
            try {
                val userNameTextView = findViewById<TextView>(R.id.userNameText)
                val logoutButton = findViewById<Button>(R.id.logoutButton)
                val userAvatar = findViewById<TextView>(R.id.userAvatar)

                userNameTextView.text = "Гость"
                logoutButton.text = "ВОЙТИ В АККАУНТ"
                userAvatar.text = "👤"

                // Находим контейнер ScrollView
                val scrollViewContent = findViewById<LinearLayout>(R.id.scrollViewContent)

                if (scrollViewContent == null) {
                    Log.e(TAG, "Не найден scrollViewContent!")
                    return@runOnUiThread
                }

                // Очищаем старые дополнительные элементы
                removeExistingGuestElements(scrollViewContent)

                // Находим индекс кнопки выхода
                val logoutIndex = scrollViewContent.indexOfChild(logoutButton)

                if (logoutIndex >= 0) {
                    // Добавляем уведомление о гостевом режиме
                    val guestWarning = TextView(this).apply {
                        text = "Вы в гостевом режиме"
                        setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                        textSize = 14f
                        setPadding(32, 16.dpToPx(), 32, 8.dpToPx())
                        gravity = View.TEXT_ALIGNMENT_CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Добавляем кнопку регистрации
                    val registerButton = Button(this).apply {
                        text = "ЗАРЕГИСТРИРОВАТЬСЯ"
                        setBackgroundColor(resources.getColor(android.R.color.transparent, theme))
                        setTextColor(resources.getColor(android.R.color.holo_blue_light, theme))
                        textSize = 16f
                        setPadding(0, 16.dpToPx(), 0, 32.dpToPx())
                        gravity = View.TEXT_ALIGNMENT_CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setOnClickListener {
                            val intent = Intent(this@ProfileActivity, RegisterActivity::class.java)
                            startActivity(intent)
                        }
                    }

                    // Добавляем элементы перед кнопкой выхода
                    scrollViewContent.addView(guestWarning, logoutIndex)
                    scrollViewContent.addView(registerButton, logoutIndex + 1)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка setupGuestUI: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun removeExistingGuestElements(parent: LinearLayout) {
        val elementsToRemove = mutableListOf<View>()

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)

            // Проверяем элементы, которые мы добавили
            if (child is TextView && child.text == "Вы в гостевом режиме") {
                elementsToRemove.add(child)
            } else if (child is Button &&
                child.text == "ЗАРЕГИСТРИРОВАТЬСЯ" &&
                child.currentTextColor == resources.getColor(android.R.color.holo_blue_light, theme)) {
                elementsToRemove.add(child)
            }
        }

        for (element in elementsToRemove) {
            parent.removeView(element)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupLogoutButton() {
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                db.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val isGuest = document.getBoolean("isGuest") ?: false

                        if (isGuest) {
                            // Для гостя - выход и переход на логин
                            auth.signOut()
                            Toast.makeText(this, "Войдите или зарегистрируйтесь", Toast.LENGTH_SHORT).show()
                            goToLogin()
                        } else {
                            // Для обычного пользователя - обычный выход
                            auth.signOut()
                            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                            goToLogin()
                        }
                    }
                    .addOnFailureListener {
                        // При ошибке считаем обычным пользователем
                        auth.signOut()
                        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                        goToLogin()
                    }
            } ?: run {
                // Пользователь не авторизован
                goToLogin()
            }
        }
    }

    private fun setupClickListeners() {
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
            // Уже в профиле
        }

        // Обработчики для разделов профиля
        setupProfileSections()
    }

    private fun setupProfileSections() {
        // Мои заказы
        val myOrdersLayout = findViewById<LinearLayout>(R.id.myOrdersLayout)
        myOrdersLayout?.setOnClickListener {
            Toast.makeText(this, "Мои заказы", Toast.LENGTH_SHORT).show()
            // Здесь можно добавить переход на экран заказов
        }

        // Избранное
        val favoritesLayout = findViewById<LinearLayout>(R.id.favoritesLayout)
        favoritesLayout?.setOnClickListener {
            Toast.makeText(this, "Избранное", Toast.LENGTH_SHORT).show()
            // Здесь можно добавить переход на экран избранного
        }

        // Настройки
        val settingsLayout = findViewById<LinearLayout>(R.id.settingsLayout)
        settingsLayout?.setOnClickListener {
            Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
            // Здесь можно добавить переход на экран настроек
        }
    }

    private fun updateBottomNavigation() {
        val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
        val navCart = findViewById<LinearLayout>(R.id.navCart)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        resetNavigationColors()

        val profileText = navProfile.getChildAt(1) as TextView
        profileText.setTextColor(resources.getColor(android.R.color.white, theme))
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

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}