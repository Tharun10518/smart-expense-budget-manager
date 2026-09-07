import { apiRequest } from './api.js'

export const budgetService = {
  list: (token, onUnauthorized) => apiRequest('/api/budgets', { token, onUnauthorized }),
  summary: (token, onUnauthorized) => apiRequest('/api/budgets/summary', { token, onUnauthorized }),
  create: (payload, token, onUnauthorized) => apiRequest('/api/budgets', { method: 'POST', body: JSON.stringify(payload), token, onUnauthorized }),
  update: (id, payload, token, onUnauthorized) => apiRequest(`/api/budgets/${id}`, { method: 'PUT', body: JSON.stringify(payload), token, onUnauthorized }),
  remove: (id, token, onUnauthorized) => apiRequest(`/api/budgets/${id}`, { method: 'DELETE', token, onUnauthorized })
}
