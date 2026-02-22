const axios = require('axios');

async function testReportsEndpoint() {
  try {
    console.log('Testing /api/admin/reports endpoint...');
    
    // First, get admin token
    const loginResponse = await axios.post('http://localhost:5000/api/auth/admin-login', {
      email: 'admin@pawsociety.com',
      password: 'admin123'
    });
    
    const token = loginResponse.data.token;
    console.log('✅ Got token:', token.substring(0, 20) + '...');
    
    // Test reports endpoint
    const reportsResponse = await axios.get('http://localhost:5000/api/admin/reports', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Reports endpoint response:');
    console.log(JSON.stringify(reportsResponse.data, null, 2));
    
    // Test users endpoint
    const usersResponse = await axios.get('http://localhost:5000/api/admin/users', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Users endpoint response:');
    console.log(JSON.stringify(usersResponse.data, null, 2));
    
  } catch (error) {
    console.error('❌ Error:', error.response?.data || error.message);
    if (error.response) {
      console.error('Status:', error.response.status);
      console.error('Headers:', error.response.headers);
    }
  }
}

testReportsEndpoint();