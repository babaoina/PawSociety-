/**
 * Helper function to check if user has permission to access a resource
 * 
 * @param {Object} req - Express request object
 * @param {string} targetUserId - The ID of the user being accessed
 * @param {string[]} allowedRoles - Roles that are allowed
 * @returns {boolean} Whether the request is authorized
 */
const canAccessResource = (req, targetUserId, allowedRoles = []) => {
  // If no user info available, deny access
  if (!req.user) {
    return false;
  }

  // Check if user has one of the allowed roles
  const hasAllowedRole = allowedRoles.some(role => req.user.role === role);
  
  // Check if user is accessing their own resource
  const isSelfAccess = req.user.firebaseUid === targetUserId;

  // Admin can access anything
  if (req.user.role === 'admin') {
    return true;
  }

  // User can access their own resource
  if (isSelfAccess && allowedRoles.includes('user')) {
    return true;
  }

  // Check if user has any of the allowed roles
  return hasAllowedRole;
};

module.exports = { canAccessResource };