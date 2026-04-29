const BASE_URL = "http://localhost:8000/api";

function getToken() {
    return sessionStorage.getItem("scae_token");
}
// Guest tokens are set by the offline-fallback login (not real JWTs).
// Real JWTs from the backend always start with "eyJ".
function isGuestToken(token) {
    return token && !token.startsWith('eyJ');
}

async function api(endpoint, method = "GET", body = null) {
    const headers = { "Content-Type": "application/json" };
    const token = getToken();
    // Only send the Authorization header for real JWT tokens —
    // sending a fake guest token would always trigger a backend 401.
    if (token && !isGuestToken(token)) headers["Authorization"] = "Bearer " + token;
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    try {
        const res = await fetch(BASE_URL + endpoint, options);
        if (res.status === 401) {
            if (token && !isGuestToken(token)) {
                // Real token rejected → force logout
                sessionStorage.clear();
                window.location.href = "index.html";
                return;
            }
            // Guest session → just throw so the caller's catch block
            // can serve local/fallback data instead.
            throw new Error("API unavailable in offline mode");
        }
        const data = await res.json();
        if (!data.success) throw new Error(data.message || "Server error");
        return data;
    } catch (err) {
        console.error("API Error:", err);
        throw err;
    }
}
