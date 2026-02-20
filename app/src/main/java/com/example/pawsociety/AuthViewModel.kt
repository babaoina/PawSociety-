package com.example.pawsociety

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    // UI State
    private val _uiState = MutableLiveData<UiState<AppUser>>()
    val uiState: LiveData<UiState<AppUser>> = _uiState

    // Current user
    private val _currentUser = MutableLiveData<AppUser?>()
    val currentUser: LiveData<AppUser?> = _currentUser

    // Auth result
    private val _authResult = MutableLiveData<AuthResult<FirebaseUser>>()
    val authResult: MutableLiveData<AuthResult<FirebaseUser>> = _authResult

    // OTP State
    private val _otpState = MutableLiveData<OtpState>()
    val otpState: LiveData<OtpState> = _otpState

    // Password reset state (separate from auth result)
    private val _passwordResetResult = MutableLiveData<AuthResult<Unit>>()
    val passwordResetResult: LiveData<AuthResult<Unit>> = _passwordResetResult

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val user = userRepository.getCurrentUser()
            _currentUser.value = user
            _uiState.value = if (user != null) {
                UiState.Success(user)
            } else {
                UiState.Error("No user logged in")
            }
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        phone: String
    ) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading

            val user = AppUser(
                fullName = fullName,
                username = username,
                email = email,
                phone = phone
            )

            val result = authRepository.register(user, password)
            _authResult.value = result

            if (result is AuthResult.Success) {
                _currentUser.value = userRepository.getCurrentUser()
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading

            val result = authRepository.login(email, password)
            _authResult.value = result

            if (result is AuthResult.Success) {
                _currentUser.value = userRepository.getCurrentUser()
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _uiState.value = UiState.Error("Logged out")
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _passwordResetResult.value = AuthResult.Loading
            val result = authRepository.sendPasswordResetEmail(email)
            _passwordResetResult.value = result
        }
    }

    // OTP Functions
    fun sendOtp(phoneNumber: String, activity: android.app.Activity) {
        viewModelScope.launch {
            _otpState.value = OtpState.Loading

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("AuthViewModel", "OTP Auto-verified")
                    viewModelScope.launch {
                        val result = authRepository.signInWithPhoneCredential(credential)
                        if (result is AuthResult.Success) {
                            _otpState.value = OtpState.Verified
                            _currentUser.value = userRepository.getCurrentUser()
                        } else if (result is AuthResult.Error) {
                            _otpState.value = OtpState.Error(result.message)
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("AuthViewModel", "OTP verification failed: ${e.message}", e)
                    _otpState.value = OtpState.Error(e.localizedMessage ?: "Verification failed")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d("AuthViewModel", "OTP sent! VerificationId: $verificationId")
                    _otpState.value = OtpState.Sent(verificationId)
                }

                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    Log.d("AuthViewModel", "OTP auto-retrieval timeout: $verificationId")
                    _otpState.value = OtpState.Error("Auto-retrieval timed out")
                }
            }

            val result = authRepository.startPhoneNumberVerification(
                phoneNumber = phoneNumber,
                activity = activity,
                callbacks = callbacks
            )

            if (result is AuthResult.Error) {
                _otpState.value = OtpState.Error(result.message)
            }
        }
    }

    fun verifyOtp(verificationId: String, otp: String) {
        viewModelScope.launch {
            _otpState.value = OtpState.Loading

            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val result = authRepository.signInWithPhoneCredential(credential)

            if (result is AuthResult.Success) {
                _otpState.value = OtpState.Verified
                _currentUser.value = userRepository.getCurrentUser()
            } else if (result is AuthResult.Error) {
                _otpState.value = OtpState.Error(result.message)
            }
        }
    }

    fun clearAuthResult() {
        _authResult.value = null
    }

    fun clearOtpState() {
        _otpState.value = OtpState.Idle
    }

    fun clearPasswordResetResult() {
        _passwordResetResult.value = null
    }
}
