package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_home)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvNIC = findViewById<TextView>(R.id.tvNIC)
        val tvRole = findViewById<TextView>(R.id.tvRole)

        /*
         * TODO:
         * Replace with SessionManager values:
         *
         * session.getNIC()
         * session.getRole()
         * session.getFullName()
         */

        tvWelcome.text = "Welcome, Solar Prosumer"
        tvNIC.text = "NIC: —"
        tvRole.text = "Role: Solar Prosumer"

        findViewById<Button>(R.id.btnProfile)
            .setOnClickListener {
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnLogout)
            .setOnClickListener {
                showLogoutConfirmation()
            }
    }

    private fun showLogoutConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage(
                "Are you sure you want to sign out?"
            )
            .setPositiveButton("Sign Out") { _, _ ->

                /*
                 * TODO:
                 * SessionManager.clearSession()
                 */

                val intent =
                    Intent(this, LoginActivity::class.java)

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}