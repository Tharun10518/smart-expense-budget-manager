import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, LoaderCircle } from 'lucide-react'
import { AuthShell, Field, FormAlert } from '../components/AuthForm.jsx'
import { useAuth } from '../context/AuthContext.jsx'

export default function Login() {
  const { login } = useAuth(); const [form, setForm] = useState({ email: '', password: '' }); const [error, setError] = useState(''); const [loading, setLoading] = useState(false)
  const submit = async (event) => { event.preventDefault(); setError(''); if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return setError('Enter a valid email address.'); if (!form.password) return setError('Enter your password.'); setLoading(true); try { await login(form) } catch { setError('Invalid email or password. Please try again.') } finally { setLoading(false) } }
  return <AuthShell eyebrow="Welcome back" title="Sign in to your workspace" footer={<>New to Ledgerly? <Link to="/register">Create an account <ArrowRight size={14} /></Link></>}><form onSubmit={submit} className="auth-form"><FormAlert>{error}</FormAlert><Field label="Email address" type="email" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@example.com" /><Field label="Password" type="password" autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Enter your password" /><button className="primary-button" disabled={loading}>{loading ? <LoaderCircle className="spin" size={18} /> : 'Sign in'}{!loading && <ArrowRight size={18} />}</button></form></AuthShell>
}