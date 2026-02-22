// js/firebase-config.js
const firebaseConfig = {
    apiKey: "AIzaSyDcNU6cS0iW79lXay2sLe3r3F_tTlL4d_w",
    authDomain: "device-streaming-5f25c661.firebaseapp.com",
    projectId: "device-streaming-5f25c661",
    storageBucket: "device-streaming-5f25c661.firebasestorage.app",
    messagingSenderId: "807156891924",
    appId: "1:807156891924:web:ff5058d28e9bc36bdfc201",
    measurementId: "G-H7S8F4W0D8"
};

// Initialize Firebase
if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
} else {
    firebase.app(); // if already initialized, use that one
}
