import { apiRequest } from './api.js'

export const authService = {
  register: (payload) => apiRequest('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  login: (payload) => apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  getCurrentUser: (token, onUnauthorized) => apiRequest('/api/users/me', { token, onUnauthorized }),
  updateProfile: (payload, token, onUnauthorized) => apiRequest('/api/users/profile', { method: 'PUT', body: JSON.stringify(payload), token, onUnauthorized }),
  changePassword: (payload, token, onUnauthorized) => apiRequest('/api/users/password', { method: 'PUT', body: JSON.stringify(payload), token, onUnauthorized })
}