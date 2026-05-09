/* RentRover Admin - minimal API integration helper (no build step required) */
(function () {
  "use strict";

  // Resolve API base from current deployment context (e.g., /RentalCarApp).
  var BASE_URL = window.location.origin + "/" + window.location.pathname.split("/")[1];

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

  window.RentRoverAdmin = {
    BASE_URL: BASE_URL,
    apiFetch: apiFetch,
    apiUrl: apiUrl,
    setText: setText,
    show: show,
    auth: auth,
    setAuth: setAuth,
    clearAuth: clearAuth,
    requireAuthOrRedirect: requireAuthOrRedirect,
    afterLoginRedirectDefault: afterLoginRedirectDefault,
    loginFlowRequestOtp: loginFlowRequestOtp,
    loginFlowVerifyOtp: loginFlowVerifyOtp
  };
})();
