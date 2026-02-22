const http = require('http');
const querystring = require('querystring');

// Test users endpoint with the token we got from curl
const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJhZG1pbi0xNzcxNzY2NjQ1NzAzIiwiZW1haWwiOiJhZG1pbkBwYXdzb2NpZXR5LmNvbSIsInJvbGUiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW5fdXNlciIsImlhdCI6MTc3MTc2Njg1OCwiZXhwIjoxNzcxODUzMjU4fQ.YYdzhwYdNd0G79Us-KS8E8Vm9YUV8sIZe0MHYOCkTiw';

const options = {
  hostname: 'localhost',
  port: 5000,
  path: '/api/admin/users',
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
};

const req = http.request(options, (res) => {
  let data = '';
  
  res.on('data', (chunk) => {
    data += chunk;
  });
  
  res.on('end', () => {
    console.log('Users endpoint response:');
    console.log(data);
    
    try {
      const parsed = JSON.parse(data);
      console.log('Parsed response:', parsed);
    } catch (e) {
      console.error('Failed to parse JSON:', e);
    }
  });
});

req.on('error', (error) => {
  console.error('Request error:', error);
});

req.end();