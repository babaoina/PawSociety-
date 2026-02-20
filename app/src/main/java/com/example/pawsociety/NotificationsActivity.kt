package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Back Button
        findViewById<android.widget.TextView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Follow Back Button
        findViewById<android.widget.TextView>(R.id.btn_follow_back).setOnClickListener {
            Toast.makeText(this, "Followed back!", Toast.LENGTH_SHORT).show()
            // In a real app, this would call an API to follow the user
        }

        // Check if user is logged in
        val currentUser = UserDatabase.getCurrentUser(this)
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view notifications", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Notification items are clickable too
        val notifications = listOf(
            findViewById<android.view.View>(R.id.notification_1),
            findViewById<android.view.View>(R.id.notification_2),
            findViewById<android.view.View>(R.id.notification_3),
            findViewById<android.view.View>(R.id.notification_4),
            findViewById<android.view.View>(R.id.notification_5)
        )

        notifications.forEachIndexed { index, notification ->
            notification?.setOnClickListener {
                val messages = listOf(
                    "View post liked by minnie_lover",
                    "View pet_rescuer_101's profile",
                    "View comment from dog_lover_ph",
                    "View new followers",
                    "View post with 48 likes"
                )
                Toast.makeText(this, messages[index], Toast.LENGTH_SHORT).show()
            }
        }
    }
}