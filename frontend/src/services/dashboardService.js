import { apiRequest } from './api.js'

export const dashboardService = {
  load: (token, onUnauthorized) => apiRequest('/api/dashboard', { token, onUnauthorized })
}