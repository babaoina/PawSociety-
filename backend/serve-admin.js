const express = require('express');
const path = require('path');
const proxy = require('http-proxy-middleware');

const app = express();
const PORT = 8080;

// Serve static files from admin-pawsociety directory
const adminPath = path.join(__dirname, 'PawSocietyWeb-main', 'admin-pawsociety');
app.use(express.static(adminPath));

// Proxy API requests to backend (localhost:5000)
app.use('/api', proxy({
  target: 'http://localhost:5000',
  changeOrigin: true,
  secure: false,
  logLevel: 'debug'
}));

// Serve index.html for all other routes (SPA fallback)
app.get('*', (req, res) => {
  res.sendFile(path.join(adminPath, 'index.html'));
});

console.log(`🚀 Admin web app server running on http://localhost:${PORT}`);
console.log(`📁 Serving from: ${adminPath}`);
console.log(`🔗 Backend API proxy: http://localhost:5000/api → http://localhost:${PORT}/api`);

app.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ Server ready at: http://localhost:${PORT}`);
});