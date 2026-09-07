import { Mail, UserCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'

export default function Profile() {
  const { currentUser, logout } = useAuth()
  return <section className="profile-page"><p className="eyebrow">Account</p><h1>Your profile</h1><div className="profile-card"><div className="profile-avatar"><UserCircle size={30} /></div><div><h2>{currentUser?.fullName || 'Your name'}</h2><p><Mail size={16} /> {currentUser?.email || 'Your email'}</p></div></div><button className="secondary-button" onClick={logout}>Log out</button></section>
}