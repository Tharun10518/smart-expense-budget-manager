import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Bell, ChevronDown, LayoutDashboard, Lightbulb, LogOut, Menu, Settings, UserCircle, WalletCards, X, FileBarChart2, ArrowDownToLine, ArrowUpFromLine } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
const navigation = [['Dashboard', '/dashboard', LayoutDashboard], ['Expenses', '/expenses', ArrowDownToLine], ['Income', '/income', ArrowUpFromLine], ['Budgets', '/budgets', WalletCards], ['Reports', '/reports', FileBarChart2], ['Smart Insights', '/insights', Lightbulb], ['Notifications', '/notifications', Bell], ['Profile', '/profile', UserCircle], ['Settings', '/settings', Settings]]
export default function DashboardLayout() {
	const { currentUser, logout } = useAuth()
	const [sidebarOpen, setSidebarOpen] = useState(false)
	const [profileOpen, setProfileOpen] = useState(false)
	const [notificationsOpen, setNotificationsOpen] = useState(false)
	const topbarUserRef = useRef(null)
	const name = currentUser?.fullName || 'there'

	useEffect(() => {
		const handleOutsideClick = (event) => {
			if (!topbarUserRef.current?.contains(event.target)) {
				setProfileOpen(false)
				setNotificationsOpen(false)
			}
		}
		document.addEventListener('mousedown', handleOutsideClick)
		return () => document.removeEventListener('mousedown', handleOutsideClick)
	}, [])

	const closeMenus = () => {
		setProfileOpen(false)
		setNotificationsOpen(false)
	}

	return <div className="app-shell"><aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}><div className="sidebar-brand"><span className="brand-mark"><WalletCards size={20} /></span> Ledgerly <button className="icon-button mobile-only" onClick={() => setSidebarOpen(false)} aria-label="Close menu"><X size={20} /></button></div><nav>{navigation.map(([label, path, Icon]) => <NavLink key={path} to={path} onClick={() => setSidebarOpen(false)} className={({ isActive }) => isActive ? 'active' : ''}><Icon size={18} />{label}</NavLink>)}</nav><button className="logout-button" onClick={logout}><LogOut size={18} /> Log out</button></aside>{sidebarOpen && <button className="sidebar-overlay" onClick={() => setSidebarOpen(false)} aria-label="Close menu" />}<div className="main-area"><header className="topbar"><button className="icon-button mobile-only" onClick={() => setSidebarOpen(true)} aria-label="Open menu"><Menu size={22} /></button><div className="topbar-title">Smart Expense &amp; Budget Manager</div><div className="topbar-user" ref={topbarUserRef}><div className="notification-wrapper"><button className="notification-button" onClick={() => { setNotificationsOpen((isOpen) => !isOpen); setProfileOpen(false) }} aria-label="Notifications" aria-expanded={notificationsOpen}><Bell size={19} /><span /></button>{notificationsOpen && <div className="topbar-dropdown notification-dropdown"><div className="dropdown-heading"><strong>Notifications</strong><span>0 new</span></div><p className="dropdown-empty">You are all caught up.</p><Link to="/notifications" onClick={closeMenus}>View all notifications</Link></div>}</div><button className="profile-trigger" onClick={() => { setProfileOpen((isOpen) => !isOpen); setNotificationsOpen(false) }} aria-label="Open profile menu" aria-expanded={profileOpen}><span className="avatar">{name.charAt(0).toUpperCase()}</span><span className="user-name">{name}</span><ChevronDown size={16} /></button>{profileOpen && <div className="topbar-dropdown profile-dropdown"><strong>{name}</strong><span className="dropdown-email">{currentUser?.email || 'Account'}</span><Link to="/profile" onClick={closeMenus}><UserCircle size={16} /> View profile</Link><button onClick={logout}><LogOut size={16} /> Log out</button></div>}</div></header><main className="page-content"><Outlet /></main></div></div>
}