import { apiRequest } from './api.js'

export const notificationService = {
  list: (token, onUnauthorized) => apiRequest('/api/notifications', { token, onUnauthorized }),
  markAsRead: (id, token, onUnauthorized) => apiRequest(`/api/notifications/${id}/read`, { method: 'PUT', token, onUnauthorized }),
  markAllAsRead: (token, onUnauthorized) => apiRequest('/api/notifications/read-all', { method: 'PUT', token, onUnauthorized })
}

