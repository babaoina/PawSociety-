// ===== API CONFIGURATION =====
// Use relative paths since admin web app is served from same server
const API_BASE_URL = '/api';
let authToken = localStorage.getItem('adminToken');

// ===== API REQUEST HELPER =====
async function apiRequest(endpoint, options = {}) {
    const token = localStorage.getItem('adminToken');

    const headers = {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers
    };

    try {
        // Fix endpoint paths - add '/api' prefix for our backend structure
        let fullEndpoint = endpoint;
        if (endpoint.startsWith('/api/')) {
            fullEndpoint = endpoint.substring(4);
        }

        const response = await fetch(`${API_BASE_URL}${fullEndpoint}`, {
            ...options,
            headers
        });

        if (response.status === 401) {
            // Token expired
            localStorage.removeItem('adminToken');
            localStorage.removeItem('adminUser');
            if (!window.location.pathname.includes('index.html')) {
                window.location.href = 'index.html';
            }
            throw new Error('Session expired');
        }

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Request failed');
        }

        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// ===== INITIALIZE ON LOAD =====
document.addEventListener('DOMContentLoaded', function () {
    console.log('Admin panel loaded with API connection');

    // Check if we're on a protected page (not login)
    const path = window.location.pathname;
    if (!path.includes('index.html') && !localStorage.getItem('adminToken')) {
        window.location.href = 'index.html';
        return;
    }

    // Load page-specific data
    if (path.includes('dashboard.html')) {
        loadDashboardData();
    } else if (path.includes('users.html')) {
        loadUsersData();
    } else if (path.includes('posts.html')) {
        loadPostsData();
    } else if (path.includes('reports.html')) {
        loadReportsData();
    } else if (path.includes('settings.html')) {
        loadSettingsData();
    }
});

// ===== DASHBOARD FUNCTIONS =====
async function loadDashboardData() {
    try {
        const stats = await apiRequest('/admin/reports');

        // Update stats cards - map our response structure
        const reports = stats.reports || {};
        document.getElementById('total-users').textContent = reports.totalUsers || 0;
        document.getElementById('total-posts').textContent = reports.totalPosts || 0;
        // For lost pets and adoptions, use active posts as approximation (or calculate from posts)
        document.getElementById('lost-pets').textContent = reports.activePosts || 0;
        document.getElementById('adoptions').textContent = reports.activePosts || 0;

        // For activity feed, we'll use a simplified version since our backend doesn't have detailed activity yet
        const activityContainer = document.getElementById('activity-container');
        if (activityContainer) {
            activityContainer.innerHTML = `
                <div class="activity-item">
                    <div class="activity-icon">📊</div>
                    <div class="activity-details">
                        <div class="text">System initialized</div>
                        <div class="time">${new Date().toLocaleString()}</div>
                    </div>
                    <div class="activity-status status-new">New</div>
                </div>
            `;
        }

        // Animate numbers
        animateNumbers();

    } catch (error) {
        console.error('Failed to load dashboard:', error);
        // Fallback to default values
        document.getElementById('total-users').textContent = '0';
        document.getElementById('total-posts').textContent = '0';
        document.getElementById('lost-pets').textContent = '0';
        document.getElementById('adoptions').textContent = '0';
    }
}

// ===== USERS FUNCTIONS =====
async function loadUsersData() {
    try {
        const users = await apiRequest('/admin/users');
        displayUsers(users.users || []);
        updateUserStats(users.users || []);
    } catch (error) {
        console.error('Failed to load users:', error);
        // Fallback
        displayUsers([]);
        updateUserStats([]);
    }
}

function displayUsers(users) {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    if (!users || users.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 50px;">
                    <div style="font-size: 48px; margin-bottom: 20px;">👥</div>
                    <div style="font-size: 18px; color: #666;">No users found</div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = users.map(user => {
        // Handle names that might be split in MongoDB
        const fullName = user.username || `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Unknown User';
        // Generate avatar from initials if no profile picture
        let avatarHtml = '';
        if (user.profilePicture) {
            avatarHtml = `<img src="${user.profilePicture}" alt="${fullName}" style="width: 35px; height: 35px; border-radius: 50%; object-fit: cover;">`;
        } else {
            const initials = fullName.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
            const colors = ['#7A4F2B', '#B88B4A', '#2196F3', '#4CAF50', '#9C27B0'];
            // Use a simple hash of the name for consistent colors
            const charCodeSum = fullName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
            const color = colors[charCodeSum % colors.length] || '#7A4F2B';
            avatarHtml = `<div style="width: 35px; height: 35px; background: ${color}; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold;">${initials}</div>`;
        }

        const role = user.role || 'user';
        const status = user.status || 'Active'; // Assuming backend might start sending status
        const statusClass = status === 'Active' ? 'status-active' : 'status-suspended';

        // MongoDB stores _id, adjust to prioritize firebaseUid for our backend routes
        const userId = user.firebaseUid || user._id;

        return `
        <tr>
            <td>
                <div style="display: flex; align-items: center; gap: 10px;">
                    ${avatarHtml}
                    <div>${fullName}</div>
                </div>
            </td>
            <td>${user.email || ''}</td>
            <td><span style="text-transform: capitalize;">${role}</span></td>
            <td><span class="user-status ${statusClass}">${status}</span></td>
            <td>${user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</td>
            <td>0</td> <!-- Placeholder if posts aren't populated here -->
            <td>
                <div class="action-icons">
                    <span title="Edit" onclick="editUser('${userId}')">✏️</span>
                    ${role !== 'admin' ? `<span title="Delete" onclick="deleteUser('${userId}')">🗑️</span>` : ''}
                </div>
            </td>
        </tr>
        `;
    }).join('');
}

function updateUserStats(users) {
    const totalUsers = document.getElementById('totalUsers');
    if (totalUsers) {
        totalUsers.textContent = users.length;
    }
}

async function editUser(firebaseUid) {
    try {
        const data = await apiRequest(`/admin/users/${firebaseUid}`);
        const user = data.user;

        document.getElementById('userId').value = firebaseUid;
        document.getElementById('userName').value = user.fullName || user.username || '';
        document.getElementById('userEmail').value = user.email || '';
        document.getElementById('userRole').value = user.role ? (user.role.charAt(0).toUpperCase() + user.role.slice(1)) : 'User';

        const modalTitle = document.getElementById('modalTitle');
        if (modalTitle) modalTitle.textContent = 'Edit User';

        openModal('userModal');
    } catch (error) {
        console.error('Failed to load user details:', error);
        alert('Failed to load user details');
    }
}

async function saveUser() {
    const firebaseUid = document.getElementById('userId').value;
    const fullName = document.getElementById('userName').value;
    const role = document.getElementById('userRole').value.toLowerCase();

    // The backend doesn't support changing email easily via this endpoint yet, 
    // but fullName and role are supported by PUT /api/admin/users/:firebaseUid
    try {
        await apiRequest(`/admin/users/${firebaseUid}`, {
            method: 'PUT',
            body: JSON.stringify({ fullName, role })
        });

        closeModal('userModal');
        loadUsersData();
    } catch (error) {
        console.error('Failed to update user:', error);
        alert('Failed to update user: ' + error.message);
    }
}

async function deleteUser(firebaseUid) {
    if (confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
        try {
            await apiRequest(`/admin/users/${firebaseUid}`, { method: 'DELETE' });
            loadUsersData();
            alert('User deleted successfully');
        } catch (error) {
            console.error('Failed to delete user:', error);
            alert('Failed to delete user');
        }
    }
}

// ===== POSTS FUNCTIONS =====
async function loadPostsData() {
    try {
        const posts = await apiRequest('/admin/posts');
        displayPosts(posts.posts || []);
        updatePostStats(posts.posts || []);
    } catch (error) {
        console.error('Failed to load posts:', error);
        // Fallback
        displayPosts([]);
        updatePostStats([]);
    }
}

function displayPosts(posts) {
    const postsGrid = document.getElementById('postsGrid');
    if (!postsGrid) return;

    if (posts.length === 0) {
        postsGrid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 50px;">
                <div style="font-size: 48px; margin-bottom: 20px;">📝</div>
                <div style="font-size: 18px; color: #666;">No posts found</div>
            </div>
        `;
        return;
    }

    postsGrid.innerHTML = posts.map(post => {
        const statusClass = post.status === 'Lost' ? 'status-lost' : post.status === 'Found' ? 'status-found' : 'status-adoption';
        const moderationStatusClass = post.moderationStatus === 'active' ? 'status-active' :
            post.moderationStatus === 'pending' ? 'status-pending' :
                post.moderationStatus === 'rejected' ? 'status-rejected' : 'status-removed';

        let imageHtml = '';
        if (post.imageUrls && post.imageUrls.length > 0) {
            imageHtml = `<img src="${post.imageUrls[0]}" alt="${post.petName || 'Pet'}" style="width: 100%; height: 200px; object-fit: cover;">`;
        } else {
            const placeholderColor = post.status === 'Lost' ? '#FFEBEE' : post.status === 'Found' ? '#E8F5E9' : '#E3F2FD';
            imageHtml = `<div style="width: 100%; height: 200px; background-color: ${placeholderColor}; display: flex; align-items: center; justify-content: center; font-size: 48px;">📸</div>`;
        }

        const authorName = post.userId?.username || 'Unknown User';
        const avatarInitial = authorName.charAt(0).toUpperCase();

        return `
        <div class="post-card">
            <div class="post-image" style="padding: 0; overflow: hidden; position: relative;">
                ${imageHtml}
                <span class="post-status-badge ${statusClass}" style="position: absolute; top: 10px; left: 10px;">${post.status?.toUpperCase() || ''}</span>
                <span class="moderation-status-badge ${moderationStatusClass}" style="position: absolute; top: 10px; right: 10px;">${post.moderationStatus?.toUpperCase() || 'ACTIVE'}</span>
            </div>
            <div class="post-content">
                <div class="post-header">
                    <span class="post-title">${post.petName || 'Unnamed Pet'}</span>
                    <div class="post-user">
                        <div class="post-user-avatar">${avatarInitial}</div>
                        <span>${authorName}</span>
                    </div>
                </div>
                <div class="post-meta">
                    <div class="meta-item">📍 ${post.location || 'Unknown location'}</div>
                    <div class="meta-item">⏱️ ${post.createdAt ? new Date(post.createdAt).toLocaleDateString() : ''}</div>
                </div>
                <div class="post-description">
                    ${post.description || ''}
                </div>
                <div class="post-actions">
                    <button class="action-btn btn-view" onclick="viewPost('${post._id}')">
                        <span>👁️</span> View
                    </button>
                    <!-- Adjust moderation actions based on current status -->
                    ${post.moderationStatus !== 'active' ?
                `<button class="action-btn btn-moderate" onclick="moderatePost('${post._id}', 'active')" style="background: #E8F5E9; color: #4CAF50;">
                            <span>✅</span> Approve
                        </button>` : ''}
                    ${post.moderationStatus !== 'rejected' ?
                `<button class="action-btn btn-moderate" onclick="moderatePost('${post._id}', 'rejected')" style="background: #FFEBEE; color: #F44336;">
                            <span>❌</span> Reject
                        </button>` : ''}
                    <button class="action-btn btn-delete" onclick="deletePost('${post._id}')">
                        <span>🗑️</span> Delete
                    </button>
                </div>
            </div>
        </div>
    `}).join('');
}

function updatePostStats(posts) {
    const totalPosts = document.getElementById('totalPosts');
    const reportedPosts = document.getElementById('reportedPosts');

    if (totalPosts) totalPosts.textContent = posts.length;
    if (reportedPosts) reportedPosts.textContent = posts.filter(p => p.reported).length;
}

async function viewPost(postId) {
    try {
        const post = await apiRequest(`/admin/posts/${postId}`);

        const modalContent = document.getElementById('postDetails');
        if (modalContent) {
            let statusColor = '#F44336';
            if (post.status === 'Found') statusColor = '#4CAF50';
            else if (post.status === 'Adoption') statusColor = '#2196F3';

            let moderationStatusColor = '#FF9800';
            if (post.moderationStatus === 'active') moderationStatusColor = '#4CAF50';
            else if (post.moderationStatus === 'rejected') moderationStatusColor = '#F44336';
            else if (post.moderationStatus === 'removed') moderationStatusColor = '#999';

            let imageHtml = '';
            if (post.imageUrls && post.imageUrls.length > 0) {
                imageHtml = `<img src="${post.imageUrls[0]}" alt="${post.petName || 'Pet'}" style="width: 100%; max-height: 300px; object-fit: contain; margin-bottom: 20px; border-radius: 8px;">`;
            }

            modalContent.innerHTML = `
                ${imageHtml}
                <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Pet Name:</span>
                    <span>${post.petName || 'Unknown'}</span>
                </div>
                <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Status:</span>
                    <span style="color: ${statusColor}; font-weight: bold;">${post.status || 'Unknown'}</span>
                </div>
                <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Moderation Status:</span>
                    <span style="color: ${moderationStatusColor}; font-weight: bold;">${post.moderationStatus?.toUpperCase() || 'ACTIVE'}</span>
                </div>
                <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Posted By:</span>
                    <span>${post.userId?.username || 'Unknown User'}</span>
                </div>
                <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Location:</span>
                    <span>${post.location || 'Unknown'}</span>
                </div>
                 <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Species:</span>
                    <span>${post.species || 'Unknown'} - ${post.breed || 'Unknown Breed'}</span>
                </div>
                <div class="post-detail-item" style="display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <span style="font-weight: bold; color: #666;">Posted Date:</span>
                    <span>${post.createdAt ? new Date(post.createdAt).toLocaleString() : 'Unknown'}</span>
                </div>
                <div class="post-detail-item" style="margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <div style="font-weight: bold; color: #666; margin-bottom: 5px;">Description:</div>
                    <div style="white-space: pre-wrap;">${post.description || 'No description provided.'}</div>
                </div>
                 <div class="post-detail-item" style="margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;">
                    <div style="font-weight: bold; color: #666; margin-bottom: 5px;">Contact Info:</div>
                    <div style="white-space: pre-wrap;">${post.contactInfo || 'N/A'}</div>
                </div>
            `;
        }

        const modal = document.getElementById('postModal');
        if (modal) {
            modal.style.display = 'flex';
        }
    } catch (error) {
        console.error('Failed to load post:', error);
        alert('Failed to load post details.');
    }
}

async function flagPost(id) {
    // Show flag modal instead of doing a direct put to allow reason entry
    const modal = document.getElementById('flagModal');
    if (modal) {
        modal.dataset.postId = id; // Store ID for submission
        modal.style.display = 'flex';
    }
}

// Function hooked to the flag button inside the modal in posts.html
async function submitFlag() {
    const modal = document.getElementById('flagModal');
    if (!modal) return;

    const id = modal.dataset.postId;
    const reason = document.getElementById('flagReason').value;

    try {
        await apiRequest(`/admin/posts/${id}/flag`, {
            method: 'PUT',
            body: JSON.stringify({ reason })
        });
        closeModal('flagModal');
        loadPostsData();
        alert('Post flagged successfully');
    } catch (error) {
        console.error('Failed to flag post:', error);
        alert('Failed to flag post');
    }
}

async function deletePost(postId) {
    const modal = document.getElementById('deleteModal');
    if (modal) {
        modal.dataset.postId = postId;
        modal.style.display = 'flex';
    }
}

// Function hooked to the delete confirmation in posts.html
async function confirmDelete() {
    const modal = document.getElementById('deleteModal');
    if (!modal) return;

    const postId = modal.dataset.postId;
    try {
        await apiRequest(`/admin/posts/${postId}`, { method: 'DELETE' });
        loadPostsData();
        closeModal('deleteModal');
    } catch (error) {
        console.error('Failed to delete post:', error);
    }
}

// ===== REPORTS FUNCTIONS =====
async function loadReportsData() {
    try {
        const stats = await apiRequest('/admin/reports');

        // Update stats numbers - map our response structure
        const reports = stats.reports || {};
        document.getElementById('total-users').textContent = reports.totalUsers || 0;
        document.getElementById('total-posts').textContent = reports.totalPosts || 0;
        document.getElementById('active-users').textContent = reports.activePosts || 0;
        document.getElementById('engagement-rate').textContent = reports.engagementRate || '0%';

        // Empty dummy table code
    } catch (error) {
        console.error('Failed to load reports:', error);
    }
}

function updateReportTable(data) {
    const tbody = document.getElementById('report-table-body');
    if (!tbody) return;

    if (!data || data.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 30px; color: #999;">
                    No data available for this period
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = data.map((row, index) => `
        <tr>
            <td>${row.month}</td>
            <td>${Math.floor(Math.random() * 20) + 5}</td> <!-- New users -->
            <td>${row.total}</td>
            <td>${row.lost}</td>
            <td>${row.found}</td>
            <td>${row.adoption}</td>
            <td>${Math.floor(Math.random() * 20) + 60}%</td>
        </tr>
    `).join('');
}

// ===== SETTINGS FUNCTIONS =====
async function loadSettingsData() {
    try {
        const settingsResponse = await apiRequest('/admin/settings');

        // Apply settings to form fields
        const settings = settingsResponse.settings || {};
        const siteName = document.getElementById('siteName');
        if (siteName) siteName.value = settings.siteName || 'PawSociety';

        const siteDesc = document.getElementById('siteDescription');
        if (siteDesc) siteDesc.value = settings.siteDescription || 'A community platform for pet lovers';

        const contactEmail = document.getElementById('contactEmail');
        if (contactEmail) contactEmail.value = settings.contactEmail || 'admin@pawsociety.com';

        const timezone = document.getElementById('timezone');
        if (timezone) timezone.value = settings.timezone || 'UTC';

        const defaultRole = document.getElementById('defaultRole');
        if (defaultRole) defaultRole.value = settings.defaultRole || 'user';

        const sessionTimeout = document.getElementById('sessionTimeout');
        if (sessionTimeout) sessionTimeout.value = settings.sessionTimeout || 120;

        const flagThreshold = document.getElementById('flagThreshold');
        if (flagThreshold) flagThreshold.value = 3;

        const apiKey = document.getElementById('apiKey');
        if (apiKey) apiKey.value = 'pk_live_xxxxxxxxxxxxx';

        const rateLimit = document.getElementById('rateLimit');
        if (rateLimit) rateLimit.value = 60;

    } catch (error) {
        console.error('Failed to load settings:', error);
        // Fallback values
        const siteName = document.getElementById('siteName');
        if (siteName) siteName.value = 'PawSociety';

        const siteDesc = document.getElementById('siteDescription');
        if (siteDesc) siteDesc.value = 'A community platform for pet lovers';

        const contactEmail = document.getElementById('contactEmail');
        if (contactEmail) contactEmail.value = 'admin@pawsociety.com';
    }
}

async function saveSettings() {
    const settings = {
        siteName: document.getElementById('siteName')?.value || 'PawSociety',
        siteDescription: document.getElementById('siteDescription')?.value || '',
        contactEmail: document.getElementById('contactEmail')?.value || '',
        timezone: document.getElementById('timezone')?.value || 'UTC',
        allowRegistration: true,
        emailVerification: true,
        defaultRole: document.getElementById('defaultRole')?.value || 'user',
        twoFactorAuth: false,
        sessionTimeout: parseInt(document.getElementById('sessionTimeout')?.value) || 120,
        autoApprovePosts: true,
        profanityFilter: true
    };

    try {
        await apiRequest('/admin/settings', {
            method: 'PUT',
            body: JSON.stringify({ settings })
        });
        alert('Settings saved successfully!');
    } catch (error) {
        console.error('Failed to save settings:', error);
        alert('Failed to save settings');
    }
}

async function clearAllData() {
    if (confirm('⚠️ WARNING: This will delete ALL non-admin data. Are you sure?')) {
        const confirmText = prompt('Type "DELETE" to confirm:');
        if (confirmText === 'DELETE') {
            try {
                await apiRequest('/admin/clear-all-data', { method: 'POST' });
                alert('All non-admin data has been cleared.');
            } catch (error) {
                console.error('Failed to clear data:', error);
                alert('Failed to clear data');
            }
        }
    }
}

// ===== MODERATION FUNCTIONS =====
async function moderatePost(postId, status) {
    if (confirm(`Are you sure you want to ${status} this post?`)) {
        try {
            await apiRequest(`/admin/posts/${postId}/moderate`, {
                method: 'POST',
                body: JSON.stringify({ status })
            });
            loadPostsData();
            alert(`Post ${status} successfully`);
        } catch (error) {
            console.error('Failed to moderate post:', error);
            alert('Failed to moderate post');
        }
    }
}

// ===== UTILITY FUNCTIONS =====
function animateNumbers() {
    document.querySelectorAll('.stat-card .number').forEach(el => {
        const target = parseInt(el.textContent.replace(/,/g, ''));
        if (!isNaN(target)) {
            animateNumber(el, target);
        }
    });
}

function animateNumber(element, target) {
    let current = 0;
    const increment = Math.ceil(target / 50);
    const timer = setInterval(() => {
        current += increment;
        if (current >= target) {
            element.textContent = target.toLocaleString();
            clearInterval(timer);
        } else {
            element.textContent = current.toLocaleString();
        }
    }, 20);
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'flex';
        // Check for specific CSS class activation requirement
        if (modal.classList.contains('modal')) {
            modal.classList.add('active'); // Some modals might need active class (like userModal)
        }
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('active');
    }
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        window.location.href = 'index.html';
    }
}

// ===== FILTER FUNCTIONS =====
function applyUserFilters() {
    // This will be handled by the API with query params in a real implementation
    loadUsersData();
}

function applyPostFilters() {
    // This will be handled by the API with query params in a real implementation
    loadPostsData();
}

// ===== MODAL CLICK OUTSIDE =====
window.onclick = function (event) {
    const modals = ['userModal', 'viewPostModal', 'deleteModal', 'flagModal'];
    modals.forEach(modalId => {
        const modal = document.getElementById(modalId);
        if (modal && event.target == modal) {
            modal.classList.remove('active');
        }
    });
};