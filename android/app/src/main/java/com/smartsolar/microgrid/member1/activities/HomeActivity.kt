package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member2.activities.M2StationsActivity
import com.smartsolar.microgrid.member3.activities.M3ReservationsActivity
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_home)

        // UI Components
        drawerLayout = findViewById(R.id.drawerLayout)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val navView = findViewById<NavigationView>(R.id.navigationView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val btnFindStations = findViewById<Button>(R.id.btnFindStations)
        val btnMyReservations = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMyReservations)
        val btnRequestDeactivation = findViewById<Button>(R.id.btnRequestDeactivation)
        val tvWelcomeName = findViewById<TextView>(R.id.tvWelcomeName)

        // Set welcome name (Later get this from TokenManager/Session)
        val nic = com.smartsolar.microgrid.network.TokenManager.getNic()
        if (nic != null) {
            tvWelcomeName.text = "Loading..."
            fetchUserProfile(nic, tvWelcomeName)
            fetchReservationDashboardStats(nic)
        } else {
            tvWelcomeName.text = "Welcome User"
        }

        // 1. Open Sidebar when Menu icon is clicked
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 2. Handle Sidebar Navigation Clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)

                // M2 Integration: Open Stations screen from Sidebar
                R.id.nav_stations -> {
                    startActivity(Intent(this, M2StationsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                }

                // M3 Integration: Open My Reservations from Sidebar
                R.id.nav_bookings -> {
                    startActivity(Intent(this, M3ReservationsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                }

                R.id.nav_logout -> showLogoutConfirmation()
            }
            true
        }
        // 3. Handle Bottom Navigation Bar Clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> true

                // M2 Integration: Open Stations screen from Bottom Bar
                R.id.bottom_stations -> {
                    startActivity(Intent(this, M2StationsActivity::class.java))
                    true
                }

                // M3 Integration: Open My Reservations from Bottom Bar
                R.id.bottom_bookings -> {
                    startActivity(Intent(this, M3ReservationsActivity::class.java))
                    true
                }

                R.id.bottom_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // 4. Handle Grid Quick Action Buttons
        btnFindStations.setOnClickListener {
            // M2 Integration: Open Stations screen from Grid Button
            startActivity(Intent(this, M2StationsActivity::class.java))
        }

        // M3 Integration: Open My Reservations from Quick Action
        btnMyReservations?.setOnClickListener {
            startActivity(Intent(this, M3ReservationsActivity::class.java))
        }

        btnRequestDeactivation.setOnClickListener {
            startActivity(Intent(this, DeactivationActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val nic = com.smartsolar.microgrid.network.TokenManager.getNic()
        if (nic != null) {
            fetchReservationDashboardStats(nic)
        }
    }

    private fun fetchReservationDashboardStats(nic: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.getUserReservationDashboard(nic)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val stats = response.body()!!
                        findViewById<TextView>(R.id.tvActiveCount)?.text = stats.approvedCount.toString()
                        findViewById<TextView>(R.id.tvPendingCount)?.text = stats.pendingCount.toString()
                        findViewById<TextView>(R.id.tvCompletedCount)?.text = stats.completedCount.toString()
                    }
                }
            } catch (e: Exception) {
                // Keep default 0 in case of error
            }
        }
    }

    private fun fetchUserProfile(nic: String, tvName: TextView) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val response = com.smartsolar.microgrid.network.ApiClient.apiService.getProfile(nic)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        tvName.text = response.body()!!.fullName
                    } else {
                        tvName.text = "Welcome!"
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    tvName.text = "Welcome!"
                }
            }
        }
    }

    // Function to show the logout dialog
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                com.smartsolar.microgrid.network.TokenManager.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
