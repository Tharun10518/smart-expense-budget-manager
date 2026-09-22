import { useEffect, useMemo, useState } from 'react'
import {
  AlertOctagon,
  AlertTriangle,
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  BarChart3,
  CheckCircle2,
  Gauge,
  Info,
  Lightbulb,
  PieChart as PieIcon,
  RefreshCw,
  Sparkles,
  Target,
  TrendingDown,
  TrendingUp,
  WalletCards
} from 'lucide-react'
import { Link } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts'
import { useAuth } from '../context/AuthContext.jsx'
import { dashboardService } from '../services/dashboardService.js'
import { budgetService } from '../services/budgetService.js'
import { formatCurrency, prettyLabel } from '../utils/finance.js'
import { EmptyState, ErrorState, LoadingState } from '../components/DataState.jsx'
import './insights.css'
import './budget-status.css'
import './dashboard.css'

const chartColors = ['#2a9b70', '#4d8db7', '#d8754d', '#b2782e', '#7b6cae', '#4d9a93', '#bd6581', '#809087']
const safe = (val) => (Number.isFinite(Number(val)) ? Number(val) : 0)

function MetricCard({ title, value, Icon, tone, subtext }) {
  return (
    <article className={`insight-metric-card ${tone}`}>
      <div className="insight-metric-icon">
        <Icon size={20} />
      </div>
      <div>
        <p>{title}</p>
        <strong>{value}</strong>
        {subtext && <small className="muted">{subtext}</small>}
      </div>
    </article>
  )
}

function InsightCard({ type, title, message, recommendation, tone }) {
  const Icon = tone === 'danger' ? AlertOctagon : tone === 'warning' ? AlertTriangle : tone === 'success' ? CheckCircle2 : Info

  return (
    <article className={`smart-insight-card tone-${tone}`}>
      <div className="smart-insight-header">
        <span className="smart-insight-tag">
          <Icon size={14} /> {type}
        </span>
      </div>
      <h3>{title}</h3>
      <p>{message}</p>
      {recommendation && (
        <div className="smart-insight-recommendation">
          <Lightbulb size={16} />
          <span>{recommendation}</span>
        </div>
      )}
    </article>
  )
}

