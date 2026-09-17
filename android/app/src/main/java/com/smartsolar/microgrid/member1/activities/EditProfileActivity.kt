package com.smartsolar.microgrid.member1.activities

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

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

        /*
         * TODO:
         * GET /api/users/{nic}
         *
         * Populate:
         * FullName
         * Email
         * Phone
         * Address
         */

        etNIC.setText("")
    }

    private fun saveProfile() {

        val fullName =
            etFullName.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }

        /*
         * TODO:
         * PUT /api/users/{nic}
         *
         * Do NOT allow normal users to update:
         * NIC
         * Role
         * AccountStatus
         */

        Toast.makeText(
            this,
            "Profile updated successfully",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }
}