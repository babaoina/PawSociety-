const mongoose = require('mongoose');
require('dotenv').config();

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/pawsociety', {
  useNewUrlParser: true,
  useUnifiedTopology: true
}).then(async () => {
  console.log('Connected to MongoDB');

  // Import User model
  const User = require('./models/User');

  // Create admin user
  const adminEmail = process.env.ADMIN_EMAIL || 'admin@pawsociety.com';
  const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';

  try {
    // Check if admin already exists
    const existingAdmin = await User.findOne({ email: adminEmail });
    if (existingAdmin) {
      console.log(`Admin user already exists: ${adminEmail}`);
      console.log(`Role: ${existingAdmin.role}`);
      return;
    }

    // Create new admin user
    const adminUser = new User({
      email: adminEmail,
      password: adminPassword, // Will be hashed by pre-save hook
      role: 'admin',
      fullName: 'Admin User',
      username: 'admin_user',
      firebaseUid: `admin-${Date.now()}`, // Unique Firebase UID for admin
      createdAt: new Date(),
      updatedAt: new Date()
    });

    await adminUser.save();
    console.log(`✅ Admin user created successfully:`);
    console.log(`   Email: ${adminUser.email}`);
    console.log(`   Role: ${adminUser.role}`);
    console.log(`   Firebase UID: ${adminUser.firebaseUid}`);
    console.log(`   Password: ${adminPassword} (will be hashed in DB)`);

  } catch (error) {
    console.error('❌ Error creating admin user:', error);
  } finally {
    mongoose.disconnect();
  }
});