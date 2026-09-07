import { apiRequest } from './api.js'

export const authService = {
  register: (payload) => apiRequest('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  login: (payload) => apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  getCurrentUser: (token, onUnauthorized) => apiRequest('/api/users/me', { token, onUnauthorized })
}