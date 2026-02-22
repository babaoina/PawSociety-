const express = require('express');
const mongoose = require('mongoose');
require('dotenv').config();

const app = express();
const PORT = 5001;

// Middleware
app.use(express.json());

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/pawsociety', {
  useNewUrlParser: true,
  useUnifiedTopology: true
}).then(() => {
  console.log('✅ Diagnostic server connected to MongoDB');
});

// Test endpoint that returns real data
app.get('/api/diagnostic', async (req, res) => {
  try {
    const User = require('./models/User');
    const Post = require('./models/Post');

    const [userCount, postCount] = await Promise.all([
      User.countDocuments(),
      Post.countDocuments()
    ]);

    res.json({
      success: true,
      data: {
        totalUsers: userCount,
        totalPosts: postCount,
        timestamp: new Date().toISOString()
      }
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Diagnostic server running on http://localhost:${PORT}`);
});