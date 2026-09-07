import { createContext, useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authService } from '../services/authService.js'

const AuthContext = createContext(null)
const STORAGE_KEY = 'smart-expense-auth'

function readStoredAuth() { try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)) || {} } catch { return {} } }

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const storedAuth = readStoredAuth()
  const [token, setToken] = useState(storedAuth.token || null)
  const [currentUser, setCurrentUser] = useState(storedAuth.user || null)
  const logout = () => { setToken(null); setCurrentUser(null); sessionStorage.removeItem(STORAGE_KEY); navigate('/login', { replace: true }) }
  useEffect(() => { if (!token) return; authService.getCurrentUser(token, logout).then(setCurrentUser).catch(() => logout()) }, [])
  const login = async (credentials) => { const response = await authService.login(credentials); const user = await authService.getCurrentUser(response.token, logout); setToken(response.token); setCurrentUser(user); sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ token: response.token, user })); navigate('/dashboard', { replace: true }) }
  const register = (details) => authService.register(details)
  return <AuthContext.Provider value={{ currentUser, token, isAuthenticated: Boolean(token), login, logout, register }}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }