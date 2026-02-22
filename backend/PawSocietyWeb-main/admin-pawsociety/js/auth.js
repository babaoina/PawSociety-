// js/auth.js

// State management
let resetCooldown = false;
let countdownInterval;

// Toggle between forms
function showForgotPassword() {
    document.getElementById('loginForm').classList.add('hidden');
    document.getElementById('forgotForm').classList.add('active');
    document.getElementById('formTitle').textContent = 'RESET PASSWORD';
    document.getElementById('formDescription').textContent = 'Enter your email and new password to reset your account.';

    // Clear any previous messages
    hideSuccessMessage();
    clearErrors();
}

function showLogin() {
    document.getElementById('loginForm').classList.remove('hidden');
    document.getElementById('forgotForm').classList.remove('active');
    document.getElementById('formTitle').textContent = 'WELCOME';
    document.getElementById('formDescription').innerHTML = 'With many pets waiting for a loving home,<br>PawSociety helps you discover adoptable pets<br>and reconnect lost animals with their families.';

    // Clear any previous messages
    hideSuccessMessage();
    clearErrors();

    // Clear reset form
    document.getElementById('resetEmail').value = '';
    document.getElementById('newPassword').value = '';
    document.getElementById('confirmPassword').value = '';
    document.getElementById('passwordStrength').textContent = '';
}

// Login function - UPDATED FOR FIREBASE INTEGRATION
async function login() {
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value.trim();

    clearErrors();

    let isValid = true;

    if (!email) {
        showError('email', 'Email is required');
        isValid = false;
    } else if (!isValidEmail(email)) {
        showError('email', 'Please enter a valid email address');
        isValid = false;
    }

    if (!password) {
        showError('password', 'Password is required');
        isValid = false;
    }

    if (!isValid) return;

    const loginBtn = document.getElementById('loginBtn');
    loginBtn.textContent = 'Logging in...';
    loginBtn.disabled = true;

    try {
        // Authenticate with Firebase
        const userCredential = await firebase.auth().signInWithEmailAndPassword(email, password);
        const user = userCredential.user;
        const idToken = await user.getIdToken();

        // Send token to backend to verify admin status
        const response = await fetch('/api/auth/verify-token', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': `Bearer ${idToken}`
            }
        });

        const data = await response.json();

        if (response.ok && data.user.role === 'admin') {
            localStorage.setItem('adminToken', idToken);
            localStorage.setItem('adminUser', JSON.stringify(data.user));
            window.location.href = 'dashboard.html';
        } else {
            loginBtn.textContent = 'Login';
            loginBtn.disabled = false;

            // Sign out from Firebase if not admin or error
            await firebase.auth().signOut();

            showError('email', 'Access Denied: Admin privileges required');
            document.getElementById('email').classList.add('error');
        }
    } catch (error) {
        console.error('Login error:', error);
        loginBtn.textContent = 'Login';
        loginBtn.disabled = false;

        if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
            showError('email', 'Invalid credentials');
            showError('password', 'Invalid credentials');
            document.getElementById('email').classList.add('error');
            document.getElementById('password').classList.add('error');
        } else {
            alert('Error during login: ' + error.message);
        }
    }
}

// Reset password function
function resetPassword() {
    if (resetCooldown) {
        alert('Please wait before requesting another password reset.');
        return;
    }

    const email = document.getElementById('resetEmail').value.trim();
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    clearErrors();

    let isValid = true;

    // Email validation
    if (!email) {
        showError('resetEmail', 'Email is required');
        isValid = false;
    } else if (!isValidEmail(email)) {
        showError('resetEmail', 'Please enter a valid email address');
        isValid = false;
    }

    // Password validation
    if (!newPassword) {
        showError('newPassword', 'New password is required');
        isValid = false;
    } else if (newPassword.length < 6) {
        showError('newPassword', 'Password must be at least 6 characters');
        isValid = false;
    } else if (!isValidPassword(newPassword)) {
        showError('newPassword', 'Password must contain at least one uppercase letter, one number, and one special character');
        isValid = false;
    }

    // Confirm password validation
    if (!confirmPassword) {
        showError('confirmPassword', 'Please confirm your password');
        isValid = false;
    } else if (newPassword !== confirmPassword) {
        showError('confirmPassword', 'Passwords do not match');
        isValid = false;
    }

    if (!isValid) return;

    // Check if email exists in our "database"
    if (email !== 'admin@pawsociety.com') {
        showError('resetEmail', 'Email not found in our system');
        return;
    }

    const resetBtn = document.getElementById('resetBtn');
    resetBtn.textContent = 'Resetting...';
    resetBtn.disabled = true;

    // Simulate API call
    setTimeout(() => {
        // Show success message
        showSuccessMessage('Password reset successfully! You can now login with your new password.');

        // Clear form
        document.getElementById('resetEmail').value = '';
        document.getElementById('newPassword').value = '';
        document.getElementById('confirmPassword').value = '';
        document.getElementById('passwordStrength').textContent = '';

        resetBtn.textContent = 'Reset Password';
        resetBtn.disabled = false;

        // Start cooldown
        startResetCooldown();

        // Auto redirect to login after 3 seconds
        setTimeout(() => {
            showLogin();
        }, 3000);
    }, 1500);
}

