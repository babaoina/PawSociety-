const mongoose = require('mongoose');
const User = require('./models/User');

async function makeAdmin() {
    await mongoose.connect('mongodb://localhost:27017/pawsociety');
    const result = await User.updateOne(
        { email: 'admin@test.com' },
        { $set: { role: 'admin' } }
    );
    console.log(`Updated admin@test.com to admin. Modified count: ${result.modifiedCount}`);
    process.exit(0);
}
makeAdmin();
