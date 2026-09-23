package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R

class GridOperatorDashboardActivity : AppCompatActivity() {

    private lateinit var btnQrApproval: MaterialButton
    private lateinit var btnManagement: MaterialButton
    private lateinit var btnMonitorStations: MaterialButton
    private lateinit var btnReservations: MaterialButton
    private lateinit var btnUsers: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.m1_activity_grid_operator)

        initializeViews()

        setupQuickActions()

        setupBottomNavigation()
    }


    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private fun initializeViews() {

        btnQrApproval = findViewById(R.id.btnQrApproval)

        btnManagement = findViewById(R.id.btnManagement)

        btnMonitorStations = findViewById(R.id.btnMonitorStations)

        btnReservations = findViewById(R.id.btnReservations)

        btnUsers = findViewById(R.id.btnUsers)

        btnLogout = findViewById(R.id.btnLogout)

        bottomNavigationView =
            findViewById(R.id.bottomNavigationView)
    }


    // ============================================================
    // QUICK ACTION BUTTONS
    // ============================================================

    private fun setupQuickActions() {

        // QR Approvals
        btnQrApproval.setOnClickListener {



            Toast.makeText(
                this,
                "QR Approvals",
                Toast.LENGTH_SHORT
            ).show()
        }


        // System Management
        btnManagement.setOnClickListener {



            Toast.makeText(
                this,
                "System Management",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Monitor Stations
        btnMonitorStations.setOnClickListener {



            Toast.makeText(
                this,
                "Monitor Stations",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Reservations
        btnReservations.setOnClickListener {



            Toast.makeText(
                this,
                "Reservations",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Manage Users
        btnUsers.setOnClickListener {



            Toast.makeText(
                this,
                "Manage Users",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Logout
        btnLogout.setOnClickListener {

            performLogout()
        }
    }


    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    private fun setupBottomNavigation() {

        // Home is current page
        bottomNavigationView.selectedItemId =
            R.id.navOperatorHome


        bottomNavigationView.setOnItemSelectedListener { item ->

            when (item.itemId) {

                // ------------------------------------------------
                // HOME
                // ------------------------------------------------

                R.id.navOperatorHome -> {

                    // Already on Grid Operator Dashboard

                    true
                }


                // ------------------------------------------------
                // APPROVALS
                // ------------------------------------------------

                R.id.navOperatorApprovals -> {



                    Toast.makeText(
                        this,
                        "QR Approvals",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }


                // ------------------------------------------------
                // STATIONS
                // ------------------------------------------------

                R.id.navOperatorStations -> {



                    Toast.makeText(
                        this,
                        "Station Monitoring",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }


                // ------------------------------------------------
                // PROFILE
                // ------------------------------------------------

                R.id.navOperatorProfile -> {



                    Toast.makeText(
                        this,
                        "Operator Profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }


                else -> false
            }
        }
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    private fun performLogout() {




        Toast.makeText(
            this,
            "Logged out successfully",
            Toast.LENGTH_SHORT
        ).show()



    }
}