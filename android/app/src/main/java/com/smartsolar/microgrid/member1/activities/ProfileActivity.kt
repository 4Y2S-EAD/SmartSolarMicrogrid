package com.smartsolar.microgrid.member1.activities

import android.os.Bundle

import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_profile)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

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

        val nic = com.smartsolar.microgrid.network.TokenManager.getNic()

        if (nic == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.getProfile(nic)
                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        findViewById<TextView>(R.id.tvFullName).text = profile.fullName
                        findViewById<TextView>(R.id.tvNIC).text = profile.nic
                        findViewById<TextView>(R.id.tvEmail).text = profile.email
                        findViewById<TextView>(R.id.tvPhone).text = profile.phoneNumber ?: "—"
                        findViewById<TextView>(R.id.tvAddress).text = profile.address ?: "—"
                        findViewById<TextView>(R.id.tvRole).text = profile.role
                        
                        val initials = profile.fullName.split(" ").map { it.first() }.take(2).joinToString("")
                        findViewById<TextView>(R.id.tvProfileInitial).text = initials
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}