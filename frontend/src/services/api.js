const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

export async function apiRequest(path, options = {}) {
  const { token, onUnauthorized, ...requestOptions } = options
  const headers = new Headers(requestOptions.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response
  try { response = await fetch(`${API_BASE_URL}${path}`, { ...requestOptions, headers }) } catch { throw new Error('The service is unavailable. Check your connection and try again.') }
  if (response.status === 401) { onUnauthorized?.(); throw new Error('Your session has expired. Please log in again.') }
  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : null
  if (!response.ok) {
    const message = data?.message || data?.error || (response.status === 409 ? 'This email is already registered.' : 'Something went wrong. Please try again.')
    throw new Error(message)
  }
  return data
}

export async function apiDownload(path, options = {}) {
  const { token, onUnauthorized, ...requestOptions } = options
  const headers = new Headers(requestOptions.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response
  try { response = await fetch(`${API_BASE_URL}${path}`, { ...requestOptions, headers }) } catch { throw new Error('The service is unavailable. Check your connection and try again.') }
  if (response.status === 401) { onUnauthorized?.(); throw new Error('Your session has expired. Please log in again.') }
  if (!response.ok) {
    const data = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null
    throw new Error(data?.message || 'Unable to export this report. Please try again.')
  }
  return { blob: await response.blob(), filename: response.headers.get('content-disposition')?.match(/filename="?([^";]+)"?/)?.[1] }
}

export { API_BASE_URL }