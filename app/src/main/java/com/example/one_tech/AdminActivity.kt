package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val TAG = "AdminActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        Log.d(TAG, "=== АДМИН ПАНЕЛЬ ЗАПУЩЕНА ===")

        // Настраиваем обработчик кнопки "Назад"
        setupBackPressedHandler()

        // Начинаем проверку и загрузку данных
        initializeAdminPanel()
    }

    private fun setupBackPressedHandler() {
        // Современный способ обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // При нажатии "Назад" показываем сообщение
                Toast.makeText(
                    this@AdminActivity,
                    "Нажмите 'Выйти' для выхода из админ панели",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initializeAdminPanel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "❌ Пользователь не авторизован")
            goToLogin()
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: ""

        Log.d(TAG, "🔍 Проверка пользователя: UID=$userId, Email=$userEmail")

        // Жесткая проверка для админа q@gmail.com
        if (userEmail.lowercase().trim() == "q@gmail.com") {
            Log.d(TAG, "✅ Обнаружен админ q@gmail.com")
            // 1. Создаем/обновляем документ пользователя
            createOrUpdateUserDocument(userId, userEmail, "Admin1")
            // 2. Создаем/обновляем документ админа
            createOrUpdateAdminDocument(userId, userEmail, "Admin1")
            // 3. Загружаем данные и показываем UI
            loadAdminData(userId, userEmail)
            // 4. Настраиваем кнопки
            setupAdminFeatures()
            return
        }

        // Для других пользователей проверяем через Firestore
        checkAdminInFirestore(userId, userEmail)
    }

    private fun checkAdminInFirestore(userId: String, userEmail: String) {
        db.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "✅ Админ найден в Firestore")
                    loadAdminData(userId, userEmail)
                    setupAdminFeatures()
                } else {
                    Log.d(TAG, "❌ Пользователь не является админом")
                    Toast.makeText(this, "Доступ запрещен", Toast.LENGTH_SHORT).show()
                    goToCatalog(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка проверки прав: ${e.message}")
                Toast.makeText(this, "Ошибка проверки прав", Toast.LENGTH_SHORT).show()
                goToCatalog(false)
            }
    }

    private fun createOrUpdateUserDocument(userId: String, email: String, name: String) {
        val userData = hashMapOf(
            "uid" to userId,
            "email" to email,
            "username" to name.lowercase().replace(" ", "_"),
            "displayName" to name,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "lastLoginAt" to com.google.firebase.Timestamp.now(),
            "isAdmin" to true
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Документ пользователя создан/обновлен")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка создания документа пользователя: ${e.message}")
            }
    }

    private fun createOrUpdateAdminDocument(userId: String, email: String, name: String) {
        val adminData = hashMapOf(
            "email" to email,
            "name" to name,
            "role" to "admin",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )

        db.collection("admins").document(userId)
            .set(adminData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Документ админа создан/обновлен")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка создания документа админа: ${e.message}")
            }
    }

    private fun loadAdminData(userId: String, userEmail: String) {
        Log.d(TAG, "📥 Загрузка данных админа...")

        // Пробуем загрузить из коллекции admins
        db.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Данные из admins
                    val adminName = document.getString("name") ?: "Администратор"
                    val adminEmail = document.getString("email") ?: userEmail

                    Log.d(TAG, "✅ Данные из admins: $adminName ($adminEmail)")
                    updateAdminUI(adminName, adminEmail)
                } else {
                    // Пробуем из users
                    loadAdminDataFromUsers(userId, userEmail)
                }
            }
            .addOnFailureListener {
                // Пробуем из users
                loadAdminDataFromUsers(userId, userEmail)
            }
    }

    private fun loadAdminDataFromUsers(userId: String, userEmail: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val adminName: String
                val adminEmail: String

                if (document.exists()) {
                    adminName = document.getString("displayName") ?:
                            document.getString("username") ?: "Администратор"
                    adminEmail = document.getString("email") ?: userEmail
                    Log.d(TAG, "✅ Данные из users: $adminName ($adminEmail)")
                } else {
                    // Используем данные из Auth
                    adminName = if (userEmail == "q@gmail.com") "Admin1" else "Администратор"
                    adminEmail = userEmail
                    Log.d(TAG, "📥 Используем данные из Auth: $adminName ($adminEmail)")
                }

                updateAdminUI(adminName, adminEmail)
            }
            .addOnFailureListener {
                // Используем данные из Auth
                val adminName = if (userEmail == "q@gmail.com") "Admin1" else "Администратор"
                updateAdminUI(adminName, userEmail)
            }
    }

    private fun updateAdminUI(adminName: String, adminEmail: String) {
        Log.d(TAG, "🎨 Обновление UI: Имя='$adminName', Email='$adminEmail'")

        runOnUiThread {
            try {
                val adminNameText = findViewById<TextView>(R.id.adminNameText)
                val adminEmailText = findViewById<TextView>(R.id.adminEmailText)
                val adminAvatar = findViewById<TextView>(R.id.adminAvatar)

                adminNameText.text = adminName
                adminEmailText.text = adminEmail

                // Устанавливаем первую букву имени в аватар (или эмодзи если пусто)
                if (adminName.isNotEmpty()) {
                    val firstChar = adminName.first()
                    if (firstChar.isLetterOrDigit()) {
                        adminAvatar.text = firstChar.toString().uppercase()
                    } else {
                        adminAvatar.text = "👨‍💼"
                    }
                } else {
                    adminAvatar.text = "👨‍💼"
                }

                Log.d(TAG, "✅ UI успешно обновлен")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка обновления UI: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun setupAdminFeatures() {
        Log.d(TAG, "⚙️ Настройка функций админ панели")

        runOnUiThread {
            try {
                // Кнопка управления товарами
                findViewById<Button>(R.id.manageProductsBtn).setOnClickListener {
                    goToCatalog(true)
                }

                // Кнопка управления заказами
                // В методе setupAdminFeatures() замените обработчик manageOrdersBtn:
                findViewById<Button>(R.id.manageOrdersBtn).setOnClickListener {
                    val intent = Intent(this, AdminOrdersActivity::class.java)
                    startActivity(intent)
                }

                // Кнопка статистики
                findViewById<Button>(R.id.statisticsBtn).setOnClickListener {
                    Toast.makeText(this, "Статистика - в разработке", Toast.LENGTH_SHORT).show()
                }

                // Кнопка управления пользователями
                findViewById<Button>(R.id.manageUsersBtn).setOnClickListener {
                    Toast.makeText(this, "Управление пользователей - в разработке", Toast.LENGTH_SHORT).show()
                }

                // Кнопка выхода
                findViewById<Button>(R.id.logoutButton).setOnClickListener {
                    auth.signOut()
                    Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                    goToLogin()
                }

                // Кнопка настроек (шестеренка)
                findViewById<TextView>(R.id.settingsButton)?.setOnClickListener {
                    Toast.makeText(this, "Настройки админа - в разработке", Toast.LENGTH_SHORT).show()
                }

                Log.d(TAG, "✅ Функции админ панели настроены")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка настройки функций: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToCatalog(isAdminMode: Boolean) {
        val intent = Intent(this, CatalogActivity::class.java)
        intent.putExtra("admin_mode", isAdminMode)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Удален старый метод onBackPressed()
}