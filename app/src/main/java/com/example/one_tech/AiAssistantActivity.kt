package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding

class AiAssistantActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var sendButton: TextView
    private lateinit var chatScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        setupViews()
        setupClickListeners()

        // Показываем приветственное сообщение при запуске
        addMessageToChat(ChatResponses.welcomeMessage)
    }

    private fun setupViews() {
        chatContainer = findViewById(R.id.chatContainer)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatScrollView = findViewById(R.id.chatScrollView)

        // Очищаем начальное сообщение из XML
        chatContainer.removeAllViews()

        // Настраиваем поле ввода
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                sendButton.isEnabled = s?.isNotBlank() == true
                sendButton.alpha = if (sendButton.isEnabled) 1f else 0.5f
            }
        })
    }

    private fun setupClickListeners() {
        // Навигация
        findViewById<LinearLayout>(R.id.navCatalog).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        // Отправка сообщения
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                // Сообщение пользователя
                addMessageToChat(ChatMessage("$message", isUser = true))
                messageInput.text.clear()

                // Ответ ИИ (с небольшой задержкой для реалистичности)
                sendButton.postDelayed({
                    val aiResponse = ChatResponses.getResponse(message)
                    addMessageToChat(aiResponse)
                }, 500)
            }
        }

        // Отправка по Enter
        messageInput.setOnEditorActionListener { _, _, _ ->
            sendButton.performClick()
            true
        }
    }

    private fun addMessageToChat(chatMessage: ChatMessage) {
        runOnUiThread {
            val messageLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(16)
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = if (chatMessage.isUser) Gravity.END else Gravity.START
            }

            // Для сообщений ИИ добавляем синюю точку
            if (!chatMessage.isUser) {
                val dotView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(8),
                        dpToPx(8)
                    ).apply {
                        topMargin = dpToPx(8)
                        marginEnd = dpToPx(12)
                    }
                    setBackgroundColor(0xFF4169E1.toInt())
                }
                messageLayout.addView(dotView)
            }

            // Контейнер для текста сообщения
            val textContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12))
                background = if (chatMessage.isUser) {
                    getDrawable(R.drawable.bg_user_message)
                } else {
                    getDrawable(R.drawable.bg_ai_message)
                }
            }

            // Текст сообщения
            val messageTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = if (chatMessage.isUser) "Вы: ${chatMessage.text}" else chatMessage.text
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 16f
                // Для сообщений ИИ делаем текст с поддержкой переноса строк
                isSingleLine = false
                maxLines = 20
            }

            textContainer.addView(messageTextView)
            messageLayout.addView(textContainer)
            chatContainer.addView(messageLayout)

            // Прокрутка вниз
            chatScrollView.post {
                chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}