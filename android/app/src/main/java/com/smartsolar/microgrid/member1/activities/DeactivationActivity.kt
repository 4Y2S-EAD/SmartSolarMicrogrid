package com.smartsolar.microgrid.member1.activities

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeactivationActivity : AppCompatActivity() {

    private lateinit var etReason: EditText
    private lateinit var cbConfirm: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_deactivation)

        etReason = findViewById(R.id.etReason)
        cbConfirm = findViewById(R.id.cbConfirm)

        findViewById<Button>(
            R.id.btnSubmitDeactivation
        ).setOnClickListener {
            validateRequest()
        }

        findViewById<Button>(
            R.id.btnCancel
        ).setOnClickListener {
            finish()
        }
    }

    private fun validateRequest() {

        val reason =
            etReason.text.toString().trim()

        if (reason.length < 10) {
            etReason.error =
                "Please provide a reason"
            return
        }

        if (!cbConfirm.isChecked) {
            Toast.makeText(
                this,
                "Please confirm that you understand the deactivation process",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Request")
            .setMessage(
                "Submit your account deactivation request?"
            )
            .setPositiveButton("Submit") { _, _ ->
                submitRequest(reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitRequest(reason: String) {
        val nic = com.smartsolar.microgrid.network.TokenManager.getNic()
        if (nic == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val request = com.smartsolar.microgrid.network.models.DeactivationRequest(reason = reason)
        val btnSubmitDeactivation = findViewById<Button>(R.id.btnSubmitDeactivation)
        btnSubmitDeactivation.isEnabled = false
        btnSubmitDeactivation.text = "Submitting..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.requestDeactivation(nic, request)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DeactivationActivity,
                        "Deactivation request submitted for review",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@DeactivationActivity, "Failed to submit: $errorBody", Toast.LENGTH_LONG).show()
                    btnSubmitDeactivation.isEnabled = true
                    btnSubmitDeactivation.text = "SUBMIT DEACTIVATION"
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeactivationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnSubmitDeactivation.isEnabled = true
                btnSubmitDeactivation.text = "SUBMIT DEACTIVATION"
            }
        }
    }
}