package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Проверяем, является ли пользователь админом
        checkAdminAccess()

        setupAdminFeatures()
        loadAdminData()
    }

    private fun checkAdminAccess() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Если пользователь не авторизован - на логин
            goToLogin()
            return
        }

        // Проверяем в Firestore, является ли пользователь админом
        db.collection("admins").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Если не админ - возвращаем в каталог (обычный режим)
                    Toast.makeText(this, "Доступ запрещен", Toast.LENGTH_SHORT).show()
                    goToCatalog(false) // false = обычный режим
                }
                // Если админ - продолжаем работу (ничего не делаем)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка проверки прав", Toast.LENGTH_SHORT).show()
                goToCatalog(false) // false = обычный режим
            }
    }

    private fun loadAdminData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Загружаем данные админа из коллекции admins
            db.collection("admins").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val adminName = document.getString("name") ?: "Администратор"
                        val adminEmail = document.getString("email") ?: user.email ?: ""

                        val adminNameText = findViewById<TextView>(R.id.adminNameText)
                        val adminEmailText = findViewById<TextView>(R.id.adminEmailText)

                        adminNameText.text = adminName
                        adminEmailText.text = adminEmail
                    } else {
                        // Если данных нет в admins, используем данные из users
                        loadUserDataAsFallback(user.uid)
                    }
                }
                .addOnFailureListener {
                    // В случае ошибки загружаем данные из users
                    loadUserDataAsFallback(user.uid)
                }
        }
    }

    private fun loadUserDataAsFallback(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "Администратор"
                    val email = document.getString("email") ?: ""

                    val adminNameText = findViewById<TextView>(R.id.adminNameText)
                    val adminEmailText = findViewById<TextView>(R.id.adminEmailText)

                    adminNameText.text = username
                    adminEmailText.text = email
                }
            }
    }

    private fun setupAdminFeatures() {
        // Кнопка управления товарами - переход в каталог в режиме админа
        val manageProductsBtn: Button = findViewById(R.id.manageProductsBtn)
        manageProductsBtn.setOnClickListener {
            goToCatalog(true) // true = режим админа
        }

        // Остальные кнопки остаются заглушками
        val manageOrdersBtn: Button = findViewById(R.id.manageOrdersBtn)
        manageOrdersBtn.setOnClickListener {
            Toast.makeText(this, "Управление заказами - в разработке", Toast.LENGTH_SHORT).show()
        }

        val statisticsBtn: Button = findViewById(R.id.statisticsBtn)
        statisticsBtn.setOnClickListener {
            Toast.makeText(this, "Статистика - в разработке", Toast.LENGTH_SHORT).show()
        }

        val manageUsersBtn: Button = findViewById(R.id.manageUsersBtn)
        manageUsersBtn.setOnClickListener {
            Toast.makeText(this, "Управление пользователями - в разработке", Toast.LENGTH_SHORT).show()
        }

        // Кнопка выхода
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToCatalog(isAdminMode: Boolean) {
        val intent = Intent(this, CatalogActivity::class.java)
        intent.putExtra("admin_mode", isAdminMode)
        startActivity(intent)
        finish()
    }
}