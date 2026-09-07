import { apiRequest } from './api.js'

export const incomeService = {
  list: (token, onUnauthorized) => apiRequest('/api/income', { token, onUnauthorized }),
  create: (payload, token, onUnauthorized) => apiRequest('/api/income', { method: 'POST', body: JSON.stringify(payload), token, onUnauthorized }),
  update: (id, payload, token, onUnauthorized) => apiRequest(`/api/income/${id}`, { method: 'PUT', body: JSON.stringify(payload), token, onUnauthorized }),
  remove: (id, token, onUnauthorized) => apiRequest(`/api/income/${id}`, { method: 'DELETE', token, onUnauthorized })
}