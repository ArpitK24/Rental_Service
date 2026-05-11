/* RentRover Admin - minimal API integration helper (no build step required) */
(function () {
  "use strict";

  // Resolve API base for both:
  // - root deployment: http://host:port/admin/Login.html
  // - context deployment: http://host:port/RentalCarApp/admin/Login.html
  function detectBaseUrl() {
    if (window.__API_BASE_URL__) return String(window.__API_BASE_URL__).replace(/\/+$/, "");
    var pathParts = window.location.pathname.split("/").filter(function (p) { return !!p; });
    if (pathParts.length > 0 && pathParts[0].toLowerCase() !== "admin" && pathParts[0].indexOf(".") === -1) {
      return window.location.origin + "/" + pathParts[0];
    }
    return window.location.origin;
  }
  var BASE_URL = detectBaseUrl();

  function nowIso() {
    try { return new Date().toISOString(); } catch (e) { return ""; }
  }

  function saveJson(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) {}
  }

  function loadJson(key) {
    try {
      var raw = localStorage.getItem(key);
      if (!raw) return null;
      return JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }

  function setText(id, text) {
    var el = document.getElementById(id);
    if (el) el.textContent = text;
  }

  function show(id, visible) {
    var el = document.getElementById(id);
    if (!el) return;
    el.style.display = visible ? "" : "none";
  }

  function auth() {
    var t = loadJson("admin_auth");
    if (!t) return null;
    return t;
  }

  function setAuth(tokenPair) {
    saveJson("admin_auth", {
      accessToken: tokenPair && tokenPair.accessToken ? tokenPair.accessToken : null,
      refreshToken: tokenPair && tokenPair.refreshToken ? tokenPair.refreshToken : null,
      savedAt: nowIso()
    });
  }

  function clearAuth() {
    try { localStorage.removeItem("admin_auth"); } catch (e) {}
  }

  function formatInr(amount) {
    var n = Number(amount || 0);
    try {
      return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
      }).format(n);
    } catch (e) {
      return "₹" + n.toFixed(0);
    }
  }

  function getAccessToken() {
    var t = auth();
    return t && t.accessToken ? t.accessToken : null;
  }

  function apiUrl(path) {
    if (!path) return BASE_URL;
    if (path.indexOf("http://") === 0 || path.indexOf("https://") === 0) return path;
    if (path.charAt(0) !== "/") path = "/" + path;
    return BASE_URL + path;
  }

  function parseErrorMessage(payload) {
    if (!payload) return "Request failed";
    if (typeof payload === "string") return payload;
    if (payload.message) return payload.message;
    if (payload.error) return payload.error;
    return "Request failed";
  }

  async function apiFetch(path, options) {
    options = options || {};
    var headers = options.headers || {};
    if (!headers["Content-Type"] && options.body && typeof options.body === "string") {
      headers["Content-Type"] = "application/json";
    }
    var token = getAccessToken();
    if (token && !headers["Authorization"]) {
      headers["Authorization"] = "Bearer " + token;
    }
    options.headers = headers;

    var res = await fetch(apiUrl(path), options);
    var text = await res.text();
    var data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
    if (!res.ok) {
      var msg = parseErrorMessage(data);
      var err = new Error(msg);
      err.status = res.status;
      err.payload = data;
      throw err;
    }
    return data;
  }

  async function loginFlowRequestOtp(mobile) {
    return apiFetch("/admin/auth/request-otp", {
      method: "POST",
      body: JSON.stringify({ mobile: mobile })
    });
  }

  async function loginFlowVerifyOtp(mobile, code) {
    var tokenPair = await apiFetch("/admin/auth/verify-otp", {
      method: "POST",
      body: JSON.stringify({ mobile: mobile, code: code })
    });
    setAuth(tokenPair);
    return tokenPair;
  }

  async function logoutAdmin() {
    try {
      await apiFetch("/admin/auth/logout", { method: "POST" });
    } catch (e) {
      // Clear session even if backend logout request fails.
    }
    clearAuth();
    window.location.href = "Login.html";
  }

  function requireAuthOrRedirect() {
    if (!getAccessToken()) {
      // Preserve current page so we can return after login.
      try { sessionStorage.setItem("after_login", window.location.href); } catch (e) {}
      window.location.href = "Login.html";
      return false;
    }
    return true;
  }

  function afterLoginRedirectDefault() {
    var next = null;
    try { next = sessionStorage.getItem("after_login"); sessionStorage.removeItem("after_login"); } catch (e) {}
    window.location.href = next || "Home.html";
  }

  function bindProfileNavigation() {
    var imgs = document.querySelectorAll("img[alt*='Admin'], img[alt*='Administrator']");
    for (var i = 0; i < imgs.length; i++) {
      (function (img) {
        img.style.cursor = "pointer";
        img.addEventListener("click", function () {
          window.location.href = "Profile.html";
        });
      })(imgs[i]);
    }
  }

  bindProfileNavigation();

  window.RentRoverAdmin = {
    BASE_URL: BASE_URL,
    apiFetch: apiFetch,
    apiUrl: apiUrl,
    setText: setText,
    show: show,
    auth: auth,
    setAuth: setAuth,
    clearAuth: clearAuth,
    formatInr: formatInr,
    requireAuthOrRedirect: requireAuthOrRedirect,
    afterLoginRedirectDefault: afterLoginRedirectDefault,
    bindProfileNavigation: bindProfileNavigation,
    loginFlowRequestOtp: loginFlowRequestOtp,
    loginFlowVerifyOtp: loginFlowVerifyOtp,
    logoutAdmin: logoutAdmin
  };
})();