export default function InsightsPage() {
  const { token, logout } = useAuth()
  const [dashboardData, setDashboardData] = useState(null)
  const [budgets, setBudgets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadAll = () => {
    setLoading(true)
    setError('')
    Promise.all([
      dashboardService.load(token, logout),
      budgetService.list(token, logout).catch(() => [])
    ])
      .then(([dash, budg]) => {
        setDashboardData(dash)
        setBudgets(Array.isArray(budg) ? budg : [])
      })
      .catch((err) => {
        setError(err.message || 'Unable to generate financial insights right now.')
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadAll()
  }, [token])

  const totalIncome = safe(dashboardData?.totalIncome)
  const totalExpenses = safe(dashboardData?.totalExpenses)
  const netSavings = totalIncome - totalExpenses
  const monthlyIncome = safe(dashboardData?.monthlyIncome)
  const monthlyExpenses = safe(dashboardData?.monthlyExpenses)
  const monthlySavings = monthlyIncome - monthlyExpenses
  const savingsRate = monthlyIncome > 0 ? (monthlySavings / monthlyIncome) * 100 : 0

  const budgetTotal = safe(dashboardData?.budget?.total)
  const budgetSpent = safe(dashboardData?.budget?.spent)
  const budgetUtilization = budgetTotal > 0 ? (budgetSpent / budgetTotal) * 100 : 0
  const transactionCount = safe(dashboardData?.transactionCount)

  const categories = useMemo(() => {
    if (!Array.isArray(dashboardData?.expensesByCategory)) return []
    return dashboardData.expensesByCategory
      .map((c) => ({
        name: prettyLabel(c.category),
        category: c.category,
        amount: safe(c.amount)
      }))
      .filter((c) => c.amount > 0)
      .sort((a, b) => b.amount - a.amount)
  }, [dashboardData])

  const trend = useMemo(() => {
    if (!Array.isArray(dashboardData?.monthlyTrend)) return []
    return dashboardData.monthlyTrend.map((item) => ({
      ...item,
      label: new Date(`${item.month}-01T00:00:00`).toLocaleDateString('en-IN', { month: 'short' }),
      income: safe(item.income),
      expenses: safe(item.expenses)
    }))
  }, [dashboardData])

  const hasData = totalIncome > 0 || totalExpenses > 0 || budgets.length > 0

  // Derived Smart Insights
  const insights = useMemo(() => {
    if (!hasData) return []
    const list = []

    // 1. Savings Health
    if (monthlyIncome > 0) {
      if (savingsRate >= 20) {
        list.push({
          type: 'Savings Rate',
          title: `Healthy Savings Rate (${savingsRate.toFixed(1)}%)`,
          message: `You are saving ${formatCurrency(monthlySavings)} this month. A savings rate above 20% provides strong financial resilience.`,
          recommendation: 'Consider allocating surplus savings toward your long-term goals or an emergency fund.',
          tone: 'success'
        })
      } else if (savingsRate > 0) {
        list.push({
          type: 'Savings Rate',
          title: `Moderate Savings Rate (${savingsRate.toFixed(1)}%)`,
          message: `You have saved ${formatCurrency(monthlySavings)} this month out of ${formatCurrency(monthlyIncome)} in earnings.`,
          recommendation: 'Look for non-essential subscriptions or discretionary expenses to boost your rate closer to 20%.',
          tone: 'warning'
        })
      } else {
        list.push({
          type: 'Cash Flow Warning',
          title: 'Expenses Exceed Monthly Income',
          message: `Your current month expenses (${formatCurrency(monthlyExpenses)}) exceed your income (${formatCurrency(monthlyIncome)}) by ${formatCurrency(Math.abs(monthlySavings))}.`,
          recommendation: 'Pause non-critical purchases and audit your highest spending categories immediately.',
          tone: 'danger'
        })
      }
    } else if (monthlyExpenses > 0) {
      list.push({
        type: 'Income Advisory',
        title: 'No Monthly Income Logged',
        message: `You have recorded ${formatCurrency(monthlyExpenses)} in expenses this month, but no incoming revenue has been logged.`,
        recommendation: 'Record your monthly salary, freelance earnings, or deposits to keep your cash flow accurate.',
        tone: 'info'
      })
    }

    // 2. Category Concentration
    if (categories.length > 0 && monthlyExpenses > 0) {
      const topCategory = categories[0]
      const categoryShare = (topCategory.amount / monthlyExpenses) * 100
      if (categoryShare >= 40) {
        list.push({
          type: 'Category Alert',
          title: `High Concentration in ${topCategory.name} (${categoryShare.toFixed(0)}%)`,
          message: `${topCategory.name} accounts for ${formatCurrency(topCategory.amount)} out of your ${formatCurrency(monthlyExpenses)} total spending this month.`,
          recommendation: `Look for cost reduction opportunities in ${topCategory.name} to balance your spending mix.`,
          tone: 'warning'
        })
      } else {
        list.push({
          type: 'Spending Distribution',
          title: `Balanced Spending across ${categories.length} Categories`,
          message: `Your highest expense is ${topCategory.name} at ${formatCurrency(topCategory.amount)} (${categoryShare.toFixed(0)}% of expenses).`,
          recommendation: 'Your expense distribution is well spread across categories.',
          tone: 'info'
        })
      }
    }

    // 3. Budget Status
    const today = new Date()
    const currentBudgets = budgets.filter(
      (b) => b.year === today.getFullYear() && b.month === today.getMonth() + 1
    )
    const exceeded = currentBudgets.filter((b) => safe(b.spent) > safe(b.totalLimit))
    const warnings = currentBudgets.filter((b) => {
      const p = safe(b.totalLimit) > 0 ? (safe(b.spent) / safe(b.totalLimit)) * 100 : 0
      return p >= 75 && p <= 100
    })

    if (exceeded.length > 0) {
      const names = exceeded.map((b) => prettyLabel(b.category)).join(', ')
      list.push({
        type: 'Budget Exceeded',
        title: `${exceeded.length} Category Budget${exceeded.length > 1 ? 's' : ''} Exceeded`,
        message: `You have overspent in: ${names}. Total overage across these categories requires attention.`,
        recommendation: 'Review the detailed transactions for these categories or adjust your budget allocation.',
        tone: 'danger'
      })
    } else if (warnings.length > 0) {
      const names = warnings.map((b) => prettyLabel(b.category)).join(', ')
      list.push({
        type: 'Budget Warning',
        title: `${warnings.length} Budget${warnings.length > 1 ? 's' : ''} Approaching Limit`,
        message: `${names} ${warnings.length > 1 ? 'have' : 'has'} reached over 75% of the allocated monthly limit.`,
        recommendation: 'Monitor transactions in these categories carefully over the remainder of the month.',
        tone: 'warning'
      })
    } else if (currentBudgets.length > 0) {
      list.push({
        type: 'Budget Discipline',
        title: 'All Active Budgets On Track',
        message: `All ${currentBudgets.length} configured category budgets are currently within safe spending limits.`,
        recommendation: 'Maintain your current pace through the end of the billing cycle.',
        tone: 'success'
      })
    }

    // 4. Month-over-Month Comparison
    const prevExpenses = safe(dashboardData?.previousMonthExpenses)
    if (prevExpenses > 0 && monthlyExpenses > 0) {
      const diff = monthlyExpenses - prevExpenses
      const pctChange = Math.abs((diff / prevExpenses) * 100)
      if (diff < 0) {
        list.push({
          type: 'Spending Trend',
          title: `Spending Reduced by ${pctChange.toFixed(0)}%`,
          message: `You spent ${formatCurrency(Math.abs(diff))} less than last month (${formatCurrency(monthlyExpenses)} vs ${formatCurrency(prevExpenses)}).`,
          recommendation: 'Your spending reduction directly contributes to higher monthly savings.',
          tone: 'success'
        })
      } else if (diff > 0 && pctChange >= 15) {
        list.push({
          type: 'Spending Trend',
          title: `Spending Increased by ${pctChange.toFixed(0)}%`,
          message: `Expenses are higher compared to last month (${formatCurrency(monthlyExpenses)} vs ${formatCurrency(prevExpenses)}).`,
          recommendation: 'Audit recent transactions in Records to identify unexpected charges.',
          tone: 'warning'
        })
      }
    }

    return list
  }, [dashboardData, budgets, categories, hasData, monthlyExpenses, monthlyIncome, monthlySavings, savingsRate])

  if (loading) {
    return (
      <section className="insights-page">
        <LoadingState />
      </section>
    )
  }

  if (error) {
    return (
      <section className="insights-page">
        <ErrorState message={error} onRetry={loadAll} />
      </section>
    )
  }

  const today = new Date()
  const activeBudgets = budgets.filter(
    (b) => b.year === today.getFullYear() && b.month === today.getMonth() + 1
  )

  return (
    <section className="insights-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Financial Intelligence</p>
          <h1>Smart Insights</h1>
          <p className="insights-intro">
            Automated spending patterns, cash flow diagnostics, and budget risk analysis.
          </p>
        </div>
        <div className="insights-actions">
          <button className="secondary-button insights-refresh" onClick={loadAll} disabled={loading}>
            <RefreshCw size={16} /> Refresh
          </button>
        </div>
      </div>

      {/* Top Metrics Row */}
      <div className="insights-metrics-grid">
        <MetricCard
          title="Total Income"
          value={formatCurrency(totalIncome)}
          Icon={ArrowUpRight}
          tone="income"
          subtext="All-time revenue"
        />
        <MetricCard
          title="Total Expenses"
          value={formatCurrency(totalExpenses)}
          Icon={ArrowDownLeft}
          tone="expense"
          subtext="All-time spending"
        />
        <MetricCard
          title="Net Savings"
          value={formatCurrency(netSavings)}
          Icon={WalletCards}
          tone="savings"
          subtext={savingsRate > 0 ? `${savingsRate.toFixed(0)}% monthly rate` : 'Overall balance'}
        />
        <MetricCard
          title="Budget Utilization"
          value={`${budgetUtilization.toFixed(0)}%`}
          Icon={Gauge}
          tone="budget"
          subtext={budgetTotal > 0 ? `${formatCurrency(budgetSpent)} / ${formatCurrency(budgetTotal)}` : 'No active budget'}
        />
        <MetricCard
          title="Transactions"
          value={transactionCount}
          Icon={Sparkles}
          tone="transactions"
          subtext="Total records logged"
        />
      </div>

      {!hasData ? (
        <EmptyState
          title="No Financial Records Logged Yet"
          text="Start adding your income, expenses, and budgets to enable real-time smart financial insights."
        />
      ) : (
        <>
          {/* Smart Insights Cards Grid */}
          <h2 className="insights-section-heading">Key Financial Insights</h2>
          {insights.length > 0 ? (
            <div className="insights-cards-grid">
              {insights.map((item, idx) => (
                <InsightCard
                  key={idx}
                  type={item.type}
                  title={item.title}
                  message={item.message}
                  recommendation={item.recommendation}
                  tone={item.tone}
                />
              ))}
            </div>
          ) : (
            <div className="dashboard-panel" style={{ marginBottom: '24px' }}>
              <p className="muted" style={{ margin: 0 }}>
                Insufficient transaction history to derive statistical patterns. Continue logging records to reveal deeper trends.
              </p>
            </div>
          )}

          {/* Visual Trends & Distributions */}
          <div className="insights-charts-grid">
            {/* Category Breakdown */}
            <article className="dashboard-panel chart-panel">
              <div className="dashboard-panel-heading">
                <div>
                  <p className="eyebrow">Spending Breakdown</p>
                  <h2>Expense Distribution</h2>
                </div>
                <PieIcon size={19} />
              </div>
              {categories.length > 0 ? (
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie
                      data={categories}
                      dataKey="amount"
                      nameKey="name"
                      cx="50%"
                      cy="46%"
                      innerRadius={60}
                      outerRadius={96}
                      paddingAngle={3}
                    >
                      {categories.map((entry, index) => (
                        <Cell key={entry.name} fill={chartColors[index % chartColors.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => formatCurrency(value)} />
                    <Legend verticalAlign="bottom" height={32} />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="analytics-empty">
                  <BarChart3 size={24} />
                  <p>No expense categories recorded yet.</p>
                </div>
              )}
            </article>

            {/* Income vs Expenses Cash Flow History */}
            <article className="dashboard-panel chart-panel">
              <div className="dashboard-panel-heading">
                <div>
                  <p className="eyebrow">6-Month Trend</p>
                  <h2>Income vs Expenses</h2>
                </div>
                <TrendingUp size={19} />
              </div>
              {trend.length > 0 ? (
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={trend} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#dce6dd" />
                    <XAxis dataKey="label" />
                    <YAxis tickFormatter={(val) => `₹${Math.round(val / 1000)}k`} />
                    <Tooltip formatter={(value) => formatCurrency(value)} />
                    <Legend />
                    <Bar dataKey="income" name="Income" fill="#2a9b70" radius={[5, 5, 0, 0]} />
                    <Bar dataKey="expenses" name="Expenses" fill="#d8754d" radius={[5, 5, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="analytics-empty">
                  <BarChart3 size={24} />
                  <p>Add records across billing periods to build historical cash flow trends.</p>
                </div>
              )}
            </article>
          </div>

          {/* Budget Health Analysis */}
          <article className="dashboard-panel" style={{ marginBottom: '24px' }}>
            <div className="dashboard-panel-heading">
              <div>
                <p className="eyebrow">Plan Variance</p>
                <h2>Active Budget Health</h2>
              </div>
              <Link className="text-link" to="/budgets">
                Manage budgets <ArrowRight size={15} />
              </Link>
            </div>

            {activeBudgets.length > 0 ? (
              <div className="table-wrap">
                <table className="budget-health-table">
                  <thead>
                    <tr>
                      <th>Category</th>
                      <th>Limit</th>
                      <th>Spent</th>
                      <th>Remaining</th>
                      <th>Utilization</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {activeBudgets.map((b) => {
                      const limit = safe(b.totalLimit)
                      const spent = safe(b.spent)
                      const remaining = Math.max(0, limit - spent)
                      const pct = limit > 0 ? (spent / limit) * 100 : 0
                      const status = b.status || (pct >= 100 ? 'EXCEEDED' : pct >= 75 ? 'WARNING' : 'ON_TRACK')
                      const barColor = status === 'EXCEEDED' ? '#b65a38' : status === 'WARNING' ? '#b2782e' : '#2a9b70'

                      return (
                        <tr key={b.id || b.category}>
                          <td>
                            <strong>{prettyLabel(b.category)}</strong>
                          </td>
                          <td>{formatCurrency(limit)}</td>
                          <td className={spent > limit ? 'negative-value' : ''}>{formatCurrency(spent)}</td>
                          <td className={spent > limit ? 'negative-value' : 'positive-value'}>
                            {spent > limit ? `-${formatCurrency(spent - limit)}` : formatCurrency(remaining)}
                          </td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <div className="budget-health-bar">
                                <span
                                  style={{
                                    width: `${Math.min(100, Math.max(0, pct))}%`,
                                    backgroundColor: barColor
                                  }}
                                />
                              </div>
                              <small>{pct.toFixed(0)}%</small>
                            </div>
                          </td>
                          <td>
                            <span className={`budget-status status-${status.toLowerCase()}`}>
                              {prettyLabel(status)}
                            </span>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="analytics-empty" style={{ minHeight: '160px' }}>
                <Target size={24} />
                <p>No active category budgets set for this month.</p>
                <Link className="primary-button compact-button" to="/budgets">
                  Create a budget
                </Link>
              </div>
            )}
          </article>
        </>
      )}
    </section>
  )
}

