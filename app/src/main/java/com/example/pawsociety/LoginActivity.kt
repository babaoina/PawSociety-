package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pawsociety.api.ApiUser
import com.example.pawsociety.data.repository.AuthRepository
import com.example.pawsociety.util.FirebaseAuthHelper
import com.example.pawsociety.util.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var forgotPassword: TextView
    
    private val authRepository = AuthRepository()
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        sessionManager = SessionManager(this)
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn() && FirebaseAuthHelper.isSignedIn) {
            navigateToHome()
            return
        }
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signupButton = findViewById(R.id.signup_button)
        forgotPassword = findViewById(R.id.forgot_password)
    }
    
    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            
            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }
        
        signupButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        forgotPassword.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            } else {
                sendPasswordResetEmail(email)
            }
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email address"
            return false
        }
        
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return false
        }
        
        return true
    }
    
    private fun performLogin(email: String, password: String) {
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."

        lifecycleScope.launch {
            try {
                // Step 1: Authenticate with Firebase
                println("🔐 Starting Firebase login for: $email")
                val firebaseResult = FirebaseAuthHelper.loginWithEmail(email, password)

                if (firebaseResult.isFailure) {
                    val error = firebaseResult.exceptionOrNull()?.message ?: "Login failed"
                    println("❌ Firebase login failed: $error")
                    showError(getFirebaseAuthErrorMessage(error))
                    loginButton.isEnabled = true
                    loginButton.text = "Log In"
                    return@launch
                }

                val firebaseUser = firebaseResult.getOrNull()!!
                println("✅ Firebase login successful! UID: ${firebaseUser.uid}")

                // Step 2: Get or create user in MongoDB via backend
                println("🌐 Connecting to backend API...")
                val backendResult = authRepository.firebaseLogin(
                    firebaseUid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    username = firebaseUser.displayName,
                    fullName = firebaseUser.displayName,
                    phone = firebaseUser.phoneNumber
                )

                if (backendResult.isFailure) {
                    val error = backendResult.exceptionOrNull()?.message ?: "Failed to connect to server"
                    println("❌ Backend login failed: $error")
                    // Firebase login succeeded but backend failed - still save Firebase user
                    // Create a local ApiUser for session
                    val localUser = ApiUser(
                        firebaseUid = firebaseUser.uid,
                        email = firebaseUser.email ?: email,
                        username = firebaseUser.displayName ?: email.split("@").first(),
                        fullName = firebaseUser.displayName ?: email.split("@").first()
                    )
                    sessionManager.saveUserSession(localUser)
                    println("⚠️ Saved local session, proceeding to home...")
                    
                    Toast.makeText(this@LoginActivity, "Logged in! (Offline mode)", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                    return@launch
                }

                val apiUser = backendResult.getOrNull()!!
                println("✅ Backend login successful! Username: ${apiUser.username}")

                // Step 3: Save session
                sessionManager.saveUserSession(apiUser)
                println("💾 Session saved")

                Toast.makeText(this@LoginActivity, "Welcome back, ${apiUser.username}!", Toast.LENGTH_SHORT).show()

                navigateToHome()

            } catch (e: Exception) {
                println("❌ Unexpected error: ${e.message}")
                e.printStackTrace()
                showError(e.message ?: "An unexpected error occurred")
            } finally {
                loginButton.isEnabled = true
                loginButton.text = "Log In"
            }
        }
    }
    
    private fun sendPasswordResetEmail(email: String) {
        lifecycleScope.launch {
            try {
                val result = FirebaseAuthHelper.sendPasswordResetEmail(email)
                
                if (result.isSuccess) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Password reset link sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    Toast.makeText(this@LoginActivity, error, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getFirebaseAuthErrorMessage(firebaseError: String): String {
        return when {
            firebaseError.contains("ERROR_USER_NOT_FOUND") -> "No account found with this email"
            firebaseError.contains("ERROR_WRONG_PASSWORD") -> "Incorrect password"
            firebaseError.contains("ERROR_INVALID_EMAIL") -> "Invalid email address"
            firebaseError.contains("ERROR_USER_DISABLED") -> "This account has been disabled"
            firebaseError.contains("ERROR_TOO_MANY_REQUESTS") -> "Too many failed attempts. Please try again later."
            else -> firebaseError
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
