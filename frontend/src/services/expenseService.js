import { apiRequest } from './api.js'

export const expenseService = {
  list: (token, onUnauthorized) => apiRequest('/api/expenses', { token, onUnauthorized }),
  create: (payload, token, onUnauthorized) => apiRequest('/api/expenses', { method: 'POST', body: JSON.stringify(payload), token, onUnauthorized }),
  update: (id, payload, token, onUnauthorized) => apiRequest(`/api/expenses/${id}`, { method: 'PUT', body: JSON.stringify(payload), token, onUnauthorized }),
  remove: (id, token, onUnauthorized) => apiRequest(`/api/expenses/${id}`, { method: 'DELETE', token, onUnauthorized })
}