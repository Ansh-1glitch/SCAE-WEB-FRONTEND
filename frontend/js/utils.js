function showLoading(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.innerHTML = `
        <div style="text-align:center;padding:30px;color:#666;">
            <div style="width:32px;height:32px;border:3px solid #ccc;
                border-top-color:#003366;border-radius:50%;
                animation:spin 0.8s linear infinite;margin:0 auto 12px;">
            </div>
            <p style="font-family:Arial;font-size:13px;">Please wait...</p>
        </div>`;
}

function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) el.innerHTML = `
        <div style="color:#C62828;padding:12px;border:1px solid #C62828;
            background:#FFF5F5;font-family:Arial;font-size:13px;">
            ⚠ ${message}
        </div>`;
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position:fixed;top:20px;right:20px;z-index:9999;
        padding:12px 20px;border-radius:4px;font-family:Arial;
        font-size:13px;color:white;max-width:320px;
        background:${type === 'success' ? '#2E7D32' : '#C62828'};
        box-shadow:0 2px 8px rgba(0,0,0,0.2);`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

function logout() {
    sessionStorage.clear();
    window.location.href = 'index.html';
}
