package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNIC: EditText
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_register)

        etNIC = findViewById(R.id.etNIC)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            register()
        }

        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            finish()
        }
    }

    private fun register() {

        val nic = etNIC.text.toString().trim()
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (nic.isEmpty()) {
            etNIC.error = "NIC is required"
            return
        }

        if (name.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }

        if (password.length < 6) {
            etPassword.error = "Use at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        /*
         * TODO:
         * POST /api/Auth/register
         *
         * Send:
         * NIC
         * FullName
         * Email
         * Password
         * Role = Prosumer
         *
         * Backend should create:
         * AccountStatus = Pending
         */

        Toast.makeText(
            this,
            "Registration submitted",
            Toast.LENGTH_SHORT
        ).show()

        val intent =
            Intent(this, PendingActivationActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
    }
}