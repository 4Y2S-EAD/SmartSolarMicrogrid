package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member2.activities.M2StationsActivity

class GridOperatorDashboardActivity : AppCompatActivity() {

    private lateinit var btnQrApproval: MaterialButton
    private lateinit var btnManagement: MaterialButton
    private lateinit var btnMonitorStations: MaterialButton
    private lateinit var btnReservations: MaterialButton
    private lateinit var btnUsers: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_grid_operator)
        initializeViews()
        setupQuickActions()
        setupBottomNavigation()
        setupSidebar()
        setupAnimations()
        setupUserProfile()
    }

    private fun setupUserProfile() {
        val fullName = com.smartsolar.microgrid.network.TokenManager.getFullName()
        val role = com.smartsolar.microgrid.network.TokenManager.getRole()
        
        val tvWelcomeName = findViewById<android.widget.TextView>(R.id.tvWelcomeName)
        val tvWelcomeRole = findViewById<android.widget.TextView>(R.id.tvWelcomeRole)
        
        if (!fullName.isNullOrEmpty()) {
            tvWelcomeName.text = fullName
        }
        
        if (!role.isNullOrEmpty()) {
            // Format role text nicely if needed, e.g. "Grid Operator" instead of "GridOperator"
            val formattedRole = if (role == "GridOperator") "Grid Operator" else role
            tvWelcomeRole.text = formattedRole
        }
    }

    // STAGGERED ENTRANCE ANIMATIONS
    private fun setupAnimations() {
        // Quick action buttons — staggered slide-in
        val quickActionIds = listOf(
            R.id.btnQrApproval, R.id.btnManagement,
            R.id.btnMonitorStations, R.id.btnReservations,
            R.id.btnUsers, R.id.btnLogout
        )
        quickActionIds.forEachIndexed { index, id ->
            val btn = findViewById<View>(id) ?: return@forEachIndexed
            val anim = AnimationUtils.loadAnimation(this, R.anim.m4_slide_in_left)
            anim.startOffset = (index * 80).toLong()
            btn.startAnimation(anim)
        }
    }

    // INITIALIZE VIEWS
    private fun initializeViews() {
        btnQrApproval = findViewById(R.id.btnQrApproval)
        btnManagement = findViewById(R.id.btnManagement)
        btnMonitorStations = findViewById(R.id.btnMonitorStations)
        btnReservations = findViewById(R.id.btnReservations)
        btnUsers = findViewById(R.id.btnUsers)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navigationView)
    }

    // QUICK ACTION BUTTONS
    private fun setupQuickActions() {
        // QR Approvals
        btnQrApproval.setOnClickListener {
            startActivity(Intent(this, com.smartsolar.microgrid.member4.qr.OperatorQrScannerActivity::class.java))
        }
        // System Management
        btnManagement.setOnClickListener {
            Toast.makeText(this, "System Management", Toast.LENGTH_SHORT).show()
        }
        // Monitor Stations
        btnMonitorStations.setOnClickListener {
            startActivity(Intent(this, M2StationsActivity::class.java))
        }

        // Reservations
        btnReservations.setOnClickListener {
            startActivity(Intent(this, com.smartsolar.microgrid.member4.operator.OperatorReservationsActivity::class.java))
        }

        // Manage Users
        btnUsers.setOnClickListener {
            Toast.makeText(this, "Manage Users", Toast.LENGTH_SHORT).show()
        }
        // Logout
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupSidebar() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_logout -> showLogoutConfirmation()
                // other items can be added here if needed
            }
            true
        }
    }

    // BOTTOM NAVIGATION
    private fun setupBottomNavigation() {
        // Home is current page
        bottomNavigationView.selectedItemId = R.id.navOperatorHome
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // HOME
                R.id.navOperatorHome -> {
                    // Already on Grid Operator Dashboard
                    true
                }

                // APPROVALS
                R.id.navOperatorApprovals -> {
                    startActivity(Intent(this, com.smartsolar.microgrid.member4.qr.OperatorQrScannerActivity::class.java))
                    true
                }

                // STATIONS
                R.id.navOperatorStations -> {
                    startActivity(Intent(this, M2StationsActivity::class.java))
                    true
                }
                // PROFILE
                R.id.navOperatorProfile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    // LOGOUT
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
