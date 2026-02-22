const mongoose = require('mongoose');
require('dotenv').config();

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/pawsociety', {
  useNewUrlParser: true,
  useUnifiedTopology: true
}).then(async () => {
  console.log('✅ Connected to MongoDB');

  // Import models
  const User = require('./models/User');
  const Post = require('./models/Post');

  try {
    // Check collections
    console.log('\n📋 Collection Counts:');
    const userCount = await User.countDocuments();
    const postCount = await Post.countDocuments();
    console.log(`Users: ${userCount}`);
    console.log(`Posts: ${postCount}`);

    // Show sample users
    console.log('\n👥 Sample Users:');
    const users = await User.find().limit(3).select('username email fullName role firebaseUid');
    users.forEach((user, index) => {
      console.log(`${index + 1}. ${user.username} (${user.email}) - Role: ${user.role} - UID: ${user.firebaseUid}`);
    });

    // Show sample posts
    console.log('\n📝 Sample Posts:');
    const posts = await Post.find().limit(3).select('title content status moderationStatus firebaseUid createdAt');
    posts.forEach((post, index) => {
      console.log(`${index + 1}. "${post.title}" - Status: ${post.status} - Moderation: ${post.moderationStatus} - UID: ${post.firebaseUid}`);
    });

    // Check if admin user exists
    console.log('\n🔐 Admin Check:');
    const adminUser = await User.findOne({ email: 'admin@pawsociety.com' });
    if (adminUser) {
      console.log(`✅ Admin user found: ${adminUser.username} (${adminUser.email})`);
    } else {
      console.log('❌ Admin user NOT found');
    }

  } catch (error) {
    console.error('❌ Error checking data:', error);
  } finally {
    mongoose.disconnect();
  }
});