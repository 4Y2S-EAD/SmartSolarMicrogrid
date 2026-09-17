package com.smartsolar.microgrid.member1.activities

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

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

        /*
         * TODO:
         *
         * POST /api/users/{nic}/deactivation-request
         *
         * {
         *    reason: reason
         * }
         *
         * Backend should create a pending
         * deactivation request for Backoffice.
         */

        Toast.makeText(
            this,
            "Deactivation request submitted for review",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}