package com.example.pawsociety

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.InetAddress

class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
            private set

        // EMULATOR-ONLY MODE: Set your PC's local IP address here
        // Find it by running `ipconfig` in cmd (look for IPv4 Address)
        val emulatorHost: String = "192.168.1.38" // REQUIRED

        // Flag to track if emulators are connected
        var isConnectedToEmulator: Boolean = false

        // Prevent any Firebase access before emulator setup
        private var _emulatorSetupComplete = false
        val isEmulatorSetupComplete: Boolean get() = _emulatorSetupComplete
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d("FirebaseEmulator", "========== APPLICATION STARTING ==========")
        Log.d("FirebaseEmulator", "BuildConfig.DEBUG: ${BuildConfig.DEBUG}")
        Log.d("FirebaseEmulator", "Emulator Host: $emulatorHost")
        Log.d("FirebaseEmulator", "MODE: EMULATOR ONLY (no production fallback)")

        // ALWAYS connect to emulators in DEBUG mode - MUST be synchronous before any Firebase usage
        if (BuildConfig.DEBUG) {
            setupFirebaseEmulatorsSync()
        } else {
            Log.e("FirebaseEmulator", "RELEASE BUILD - Firebase emulators only work in DEBUG mode")
        }
    }

    private fun setupFirebaseEmulatorsSync() {
        try {
            // Test connectivity first
            val testHost = InetAddress.getByName(emulatorHost)
            val isReachable = testHost.isReachable(5000)

            if (!isReachable) {
                Log.e("FirebaseEmulator", "⚠️ Warning: Cannot ping $emulatorHost")
                Log.e("FirebaseEmulator", "Check: 1) Same WiFi  2) Firewall  3) Emulators running")
                Log.e("FirebaseEmulator", "Attempting connection anyway...")
            }
        } catch (e: Exception) {
            Log.e("FirebaseEmulator", "⚠️ Connectivity test failed: ${e.message}")
            Log.e("FirebaseEmulator", "Attempting connection anyway...")
        }

        setupFirebaseEmulators()
        _emulatorSetupComplete = true
    }

    private fun setupFirebaseEmulators() {
        try {
            Log.d("FirebaseEmulator", "========== FIREBASE EMULATOR SETUP ==========")
            Log.d("FirebaseEmulator", "Target Host: $emulatorHost")

            // IMPORTANT: Call useEmulator() BEFORE any other Firebase operations
            // This MUST be done before any getInstance() calls in repositories

            Log.d("FirebaseEmulator", "Step 1: Connecting to Auth Emulator at $emulatorHost:9099")
            val auth = FirebaseAuth.getInstance()
            auth.useEmulator(emulatorHost, 9099)
            Log.d("FirebaseEmulator", "✓ Auth emulator connected")

            Log.d("FirebaseEmulator", "Step 2: Connecting to Firestore Emulator at $emulatorHost:8080")
            val firestore = FirebaseFirestore.getInstance()
            firestore.useEmulator(emulatorHost, 8080)
            Log.d("FirebaseEmulator", "✓ Firestore emulator connected")

            Log.d("FirebaseEmulator", "Step 3: Connecting to Storage Emulator at $emulatorHost:9199")
            val storage = FirebaseStorage.getInstance()
            storage.useEmulator(emulatorHost, 9199)
            Log.d("FirebaseEmulator", "✓ Storage emulator connected")

            Log.d("FirebaseEmulator", "==========================================")
            Log.d("FirebaseEmulator", "✅✅✅ EMULATOR CONNECTION SUCCESSFUL ✅✅✅")
            Log.d("FirebaseEmulator", "==========================================")
            Log.d("FirebaseEmulator", "Endpoints:")
            Log.d("FirebaseEmulator", "  Auth:      http://$emulatorHost:9099")
            Log.d("FirebaseEmulator", "  Firestore: http://$emulatorHost:8080")
            Log.d("FirebaseEmulator", "  Storage:   http://$emulatorHost:9199")
            Log.d("FirebaseEmulator", "  UI:        http://$emulatorHost:4000")
            Log.d("FirebaseEmulator", "==========================================")
            Log.d("FirebaseEmulator", "🎯 ALL DATA WILL GO TO LOCAL EMULATORS ONLY")
            Log.d("FirebaseEmulator", "🎯 NO PRODUCTION DATA WILL BE AFFECTED")
            Log.d("FirebaseEmulator", "==========================================")

            isConnectedToEmulator = true

        } catch (e: Exception) {
            Log.e("FirebaseEmulator", "❌ Failed to connect: ${e.message}", e)
            isConnectedToEmulator = false
        }
    }
}
