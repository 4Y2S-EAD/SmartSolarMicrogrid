package com.smartsolar.microgrid.member1.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

class PendingActivationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m1_activity_pending_activation)

        findViewById<Button>(R.id.btnCheckStatus)
            .setOnClickListener {

                /*
                 * TODO:
                 * Call backend and retrieve current
                 * account status.
                 *
                 * Pending -> remain here
                 * Active -> LoginActivity
                 * Rejected -> display reason
                 */

                Toast.makeText(
                    this,
                    "Account is still pending approval",
                    Toast.LENGTH_SHORT
                ).show()
            }

        findViewById<Button>(R.id.btnBackLogin)
            .setOnClickListener {

                val intent =
                    Intent(this, LoginActivity::class.java)

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
            }
    }
}