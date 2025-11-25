package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val registerButton: Button = findViewById(R.id.registerButton)
        val loginLink: TextView = findViewById(R.id.loginLink)

        registerButton.setOnClickListener {
            performRegistration()
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performRegistration() {
        val loginInput: TextInputEditText = findViewById(R.id.loginInput)
        val emailInput: TextInputEditText = findViewById(R.id.emailInput)
        val passwordInput: TextInputEditText = findViewById(R.id.passwordInput)

        val username = loginInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (validateInput(username, email, password)) {
            showLoading(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            // Сохраняем данные пользователя в Firestore
                            saveUserToFirestore(
                                user.uid,
                                username,
                                email,
                                ""
                            )
                        }
                        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()

                        // Проверяем роль пользователя и переходим на соответствующий экран
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            checkUserRoleAndNavigate(userId)
                        } else {
                            goToCatalog()
                        }
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun saveUserToFirestore(uid: String, username: String, email: String, photoUrl: String) {
        val userData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "displayName" to username,
            "photoUrl" to photoUrl,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                println("Пользователь сохранен в Firestore")
            }
            .addOnFailureListener { e ->
                println("Ошибка сохранения пользователя: $e")
                // Даже если сохранение в Firestore не удалось, продолжаем
                // Пользователь уже зарегистрирован в Firebase Auth
            }
    }

    /**
     * Проверяет роль пользователя и перенаправляет на соответствующий экран
     */
    private fun checkUserRoleAndNavigate(userId: String) {
        showLoading(true)

        db.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    // Пользователь является администратором
                    Toast.makeText(this, "Добро пожаловать в панель администратора!", Toast.LENGTH_SHORT).show()
                    goToAdmin()
                } else {
                    // Обычный пользователь
                    goToCatalog()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                println("Ошибка проверки прав администратора: ${exception.message}")
                // В случае ошибки считаем обычным пользователем
                goToCatalog()
            }
    }

    private fun validateInput(username: String, email: String, password: String): Boolean {
        if (username.isEmpty()) {
            Toast.makeText(this, "Введите логин", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showLoading(isLoading: Boolean) {
        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.isEnabled = !isLoading
        registerButton.text = if (isLoading) "РЕГИСТРАЦИЯ..." else "ЗАРЕГИСТРИРОВАТЬСЯ"
    }

    private fun goToCatalog() {
        val intent = Intent(this, CatalogActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
        finish()
    }
}