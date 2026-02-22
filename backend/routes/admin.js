const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Post = require('../models/Post');
const { v4: uuidv4 } = require('uuid');
const { verifyFirebaseToken } = require('./auth');
const verifyAdminToken = require('./jwtAuth');

/**
 * GET /api/admin/users
 * Get all users (admin-only)
 */
router.get('/users', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    const { limit = 50, skip = 0, search = '' } = req.query;

    const query = {};
    if (search) {
      query.$or = [
        { username: { $regex: search, $options: 'i' } },
        { email: { $regex: search, $options: 'i' } },
        { fullName: { $regex: search, $options: 'i' } }
      ];
    }

    const users = await User.find(query)
      .select('username email fullName phone profileImageUrl bio location createdAt firebaseUid role')
      .limit(parseInt(limit))
      .skip(parseInt(skip))
      .sort({ createdAt: -1 });

    res.json({
      success: true,
      count: users.length,
      users
    });
  } catch (error) {
    console.error('Get admin users error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

/**
 * GET /api/admin/posts
 * Get all posts with moderation status (admin-only)
 */
router.get('/posts', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    const { status, limit = 50, skip = 0, search = '' } = req.query;

    const query = {};
    if (status) query.moderationStatus = status;
    if (search) {
      query.$or = [
        { petName: { $regex: search, $options: 'i' } },
        { description: { $regex: search, $options: 'i' } },
        { userName: { $regex: search, $options: 'i' } }
      ];
    }

    const posts = await Post.find(query)
      .populate('firebaseUid', 'username profileImageUrl')
      .limit(parseInt(limit))
      .skip(parseInt(skip))
      .sort({ createdAt: -1 });

    res.json({
      success: true,
      count: posts.length,
      posts
    });
  } catch (error) {
    console.error('Get admin posts error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

/**
 * GET /api/admin/posts/:id
 * Get a specific post details
 */
router.get('/posts/:id', verifyAdminToken, async (req, res) => {
  try {
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({ success: false, message: 'Admin access only' });
    }
    const post = await Post.findById(req.params.id).populate('firebaseUid', 'username profileImageUrl');
    if (!post) {
      return res.status(404).json({ success: false, message: 'Post not found' });
    }
    res.json(post);
  } catch (error) {
    console.error('Get admin post error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

/**
 * POST /api/admin/posts/:id/moderate
 * Update moderation status of a post
 */
router.post('/posts/:id/moderate', verifyAdminToken, async (req, res) => {
  try {
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({ success: false, message: 'Admin access only' });
    }
    const { status } = req.body;
    if (!['active', 'pending', 'rejected', 'removed'].includes(status)) {
      return res.status(400).json({ success: false, message: 'Invalid status' });
    }

    const post = await Post.findByIdAndUpdate(
      req.params.id,
      { moderationStatus: status },
      { new: true }
    );

    if (!post) {
      return res.status(404).json({ success: false, message: 'Post not found' });
    }
    res.json({ success: true, post });
  } catch (error) {
    console.error('Moderate post error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

/**
 * GET /api/admin/reports
 * Get system reports (admin-only)
 */
router.get('/reports', verifyAdminToken, async (req, res) => {
  try {
    // Check if user is admin
    if (!req.adminUser || req.adminUser.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: 'Admin access only'
      });
    }

    // Get basic system stats
    const [userCount, postCount, activePosts, pendingPosts] = await Promise.all([
      User.countDocuments(),
      Post.countDocuments(),
      Post.countDocuments({ moderationStatus: 'active' }),
      Post.countDocuments({ moderationStatus: 'pending' })
    ]);

    res.json({
      success: true,
      reports: {
        totalUsers: userCount,
        totalPosts: postCount,
        activePosts: activePosts,
        pendingModeration: pendingPosts,
        rejectedPosts: await Post.countDocuments({ moderationStatus: 'rejected' }),
        removedPosts: await Post.countDocuments({ moderationStatus: 'removed' }),
        today: new Date().toISOString().split('T')[0]
      }
    });
  } catch (error) {
    console.error('Get admin reports error:', error);
    res.status(500).json({
      success: false,
      message: error.message
    });
  }
});

module.exports = router;