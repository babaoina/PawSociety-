package com.example.pawsociety

/**
 * OTP Authentication State
 */
sealed class OtpState {
    object Loading : OtpState()
    data class Sent(val verificationId: String) : OtpState()
    object Verified : OtpState()
    data class Error(val message: String) : OtpState()
    object Idle : OtpState()
}
