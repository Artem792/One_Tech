package com.example.one_tech

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val TAG = "ProfileActivity"
    private val PICK_IMAGE_REQUEST = 1
    private val PREFS_NAME = "user_prefs"
    private val KEY_USER_NAME = "user_name"
    private val KEY_USER_EMAIL = "user_email"

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        Log.d(TAG, "ProfileActivity создан")

        setupClickListeners()
        setupAiAssistantButton()
        updateBottomNavigation()
        loadUserData()
        setupLogoutButton()
        setupEditProfileSection()
    }

    private fun setupEditProfileSection() {
        val settingsLayout = findViewById<LinearLayout>(R.id.settingsLayout)
        val editProfileSection = findViewById<LinearLayout>(R.id.editProfileSection)
        val settingsArrow = findViewById<TextView>(R.id.settingsArrow)
        val saveChangesButton = findViewById<Button>(R.id.saveChangesButton)
        val changePhotoButton = findViewById<Button>(R.id.changePhotoButton)

        settingsLayout.setOnClickListener {
            isEditMode = !isEditMode

            if (isEditMode) {
                editProfileSection.visibility = View.VISIBLE
                settingsArrow.text = "▲"

                val currentName = findViewById<TextView>(R.id.userNameText).text.toString()
                val editNameInput = findViewById<TextInputEditText>(R.id.editNameInput)
                editNameInput.setText(currentName)
            } else {
                editProfileSection.visibility = View.GONE
                settingsArrow.text = "▼"
            }
        }

        saveChangesButton.setOnClickListener {
            saveProfileChanges()
        }

        changePhotoButton.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            if (selectedImageUri != null) {
                Toast.makeText(this, "Фото выбрано (локально)", Toast.LENGTH_SHORT).show()
                val userAvatar = findViewById<TextView>(R.id.userAvatar)
                userAvatar.text = "📷"
                userAvatar.setTextColor(resources.getColor(android.R.color.holo_green_light, theme))
            }
        }
    }

    private fun saveProfileChanges() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Пользователь не авторизован")
            return
        }

        val editNameInput = findViewById<TextInputEditText>(R.id.editNameInput)
        val newName = editNameInput.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Сохранение профиля для пользователя: ${currentUser.uid}")
        Log.d(TAG, "Новое имя: $newName")

        // 1. Сначала сохраняем локально - это главное!
        saveUserDataLocally(newName, currentUser.email ?: "")

        // 2. Обновляем UI немедленно
        updateProfileUI(newName)

        // 3. Затем пробуем обновить Firestore (в фоне)
        updateUserInFirestore(currentUser.uid, newName, currentUser.email ?: "")

        Toast.makeText(this, "Профиль обновлен", Toast.LENGTH_SHORT).show()
    }

    private fun updateUserInFirestore(userId: String, newName: String, email: String) {
        Log.d(TAG, "Обновление Firestore: userId=$userId, name=$newName")

        // Создаем полный документ для set с merge
        val userData = hashMapOf<String, Any>(
            "userId" to userId,
            "username" to newName,
            "displayName" to newName,
            "email" to email,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // Добавляем createdAt только если документ новый
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    userData["createdAt"] = FieldValue.serverTimestamp()
                    userData["isGuest"] = false
                }

                // Используем set с merge для создания/обновления
                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Firestore успешно обновлен через set() с merge")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Ошибка set(): ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка проверки документа: ${e.message}")

                // Пробуем просто set
                userData["createdAt"] = FieldValue.serverTimestamp()
                userData["isGuest"] = false

                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Firestore успешно обновлен через set()")
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(TAG, "❌ Ошибка set(): ${e2.message}")
                    }
            }
    }

    private fun saveUserDataLocally(name: String, email: String) {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
        Log.d(TAG, "Данные сохранены локально: name=$name, email=$email")
    }

    private fun loadUserDataLocally(): Pair<String?, String?> {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val name = sharedPref.getString(KEY_USER_NAME, null)
        val email = sharedPref.getString(KEY_USER_EMAIL, null)
        return Pair(name, email)
    }

    private fun updateProfileUI(name: String) {
        runOnUiThread {
            try {
                val userNameTextView = findViewById<TextView>(R.id.userNameText)
                val userEmailTextView = findViewById<TextView>(R.id.userEmailText)
                val userAvatar = findViewById<TextView>(R.id.userAvatar)
                val editProfileSection = findViewById<LinearLayout>(R.id.editProfileSection)
                val settingsArrow = findViewById<TextView>(R.id.settingsArrow)

                userNameTextView.text = name

                // Обновляем email из локальных данных
                val (_, email) = loadUserDataLocally()
                userEmailTextView.text = email ?: ""

                if (name.isNotEmpty() && name.first().isLetter()) {
                    userAvatar.text = name.first().uppercaseChar().toString()
                    userAvatar.setTextColor(resources.getColor(android.R.color.white, theme))
                } else {
                    userAvatar.text = "👤"
                }

                editProfileSection.visibility = View.GONE
                settingsArrow.text = "▼"
                isEditMode = false

                Log.d(TAG, "UI обновлен с именем: $name")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления UI: ${e.message}")
            }
        }
    }

    private fun setupAiAssistantButton() {
        val aiAssistantButton = findViewById<TextView>(R.id.aiAssistantButton)
        aiAssistantButton?.setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.d(TAG, "Пользователь не авторизован - показываем гостевой UI")
            setupGuestUI()
            return
        }

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: ""

        Log.d(TAG, "Загрузка данных для пользователя: $userId")

        // ВСЕГДА сначала проверяем локальные данные
        val (localName, localEmail) = loadUserDataLocally()

        if (localName != null) {
            // Локальные данные есть - используем их сразу
            Log.d(TAG, "Используем локальные данные: name=$localName")
            updateUIWithUserData(localName, localEmail ?: userEmail)

            // В фоне синхронизируем с Firestore
            syncWithFirestore(userId, localName, localEmail ?: userEmail)
        } else {
            // Локальных данных нет - грузим из Firestore
            loadFromFirestore(userId, userEmail)
        }
    }

    private fun syncWithFirestore(userId: String, name: String, email: String) {
        // Просто обновляем Firestore из локальных данных
        val updates = hashMapOf<String, Any>(
            "username" to name,
            "displayName" to name,
            "email" to email,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Firestore синхронизирован с локальными данными")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Ошибка синхронизации Firestore, создаем документ: ${e.message}")

                // Пробуем создать документ
                val fullData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "username" to name,
                    "displayName" to name,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "isGuest" to false
                )

                db.collection("users").document(userId)
                    .set(fullData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Создан новый документ в Firestore")
                    }
            }
    }

    private fun loadFromFirestore(userId: String, userEmail: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firestoreName = document.getString("username") ?:
                    document.getString("displayName") ?:
                    document.getString("name") ?:
                    userEmail.substringBefore("@")

                    val firestoreEmail = document.getString("email") ?: userEmail

                    Log.d(TAG, "Данные из Firestore: name=$firestoreName")

                    // Сохраняем локально
                    saveUserDataLocally(firestoreName, firestoreEmail)

                    // Обновляем UI
                    updateUIWithUserData(firestoreName, firestoreEmail)
                } else {
                    // Документа нет - создаем с email как именем
                    val defaultName = userEmail.substringBefore("@")

                    // Сохраняем локально
                    saveUserDataLocally(defaultName, userEmail)

                    // Обновляем UI
                    updateUIWithUserData(defaultName, userEmail)

                    // Создаем в Firestore
                    syncWithFirestore(userId, defaultName, userEmail)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка загрузки из Firestore, используем email: ${e.message}")

                val defaultName = userEmail.substringBefore("@")

                // Сохраняем локально
                saveUserDataLocally(defaultName, userEmail)

                // Обновляем UI
                updateUIWithUserData(defaultName, userEmail)
            }
    }

    private fun updateUIWithUserData(name: String, email: String) {
        runOnUiThread {
            try {
                val userNameTextView = findViewById<TextView>(R.id.userNameText)
                val userEmailTextView = findViewById<TextView>(R.id.userEmailText)
                val userAvatar = findViewById<TextView>(R.id.userAvatar)

                userNameTextView.text = name
                userEmailTextView.text = email

                if (name.isNotEmpty() && name.first().isLetter()) {
                    userAvatar.text = name.first().uppercaseChar().toString()
                    userAvatar.setTextColor(resources.getColor(android.R.color.white, theme))
                } else {
                    userAvatar.text = "👤"
                }

                Log.d(TAG, "UI обновлен: name=$name, email=$email")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления UI с данными: ${e.message}")
            }
        }
    }

    private fun setupGuestUI() {
        runOnUiThread {
            try {
                val userNameTextView = findViewById<TextView>(R.id.userNameText)
                val userEmailTextView = findViewById<TextView>(R.id.userEmailText)
                val logoutButton = findViewById<Button>(R.id.logoutButton)
                val userAvatar = findViewById<TextView>(R.id.userAvatar)
                val settingsLayout = findViewById<LinearLayout>(R.id.settingsLayout)
                val editProfileSection = findViewById<LinearLayout>(R.id.editProfileSection)

                userNameTextView.text = "Гость"
                userEmailTextView.text = "Войдите в аккаунт"
                logoutButton.text = "ВОЙТИ В АККАУНТ"
                userAvatar.text = "👤"
                settingsLayout.visibility = View.GONE
                editProfileSection.visibility = View.GONE

                // Очищаем локальные данные для гостя
                clearLocalData()

                val scrollViewContent = findViewById<LinearLayout>(R.id.scrollViewContent)

                if (scrollViewContent == null) {
                    Log.e(TAG, "Не найден scrollViewContent!")
                    return@runOnUiThread
                }

                removeExistingGuestElements(scrollViewContent)

                val logoutIndex = scrollViewContent.indexOfChild(logoutButton)

                if (logoutIndex >= 0) {
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
            auth.signOut()
            clearLocalData()
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun clearLocalData() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
        Log.d(TAG, "Локальные данные очищены")
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

        setupProfileSections()
    }

    private fun setupProfileSections() {
        // Мои заказы - РЕАЛЬНЫЙ ПЕРЕХОД
        val myOrdersLayout = findViewById<LinearLayout>(R.id.myOrdersLayout)
        myOrdersLayout?.setOnClickListener {
            // Проверяем авторизацию
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Войдите в аккаунт, чтобы просмотреть заказы", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Переходим в активность "Мои заказы"
            try {
                Log.d(TAG, "Переход в UserOrdersActivity для пользователя: ${currentUser.uid}")
                val intent = Intent(this, UserOrdersActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка открытия UserOrdersActivity: ${e.message}", e)
                Toast.makeText(this, "Ошибка открытия заказов: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        // Избранное
        val favoritesLayout = findViewById<LinearLayout>(R.id.favoritesLayout)
        favoritesLayout?.setOnClickListener {
            Toast.makeText(this, "Избранное - в разработке", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBottomNavigation() {
        val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
        val navCart = findViewById<LinearLayout>(R.id.navCart)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        resetNavigationColors()

        val profileText = navProfile.getChildAt(1) as? TextView
        profileText?.setTextColor(resources.getColor(android.R.color.white, theme))
    }

    private fun resetNavigationColors() {
        val navCatalog = findViewById<LinearLayout>(R.id.navCatalog)
        val navCart = findViewById<LinearLayout>(R.id.navCart)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        val catalogText = navCatalog.getChildAt(1) as? TextView
        val cartText = navCart.getChildAt(1) as? TextView
        val profileText = navProfile.getChildAt(1) as? TextView

        catalogText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        cartText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        profileText?.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}