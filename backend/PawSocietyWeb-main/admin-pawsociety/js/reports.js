// js/reports.js

// Animate numbers counting up
function animateNumber(element, target) {
    if (!element) return;
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

// Update all report data
function updateReports() {
    // Get data via api request or fallback
    // Calculate stats
    const users = []; // placeholder, normally fetched via api
    const posts = []; // placeholder

    const totalUsers = users.length;
    const totalPosts = posts.length;
    const activeUsers = users.filter(u => u.status === 'Active').length;
    const engagementRate = totalUsers > 0 ? Math.round((activeUsers / totalUsers) * 100) : 0;

    // Post counts by status
    const lostCount = posts.filter(p => p.status === 'Lost').length;
    const foundCount = posts.filter(p => p.status === 'Found').length;
    const adoptionCount = posts.filter(p => p.status === 'Adoption').length;

    // Calculate percentages
    const total = posts.length || 1;
    const lostPercent = Math.round((lostCount / total) * 100);
    const foundPercent = Math.round((foundCount / total) * 100);
    const adoptionPercent = Math.round((adoptionCount / total) * 100);

    // Update stats cards with animation
    animateNumber(document.getElementById('total-users'), totalUsers);
    animateNumber(document.getElementById('total-posts'), totalPosts);
    animateNumber(document.getElementById('active-users'), activeUsers);
    document.getElementById('engagement-rate').textContent = engagementRate + '%';

    // Update percentages
    document.getElementById('lost-percent').textContent = lostPercent;
    document.getElementById('found-percent').textContent = foundPercent;
    document.getElementById('adoption-percent').textContent = adoptionPercent;

    // Update pie chart
    const pieChart = document.getElementById('pie-chart');
    if (pieChart) {
        const totalDegrees = 360;
        const lostDegrees = (lostPercent / 100) * totalDegrees;
        const foundDegrees = (foundPercent / 100) * totalDegrees;
        const adoptionDegrees = (adoptionPercent / 100) * totalDegrees;

        pieChart.style.background = `conic-gradient(
            #F44336 ${lostDegrees}deg,
            #4CAF50 ${lostDegrees}deg ${lostDegrees + foundDegrees}deg,
            #2196F3 ${lostDegrees + foundDegrees}deg ${lostDegrees + foundDegrees + adoptionDegrees}deg
        )`;
    }

    // Update user growth chart
    updateUserGrowthChart(users);

    // Update monthly report table
    updateReportTable(users, posts);
}

// Update user growth chart
function updateUserGrowthChart(users) {
    const bars = document.querySelectorAll('.bar-chart .bar');
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'];

    // Group users by month (simplified - you can make this more sophisticated)
    const monthlyData = months.map(() => Math.floor(Math.random() * 50) + 20);

    bars.forEach((bar, index) => {
        setTimeout(() => {
            bar.style.height = monthlyData[index] + 'px';
        }, index * 100);
    });
}

// Update monthly report table
function updateReportTable(users, posts) {
    const tbody = document.getElementById('report-table-body');

    if (users.length === 0 && posts.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 30px; color: #999;">
                    No data available
                </td>
            </tr>
        `;
        return;
    }

    // Generate monthly summary (simplified)
    const months = ['January', 'February', 'March', 'April', 'May', 'June'];
    let tableHtml = '';

    months.forEach((month, index) => {
        const newUsers = Math.floor(Math.random() * 20) + 5;
        const newPosts = Math.floor(Math.random() * 15) + 3;
        const lost = Math.floor(Math.random() * 8) + 1;
        const found = Math.floor(Math.random() * 6) + 1;
        const adoptions = Math.floor(Math.random() * 5) + 1;
        const engagement = Math.floor(Math.random() * 20) + 60;

        tableHtml += `
            <tr style="animation: fadeIn 0.3s ease forwards; animation-delay: ${index * 0.1}s; opacity: 0;">
                <td>${month} 2024</td>
                <td>${newUsers}</td>
                <td>${newPosts}</td>
                <td>${lost}</td>
                <td>${found}</td>
                <td>${adoptions}</td>
                <td>${engagement}%</td>
            </tr>
        `;
    });

    tbody.innerHTML = tableHtml;
}

// Date range filter
function applyDateRange() {
    const start = document.getElementById('startDate').value;
    const end = document.getElementById('endDate').value;

    if (start && end) {
        // Add animation to show filtering
        const button = event.target;
        button.style.transform = 'scale(0.95)';
        setTimeout(() => {
            button.style.transform = 'scale(1)';
        }, 200);

        // Simulate filtering
        setTimeout(() => {
            updateReports();
            Toast('Filtering reports from ' + start + ' to ' + end);
        }, 500);
    }
}

// Simple toast notification
function Toast(message) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        background: #7A4F2B;
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        animation: slideIn 0.3s ease forwards;
        z-index: 1000;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Export functions
function exportReport() {
    const btn = event.target;
    btn.style.transform = 'scale(0.95)';
    setTimeout(() => btn.style.transform = 'scale(1)', 200);
    Toast('Exporting full report...');
}

function exportCSV() {
    const btn = event.target;
    btn.style.transform = 'scale(0.95)';
    setTimeout(() => btn.style.transform = 'scale(1)', 200);
    Toast('Exporting data as CSV...');
}

function exportPDF() {
    const btn = event.target;
    btn.style.transform = 'scale(0.95)';
    setTimeout(() => btn.style.transform = 'scale(1)', 200);
    Toast('Exporting data as PDF...');
}

function printReport() {
    Toast('Preparing report for printing...');
    setTimeout(() => window.print(), 1000);
}

// Add fade-in animation to table rows
document.addEventListener('animationstart', function (e) {
    if (e.target.tagName === 'TR') {
        e.target.style.opacity = '1';
    }
});