// Start cooldown timer
function startResetCooldown() {
    resetCooldown = true;
    let seconds = 30;

    const forgotLink = document.querySelector('.forgot-password a');
    const originalText = forgotLink.textContent;

    countdownInterval = setInterval(() => {
        seconds--;
        forgotLink.textContent = `Try again in ${seconds}s`;

        if (seconds <= 0) {
            clearInterval(countdownInterval);
            resetCooldown = false;
            forgotLink.textContent = originalText;
        }
    }, 1000);
}

// Show success message
function showSuccessMessage(message) {
    const successDiv = document.getElementById('successMessage');
    successDiv.style.display = 'block';
    successDiv.className = 'success-message';
    successDiv.textContent = message;
}

// Hide success message
function hideSuccessMessage() {
    document.getElementById('successMessage').style.display = 'none';
}

// Email validation
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Password strength validation
function isValidPassword(password) {
    // At least one uppercase, one number, one special character
    const passwordRegex = /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,}$/;
    return passwordRegex.test(password);
}

// Show error message
function showError(fieldId, message) {
    const field = document.getElementById(fieldId);
    field.classList.add('error');

    const existingError = field.parentNode.querySelector('.error-message');
    if (existingError) existingError.remove();

    const error = document.createElement('div');
    error.className = 'error-message';
    error.textContent = message;

    field.parentNode.appendChild(error);
}

// Clear all errors
function clearErrors() {
    document.querySelectorAll('input').forEach(input => {
        input.classList.remove('error');
    });
    document.querySelectorAll('.error-message').forEach(error => error.remove());
}

// Real-time validation
document.getElementById('email').addEventListener('input', function () {
    this.classList.remove('error');
    const error = this.parentNode.querySelector('.error-message');
    if (error) error.remove();
});

document.getElementById('password').addEventListener('input', function () {
    this.classList.remove('error');
    const error = this.parentNode.querySelector('.error-message');
    if (error) error.remove();
});

document.getElementById('resetEmail').addEventListener('input', function () {
    this.classList.remove('error');
    const error = this.parentNode.querySelector('.error-message');
    if (error) error.remove();
});

document.getElementById('newPassword').addEventListener('input', function () {
    this.classList.remove('error');
    const error = this.parentNode.querySelector('.error-message');
    if (error) error.remove();

    // Real-time password strength indicator
    const password = this.value;
    const strengthIndicator = document.getElementById('passwordStrength');

    if (password.length > 0) {
        if (password.length < 6) {
            strengthIndicator.textContent = 'Too short';
            strengthIndicator.style.color = '#F44336';
        } else if (isValidPassword(password)) {
            strengthIndicator.textContent = 'Strong password ✓';
            strengthIndicator.style.color = '#4CAF50';
        } else {
            strengthIndicator.textContent = 'Need: uppercase, number, and special character';
            strengthIndicator.style.color = '#FF9800';
        }
    } else {
        strengthIndicator.textContent = '';
    }
});

document.getElementById('confirmPassword').addEventListener('input', function () {
    this.classList.remove('error');
    const error = this.parentNode.querySelector('.error-message');
    if (error) error.remove();

    // Real-time match indicator
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = this.value;
    const strengthIndicator = document.getElementById('passwordStrength');

    if (confirmPassword.length > 0) {
        if (newPassword === confirmPassword) {
            strengthIndicator.textContent = 'Passwords match ✓';
            strengthIndicator.style.color = '#4CAF50';
        } else {
            strengthIndicator.textContent = 'Passwords do not match';
            strengthIndicator.style.color = '#F44336';
        }
    }
});

// Enter key press for login
document.getElementById('password').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
        login();
    }
});

// Enter key press for reset
document.getElementById('confirmPassword').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
        resetPassword();
    }
});

// Auto-focus email field on load
window.addEventListener('load', function () {
    document.getElementById('email').focus();
});

// Clean up interval on page unload
window.addEventListener('beforeunload', function () {
    if (countdownInterval) {
        clearInterval(countdownInterval);
    }
});
