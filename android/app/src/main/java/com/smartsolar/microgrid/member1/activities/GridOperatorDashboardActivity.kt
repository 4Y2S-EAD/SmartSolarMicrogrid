package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.network.TokenManager

class GridOperatorDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_operator_dashboard)

        val btnQrApproval = findViewById<Button>(R.id.btnQrApproval)
        val btnManagement = findViewById<Button>(R.id.btnManagement)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnQrApproval.setOnClickListener {
            Toast.makeText(this, "QR Approvals Clicked", Toast.LENGTH_SHORT).show()
        }

        btnManagement.setOnClickListener {
            Toast.makeText(this, "System Management Clicked", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            TokenManager.clear()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
