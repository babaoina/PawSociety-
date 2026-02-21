package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pawsociety.api.ApiUser
import com.example.pawsociety.data.repository.AuthRepository
import com.example.pawsociety.util.FirebaseAuthHelper
import com.example.pawsociety.util.SessionManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etLastName: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etMiddleInitial: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPhone: EditText
    
    private val authRepository = AuthRepository()
    private lateinit var sessionManager: SessionManager

    private var isCreatingAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        sessionManager = SessionManager(this)

        initializeViews()
        setupValidationListeners()
        setupClickListeners()
    }

    private fun initializeViews() {
        etLastName = findViewById(R.id.et_last_name)
        etFirstName = findViewById(R.id.et_first_name)
        etMiddleInitial = findViewById(R.id.et_middle_initial)
        etUsername = findViewById(R.id.et_username)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        etPhone = findViewById(R.id.et_phone)
    }

    private fun setupClickListeners() {
        val backButton = findViewById<Button>(R.id.btn_back)
        val createAccountButton = findViewById<Button>(R.id.btn_create_account)

        backButton.setOnClickListener {
            finish()
        }

        createAccountButton.setOnClickListener {
            if (!isCreatingAccount && validateAllFields()) {
                createAccount()
            }
        }
    }

    private fun setupValidationListeners() {
        // Last Name validation
        etLastName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateLastName()
            }
        })

        // First Name validation
        etFirstName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateFirstName()
            }
        })

        // Middle initial validation
        etMiddleInitial.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateMiddleInitial()
            }
        })

        // Username validation
        etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateUsername()
            }
        })

        // Email validation
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail()
            }
        })

        // Password validation
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePassword()
                if (etConfirmPassword.text.toString().isNotEmpty()) {
                    validateConfirmPassword()
                }
            }
        })

        // Confirm Password validation
        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateConfirmPassword()
            }
        })

        // Phone validation with auto-formatting
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                val digits = input.filter { it.isDigit() }
                if (digits.length > 11) {
                    etPhone.setText(digits.substring(0, 11))
                    etPhone.setSelection(11)
                } else if (input != digits) {
                    etPhone.setText(digits)
                    etPhone.setSelection(digits.length)
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePhone()
            }
        })
    }

    private fun validateLastName(): Boolean {
        val name = etLastName.text.toString().trim()
        return when {
            name.isEmpty() -> {
                etLastName.error = "Last name is required"
                false
            }
            name.length < 2 -> {
                etLastName.error = "Last name must be at least 2 characters"
                false
            }
            !name.matches(Regex("^[a-zA-Z\\s.-]+$")) -> {
                etLastName.error = "Only letters, spaces, dots, and hyphens allowed"
                false
            }
            else -> {
                etLastName.error = null
                true
            }
        }
    }

    private fun validateFirstName(): Boolean {
        val name = etFirstName.text.toString().trim()
        return when {
            name.isEmpty() -> {
                etFirstName.error = "First name is required"
                false
            }
            name.length < 2 -> {
                etFirstName.error = "First name must be at least 2 characters"
                false
            }
            !name.matches(Regex("^[a-zA-Z\\s.-]+$")) -> {
                etFirstName.error = "Only letters, spaces, dots, and hyphens allowed"
                false
            }
            else -> {
                etFirstName.error = null
                true
            }
        }
    }

    private fun validateMiddleInitial(): Boolean {
        val initial = etMiddleInitial.text.toString().trim()
        return when {
            initial.isEmpty() -> true // Optional
            initial.length != 1 -> {
                etMiddleInitial.error = "Middle initial must be a single letter"
                false
            }
            !initial[0].isLetter() -> {
                etMiddleInitial.error = "Middle initial must be a letter"
                false
            }
            else -> {
                etMiddleInitial.error = null
                true
            }
        }
    }

    private fun validateUsername(): Boolean {
        val username = etUsername.text.toString().trim()
        return when {
            username.isEmpty() -> {
                etUsername.error = "Username is required"
                false
            }
            username.length < 3 -> {
                etUsername.error = "Username must be at least 3 characters"
                false
            }
            username.length > 20 -> {
                etUsername.error = "Username must be less than 20 characters"
                false
            }
            !username.matches(Regex("^[a-zA-Z0-9._]+$")) -> {
                etUsername.error = "Only letters, numbers, dots, and underscores allowed"
                false
            }
            username.matches(Regex("^[0-9].*")) -> {
                etUsername.error = "Username cannot start with a number"
                false
            }
            else -> {
                etUsername.error = null
                true
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        return when {
            email.isEmpty() -> {
                etEmail.error = "Email is required"
                false
            }
            !email.endsWith("@gmail.com", ignoreCase = true) -> {
                etEmail.error = "Only Gmail addresses are allowed (@gmail.com)"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Please enter a valid email address"
                false
            }
            else -> {
                etEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = etPassword.text.toString()

        return when {
            password.isEmpty() -> {
                etPassword.error = "Password is required"
                false
            }
            password.length < 8 -> {
                etPassword.error = "Password must be at least 8 characters"
                false
            }
            !password.matches(Regex(".*[A-Z].*")) -> {
                etPassword.error = "Must contain at least one uppercase letter"
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                etPassword.error = "Must contain at least one number"
                false
            }
            password.contains(" ") -> {
                etPassword.error = "Password cannot contain spaces"
                false
            }
            else -> {
                etPassword.error = null
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        return when {
            confirmPassword.isEmpty() -> {
                etConfirmPassword.error = "Please confirm your password"
                false
            }
            password != confirmPassword -> {
                etConfirmPassword.error = "Passwords do not match"
                false
            }
            else -> {
                etConfirmPassword.error = null
                true
            }
        }
    }

    private fun validatePhone(): Boolean {
        val phone = etPhone.text.toString().trim()

        return when {
            phone.isEmpty() -> {
                etPhone.error = "Phone number is required"
                false
            }
            phone.length != 11 -> {
                etPhone.error = "Phone number must be exactly 11 digits"
                false
            }
            !phone.matches(Regex("^09\\d{9}$")) -> {
                etPhone.error = "Must start with 09 (e.g., 09123456789)"
                false
            }
            else -> {
                etPhone.error = null
                true
            }
        }
    }

    private fun validateAllFields(): Boolean {
        val isLastNameValid = validateLastName()
        val isFirstNameValid = validateFirstName()
        val isMiddleInitialValid = validateMiddleInitial()
        val isUsernameValid = validateUsername()
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmPasswordValid = validateConfirmPassword()
        val isPhoneValid = validatePhone()

        return isLastNameValid && isFirstNameValid && isMiddleInitialValid &&
                isUsernameValid && isEmailValid && isPasswordValid &&
                isConfirmPasswordValid && isPhoneValid
    }

    private fun createAccount() {
        isCreatingAccount = true
        val createAccountButton = findViewById<Button>(R.id.btn_create_account)
        createAccountButton.isEnabled = false
        createAccountButton.text = "Creating Account..."

        val lastName = etLastName.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val middleInitial = etMiddleInitial.text.toString().trim()
        val fullName = if (middleInitial.isNotEmpty()) {
            "$lastName, $firstName $middleInitial."
        } else {
            "$lastName, $firstName"
        }

        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val phone = etPhone.text.toString().trim()

        lifecycleScope.launch {
            try {
                // Step 1: Register with Firebase
                println("🔐 Starting Firebase registration for: $email")
                val firebaseResult = FirebaseAuthHelper.registerWithEmail(email, password)

                if (firebaseResult.isFailure) {
                    val error = firebaseResult.exceptionOrNull()?.message ?: "Registration failed"
                    println("❌ Firebase registration failed: $error")
                    showError(getFirebaseAuthErrorMessage(error))
                    isCreatingAccount = false
                    createAccountButton.isEnabled = true
                    createAccountButton.text = "Create Account"
                    return@launch
                }

                val firebaseUser = firebaseResult.getOrNull()!!
                println("✅ Firebase registration successful! UID: ${firebaseUser.uid}")

                // Step 2: Create user in MongoDB via backend
                println("🌐 Creating user in backend...")
                val backendResult = authRepository.firebaseLogin(
                    firebaseUid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    username = username,
                    fullName = fullName,
                    phone = phone
                )

                if (backendResult.isFailure) {
                    val error = backendResult.exceptionOrNull()?.message ?: "Failed to connect to server"
                    println("❌ Backend registration failed: $error")
                    // Firebase succeeded but backend failed - create local session
                    val localUser = com.example.pawsociety.api.ApiUser(
                        firebaseUid = firebaseUser.uid,
                        email = firebaseUser.email ?: email,
                        username = username,
                        fullName = fullName
                    )
                    sessionManager.saveUserSession(localUser)
                    println("⚠️ Saved local session, proceeding to home...")
                    
                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created! (Offline mode)",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Navigate to Home
                    val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@launch
                }

                val apiUser = backendResult.getOrNull()!!
                println("✅ Backend user created! Username: ${apiUser.username}")

                // Step 3: Send email verification
                FirebaseAuthHelper.sendEmailVerification()
                println("📧 Email verification sent")

                // Step 4: Save session
                sessionManager.saveUserSession(apiUser)
                println("💾 Session saved")

                Toast.makeText(
                    this@RegisterActivity,
                    "Account created successfully! Please check your email to verify.",
                    Toast.LENGTH_LONG
                ).show()

                // Navigate to Home
                val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                println("❌ Unexpected error: ${e.message}")
                e.printStackTrace()
                showError(e.message ?: "An unexpected error occurred")
            } finally {
                isCreatingAccount = false
                createAccountButton.isEnabled = true
                createAccountButton.text = "Create Account"
            }
        }
    }
    
    private fun getFirebaseAuthErrorMessage(firebaseError: String): String {
        return when {
            firebaseError.contains("ERROR_EMAIL_ALREADY_IN_USE") -> "This email is already registered"
            firebaseError.contains("ERROR_INVALID_EMAIL") -> "Invalid email address"
            firebaseError.contains("ERROR_WEAK_PASSWORD") -> "Password is too weak"
            firebaseError.contains("ERROR_USER_DISABLED") -> "This account has been disabled"
            firebaseError.contains("ERROR_TOO_MANY_REQUESTS") -> "Too many attempts. Please try again later."
            else -> firebaseError
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
