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
                        findViewById<TextView>(R.id.tvNIC).text = "NIC\n${profile.nic}"
                        findViewById<TextView>(R.id.tvEmail).text = "Email\n${profile.email}"
                        findViewById<TextView>(R.id.tvPhone).text = "Phone Number\n${profile.phoneNumber ?: "—"}"
                        findViewById<TextView>(R.id.tvAddress).text = "Address\n${profile.address ?: "—"}"
                        findViewById<TextView>(R.id.tvRole).text = "Role\n${profile.role}"
                        
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