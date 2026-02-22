const express = require('express');
const router = express.Router();
const User = require('../models/User');
const { verifyFirebaseToken } = require('./auth');
const verifyAdminToken = require('./jwtAuth');

/**
 * GET /api/admin/users/:firebaseUid
 * Get single user (admin-only)
 */
router.get('/users/:firebaseUid', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    const { firebaseUid } = req.params;
    const user = await User.findOne({ firebaseUid })
      .select('-firebaseUid');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      user
    });
  } catch (error) {
    console.error('Get user error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

/**
 * PUT /api/admin/users/:firebaseUid
 * Update user (admin-only)
 */
router.put('/users/:firebaseUid', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    const { firebaseUid } = req.params;
    const { username, fullName, bio, profileImageUrl, phone, location, role } = req.body;

    // Validate role if provided
    if (role && role !== 'user' && role !== 'admin') {
      return res.status(400).json({
        success: false,
        message: 'Invalid role. Must be "user" or "admin"'
      });
    }

    // Prevent admin from changing their own role
    if (role && req.adminUser.firebaseUid === firebaseUid && role !== req.adminUser.role) {
      return res.status(400).json({
        success: false,
        message: 'You cannot change your own role'
      });
    }

    // Check if username is taken by another user
    if (username) {
      const existingUser = await User.findOne({
        username,
        firebaseUid: { $ne: firebaseUid }
      });

      if (existingUser) {
        return res.status(400).json({
          success: false,
          message: 'Username already taken'
        });
      }
    }

    const user = await User.findOneAndUpdate(
      { firebaseUid },
      { username, fullName, bio, profileImageUrl, phone, location, role },
      { new: true, runValidators: true }
    ).select('-firebaseUid');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      message: 'User updated',
      user
    });
  } catch (error) {
    console.error('Update user error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

/**
 * DELETE /api/admin/users/:firebaseUid
 * Delete user (admin-only)
 */
router.delete('/users/:firebaseUid', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    const { firebaseUid } = req.params;

    // Don't allow deleting the current admin
    if (req.user.firebaseUid === firebaseUid) {
      return res.status(400).json({
        success: false,
        message: 'Cannot delete yourself'
      });
    }

    const user = await User.findOneAndDelete({ firebaseUid });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      message: 'User deleted'
    });
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

module.exports = router;