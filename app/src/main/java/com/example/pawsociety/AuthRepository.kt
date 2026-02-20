package com.example.pawsociety

import android.util.Log
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth by lazy {
        // Ensure emulator is set up before accessing
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("AuthRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseAuth.getInstance()
    }
    private val db: FirebaseFirestore by lazy {
        if (BuildConfig.DEBUG && !MyApplication.isEmulatorSetupComplete) {
            Log.e("AuthRepository", "⚠️ WARNING: Firebase accessed before emulator setup!")
        }
        FirebaseFirestore.getInstance()
    }

    init {
        Log.d("AuthRepository", "Initialized - Emulator connected: ${MyApplication.isConnectedToEmulator}")
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun register(user: AppUser, password: String): AuthResult<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Registering user: ${user.email}")

            val result = auth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Registration failed", "registration-failed")

            // Auto-verify email in emulator mode
            if (MyApplication.isConnectedToEmulator) {
                Log.d("AuthRepository", "Emulator mode: Auto-verifying email")
            }

            // Create user document in Firestore
            val userToSave = user.copy(
                uid = firebaseUser.uid,
                isEmailVerified = true // Auto-verified in emulator
            )

            db.collection("Users").document(firebaseUser.uid).set(userToSave).await()
            Log.d("AuthRepository", "User document created in Firestore")

            AuthResult.Success(firebaseUser)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Log.e("AuthRepository", "Weak password: ${e.message}")
            AuthResult.Error("Password should be at least 6 characters", "weak-password")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthRepository", "Invalid credentials: ${e.message}")
            AuthResult.Error("Invalid email address", "invalid-email")
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.e("AuthRepository", "User collision: ${e.message}")
            AuthResult.Error("Email already registered", "email-in-use")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Registration failed", null)
        }
    }

    suspend fun login(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Logging in user: $email")

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Login failed", "login-failed")

            // Skip email verification check in emulator mode
            if (MyApplication.isConnectedToEmulator) {
                Log.d("AuthRepository", "Emulator mode: Skipping email verification")
                return AuthResult.Success(firebaseUser)
            }

            // Production mode: verify email is verified
            firebaseUser.reload().await()
            val currentUser = auth.currentUser
            if (currentUser != null && !currentUser.isEmailVerified) {
                auth.signOut()
                return AuthResult.Error("Please verify your email address", "email-not-verified")
            }

            AuthResult.Success(firebaseUser)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthRepository", "Invalid credentials: ${e.message}")
            AuthResult.Error("Invalid email or password", "invalid-credentials")
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.e("AuthRepository", "Invalid user: ${e.message}")
            AuthResult.Error("No account found with this email", "user-not-found")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Login failed", null)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        return try {
            Log.d("AuthRepository", "Sending password reset to: $email")
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Error("No account found with this email", "user-not-found")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Password reset error: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Failed to send reset email", null)
        }
    }

    suspend fun resendEmailVerification(): AuthResult<Unit> {
        return try {
            val user = auth.currentUser ?: return AuthResult.Error("Not signed in", "not-signed-in")
            user.sendEmailVerification().await()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Resend verification error: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Failed to send verification email", null)
        }
    }

    fun logout() {
        Log.d("AuthRepository", "Logging out user")
        auth.signOut()
    }

    // Phone Authentication (Emulator-compatible)
    suspend fun startPhoneNumberVerification(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): AuthResult<Unit> {
        return try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Phone auth start error: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Failed to start phone verification", null)
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult<FirebaseUser> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error("Phone auth failed", "phone-auth-failed")
            AuthResult.Success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Invalid OTP code", "invalid-otp")
        } catch (e: FirebaseTooManyRequestsException) {
            AuthResult.Error("SMS quota exceeded. Try later.", "too-many-requests")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Phone auth error: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Phone auth failed", null)
        }
    }
}
