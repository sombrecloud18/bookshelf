const AUTH_TOKEN_KEY = 'bookshelf_auth_token';

async function request(path, options = {}) {
  const method = options.method || 'GET';
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const headers = { ...options.headers };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  const url = `/api${path}`;
  const start = performance.now();

  console.groupCollapsed(`[API] ${method} ${path}`);
  console.log('URL:', url);
  if (options.body) {
    console.log('Body:', options.body);
  }
  console.log('Auth:', token ? 'Bearer ***' : 'none');

  let res;
  try {
    res = await fetch(url, { ...options, headers, body });
  } catch (networkErr) {
    const ms = Math.round(performance.now() - start);
    console.error(`Network error after ${ms}ms:`, networkErr.message);
    console.groupEnd();
    throw networkErr;
  }

  const ms = Math.round(performance.now() - start);
  const statusColor = res.ok ? 'color: green' : res.status >= 500 ? 'color: red' : 'color: orange';
  console.log(`%cStatus: ${res.status} (${ms}ms)`, statusColor);

  if (res.status === 401) {
    console.warn('401 — сессия истекла, перенаправление на /auth');
    console.groupEnd();
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem('bookshelf_auth_role');
    window.location.href = '/auth';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    console.error('Ошибка ответа:', err);
    console.groupEnd();
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  if (res.status === 204) {
    console.log('Response: (no content)');
    console.groupEnd();
    return null;
  }

  const data = await res.json();
  if (Array.isArray(data)) {
    console.log(`Response: Array[${data.length}]`, data.length <= 5 ? data : data.slice(0, 5).concat(['...']));
  } else if (data && typeof data === 'object' && 'content' in data) {
    console.log(`Response: Page { total: ${data.totalElements}, page: ${data.number}, items: ${data.content?.length} }`);
  } else {
    console.log('Response:', data);
  }
  console.groupEnd();

  return data;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
};
