const { verifyFirebaseToken } = require('./auth');

/**
 * Verify Firebase token for admin authentication
 */
const verifyAdminToken = (req, res, next) => {
  verifyFirebaseToken(req, res, () => {
    if (req.user) {
      // Map user object to adminUser to satisfy backend checks
      req.adminUser = {
        userId: req.user.firebaseUid,
        email: req.user.email,
        role: req.user.role,
        username: req.user.username,
        firebaseUid: req.user.firebaseUid
      };
      next();
    } else {
      res.status(401).json({ success: false, message: 'Invalid token or user not found' });
    }
  });
};

module.exports = verifyAdminToken;