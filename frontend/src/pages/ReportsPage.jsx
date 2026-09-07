import { useEffect, useMemo, useState } from 'react'
import { ArrowDownLeft, ArrowUpRight, BarChart3, CalendarRange, Download, FileBarChart2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { reportService } from '../services/reportService.js'
import { dateLabel, formatCurrency, prettyLabel } from '../utils/finance.js'
import { ErrorState, LoadingState } from '../components/DataState.jsx'
import './reports.css'
import './reports-export.css'

const today = new Date()
const iso = (date) => date.toISOString().slice(0, 10)
const rangeFor = (value) => { const end = new Date(); const start = new Date(end); if (value === 'month') start.setDate(1); if (value === 'previous') { start.setMonth(start.getMonth() - 1); start.setDate(1); end.setDate(0) } if (value === '3months') start.setMonth(start.getMonth() - 3); if (value === '6months') start.setMonth(start.getMonth() - 6); if (value === 'year') { start.setMonth(0); start.setDate(1) } return { startDate: iso(start), endDate: iso(end) } }
const safe = (value) => Number.isFinite(Number(value)) ? Number(value) : 0

export default function ReportsPage() {
  const { token, logout } = useAuth()
  const [period, setPeriod] = useState('month')
  const [reportType, setReportType] = useState('financial')
  const [customStart, setCustomStart] = useState(iso(new Date(today.getFullYear(), today.getMonth(), 1)))
  const [customEnd, setCustomEnd] = useState(iso(today))
  const [range, setRange] = useState(rangeFor('month'))
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const load = (nextRange = range) => { setLoading(true); setError(''); reportService.generate(nextRange.startDate, nextRange.endDate, token, logout).then(setData).catch((requestError) => setError(requestError.message || "We couldn't generate this report.")).finally(() => setLoading(false)) }
  useEffect(() => { load(range) }, [token])
  const applyPeriod = (value) => { setPeriod(value); if (value !== 'custom') { const nextRange = rangeFor(value); setRange(nextRange); load(nextRange) } }
  const generate = () => { const nextRange = period === 'custom' ? { startDate: customStart, endDate: customEnd } : rangeFor(period); if (!nextRange.startDate || !nextRange.endDate || nextRange.startDate > nextRange.endDate) return setError('Choose a valid date range where the start is not after the end.'); setRange(nextRange); load(nextRange) }
  const download = async (type) => { setExporting(type); setError(''); try { const result = await reportService.export(type, range.startDate, range.endDate, token, logout); const url = URL.createObjectURL(result.blob); const link = document.createElement('a'); link.href = url; link.download = result.filename || `${type}-report-${range.startDate}.csv`; link.click(); URL.revokeObjectURL(url); setNotice(`${prettyLabel(type)} report exported successfully.`); setTimeout(() => setNotice(''), 2800) } catch (exportError) { setError(exportError.message || 'Unable to export this report.') } finally { setExporting('') } }

  const expenses = data?.expenses || []
  const incomes = data?.income || []
  const budgets = data?.budgets || []
  const visibleRows = useMemo(() => reportType === 'expenses' ? expenses : reportType === 'income' ? incomes : [...expenses.map((item) => ({ ...item, type: 'Expense', label: item.category })), ...incomes.map((item) => ({ ...item, type: 'Income', label: item.source }))].sort((a, b) => b.date.localeCompare(a.date)), [reportType, expenses, incomes])
  if (loading && !data) return <section className="reports-page"><LoadingState /></section>

  return <section className="reports-page"><div className="page-heading"><div><p className="eyebrow">Financial intelligence</p><h1>Reports</h1><p className="reports-intro">Generate and export reports from your authenticated financial records.</p></div><Link className="secondary-button reports-link" to="/dashboard"><BarChart3 size={16} /> Dashboard</Link></div>{notice && <div className="success-alert page-notice">{notice}</div>}{error && <div className="report-error">{error}</div>}<article className="report-controls"><div><label>Report type<select value={reportType} onChange={(event) => setReportType(event.target.value)}><option value="financial">Financial report</option><option value="expenses">Expense report</option><option value="income">Income report</option></select></label><label>Date range<select value={period} onChange={(event) => applyPeriod(event.target.value)}><option value="month">This month</option><option value="previous">Previous month</option><option value="3months">Last 3 months</option><option value="6months">Last 6 months</option><option value="year">This year</option><option value="custom">Custom range</option></select></label>{period === 'custom' && <><label>Start date<input type="date" value={customStart} onChange={(event) => setCustomStart(event.target.value)} /></label><label>End date<input type="date" value={customEnd} onChange={(event) => setCustomEnd(event.target.value)} /></label></>}</div><button className="primary-button report-generate" onClick={generate} disabled={loading}><FileBarChart2 size={16} /> {loading ? 'Generating...' : 'Generate report'}</button></article><div className="report-summary"><div><ArrowUpRight size={18} /><span>Total income</span><strong className="positive-value">{formatCurrency(data?.summary?.totalIncome)}</strong></div><div><ArrowDownLeft size={18} /><span>Total expenses</span><strong className="negative-value">{formatCurrency(data?.summary?.totalExpenses)}</strong></div><div><FileBarChart2 size={18} /><span>Savings</span><strong>{formatCurrency(data?.summary?.totalSavings)}</strong></div><div><CalendarRange size={18} /><span>Total budget</span><strong>{formatCurrency(data?.summary?.totalBudget)}</strong></div></div><article className="report-panel"><div className="report-heading"><div><p className="eyebrow">Report data</p><h2>{prettyLabel(reportType)} records</h2></div><div className="report-export-actions"><button className="secondary-button" onClick={() => download('expenses')} disabled={Boolean(exporting)}><Download size={15} /> {exporting === 'expenses' ? 'Exporting...' : 'Expenses CSV'}</button><button className="secondary-button" onClick={() => download('income')} disabled={Boolean(exporting)}><Download size={15} /> {exporting === 'income' ? 'Exporting...' : 'Income CSV'}</button><button className="secondary-button" onClick={() => download('financial')} disabled={Boolean(exporting)}><Download size={15} /> {exporting === 'financial' ? 'Exporting...' : 'Financial CSV'}</button></div></div>{visibleRows.length ? <div className="report-table-wrap"><table><thead><tr><th>Date</th><th>Type</th><th>Category/source</th><th>Description</th><th className="amount-cell">Amount</th></tr></thead><tbody>{visibleRows.map((row, index) => { const expense = row.type === 'Expense' || reportType === 'expenses'; return <tr key={`${row.date}-${index}`}><td>{dateLabel(row.date)}</td><td>{row.type || 'Expense'}</td><td>{prettyLabel(row.label || row.category || row.source)}</td><td>{row.description || 'No description'}</td><td className={`amount-cell ${expense ? 'negative-value' : 'positive-value'}`}>{expense ? '-' : '+'}{formatCurrency(row.amount)}</td></tr> })}</tbody></table></div> : <p className="report-empty">No data exists for this date range.</p>}</article>{budgets.length > 0 && <article className="report-panel budget-report"><div className="report-heading"><div><p className="eyebrow">Budget analysis</p><h2>Budget usage</h2></div></div><div className="budget-report-list">{budgets.map((budget, index) => <div className="budget-report-row" key={`${budget.category}-${budget.year}-${budget.month}-${index}`}><span>{prettyLabel(budget.category)} · {budget.month}/{budget.year}</span><strong>{formatCurrency(budget.spent)} / {formatCurrency(budget.amount)} · {safe(budget.percentageUsed).toFixed(0)}% · {prettyLabel(budget.status)}</strong></div>)}</div></article>}</section>
}
