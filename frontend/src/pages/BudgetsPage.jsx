import { useEffect, useMemo, useState } from 'react'
import { Edit3, Gauge, Plus, Target, Trash2, WalletCards, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { budgetService } from '../services/budgetService.js'
import { EmptyState, ErrorState, LoadingState } from '../components/DataState.jsx'
import { formatCurrency, prettyLabel } from '../utils/finance.js'
import './budgets.css'
import './budget-status.css'

const categories = ['FOOD', 'TRANSPORTATION', 'SHOPPING', 'UTILITIES', 'ENTERTAINMENT', 'HEALTHCARE', 'EDUCATION', 'TRAVEL', 'HOUSING', 'PERSONAL', 'DEBT', 'OTHER']
const now = new Date()
const monthNames = Array.from({ length: 12 }, (_, index) => new Date(2020, index, 1).toLocaleDateString('en-IN', { month: 'long' }))
const years = Array.from({ length: 5 }, (_, index) => now.getFullYear() - 2 + index)
const initialForm = { category: '', totalLimit: '', month: now.getMonth() + 1, year: now.getFullYear() }
const safe = (value) => Number.isFinite(Number(value)) ? Number(value) : 0

function BudgetModal({ budget, onClose, onSubmit, saving, error }) {
  const [form, setForm] = useState(budget ? { category: budget.category || 'OTHER', totalLimit: budget.totalLimit, month: budget.month, year: budget.year } : initialForm)
  const [validationError, setValidationError] = useState('')
  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))
  const submit = (event) => {
    event.preventDefault()
    if (!form.category) return setValidationError('Choose a budget category.')
    if (!form.totalLimit || Number(form.totalLimit) <= 0) return setValidationError('Budget amount must be greater than zero.')
    if (!form.month || Number(form.month) < 1 || Number(form.month) > 12) return setValidationError('Choose a valid month.')
    if (!form.year || Number(form.year) < 2000 || Number(form.year) > 2100) return setValidationError('Choose a valid year.')
    setValidationError('')
    onSubmit({ ...form, month: Number(form.month), year: Number(form.year), totalLimit: Number(form.totalLimit) })
  }
  return <div className="modal-backdrop"><section className="budget-modal"><div className="modal-heading"><div><p className="eyebrow">{budget ? 'Update plan' : 'New plan'}</p><h2>{budget ? 'Edit budget' : 'Create budget'}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close"><X size={20} /></button></div><form className="budget-form" onSubmit={submit}>{(error || validationError) && <div className="form-alert">{error || validationError}</div>}<label className="field"><span>Category</span><select value={form.category} onChange={update('category')} required><option value="">Select category</option>{categories.map((category) => <option key={category} value={category}>{prettyLabel(category)}</option>)}</select></label><label className="field"><span>Budget amount</span><input type="number" min="0.01" step="0.01" value={form.totalLimit} onChange={update('totalLimit')} placeholder="0.00" required /></label><div className="field-row"><label className="field"><span>Month</span><select value={form.month} onChange={update('month')} required>{monthNames.map((month, index) => <option key={month} value={index + 1}>{month}</option>)}</select></label><label className="field"><span>Year</span><select value={form.year} onChange={update('year')} required>{years.map((year) => <option key={year} value={year}>{year}</option>)}</select></label></div><div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancel</button><button className="primary-button" disabled={saving}>{saving ? 'Saving...' : budget ? 'Save changes' : 'Create budget'}</button></div></form></section></div>
}

function BudgetCard({ budget, onEdit, onDelete }) {
  const limit = safe(budget.totalLimit)
  const spent = safe(budget.spent)
  const percentage = safe(budget.percentageUsed)
  const status = budget.status || (percentage >= 100 ? 'EXCEEDED' : percentage >= 75 ? 'WARNING' : 'ON_TRACK')
  const over = status === 'EXCEEDED'
  return <article className={`budget-card-v2 ${over ? 'over-budget' : ''}`}><div className="budget-card-heading"><div className="budget-category-icon"><Target size={18} /></div><div><h3>{prettyLabel(budget.category || 'OTHER')}</h3><span>{monthNames[budget.month - 1]} {budget.year}</span></div><span className={`budget-status status-${status.toLowerCase()}`}>{prettyLabel(status)}</span><div className="budget-card-actions"><button className="icon-button" onClick={() => onEdit(budget)} aria-label="Edit budget"><Edit3 size={16} /></button><button className="icon-button danger-icon" onClick={() => onDelete(budget)} aria-label="Delete budget"><Trash2 size={16} /></button></div></div><div className="budget-values"><div><span>Budget</span><strong>{formatCurrency(limit)}</strong></div><div><span>Spent</span><strong>{formatCurrency(spent)}</strong></div><div><span>{over ? 'Over by' : 'Remaining'}</span><strong className={over ? 'negative-value' : ''}>{formatCurrency(over ? spent - limit : safe(budget.remaining))}</strong></div></div><div className="budget-progress-track"><span style={{ width: `${Math.min(100, Math.max(0, percentage))}%` }} /></div><div className="budget-progress-meta"><span>{status === 'EXCEEDED' && <strong className="negative-value">Over Budget · </strong>}{percentage.toFixed(0)}% used</span><span>{formatCurrency(spent)} spent</span></div></article>
}

