const User = require('../models/User');

/**
 * Role-based authorization middleware
 * 
 * Usage:
 * - For admin-only routes: router.use(roleAuth(['admin']))
 * - For user-only routes: router.use(roleAuth(['user']))
 * - For multiple roles: router.use(roleAuth(['user', 'admin']))
 * 
 * @param {string[]} allowedRoles - Array of roles that are allowed
 * @returns {Function} Express middleware function
 */
const roleAuth = (allowedRoles) => {
  return async (req, res, next) => {
    try {
      // Check if request has Firebase token (Android app)
      const firebaseToken = req.headers.authorization?.replace('Bearer ', '');
      
      // Check if request has JWT token (Admin web app)
      const jwtToken = req.headers.authorization?.replace('Bearer ', '');
      
      let userId = null;
      let userRole = null;

      // Try to extract user ID from Firebase token (if available)
      if (firebaseToken && !jwtToken) {
        // In production, this would be verified via Firebase Admin SDK
        // For now, we'll assume the token contains the UID (dev mode)
        // In real implementation, you would verify the token and extract uid
        userId = firebaseToken; // This is temporary - in production, verify token first
      } else if (jwtToken) {
        // Parse JWT token (this would be handled by jwt.verify in production)
        // For now, we'll simulate extracting from JWT payload
        // In real implementation, use jwt.verify with secret
        try {
          // Simulate JWT parsing - in production, use actual JWT verification
          // This is a placeholder for development
          const decoded = { userId: jwtToken.split('.')[1] }; // Simplified for demo
          userId = decoded.userId;
        } catch (error) {
          console.warn('JWT parsing failed, falling back to header extraction');
          userId = jwtToken;
        }
      }

      // If we have a userId, find the user in database
      if (userId) {
        const user = await User.findOne({ firebaseUid: userId });
        
        if (!user) {
          return res.status(404).json({
            success: false,
            message: 'User not found'
          });
        }

        userRole = user.role;

        // Check if user role is allowed
        if (!allowedRoles.includes(userRole)) {
          return res.status(403).json({
            success: false,
            message: `Access denied. Required role(s): ${allowedRoles.join(', ')}. Your role: ${userRole}`
          });
        }

        // Attach user info to request for downstream handlers
        req.user = {
          firebaseUid: user.firebaseUid,
          username: user.username,
          email: user.email,
          role: user.role,
          fullName: user.fullName
        };

        return next();
      }

      // No valid token found
      return res.status(401).json({
        success: false,
        message: 'Authentication required'
      });

    } catch (error) {
      console.error('Role authentication error:', error);
      return res.status(500).json({
        success: false,
        message: 'Internal server error'
      });
    }
  };
};

module.exports = roleAuth;