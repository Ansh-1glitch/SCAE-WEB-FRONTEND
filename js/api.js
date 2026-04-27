const BASE_URL = "https://daa-7.onrender.com/api";

function getToken() {
    return sessionStorage.getItem("scae_token");
}

async function api(endpoint, method = "GET", body = null) {
    const headers = { "Content-Type": "application/json" };
    if (getToken()) headers["Authorization"] = "Bearer " + getToken();
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    try {
        const res = await fetch(BASE_URL + endpoint, options);
        if (res.status === 401) {
            sessionStorage.clear();
            window.location.href = "index.html";
            return;
        }
        const data = await res.json();
        if (!data.success) throw new Error(data.message || "Server error");
        return data;
    } catch (err) {
        console.error("API Error:", err);
        throw err;
    }
}