export default function BudgetsPage() {
  const { token, logout } = useAuth()
  const [budgets, setBudgets] = useState([])
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [year, setYear] = useState(now.getFullYear())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [modal, setModal] = useState(null)
  const load = () => { setLoading(true); setError(''); budgetService.list(token, logout).then(setBudgets).catch(() => setError("We couldn't load your budgets right now. Please try again.")).finally(() => setLoading(false)) }
  useEffect(() => { load() }, [token])
  const visible = useMemo(() => budgets.filter((budget) => budget.month === Number(month) && budget.year === Number(year)), [budgets, month, year])
  const summary = useMemo(() => visible.reduce((result, budget) => { result.total += safe(budget.totalLimit); result.spent += safe(budget.spent); return result }, { total: 0, spent: 0 }), [visible])
  const percentage = summary.total ? summary.spent / summary.total * 100 : 0
  const save = async (payload) => { setSaving(true); try { const saved = modal.budget ? await budgetService.update(modal.budget.id, payload, token, logout) : await budgetService.create(payload, token, logout); setBudgets((current) => modal.budget ? current.map((item) => item.id === saved.id ? saved : item) : [saved, ...current]); setModal(null); setNotice(modal.budget ? 'Budget updated successfully.' : 'Budget created successfully.'); setTimeout(() => setNotice(''), 2800) } catch (saveError) { setModal((current) => ({ ...current, error: saveError.message || 'Unable to save this budget.' })) } finally { setSaving(false) } }
  const remove = async (budget) => { if (!window.confirm('Are you sure you want to delete this budget?')) return; try { await budgetService.remove(budget.id, token, logout); setBudgets((current) => current.filter((item) => item.id !== budget.id)); setNotice('Budget deleted successfully.'); setTimeout(() => setNotice(''), 2800) } catch (deleteError) { setError(deleteError.message || 'Unable to delete this budget.') } }
  return <section className="budgets-page"><div className="page-heading budgets-heading"><div><p className="eyebrow">Plan your spending</p><h1>Budget Management</h1><p className="budget-intro">Set category limits and keep your monthly spending intentional.</p></div><button className="primary-button" onClick={() => setModal({})}><Plus size={17} /> Add budget</button></div>{notice && <div className="success-alert page-notice">{notice}</div>}<div className="budget-filters"><label><span>Month</span><select value={month} onChange={(event) => setMonth(event.target.value)}>{monthNames.map((name, index) => <option key={name} value={index + 1}>{name}</option>)}</select></label><label><span>Year</span><select value={year} onChange={(event) => setYear(event.target.value)}>{years.map((option) => <option key={option} value={option}>{option}</option>)}</select></label></div>{loading ? <LoadingState /> : error ? <ErrorState message={error} onRetry={load} /> : <><div className="budget-summary-grid"><div><Gauge size={18} /><span>Total budget</span><strong>{formatCurrency(summary.total)}</strong></div><div><WalletCards size={18} /><span>Total spent</span><strong>{formatCurrency(summary.spent)}</strong></div><div><Target size={18} /><span>Remaining budget</span><strong className={summary.spent > summary.total ? 'negative-value' : ''}>{formatCurrency(Math.max(0, summary.total - summary.spent))}</strong></div><div><Gauge size={18} /><span>Overall used</span><strong className={summary.spent > summary.total ? 'negative-value' : ''}>{percentage.toFixed(0)}%</strong></div></div>{visible.length ? <div className="budget-card-grid">{visible.map((budget) => <BudgetCard key={budget.id} budget={budget} onEdit={(item) => setModal({ budget: item })} onDelete={remove} />)}</div> : <EmptyState title="No budgets for this period" text="Create a category budget to start tracking your plan." onAdd={() => setModal({})} />}</>}{modal && <BudgetModal budget={modal.budget} error={modal.error} saving={saving} onClose={() => setModal(null)} onSubmit={save} />}</section>
}
