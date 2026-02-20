package com.example.pawsociety

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class OtpActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var etPhone: EditText
    private lateinit var etOtp: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var btnVerifyOtp: Button
    private lateinit var btnBack: Button
    private lateinit var tvInstructions: TextView

    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        initializeViews()
        setupObservers()

        btnSendOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty() && phone.length == 11) {
                sendOtp(phone)
            } else {
                Toast.makeText(this, "Enter valid 11-digit phone number", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.isNotEmpty()) {
                verifyOtp(otp)
            } else {
                Toast.makeText(this, "Enter OTP code", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun initializeViews() {
        etPhone = findViewById(R.id.et_phone)
        etOtp = findViewById(R.id.et_otp)
        btnSendOtp = findViewById(R.id.btn_send_otp)
        btnVerifyOtp = findViewById(R.id.btn_verify_otp)
        btnBack = findViewById(R.id.btn_back)
        tvInstructions = findViewById(R.id.tv_instructions)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupObservers() {
        viewModel.otpState.observe(this) { state ->
            when (state) {
                is OtpState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is OtpState.Sent -> {
                    progressBar.visibility = View.GONE
                    verificationId = state.verificationId
                    tvInstructions.text = "OTP sent! Check terminal for code.\nEnter the 6-digit code below:"
                    etPhone.isEnabled = false
                    btnSendOtp.isEnabled = false
                    etOtp.isEnabled = true
                    btnVerifyOtp.isEnabled = true
                    Toast.makeText(this, "OTP sent! Check terminal", Toast.LENGTH_LONG).show()
                }
                is OtpState.Verified -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Phone verified!", Toast.LENGTH_SHORT).show()
                    // Auto-login after verification
                    lifecycleScope.launch {
                        val user = UserRepository().getCurrentUser()
                        if (user != null) {
                            startActivity(Intent(this@OtpActivity, HomeActivity::class.java))
                            finish()
                        }
                    }
                }
                is OtpState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun sendOtp(phone: String) {
        viewModel.sendOtp(phone, this)
    }

    private fun verifyOtp(otp: String) {
        val verificationId = verificationId
        if (verificationId != null) {
            viewModel.verifyOtp(verificationId, otp)
        }
    }
}
