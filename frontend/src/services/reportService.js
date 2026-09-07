import { apiDownload, apiRequest } from './api.js'

const query = (startDate, endDate) => `startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`

export const reportService = {
  generate: (startDate, endDate, token, onUnauthorized) => apiRequest(`/api/reports?${query(startDate, endDate)}`, { token, onUnauthorized }),
  export: (type, startDate, endDate, token, onUnauthorized) => apiDownload(`/api/reports/export?type=${type}&${query(startDate, endDate)}`, { token, onUnauthorized })
}