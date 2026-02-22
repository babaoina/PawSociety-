require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const path = require('path');

// Import routes
const { router: authRoutes } = require('./routes/auth');
const userRoutes = require('./routes/users');
const postRoutes = require('./routes/posts');
const commentRoutes = require('./routes/comments');
const chatRoutes = require('./routes/chat');
const petRoutes = require('./routes/pets');
const favoriteRoutes = require('./routes/favorites');
const uploadRoutes = require('./routes/upload');
const notificationsRoutes = require('./routes/notifications');
const adminRoutes = require('./routes/admin');
const adminAuthRoutes = require('./routes/adminAuth');
const adminSettingsRoutes = require('./routes/adminSettings');
const adminUsersRoutes = require('./routes/adminUsers');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors({
  origin: function (origin, callback) {
    // Allow all origins for development (restrict in production)
    callback(null, true);
  },
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve admin web app at /admin path
const adminWebPath = path.join(__dirname, 'PawSocietyWeb-main', 'admin-pawsociety');
app.use('/admin', express.static(adminWebPath));

// Serve fixed dashboard at /admin/dashboard
app.get('/admin/dashboard', (req, res) => {
  res.sendFile(path.join(adminWebPath, 'fixed-dashboard.html'));
});

// Serve index.html for /admin/ routes (SPA fallback)
app.get('/admin/*', (req, res) => {
  res.sendFile(path.join(adminWebPath, 'index.html'));
});

// Serve static files for uploaded images
app.use('/api/uploads', express.static('uploads'));

// Request logging middleware
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
  next();
});

// MongoDB Connection
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/pawsociety';

mongoose.connect(MONGODB_URI)
  .then(() => {
    console.log('✅ Connected to MongoDB:', MONGODB_URI);
  })
  .catch((error) => {
    console.error('❌ MongoDB connection error:', error);
    process.exit(1);
  });

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/posts', postRoutes);
app.use('/api/comments', commentRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/pets', petRoutes);
app.use('/api/favorites', favoriteRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/notifications', notificationsRoutes);
// Admin routes - use specific paths to avoid conflicts
app.use('/api/admin', adminUsersRoutes);
app.use('/api/admin', adminSettingsRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/auth/admin', adminAuthRoutes);

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({
    status: 'OK',
    message: 'PawSociety Backend is running',
    timestamp: new Date().toISOString()
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal server error',
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found'
  });
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
  console.log('🚀 PawSociety Backend running on port', PORT);
  console.log('📍 Local:', `http://localhost:${PORT}`);
  console.log('📱 Network:', `http://192.168.x.x:${PORT} (replace with your IP)`);
  console.log('📲 Emulator:', `http://10.0.2.2:${PORT}`);
});

module.exports = app;
