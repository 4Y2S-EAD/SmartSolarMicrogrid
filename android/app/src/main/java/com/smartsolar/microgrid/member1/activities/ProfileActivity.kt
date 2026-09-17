package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_profile)

        loadProfile()

        findViewById<Button>(R.id.btnEditProfile)
            .setOnClickListener {
                startActivity(
                    Intent(this, EditProfileActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnDeactivate)
            .setOnClickListener {
                startActivity(
                    Intent(this, DeactivationActivity::class.java)
                )
            }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {

        /*
         * TODO:
         *
         * 1. Read NIC from SessionManager
         * 2. GET /api/users/{nic}
         * 3. Display returned MongoDB user
         */

        findViewById<TextView>(R.id.tvFullName)
            .text = "Solar Prosumer"

        findViewById<TextView>(R.id.tvNIC)
            .text = "NIC\n—"

        findViewById<TextView>(R.id.tvEmail)
            .text = "Email\n—"

        findViewById<TextView>(R.id.tvPhone)
            .text = "Phone Number\n—"

        findViewById<TextView>(R.id.tvAddress)
            .text = "Address\n—"

        findViewById<TextView>(R.id.tvRole)
            .text = "Role\nSolar Prosumer"
    }
}