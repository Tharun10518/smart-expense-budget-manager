import { useEffect, useState } from 'react'
import { ArrowDownLeft, ArrowRight, ArrowUpRight, BarChart3, CalendarDays, CircleDollarSign, Gauge, Lightbulb, PiggyBank, Receipt, RefreshCw, Target, WalletCards } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useAuth } from '../context/AuthContext.jsx'
import { dashboardService } from '../services/dashboardService.js'
import { dateLabel, formatCurrency, prettyLabel } from '../utils/finance.js'
import { ErrorState, LoadingState } from '../components/DataState.jsx'
import './dashboard.css'
import './budget-status.css'

const safeNumber = (value) => Number.isFinite(Number(value)) ? Number(value) : 0
const displayName = (user) => user?.fullName?.split(' ')[0] || user?.email?.split('@')[0] || 'there'
const chartColors = ['#2a9b70', '#4d8db7', '#d8754d', '#b2782e', '#7b6cae', '#4d9a93', '#bd6581', '#809087']
const tooltipFormatter = (value) => formatCurrency(value)

function SummaryCard({ title, value, Icon, tone, suffix = '' }) {
  return <article className={`dashboard-stat ${tone}`}><div className="dashboard-stat-icon"><Icon size={20} /></div><div><p>{title}</p><strong>{formatCurrency(safeNumber(value))}{suffix}</strong></div></article>
}

function QuickAction({ to, label, Icon }) {
  return <Link className="quick-action" to={to}><span><Icon size={18} />{label}</span><ArrowRight size={16} /></Link>
}

function ChartEmpty({ text }) {
  return <div className="analytics-empty"><BarChart3 size={24} /><p>{text}</p></div>
}

