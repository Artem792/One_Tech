package com.example.one_tech

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        checkUserAuthentication()
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToCatalog()
        } else {
            navigateToLogin()
        }
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