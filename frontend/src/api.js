const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) {
    const error = new Error(data?.message || '요청에 실패했습니다.')
    error.code = data?.error
    throw error
  }
  return data
}

export const api = {
  createSpace: (name) => request('/api/spaces', { method: 'POST', body: JSON.stringify({ name }) }),
  joinSpace: (code, body) =>
    request(`/api/spaces/${encodeURIComponent(code)}/join`, { method: 'POST', body: JSON.stringify(body) }),
  getSpace: (id) => request(`/api/spaces/${id}`),
  getGraph: (id) => request(`/api/spaces/${id}/graph`),
  listArtifacts: (spaceId) => request(`/api/spaces/${spaceId}/artifacts`),
  createArtifact: (spaceId, body) =>
    request(`/api/spaces/${spaceId}/artifacts`, { method: 'POST', body: JSON.stringify(body) }),
  getArtifact: (id) => request(`/api/artifacts/${id}`),
  getGroup: (id) => request(`/api/groups/${id}`),
}

export { API_BASE }
