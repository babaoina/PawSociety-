package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Back Button
        findViewById<android.widget.TextView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Settings Options (keep existing code)
        val settingsOptions = listOf(
            R.id.option_follow_friends,
            R.id.option_notifications,
            R.id.option_privacy,
            R.id.option_security,
            R.id.option_ads,
            R.id.option_help_center,
            R.id.option_about
        )

        settingsOptions.forEach { optionId ->
            findViewById<android.view.View>(optionId).setOnClickListener {
                val optionName = when (optionId) {
                    R.id.option_follow_friends -> "Follow and invite friends"
                    R.id.option_notifications -> "Notifications"
                    R.id.option_privacy -> "Privacy"
                    R.id.option_security -> "Security"
                    R.id.option_ads -> "Ads"
                    R.id.option_help_center -> "Help Center"
                    R.id.option_about -> "About PawSociety"
                    else -> "Settings"
                }
                Toast.makeText(this, optionName, Toast.LENGTH_SHORT).show()
            }
        }

        // LOGOUT BUTTON - Updated with UserDatabase
        findViewById<android.view.View>(R.id.btn_logout).setOnClickListener {
            // Logout from database
            UserDatabase.logout(this)

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Go back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}