package com.example.one_tech

import android.content.Intent
import android.os.Bundle
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
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton.setOnClickListener {
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
                        val username = document.getString("username") ?: "Пользователь"
                        val displayName = document.getString("displayName") ?: username

                        val userNameTextView = findViewById<TextView>(R.id.userNameText)
                        userNameTextView.text = displayName
                    } else {
                        val userNameTextView = findViewById<TextView>(R.id.userNameText)
                        userNameTextView.text = user.email ?: "Пользователь"
                    }
                }
                .addOnFailureListener { e ->
                    val userNameTextView = findViewById<TextView>(R.id.userNameText)
                    userNameTextView.text = user.email ?: "Пользователь"
                    Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            val userNameTextView = findViewById<TextView>(R.id.userNameText)
            userNameTextView.text = "Гость"
        }
    }

    private fun setupLogoutButton() {
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            goToLogin()
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