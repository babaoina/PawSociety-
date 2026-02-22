const express = require('express');
const router = express.Router();
const User = require('../models/User');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');

// JWT secret from environment variable
const JWT_SECRET = process.env.JWT_SECRET || 'pawsociety-admin-secret-key-2026';

/**
 * POST /api/auth/admin-login
 * Admin login using email/password (for admin web app)
 * Body: { email, password }
 */
router.post('/admin-login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required'
      });
    }

    // Find user by email
    const user = await User.findOne({ email });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Check if user is admin
    if (user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    // Verify password (assuming passwords are hashed - but current system uses Firebase)
    // For now, we'll use a simple check for development
    // In production, you would hash passwords during user creation
    const isPasswordValid = password === 'admin123' || 
                           (process.env.ADMIN_PASSWORD && password === process.env.ADMIN_PASSWORD);

    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'Invalid credentials'
      });
    }

    // Generate JWT token
    const token = jwt.sign(
      { 
        userId: user.firebaseUid,
        email: user.email,
        role: user.role,
        username: user.username
      },
      JWT_SECRET,
      { expiresIn: '24h' }
    );

    res.json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        firebaseUid: user.firebaseUid,
        username: user.username,
        email: user.email,
        role: user.role,
        fullName: user.fullName,
        profileImageUrl: user.profileImageUrl
      }
    });

  } catch (error) {
    console.error('Admin login error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

module.exports = router;