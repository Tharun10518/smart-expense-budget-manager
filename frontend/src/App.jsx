import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import DashboardLayout from './layouts/DashboardLayout.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Profile from './pages/Profile.jsx'
import RecordsPage from './pages/RecordsPage.jsx'
import BudgetsPage from './pages/BudgetsPage.jsx'
import ReportsPage from './pages/ReportsPage.jsx'
function PublicRoute({ children }) { const { isAuthenticated } = useAuth(); return isAuthenticated ? <Navigate to="/dashboard" replace /> : children }
function Placeholder({ title }) { return <section className="placeholder-page"><p className="eyebrow">Coming in Step 6</p><h1>{title}</h1><p>This protected workspace is ready for the next feature set.</p></section> }
function App() { return <Routes><Route path="/login" element={<PublicRoute><Login /></PublicRoute>} /><Route path="/register" element={<PublicRoute><Register /></PublicRoute>} /><Route element={<ProtectedRoute />}><Route element={<DashboardLayout />}><Route path="/dashboard" element={<Dashboard />} /><Route path="/records" element={<RecordsPage />} /><Route path="/expenses" element={<RecordsPage type="expense" />} /><Route path="/income" element={<RecordsPage type="income" />} /><Route path="/budgets" element={<BudgetsPage />} /><Route path="/reports" element={<ReportsPage />} /><Route path="/profile" element={<Profile />} />{[['insights', 'Smart Insights'], ['notifications', 'Notifications'], ['settings', 'Settings']].map(([path, title]) => <Route key={path} path={`/${path}`} element={<Placeholder title={title} />} />)}</Route></Route><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes> }

export default App
