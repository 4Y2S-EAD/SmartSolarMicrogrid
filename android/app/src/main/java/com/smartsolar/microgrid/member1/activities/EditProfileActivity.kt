package com.smartsolar.microgrid.member1.activities

import android.widget.Button

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etNIC: EditText
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_edit_profile)

        etNIC = findViewById(R.id.etNIC)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)

        loadCurrentProfile()

        findViewById<Button>(R.id.btnSave)
            .setOnClickListener {
                saveProfile()
            }

        findViewById<Button>(R.id.btnCancel)
            .setOnClickListener {
                finish()
            }
    }

    private fun loadCurrentProfile() {
        val nic = com.smartsolar.microgrid.network.TokenManager.getNic()

        if (nic == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etNIC.setText(nic)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.getProfile(nic)
                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        etFullName.setText(profile.fullName)
                        etEmail.setText(profile.email)
                        etPhone.setText(profile.phoneNumber ?: "")
                        etAddress.setText(profile.address ?: "")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveProfile() {
        val nic = com.smartsolar.microgrid.network.TokenManager.getNic()
        if (nic == null) {
            finish()
            return
        }

        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }

        val request = com.smartsolar.microgrid.network.models.UpdateProfileRequest(
            fullName = fullName,
            email = email,
            phoneNumber = phone,
            address = address
        )

        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.updateProfile(nic, request)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile: $errorBody", Toast.LENGTH_LONG).show()
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE CHANGES"
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnSave.text = "SAVE CHANGES"
            }
        }
    }
}