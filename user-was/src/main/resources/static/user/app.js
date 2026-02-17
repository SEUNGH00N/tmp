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

  function getWorkspaceId() {
    return localStorage.getItem('user_workspace_id') || localStorage.getItem('user_tenant_id') || '';
  }

  function setWorkspaceId(v) {
    localStorage.setItem('user_workspace_id', v);
    localStorage.removeItem('user_tenant_id');
  }

  async function bindWorkspaceSession(workspaceId) {
    await api(`/api/v1/user/imports/session/workspace?workspace_id=${encodeURIComponent(workspaceId)}`, { method: 'POST' });
  }

  function requireWorkspace() {
    const w = getWorkspaceId();
    if (!w) {
      window.location.href = '/user/login.html';
      return '';
    }
    return w;
  }

  function clearWorkspace() {
    localStorage.removeItem('user_workspace_id');
    localStorage.removeItem('user_tenant_id');
    window.location.href = '/user/login.html';
  }

  function fmt(v) {
    return v ? new Date(v).toLocaleString() : '-';
  }

  return { api, getWorkspaceId, setWorkspaceId, bindWorkspaceSession, requireWorkspace, clearWorkspace, fmt };
})();