export default function Dashboard() {
  const { currentUser, token, logout } = useAuth()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const load = () => { setLoading(true); setError(''); dashboardService.load(token, logout).then(setData).catch(() => setError("We couldn't load your dashboard right now. Please try again.")).finally(() => setLoading(false)) }
  useEffect(() => { load() }, [token])

  if (loading) return <section className="dashboard-page"><LoadingState /></section>
  if (error) return <section className="dashboard-page"><ErrorState message={error} onRetry={load} /></section>

  const dashboard = data || {}
  const budget = dashboard.budget || { total: 0, spent: 0, remaining: 0, percentageUsed: 0 }
  const categories = Array.isArray(dashboard.expensesByCategory) ? dashboard.expensesByCategory.map((item) => ({ ...item, name: prettyLabel(item.category), amount: safeNumber(item.amount) })) : []
  const trend = Array.isArray(dashboard.monthlyTrend) ? dashboard.monthlyTrend.map((item) => ({ ...item, label: new Date(`${item.month}-01T00:00:00`).toLocaleDateString('en-IN', { month: 'short' }), income: safeNumber(item.income), expenses: safeNumber(item.expenses) })) : []
  const transactions = Array.isArray(dashboard.recentTransactions) ? dashboard.recentTransactions : []
  const totalIncome = safeNumber(dashboard.totalIncome)
  const totalExpenses = safeNumber(dashboard.totalExpenses)
  const monthlySavings = safeNumber(dashboard.monthlyBalance)
  const savingsPercentage = safeNumber(dashboard.monthlySavingsPercentage)
  const budgetTotal = safeNumber(budget.total)
  const budgetSpent = safeNumber(budget.spent)
  const budgetRemaining = safeNumber(budget.remaining)
  const budgetPercentage = safeNumber(budget.percentageUsed)
  const hasTransactions = transactions.length > 0 || totalIncome > 0 || totalExpenses > 0
  const insight = (() => {
    if (!hasTransactions) return 'No transactions yet. Add your first income or expense to start tracking your finances.'
    if (safeNumber(dashboard.monthlyExpenses) > safeNumber(dashboard.monthlyIncome)) return 'Your expenses are higher than your income this month.'
    if (budgetTotal > 0 && budgetPercentage >= 100) return 'You have exceeded your monthly budget. Review your largest categories.'
    if (categories[0]?.name) return `${categories[0].name} is your highest spending category this month.`
    return 'You are within your monthly budget this month.'
  })()

  return <section className="dashboard-page dashboard-v2">
    <div className="dashboard-hero page-heading"><div><p className="eyebrow">Personal overview</p><h1>Dashboard</h1><p className="dashboard-welcome">Welcome back, {displayName(currentUser)}. Here is your financial picture.</p></div><button className="secondary-button dashboard-refresh" onClick={load} disabled={loading}><RefreshCw size={16} /> Refresh</button></div>

    <div className="dashboard-stat-grid analytics-stat-grid"><SummaryCard title="Total income" value={totalIncome} Icon={ArrowUpRight} tone="income" /><SummaryCard title="Total expenses" value={totalExpenses} Icon={ArrowDownLeft} tone="expense" /><SummaryCard title="Current balance" value={dashboard.totalBalance} Icon={CircleDollarSign} tone="balance" /><SummaryCard title="Current savings" value={monthlySavings} Icon={WalletCards} tone="remaining" /><SummaryCard title="Current month budget" value={budgetTotal} Icon={Target} tone="budget" /><SummaryCard title="Budget used" value={budgetPercentage} suffix="%" Icon={Gauge} tone="usage" /></div>

    {!hasTransactions && <article className="dashboard-panel dashboard-empty-panel"><Receipt size={28} /><div><h2>Start your financial story</h2><p>{insight}</p></div><div className="empty-actions"><Link className="primary-button" to="/income"><ArrowUpRight size={17} /> Add income</Link><Link className="secondary-button" to="/expenses"><ArrowDownLeft size={17} /> Add expense</Link></div></article>}

    <div className="analytics-chart-grid"><article className="dashboard-panel chart-panel"><div className="dashboard-panel-heading"><div><p className="eyebrow">Spending breakdown</p><h2>Expenses by category</h2></div><Receipt size={19} /></div>{categories.length ? <ResponsiveContainer width="100%" height={280}><PieChart><Pie data={categories} dataKey="amount" nameKey="name" cx="50%" cy="46%" innerRadius={62} outerRadius={98} paddingAngle={3}>{categories.map((entry, index) => <Cell key={entry.name} fill={chartColors[index % chartColors.length]} />)}</Pie><Tooltip formatter={tooltipFormatter} /><Legend verticalAlign="bottom" height={28} /></PieChart></ResponsiveContainer> : <ChartEmpty text="No expense data for this month." />}</article><article className="dashboard-panel chart-panel"><div className="dashboard-panel-heading"><div><p className="eyebrow">Cash flow history</p><h2>Monthly income vs expenses</h2></div><CalendarDays size={19} /></div>{trend.length ? <ResponsiveContainer width="100%" height={280}><BarChart data={trend} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#dce6dd" /><XAxis dataKey="label" /><YAxis tickFormatter={(value) => `₹${Math.round(value / 1000)}k`} /><Tooltip formatter={tooltipFormatter} /><Legend /><Bar dataKey="income" name="Income" fill="#2a9b70" radius={[5, 5, 0, 0]} /><Bar dataKey="expenses" name="Expenses" fill="#d8754d" radius={[5, 5, 0, 0]} /></BarChart></ResponsiveContainer> : <ChartEmpty text="Add records to build your monthly trend." />}</article></div>

    <div className="analytics-lower-grid"><article className="dashboard-panel budget-compare-panel"><div className="dashboard-panel-heading"><div><p className="eyebrow">Plan versus reality</p><h2>Budget vs spending</h2></div><Target size={19} /></div><div className="comparison-values"><div><span>Budget</span><strong>{formatCurrency(budgetTotal)}</strong></div><div><span>Spent</span><strong className="negative-value">{formatCurrency(budgetSpent)}</strong></div><div><span>Remaining</span><strong>{formatCurrency(budgetRemaining)}</strong></div></div><div className="comparison-bar"><span className="comparison-spent" style={{ width: `${Math.min(100, Math.max(0, budgetPercentage))}%` }} /></div><div className="budget-meta"><span>{budgetTotal ? `${budgetPercentage.toFixed(0)}% used` : 'No budget set'} {budget.status && <strong className={`dashboard-status status-${budget.status.toLowerCase()}`}>{prettyLabel(budget.status)}</strong>}</span><Link className="text-link" to="/budgets">Manage budget <ArrowRight size={15} /></Link></div></article><article className="dashboard-panel insight-panel"><div className="dashboard-panel-heading"><div><p className="eyebrow">Financial health</p><h2>One useful signal</h2></div><Lightbulb size={19} /></div><div className={`insight-message ${budgetPercentage >= 100 ? 'insight-warning' : ''}`}><Lightbulb size={20} /><p>{insight} Savings this month: {formatCurrency(monthlySavings)} ({savingsPercentage.toFixed(0)}%).</p></div></article></div>

    <article className="dashboard-panel recent-panel"><div className="dashboard-panel-heading"><div><p className="eyebrow">Latest activity</p><h2>Recent transactions</h2></div><Link className="text-link" to="/records">View all <ArrowRight size={15} /></Link></div>{transactions.length ? <div className="dashboard-transactions">{transactions.map((transaction, index) => { const income = transaction.type === 'INCOME'; return <div className="dashboard-transaction" key={`${transaction.type}-${transaction.date}-${index}`}><span className={`transaction-badge ${income ? 'transaction-income' : 'transaction-expense'}`}>{income ? <ArrowUpRight size={17} /> : <ArrowDownLeft size={17} />}</span><div className="transaction-copy"><strong>{transaction.description || prettyLabel(transaction.label)}</strong><span>{income ? 'Income' : 'Expense'} · {prettyLabel(transaction.label)} · {dateLabel(transaction.date)}</span></div><strong className={income ? 'positive-value' : 'negative-value'}>{income ? '+' : '-'}{formatCurrency(transaction.amount)}</strong></div> })}</div> : <div className="analytics-empty recent-empty"><p>No recent transactions yet.</p><div className="empty-actions"><Link className="text-link" to="/income">Add income <ArrowRight size={15} /></Link><Link className="text-link" to="/expenses">Add expense <ArrowRight size={15} /></Link></div></div>}</article>

    <article className="dashboard-panel quick-panel"><div className="dashboard-panel-heading"><div><p className="eyebrow">Shortcuts</p><h2>Quick actions</h2></div><WalletCards size={19} /></div><div className="quick-actions"><QuickAction to="/expenses" label="Add expense" Icon={ArrowDownLeft} /><QuickAction to="/income" label="Add income" Icon={ArrowUpRight} /><QuickAction to="/records" label="View records" Icon={Receipt} /><QuickAction to="/budgets" label="Manage budget" Icon={Target} /></div></article>
  </section>
}
