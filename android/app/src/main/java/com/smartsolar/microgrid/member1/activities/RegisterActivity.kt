package com.smartsolar.microgrid.member1.activities

import android.widget.EditText

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

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
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
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

        val request = com.smartsolar.microgrid.network.models.RegisterRequest(
            nic = nic,
            fullName = name,
            email = email,
            phoneNumber = phone,
            address = address,
            password = password
        )

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.isEnabled = false
        btnRegister.text = "Registering..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.register(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, "Registration submitted successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, PendingActivationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@RegisterActivity, "Registration failed: $errorBody", Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "REGISTER"
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnRegister.isEnabled = true
                btnRegister.text = "REGISTER"
            }
        }
    }
}