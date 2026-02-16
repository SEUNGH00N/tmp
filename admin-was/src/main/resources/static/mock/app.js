window.ExcelApp = (() => {
  async function api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    const isFormData = options.body instanceof FormData;
    if (!isFormData && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    const res = await fetch(path, {
      credentials: 'same-origin',
      ...options,
      headers
    });

    const isJson = (res.headers.get('content-type') || '').includes('application/json');
    const body = isJson ? await res.json() : null;

    if (!res.ok) {
      throw new Error(body?.detail || body?.title || `HTTP ${res.status}`);
    }
    return body;
  }

  async function authStatus() {
    const result = await api('/api/v1/auth/me', { method: 'GET' });
    return result.data;
  }

  async function requireAuth() {
    const auth = await authStatus();
    if (!auth.authenticated) {
      window.location.href = '/login.html';
      return null;
    }
    return auth;
  }

  async function ensureGuest() {
    const auth = await authStatus();
    if (auth.authenticated) {
      window.location.href = '/mock/dashboard.html';
      return false;
    }
    return true;
  }

  async function logout() {
    await api('/api/v1/auth/logout', { method: 'POST' });
    window.location.href = '/login.html';
  }

  function formatDate(v) {
    if (!v) return '-';
    return new Date(v).toLocaleString();
  }

  return { api, authStatus, requireAuth, ensureGuest, logout, formatDate };
})();
