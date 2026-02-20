package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check if user is already logged in
        val currentUser = UserDatabase.getCurrentUser(this)
        if (currentUser != null) {
            // User is already logged in, go to Home
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val loginButton = findViewById<Button>(R.id.login_button)
        val signupButton = findViewById<Button>(R.id.signup_button)
        val forgotPassword = findViewById<TextView>(R.id.forgot_password)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Try to login
            val user = UserDatabase.loginUser(this, email, password)

            if (user != null) {
                // Login successful
                Toast.makeText(this, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Login failed
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        forgotPassword.setOnClickListener {
            // Show forgot password dialog
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        // Create a simple dialog for password reset
        val emailInput = findViewById<EditText>(R.id.email_input)
        val email = emailInput.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email above first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if email exists
        if (UserDatabase.checkEmailExists(this, email)) {
            Toast.makeText(this, "Password reset link would be sent to $email", Toast.LENGTH_LONG).show()
            // In a real app, you would send an email here
        } else {
            Toast.makeText(this, "Email not found in our system", Toast.LENGTH_SHORT).show()
        }
    }
}