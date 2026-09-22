import { useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  Bell,
  Check,
  CheckCheck,
  CircleAlert,
  Info,
  RefreshCw,
  TrendingDown
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { notificationService } from '../services/notificationService.js'
import { EmptyState, ErrorState, LoadingState } from '../components/DataState.jsx'
import './notifications.css'

function formatTimestamp(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffMinutes < 1) return 'Just now'
  if (diffMinutes < 60) return `${diffMinutes}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

export default function NotificationsPage() {
  const { token, logout } = useAuth()
  const [notifications, setNotifications] = useState([])
  const [filter, setFilter] = useState('all')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadNotifications = () => {
    setLoading(true)
    setError('')
    notificationService
      .list(token, logout)
      .then((data) => {
        setNotifications(Array.isArray(data) ? data : [])
      })
      .catch((err) => {
        setError(err.message || "We couldn't load your notifications.")
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadNotifications()
  }, [token])

  const markRead = async (id) => {
    try {
      const updated = await notificationService.markAsRead(id, token, logout)
      setNotifications((prev) =>
        prev.map((item) => (item.id === id ? { ...item, status: updated.status, readAt: updated.readAt } : item))
      )
    } catch {
      // ignore or show temporary error
    }
  }

  const markAllRead = async () => {
    setActionLoading(true)
    try {
      await notificationService.markAllAsRead(token, logout)
      setNotifications((prev) =>
        prev.map((item) => ({ ...item, status: 'READ', readAt: new Date().toISOString() }))
      )
      setNotice('All notifications marked as read.')
      setTimeout(() => setNotice(''), 2500)
    } catch (err) {
      setError(err.message || 'Unable to mark all notifications as read.')
    } finally {
      setActionLoading(false)
    }
  }

  const unreadCount = useMemo(
    () => notifications.filter((n) => n.status === 'UNREAD').length,
    [notifications]
  )

  const filteredNotifications = useMemo(() => {
    if (filter === 'unread') return notifications.filter((n) => n.status === 'UNREAD')
    if (filter === 'BUDGET_ALERT') return notifications.filter((n) => n.type === 'BUDGET_ALERT')
    if (filter === 'SPENDING_INSIGHT') return notifications.filter((n) => n.type === 'SPENDING_INSIGHT')
    if (filter === 'SYSTEM') return notifications.filter((n) => n.type === 'SYSTEM')
    return notifications
  }, [notifications, filter])

  if (loading) {
    return (
      <section className="notifications-page">
        <LoadingState />
      </section>
    )
  }

  return (
    <section className="notifications-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Activity &amp; Alerts</p>
          <h1>Notifications</h1>
          <p className="notifications-intro">
            Real-time updates, spending alerts, and budget threshold warnings.
          </p>
        </div>
        <div className="notifications-top-actions">
          {unreadCount > 0 && (
            <button
              className="secondary-button"
              onClick={markAllRead}
              disabled={actionLoading}
              style={{ margin: 0 }}
            >
              <CheckCheck size={16} /> Mark all read
            </button>
          )}
          <button className="secondary-button" onClick={loadNotifications} disabled={loading} style={{ margin: 0 }}>
            <RefreshCw size={16} />
          </button>
        </div>
      </div>

      {notice && <div className="success-alert page-notice">{notice}</div>}
      {error && <ErrorState message={error} onRetry={loadNotifications} />}

      {/* Filter Tabs */}
      <div className="notifications-filter-bar">
        <button
          className={`notifications-filter-btn ${filter === 'all' ? 'active' : ''}`}
          onClick={() => setFilter('all')}
        >
          All <span className="notification-count-badge">{notifications.length}</span>
        </button>
        <button
          className={`notifications-filter-btn ${filter === 'unread' ? 'active' : ''}`}
          onClick={() => setFilter('unread')}
        >
          Unread <span className="notification-count-badge">{unreadCount}</span>
        </button>
        <button
          className={`notifications-filter-btn ${filter === 'BUDGET_ALERT' ? 'active' : ''}`}
          onClick={() => setFilter('BUDGET_ALERT')}
        >
          Budget Alerts
        </button>
        <button
          className={`notifications-filter-btn ${filter === 'SPENDING_INSIGHT' ? 'active' : ''}`}
          onClick={() => setFilter('SPENDING_INSIGHT')}
        >
          Spending Insights
        </button>
        <button
          className={`notifications-filter-btn ${filter === 'SYSTEM' ? 'active' : ''}`}
          onClick={() => setFilter('SYSTEM')}
        >
          System
        </button>
      </div>

      {/* Notifications List */}
      {filteredNotifications.length === 0 ? (
        <EmptyState
          title={
            filter === 'unread'
              ? 'You are all caught up!'
              : 'No notifications in this category'
          }
          text={
            filter === 'unread'
              ? 'There are no unread notifications right now.'
              : 'Notifications will automatically appear when budgets or spending trigger alerts.'
          }
        />
      ) : (
        <div className="notifications-list">
          {filteredNotifications.map((n) => {
            const isUnread = n.status === 'UNREAD'
            const isBudget = n.type === 'BUDGET_ALERT'
            const isSpending = n.type === 'SPENDING_INSIGHT'
            const Icon = isBudget ? AlertTriangle : isSpending ? TrendingDown : Info
            const iconClass = isBudget ? 'budget' : isSpending ? 'spending' : 'system'

            return (
              <article key={n.id} className={`notification-card ${isUnread ? 'unread' : ''}`}>
                <div className={`notification-type-icon ${iconClass}`}>
                  <Icon size={20} />
                </div>
                <div className="notification-content">
                  <div className="notification-header">
                    <h3 className="notification-title">{n.title}</h3>
                    <span className="notification-time">{formatTimestamp(n.createdAt)}</span>
                  </div>
                  <p className="notification-message">{n.message}</p>
                  <div className="notification-actions">
                    {isUnread && (
                      <button
                        className="notification-action-btn"
                        onClick={() => markRead(n.id)}
                      >
                        <Check size={14} /> Mark as read
                      </button>
                    )}
                    {isBudget && (
                      <Link className="notification-action-btn" to="/budgets">
                        Manage Budgets <ArrowRight size={13} />
                      </Link>
                    )}
                    {isSpending && (
                      <Link className="notification-action-btn" to="/expenses">
                        View Expenses <ArrowRight size={13} />
                      </Link>
                    )}
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

