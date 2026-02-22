// js/settings.js

// Tab switching function
function switchTab(tabName) {
    // Hide all sections
    document.querySelectorAll('.settings-section').forEach(section => {
        section.style.display = 'none';
    });

    // Show selected section
    const section = document.getElementById(tabName + '-settings');
    if (section) {
        section.style.display = 'block';
    }

    // Update tab styling
    document.querySelectorAll('.settings-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // Find and activate the clicked tab
    event.target.classList.add('active');
}

// Toggle switch function
function toggleSwitch(element) {
    element.classList.toggle('active');
}

// Load settings from API
async function loadSettings() {
    try {
        const token = localStorage.getItem('adminToken');
        const response = await fetch('/api/admin/settings', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const settings = await response.json();

            // Update form fields
            document.getElementById('siteName').value = settings.siteName || 'PawSociety';
            document.getElementById('siteDescription').value = settings.siteDescription || 'Because Every Pet Deserves a Home';
            document.getElementById('contactEmail').value = settings.contactEmail || 'admin@pawsociety.com';
            document.getElementById('timezone').value = settings.timezone || 'PST';
            document.getElementById('defaultRole').value = settings.defaultRole || 'user';
            document.getElementById('sessionTimeout').value = settings.sessionTimeout || 120;

            // Update toggle switches
            updateToggle('allowRegistration', settings.allowRegistration);
            updateToggle('emailVerification', settings.emailVerification);
            updateToggle('twoFactorAuth', settings.twoFactorAuth);
            updateToggle('autoApprovePosts', settings.autoApprovePosts);
            updateToggle('profanityFilter', settings.profanityFilter);
        }
    } catch (error) {
        console.error('Failed to load settings:', error);
    }
}

function updateToggle(id, value) {
    const element = document.getElementById(id);
    if (element) {
        if (value) {
            element.classList.add('active');
        } else {
            element.classList.remove('active');
        }
    }
}

// Save settings
async function saveSettings() {
    const settings = {
        siteName: document.getElementById('siteName')?.value || 'PawSociety',
        siteDescription: document.getElementById('siteDescription')?.value || '',
        contactEmail: document.getElementById('contactEmail')?.value || '',
        timezone: document.getElementById('timezone')?.value || 'PST',
        allowRegistration: document.getElementById('allowRegistration')?.classList.contains('active') || false,
        emailVerification: document.getElementById('emailVerification')?.classList.contains('active') || false,
        defaultRole: document.getElementById('defaultRole')?.value || 'user',
        twoFactorAuth: document.getElementById('twoFactorAuth')?.classList.contains('active') || false,
        sessionTimeout: parseInt(document.getElementById('sessionTimeout')?.value) || 120,
        autoApprovePosts: document.getElementById('autoApprovePosts')?.classList.contains('active') || false,
        profanityFilter: document.getElementById('profanityFilter')?.classList.contains('active') || false
    };

    try {
        const token = localStorage.getItem('adminToken');
        const response = await fetch('/api/admin/settings', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(settings)
        });

        if (response.ok) {
            alert('Settings saved successfully!');
        } else {
            alert('Failed to save settings');
        }
    } catch (error) {
        console.error('Failed to save settings:', error);
        alert('Failed to save settings');
    }
}

// Danger zone functions
function clearCache() {
    if (confirm('Are you sure you want to clear the system cache?')) {
        alert('Cache cleared successfully! (Demo only)');
    }
}

function resetSettings() {
    if (confirm('Are you sure you want to reset all settings to default?')) {
        // Reset form fields to defaults
        document.getElementById('siteName').value = 'PawSociety';
        document.getElementById('siteDescription').value = 'Because Every Pet Deserves a Home';
        document.getElementById('contactEmail').value = 'admin@pawsociety.com';
        document.getElementById('timezone').value = 'PST';
        document.getElementById('defaultRole').value = 'user';
        document.getElementById('sessionTimeout').value = '120';

        // Reset toggles
        document.getElementById('allowRegistration').classList.add('active');
        document.getElementById('emailVerification').classList.add('active');
        document.getElementById('twoFactorAuth').classList.remove('active');
        document.getElementById('autoApprovePosts').classList.remove('active');
        document.getElementById('profanityFilter').classList.add('active');

        alert('Settings reset to default!');
    }
}

async function deleteAllData() {
    if (confirm('⚠️ WARNING: This will delete ALL non-admin data. Are you ABSOLUTELY sure?')) {
        const confirmText = prompt('Type "DELETE" to confirm:');
        if (confirmText === 'DELETE') {
            try {
                const token = localStorage.getItem('adminToken');
                const response = await fetch('/api/admin/clear-all-data', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.ok) {
                    alert('All non-admin data has been cleared.');
                } else {
                    alert('Failed to clear data');
                }
            } catch (error) {
                console.error('Failed to clear data:', error);
                alert('Failed to clear data');
            }
        }
    }
}

// Load settings on page load
document.addEventListener('DOMContentLoaded', function () {
    loadSettings();
});
