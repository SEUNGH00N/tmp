const app = (() => {
  async function api(path, options = {}) {
    const response = await fetch(path, {
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      },
      ...options
    });

    const isJson = (response.headers.get('content-type') || '').includes('application/json');
    const body = isJson ? await response.json() : null;

    if (!response.ok) {
      const message = body?.detail || body?.title || 'request failed';
      throw new Error(message);
    }

    return body;
  }

  async function getAuthStatus() {
    const res = await api('/api/v1/auth/me', { method: 'GET' });
    return res.data;
  }

  async function requireAuth() {
    const auth = await getAuthStatus();
    if (!auth.authenticated) {
      window.location.href = '/login.html';
      return null;
    }
    return auth;
  }

  async function ensureGuest() {
    const auth = await getAuthStatus();
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

  function showMessage(el, message, isError = false) {
    if (!el) return;
    el.textContent = message || '';
    el.style.color = isError ? '#b91c1c' : '#065f46';
  }

  function formatDate(value) {
    if (!value) return '-';
    return new Date(value).toLocaleString();
  }

  function navKeyByText(text) {
    const t = (text || '').toLowerCase();
    if (t.includes('dashboard')) return 'dashboard';
    if (t.includes('upload')) return 'upload';
    if (t.includes('job') || t.includes('import')) return 'jobs';
    if (t.includes('setting')) return 'settings';
    return '';
  }

  function navHref(key) {
    if (key === 'dashboard') return '/mock/dashboard.html';
    if (key === 'upload') return '/mock/dashboard.html#upload-section';
    if (key === 'jobs') return '/mock/job-list.html';
    if (key === 'settings') return '/mock/settings.html';
    return '#';
  }

  function optimizeSidebar(activeKey) {
    const links = Array.from(document.querySelectorAll('a[data-purpose="nav-item"], a[href="#"]'));
    links.forEach((a) => {
      const key = navKeyByText(a.textContent || '');
      if (key) a.href = navHref(key);

      if (a.matches('[data-purpose="nav-item"]')) {
        const isActive = key === activeKey || (activeKey === 'upload' && key === 'dashboard' && window.location.hash === '#upload-section');
        a.classList.remove('bg-blue-50', 'text-blue-700', 'text-gray-600');
        if (isActive) {
          a.classList.add('bg-blue-50', 'text-blue-700');
        } else {
          a.classList.add('text-gray-600');
        }
      }
    });
  }

  function bindLogout() {
    const logoutCandidate = Array.from(document.querySelectorAll('button,a'))
      .find((el) => /logout|sign out/i.test(el.textContent || ''));
    if (logoutCandidate) {
      logoutCandidate.addEventListener('click', (e) => {
        e.preventDefault();
        logout();
      });
    }
  }

  function mountFloatingPanel(title, bodyHtml) {
    const panel = document.createElement('section');
    panel.innerHTML = '<h3 style="margin:0 0 10px;font-size:14px;font-weight:700;">' + title + '</h3>' + bodyHtml;
    panel.style.position = 'fixed';
    panel.style.right = '16px';
    panel.style.bottom = '16px';
    panel.style.width = 'min(540px, calc(100vw - 24px))';
    panel.style.maxHeight = '70vh';
    panel.style.overflow = 'auto';
    panel.style.background = 'rgba(255,255,255,0.98)';
    panel.style.border = '1px solid #cbd5e1';
    panel.style.borderRadius = '10px';
    panel.style.boxShadow = '0 10px 24px rgba(15,23,42,.18)';
    panel.style.padding = '12px';
    panel.style.zIndex = '9999';
    document.body.appendChild(panel);
    return panel;
  }

  return {
    api,
    requireAuth,
    ensureGuest,
    logout,
    showMessage,
    formatDate,
    optimizeSidebar,
    bindLogout,
    mountFloatingPanel
  };
})();
