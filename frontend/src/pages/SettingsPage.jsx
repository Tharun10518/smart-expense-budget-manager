import { useState, useEffect } from 'react'
import {
  Eye,
  EyeOff,
  Lock,
  Moon,
  Palette,
  Save,
  ShieldCheck,
  Sun,
  User,
  UserCircle,
  Laptop,
  Check,
  AlertCircle
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { authService } from '../services/authService.js'
import './settings.css'

export default function SettingsPage() {
  const { currentUser, token, logout, updateCurrentUser } = useAuth()
  const [activeTab, setActiveTab] = useState('profile')

  // Profile Form State
  const [fullName, setFullName] = useState(currentUser?.fullName || '')
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileNotice, setProfileNotice] = useState('')
  const [profileError, setProfileError] = useState('')

  // Password Form State
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordNotice, setPasswordNotice] = useState('')
  const [passwordError, setPasswordError] = useState('')

  // Preferences State
  const [theme, setTheme] = useState(() => localStorage.getItem('ledgerly-theme') || 'system')
  const [inAppAlerts, setInAppAlerts] = useState(() => localStorage.getItem('ledgerly-inapp-alerts') !== 'false')

  useEffect(() => {
    if (currentUser?.fullName) {
      setFullName(currentUser.fullName)
    }
  }, [currentUser])

  // Apply Theme
  const handleThemeChange = (selectedTheme) => {
    setTheme(selectedTheme)
    localStorage.setItem('ledgerly-theme', selectedTheme)
    const root = document.documentElement
    if (selectedTheme === 'dark') {
      root.classList.add('dark')
      root.style.colorScheme = 'dark'
    } else if (selectedTheme === 'light') {
      root.classList.remove('dark')
      root.style.colorScheme = 'light'
    } else {
      root.classList.remove('dark')
      root.style.colorScheme = ''
    }
  }

  // Toggle in-app alerts
  const handleAlertsToggle = (e) => {
    const checked = e.target.checked
    setInAppAlerts(checked)
    localStorage.setItem('ledgerly-inapp-alerts', String(checked))
  }

  // Save Profile
  const handleProfileSubmit = async (e) => {
    e.preventDefault()
    if (!fullName.trim()) {
      setProfileError('Full name cannot be empty.')
      return
    }
    setProfileSaving(true)
    setProfileError('')
    setProfileNotice('')
    try {
      const updatedUser = await authService.updateProfile({ fullName: fullName.trim() }, token, logout)
      updateCurrentUser(updatedUser)
      setProfileNotice('Profile updated successfully.')
      setTimeout(() => setProfileNotice(''), 3000)
    } catch (err) {
      setProfileError(err.message || 'Unable to update profile.')
    } finally {
      setProfileSaving(false)
    }
  }

  // Change Password
  const handlePasswordSubmit = async (e) => {
    e.preventDefault()
    setPasswordError('')
    setPasswordNotice('')

    if (!currentPassword) {
      setPasswordError('Current password is required.')
      return
    }
    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters long.')
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match.')
      return
    }

    setPasswordSaving(true)
    try {
      await authService.changePassword(
        { currentPassword, newPassword },
        token,
        logout
      )
      setPasswordNotice('Password changed successfully.')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setTimeout(() => setPasswordNotice(''), 3000)
    } catch (err) {
      setPasswordError(err.message || 'Unable to change password.')
    } finally {
      setPasswordSaving(false)
    }
  }

  return (
    <section className="settings-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Preferences &amp; Security</p>
          <h1>Settings</h1>
          <p className="settings-intro">
            Manage your personal profile, credentials, and application preferences.
          </p>
        </div>
      </div>

      <div className="settings-layout">
        {/* Navigation Sidebar */}
        <aside className="settings-nav">
          <button
            className={`settings-nav-btn ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => setActiveTab('profile')}
          >
            <User size={17} /> Account Profile
          </button>
          <button
            className={`settings-nav-btn ${activeTab === 'security' ? 'active' : ''}`}
            onClick={() => setActiveTab('security')}
          >
            <Lock size={17} /> Security &amp; Password
          </button>
          <button
            className={`settings-nav-btn ${activeTab === 'preferences' ? 'active' : ''}`}
            onClick={() => setActiveTab('preferences')}
          >
            <Palette size={17} /> Preferences
          </button>
          <button
            className={`settings-nav-btn ${activeTab === 'account' ? 'active' : ''}`}
            onClick={() => setActiveTab('account')}
          >
            <ShieldCheck size={17} /> Session &amp; Actions
          </button>
        </aside>

        {/* Settings Panels */}
        <div className="settings-content">
          {/* TAB 1: PROFILE */}
          {activeTab === 'profile' && (
            <div className="settings-card">
              <div className="settings-card-header">
                <h2>Account Profile</h2>
                <p>Update your public display name and account details.</p>
              </div>

              {profileNotice && <div className="success-alert page-notice"><Check size={16} /> {profileNotice}</div>}
              {profileError && <div className="form-alert page-notice"><AlertCircle size={16} /> {profileError}</div>}

              <form className="settings-form" onSubmit={handleProfileSubmit}>
                <label className="field">
                  <span>Full Name</span>
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Enter your full name"
                    maxLength={100}
                    required
                  />
                </label>

                <label className="field">
                  <span>Email Address</span>
                  <input
                    type="email"
                    value={currentUser?.email || ''}
                    disabled
                    style={{ opacity: 0.75, cursor: 'not-allowed' }}
                  />
                  <small className="muted">Email address cannot be changed.</small>
                </label>

                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginTop: '10px' }}>
                  <button className="primary-button" type="submit" disabled={profileSaving}>
                    <Save size={16} /> {profileSaving ? 'Saving...' : 'Save changes'}
                  </button>
                  <Link className="text-link" to="/profile">
                    <UserCircle size={16} /> View Profile Card
                  </Link>
                </div>
              </form>
            </div>
          )}

          {/* TAB 2: SECURITY */}
          {activeTab === 'security' && (
            <div className="settings-card">
              <div className="settings-card-header">
                <h2>Security &amp; Password</h2>
                <p>Keep your account secure by using a strong password.</p>
              </div>

              {passwordNotice && <div className="success-alert page-notice"><Check size={16} /> {passwordNotice}</div>}
              {passwordError && <div className="form-alert page-notice"><AlertCircle size={16} /> {passwordError}</div>}

              <form className="settings-form" onSubmit={handlePasswordSubmit}>
                <label className="field">
                  <span>Current Password</span>
                  <div className="password-input-wrapper">
                    <input
                      type={showCurrent ? 'text' : 'password'}
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      placeholder="Enter current password"
                      required
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowCurrent(!showCurrent)}
                      aria-label={showCurrent ? 'Hide current password' : 'Show current password'}
                    >
                      {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </label>

                <label className="field">
                  <span>New Password</span>
                  <div className="password-input-wrapper">
                    <input
                      type={showNew ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Minimum 6 characters"
                      minLength={6}
                      maxLength={100}
                      required
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowNew(!showNew)}
                      aria-label={showNew ? 'Hide new password' : 'Show new password'}
                    >
                      {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </label>

                <label className="field">
                  <span>Confirm New Password</span>
                  <div className="password-input-wrapper">
                    <input
                      type={showConfirm ? 'text' : 'password'}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Repeat new password"
                      minLength={6}
                      maxLength={100}
                      required
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowConfirm(!showConfirm)}
                      aria-label={showConfirm ? 'Hide confirmed password' : 'Show confirmed password'}
                    >
                      {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </label>

                <div style={{ marginTop: '10px' }}>
                  <button className="primary-button" type="submit" disabled={passwordSaving}>
                    <Lock size={16} /> {passwordSaving ? 'Updating...' : 'Update Password'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* TAB 3: PREFERENCES */}
          {activeTab === 'preferences' && (
            <div className="settings-card">
              <div className="settings-card-header">
                <h2>Application Preferences</h2>
                <p>Customize your visual presentation and notification alerts.</p>
              </div>

              <div className="preference-item">
                <div className="preference-info">
                  <strong>Color Theme</strong>
                  <span>Select appearance preference for Ledgerly.</span>
                </div>
                <div className="preference-control">
                  <div className="theme-options">
                    <button
                      type="button"
                      className={`theme-btn ${theme === 'system' ? 'active' : ''}`}
                      onClick={() => handleThemeChange('system')}
                    >
                      <Laptop size={15} /> System
                    </button>
                    <button
                      type="button"
                      className={`theme-btn ${theme === 'light' ? 'active' : ''}`}
                      onClick={() => handleThemeChange('light')}
                    >
                      <Sun size={15} /> Light
                    </button>
                    <button
                      type="button"
                      className={`theme-btn ${theme === 'dark' ? 'active' : ''}`}
                      onClick={() => handleThemeChange('dark')}
                    >
                      <Moon size={15} /> Dark
                    </button>
                  </div>
                </div>
              </div>

              <div className="preference-item">
                <div className="preference-info">
                  <strong>Default Currency</strong>
                  <span>Standard currency format used across your financial calculations.</span>
                </div>
                <div className="preference-control">
                  <span style={{ fontWeight: 800, color: '#126b4d', fontSize: '0.9rem' }}>
                    ₹ INR (Indian Rupee)
                  </span>
                </div>
              </div>

              <div className="preference-item">
                <div className="preference-info">
                  <strong>In-App Alerts</strong>
                  <span>Show instant visual alerts for budget thresholds and spending trends.</span>
                </div>
                <div className="preference-control">
                  <label className="toggle-switch">
                    <input
                      type="checkbox"
                      checked={inAppAlerts}
                      onChange={handleAlertsToggle}
                    />
                    <span className="toggle-slider" />
                  </label>
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: SESSION & ACTIONS */}
          {activeTab === 'account' && (
            <>
              <div className="settings-card">
                <div className="settings-card-header">
                  <h2>Active Session</h2>
                  <p>Details about your current authenticated session.</p>
                </div>
                <div className="preference-item">
                  <div className="preference-info">
                    <strong>Authentication Method</strong>
                    <span>Stateless JSON Web Token (JWT) with HMAC-SHA256 signature.</span>
                  </div>
                  <span className="badge" style={{ color: '#126b4d', fontWeight: 800 }}>
                    Secure &amp; Active
                  </span>
                </div>
                <div className="preference-item">
                  <div className="preference-info">
                    <strong>Account Identifier</strong>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                      {currentUser?.id || 'Authenticated'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="settings-card danger-zone-card">
                <div className="settings-card-header">
                  <h2>Sign Out</h2>
                  <p>Terminate your current session securely from this device.</p>
                </div>
                <div>
                  <p style={{ margin: '0 0 16px', fontSize: '0.85rem', color: '#718078' }}>
                    Signing out will clear your authentication token from this browser session.
                  </p>
                  <button
                    className="danger-button"
                    onClick={() => {
                      if (window.confirm('Are you sure you want to log out?')) {
                        logout()
                      }
                    }}
                  >
                    Log out from this device
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  )
}

