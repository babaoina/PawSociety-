const express = require('express');
const router = express.Router();
const User = require('../models/User');
const { verifyFirebaseToken } = require('./auth');
const verifyAdminToken = require('./jwtAuth');

/**
 * GET /api/admin/settings
 * Get admin settings (admin-only)
 */
router.get('/settings', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    // Return default settings
    res.json({
      success: true,
      settings: {
        siteName: 'PawSociety',
        siteDescription: 'A community platform for pet lovers',
        contactEmail: 'admin@pawsociety.com',
        timezone: 'UTC',
        allowRegistration: true,
        emailVerification: true,
        defaultRole: 'user',
        twoFactorAuth: false,
        sessionTimeout: 120,
        autoApprovePosts: false,
        profanityFilter: true,
        maxUploadSize: 5 * 1024 * 1024, // 5MB
        allowedFileTypes: ['image/jpeg', 'image/png', 'image/gif']
      }
    });
  } catch (error) {
    console.error('Get admin settings error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

/**
 * PUT /api/admin/settings
 * Update admin settings (admin-only)
 */
router.put('/settings', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    const { settings } = req.body;

    // In a real implementation, save settings to database
    // For now, just return success
    res.json({
      success: true,
      message: 'Settings updated successfully'
    });
  } catch (error) {
    console.error('Update admin settings error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

module.exports = router;