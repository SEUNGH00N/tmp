window.UserApp = (() => {
  async function api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    const isFormData = options.body instanceof FormData;
    if (!isFormData && !headers['Content-Type']) headers['Content-Type'] = 'application/json';

    const res = await fetch(path, { credentials: 'same-origin', ...options, headers });
    const isJson = (res.headers.get('content-type') || '').includes('application/json');
    const body = isJson ? await res.json() : null;
    if (!res.ok) throw new Error(body?.detail || body?.title || `HTTP ${res.status}`);
    return body;
  }

  function getTenantId() {
    return localStorage.getItem('user_tenant_id') || '';
  }

  function setTenantId(v) {
    localStorage.setItem('user_tenant_id', v);
  }

  function requireTenant() {
    const t = getTenantId();
    if (!t) {
      window.location.href = '/user/login.html';
      return '';
    }
    return t;
  }

  function clearTenant() {
    localStorage.removeItem('user_tenant_id');
    window.location.href = '/user/login.html';
  }

  function fmt(v) {
    return v ? new Date(v).toLocaleString() : '-';
  }

  return { api, getTenantId, setTenantId, requireTenant, clearTenant, fmt };
})();
