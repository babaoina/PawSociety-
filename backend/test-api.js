const axios = require('axios');

async function testApi() {
  try {
    console.log('Testing API endpoints...');
    
    // Test admin login first
    const loginResponse = await axios.post('http://localhost:5000/api/auth/admin-login', {
      email: 'admin@pawsociety.com',
      password: 'admin123'
    });
    
    console.log('✅ Login successful');
    const token = loginResponse.data.token;
    
    // Test users endpoint with auth
    const usersResponse = await axios.get('http://localhost:5000/api/admin/users', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Users endpoint response:');
    console.log(JSON.stringify(usersResponse.data, null, 2));
    
    // Test posts endpoint
    const postsResponse = await axios.get('http://localhost:5000/api/admin/posts', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Posts endpoint response:');
    console.log(JSON.stringify(postsResponse.data, null, 2));
    
  } catch (error) {
    console.error('❌ API Test failed:', error.response?.data || error.message);
  }
}

testApi();