package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var progressBar: ProgressBar

    private lateinit var etLastName: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etMiddleInitial: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPhone: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        initializeViews()
        setupValidationListeners()
        setupObservers()

        val backButton = findViewById<Button>(R.id.btn_back)
        val createAccountButton = findViewById<Button>(R.id.btn_create_account)

        backButton.setOnClickListener { finish() }

        createAccountButton.setOnClickListener {
            if (validateAllFields()) {
                createAccount()
            }
        }
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
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupValidationListeners() {
        etLastName.addTextChangedListener(SimpleTextWatcher { validateLastName() })
        etFirstName.addTextChangedListener(SimpleTextWatcher { validateFirstName() })
        etMiddleInitial.addTextChangedListener(SimpleTextWatcher { validateMiddleInitial() })
        etUsername.addTextChangedListener(SimpleTextWatcher { validateUsername() })
        etEmail.addTextChangedListener(SimpleTextWatcher { validateEmail() })
        etPassword.addTextChangedListener(SimpleTextWatcher {
            validatePassword()
            if (etConfirmPassword.text.toString().isNotEmpty()) validateConfirmPassword()
        })
        etConfirmPassword.addTextChangedListener(SimpleTextWatcher { validateConfirmPassword() })
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validatePhone() }
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
        })
    }

    private fun setupObservers() {
        viewModel.authResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is AuthResult.Success -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "✅ Account created! (Email auto-verified)", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is AuthResult.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun createAccount() {
        // Check emulator connection
        if (!MyApplication.isConnectedToEmulator) {
            Toast.makeText(this, "⚠️ Emulators not connected! Check logcat.", Toast.LENGTH_LONG).show()
            return
        }

        val fullName = "${etFirstName.text.toString().trim()} ${etLastName.text.toString().trim()}"
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val phone = etPhone.text.toString().trim()

        viewModel.register(fullName, username, email, password, phone)
    }

    private class SimpleTextWatcher(val onTextChanged: () -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onTextChanged() }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun validateLastName() = validateRequired(etLastName, "Last name")
    private fun validateFirstName() = validateRequired(etFirstName, "First name")
    
    private fun validateMiddleInitial(): Boolean {
        val initial = etMiddleInitial.text.toString().trim()
        if (initial.isNotEmpty() && (initial.length != 1 || !initial[0].isLetter())) {
            etMiddleInitial.error = "Invalid initial"
            return false
        }
        etMiddleInitial.error = null
        return true
    }

    private fun validateRequired(editText: EditText, fieldName: String): Boolean {
        val text = editText.text.toString().trim()
        return if (text.isEmpty()) {
            editText.error = "$fieldName is required"
            false
        } else {
            editText.error = null
            true
        }
    }

    private fun validateUsername(): Boolean {
        val username = etUsername.text.toString().trim()
        return if (username.length < 3) {
            etUsername.error = "Too short"
            false
        } else {
            etUsername.error = null
            true
        }
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        return if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email"
            false
        } else {
            etEmail.error = null
            true
        }
    }

    private fun validatePassword(): Boolean {
        val pass = etPassword.text.toString()
        return if (pass.length < 8) {
            etPassword.error = "Min 8 characters"
            false
        } else {
            etPassword.error = null
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val pass = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        return if (pass != confirm) {
            etConfirmPassword.error = "Passwords don't match"
            false
        } else {
            etConfirmPassword.error = null
            true
        }
    }

    private fun validatePhone(): Boolean {
        val phone = etPhone.text.toString().trim()
        return if (phone.length != 11) {
            etPhone.error = "11 digits required"
            false
        } else {
            etPhone.error = null
            true
        }
    }

    private fun validateAllFields(): Boolean {
        return validateLastName() && validateFirstName() && validateMiddleInitial() &&
                validateUsername() && validateEmail() && validatePassword() &&
                validateConfirmPassword() && validatePhone()
    }
}
