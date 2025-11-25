package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        checkUserAuthentication()
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Пользователь авторизован - проверяем его роль
            checkUserRoleAndNavigate(currentUser.uid)
        } else {
            // Пользователь не авторизован - переходим на логин
            navigateToLogin()
        }
    }

    /**
     * Проверяет роль пользователя и перенаправляет на соответствующий экран
     */
    private fun checkUserRoleAndNavigate(userId: String) {
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
                println("Ошибка проверки прав администратора: ${exception.message}")
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
}