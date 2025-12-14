package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val SPLASH_DELAY = 2000L // 2 секунды задержки
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Показываем загрузочный экран
        showSplashScreen()

        // Инициализируем Firebase
        auth = Firebase.auth
    }

    private fun showSplashScreen() {
        // Весь контент уже в layout
        // Просто ждем 2 секунды и проверяем аутентификацию
        handler.postDelayed({
            checkUserAuthentication()
        }, SPLASH_DELAY)
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Пользователь авторизован - проверяем его роль
            checkUserRoleAndNavigate(currentUser.uid, currentUser.email ?: "")
        } else {
            // Пользователь не авторизован - переходим на логин
            navigateToLogin()
        }
    }

    /**
     * Проверяет роль пользователя и перенаправляет на соответствующий экран
     */
    private fun checkUserRoleAndNavigate(userId: String, email: String) {
        Log.d("MainActivity", "Проверка: Email=$email, UID=$userId")

        // Жесткая проверка для админа
        if (email.lowercase().trim() == "q@gmail.com") {
            Log.d("MainActivity", "✅ Админ q@gmail.com - переход в админ панель")
            navigateToAdmin()
            return
        }

        // Для остальных пользователей проверяем через Firestore
        db.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Пользователь является администратором
                    navigateToAdmin()
                } else {
                    // Обычный пользователь
                    navigateToCatalog()
                }
            }
            .addOnFailureListener { exception ->
                // В случае ошибки считаем обычным пользователем
                Log.e("MainActivity", "Ошибка проверки прав администратора: ${exception.message}")
                navigateToCatalog()
            }
    }

    private fun navigateToAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCatalog() {
        val intent = Intent(this, CatalogActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        // Очищаем Handler чтобы избежать утечек памяти
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}