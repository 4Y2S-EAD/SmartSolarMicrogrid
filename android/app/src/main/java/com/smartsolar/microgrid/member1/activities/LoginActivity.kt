package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email address"
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }

        /*
         * TODO:
         * Call:
         * POST /api/Auth/login
         *
         * Backend should:
         * 1. Check credentials
         * 2. Check role
         * 3. Check account status
         * 4. Return JWT + user information
         */

        Toast.makeText(
            this,
            "Connect this button to /api/Auth/login",
            Toast.LENGTH_SHORT
        ).show()

        // Temporary navigation for UI testing.
        startActivity(Intent(this, HomeActivity::class.java))
    }
}