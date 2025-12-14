package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = Firebase.firestore
    private val RC_SIGN_IN = 9001
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        // Настройка Google Sign-In
        setupGoogleSignIn()

        // Если уже авторизован - проверяем права и переходим на соответствующий экран
        if (auth.currentUser != null) {
            val userId = auth.currentUser!!.uid
            val userEmail = auth.currentUser!!.email ?: ""
            checkUserRoleAndNavigate(userId, userEmail)
            return
        }

        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
            Log.d(TAG, "Google Sign-In настроен успешно")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки Google Sign-In: " + e.message)
            Toast.makeText(this, "Ошибка настройки Google входа", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        val loginButton: Button = findViewById(R.id.loginButton)
        val registerLink: TextView = findViewById(R.id.registerLink)
        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)

        loginButton.setOnClickListener {
            performLogin()
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun performLogin() {
        val emailInput: TextInputEditText = findViewById(R.id.emailInput)
        val passwordInput: TextInputEditText = findViewById(R.id.passwordInput)

        val loginOrEmail = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (validateInput(loginOrEmail, password)) {
            showLoading(true)

            // Определяем, что ввел пользователь - email или логин
            val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(loginOrEmail).matches()

            if (isEmail) {
                // Если это email - обычная авторизация
                signInWithEmail(loginOrEmail, password)
            } else {
                // Если это логин - ищем email по логину в Firestore
                signInWithUsername(loginOrEmail, password)
            }
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "✅ Вход выполнен!")
                    Log.d(TAG, "📧 Email: ${user?.email}")
                    Log.d(TAG, "🔑 UID: ${user?.uid}")

                    Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()

                    // Создаем/обновляем документ пользователя
                    user?.let {
                        createOrUpdateUserDocument(it.uid, email, email.substringBefore("@"))
                    }

                    // Проверяем роль пользователя и переходим на соответствующий экран
                    val userId = user?.uid
                    val userEmail = user?.email ?: email
                    if (userId != null) {
                        checkUserRoleAndNavigate(userId, userEmail)
                    } else {
                        goToCatalog()
                    }
                } else {
                    Toast.makeText(this, "Неверный email или пароль", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Ошибка входа: " + task.exception?.message)
                }
            }
    }

    private fun createOrUpdateUserDocument(uid: String, email: String, username: String) {
        val userData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "username" to username.lowercase(),
            "displayName" to username,
            "lastLoginAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Документ пользователя создан/обновлен: $username")

                // Если это админ q@gmail.com, создаем документ админа
                if (email.lowercase().trim() == "q@gmail.com") {
                    createAdminDocument(uid, email, username)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка сохранения пользователя: $e")
            }
    }

    private fun createAdminDocument(uid: String, email: String, username: String) {
        val adminData = hashMapOf(
            "email" to email,
            "name" to "Admin1",
            "role" to "admin",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("admins").document(uid)
            .set(adminData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Документ админа создан для: $email")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка создания документа админа: ${e.message}")
            }
    }

    private fun signInWithUsername(username: String, password: String) {
        // Ищем пользователя по username в Firestore
        db.collection("users")
            .whereEqualTo("username", username.lowercase())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this, "Пользователь с таким логином не найден", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Берем первый документ (username должен быть уникальным)
                val document = documents.documents[0]
                val email = document.getString("email") ?: ""

                if (email.isEmpty()) {
                    showLoading(false)
                    Toast.makeText(this, "Ошибка: email не найден", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Авторизуемся по найденному email
                signInWithEmail(email, password)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Ошибка поиска пользователя", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Ошибка поиска пользователя: " + exception.message)
            }
    }

    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска Google Sign-In: " + e.message)
            Toast.makeText(this, "Ошибка запуска Google входа", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google Sign-In успешен: " + account?.email)
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                showLoading(false)
                Log.e(TAG, "Google Sign-In провален: код=" + e.statusCode + ", сообщение=" + e.message)
                handleGoogleSignInError(e.statusCode)
            }
        }
    }

    private fun handleGoogleSignInError(statusCode: Int) {
        val errorMessage = when (statusCode) {
            4, 7 -> "Ошибка сети. Проверьте интернет-соединение"
            6, 8 -> "Внутренняя ошибка сервиса Google"
            10 -> "Неверная конфигурация приложения"
            13 -> "Ошибка безопасности"
            14 -> "Ошибка сервиса авторизации"
            16 -> "Вход отменен"
            17 -> "Ошибка авторизации"
            20 -> "Аккаунт не найден"
            12501 -> "Вход отменен пользователем"
            12502 -> "Ошибка выбора аккаунта"
            12500 -> "Ошибка входа в приложении"
            else -> "Неизвестная ошибка: $statusCode"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase аутентификация успешна: " + user?.email)
                    user?.let {
                        // Создаем username из email (все до @)
                        val username = it.email?.substringBefore("@") ?: "google_user"
                        val email = it.email ?: ""

                        // Сохраняем или обновляем пользователя в Firestore
                        createOrUpdateUserDocument(it.uid, email, username)
                    }
                    Toast.makeText(this, "Вход через Google выполнен!", Toast.LENGTH_SHORT).show()

                    // Проверяем роль пользователя и переходим на соответствующий экран
                    val userId = auth.currentUser?.uid
                    val userEmail = auth.currentUser?.email ?: ""
                    if (userId != null) {
                        checkUserRoleAndNavigate(userId, userEmail)
                    } else {
                        goToCatalog()
                    }
                } else {
                    Log.e(TAG, "Firebase аутентификация провалена: " + task.exception?.message)
                    Toast.makeText(this, "Ошибка аутентификации Firebase", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Проверяет роль пользователя и перенаправляет на соответствующий экран
     */
    private fun checkUserRoleAndNavigate(userId: String, email: String) {
        showLoading(true)

        Log.d(TAG, "=== ПРОВЕРКА ПРАВ ПОЛЬЗОВАТЕЛЯ ===")
        Log.d(TAG, "📧 Email: $email")
        Log.d(TAG, "🔑 UID: $userId")

        // Жесткая проверка по email
        if (email.lowercase().trim() == "q@gmail.com") {
            Log.d(TAG, "✅ ЭТО АДМИН q@gmail.com - ПЕРЕХОД В АДМИН ПАНЕЛЬ")
            showLoading(false)
            goToAdmin()
            return
        }

        // Для других пользователей проверяем через Firestore
        db.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    // Пользователь является администратором
                    Log.d(TAG, "✅ Админ найден в Firestore")
                    goToAdmin()
                } else {
                    // Обычный пользователь
                    Log.d(TAG, "❌ Не админ, переход в каталог")
                    goToCatalog()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "❌ Ошибка проверки прав администратора: ${exception.message}")
                // В случае ошибки считаем обычным пользователем
                goToCatalog()
            }
    }

    private fun validateInput(loginOrEmail: String, password: String): Boolean {
        if (loginOrEmail.isEmpty()) {
            Toast.makeText(this, "Введите логин или email", Toast.LENGTH_SHORT).show()
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
        val loginButton: Button = findViewById(R.id.loginButton)
        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)

        loginButton.isEnabled = !isLoading
        googleSignInButton.isEnabled = !isLoading
        loginButton.text = if (isLoading) "ВХОД..." else "ВОЙТИ"
    }

    private fun goToCatalog() {
        val intent = Intent(this, CatalogActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToAdmin() {
        Log.d(TAG, "🚀 ПЕРЕХОД В АДМИН ПАНЕЛЬ")
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}