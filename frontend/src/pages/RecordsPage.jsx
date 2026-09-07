import { useEffect, useMemo, useState } from 'react'
import { Edit3, Plus, Search, Trash2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { expenseService } from '../services/expenseService.js'
import { incomeService } from '../services/incomeService.js'
import { dateLabel, formatCurrency, prettyLabel } from '../utils/finance.js'
import { EmptyState, ErrorState, LoadingState } from '../components/DataState.jsx'
import RecordModal from '../components/RecordModal.jsx'
import './records.css'

const expenseCategories = ['HOUSING', 'TRANSPORTATION', 'FOOD', 'UTILITIES', 'HEALTHCARE', 'ENTERTAINMENT', 'SHOPPING', 'EDUCATION', 'TRAVEL', 'PERSONAL', 'DEBT', 'OTHER']
const paymentMethods = ['CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET', 'DIRECT_DEBIT', 'OTHER']
const incomeSources = ['SALARY', 'FREELANCE', 'BUSINESS', 'INVESTMENTS', 'GIFT', 'OTHER']

const serviceFor = (recordType) => recordType === 'expense' ? expenseService : incomeService
const typeLabel = (recordType) => recordType === 'expense' ? 'Expense' : 'Income'

export default function RecordsPage({ type }) {
  const allMode = !type
  const { token, logout } = useAuth()
  const [selectedType, setSelectedType] = useState(type || 'all')
  const [newType, setNewType] = useState('expense')
  const [expenses, setExpenses] = useState([])
  const [income, setIncome] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [modal, setModal] = useState(null)
  const [saving, setSaving] = useState(false)
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const [method, setMethod] = useState('')
  const [month, setMonth] = useState('')
  const [sort, setSort] = useState('date-desc')

  const load = () => {
    setLoading(true)
    setError('')
    Promise.all([expenseService.list(token, logout), incomeService.list(token, logout)])
      .then(([nextExpenses, nextIncome]) => { setExpenses(nextExpenses); setIncome(nextIncome) })
      .catch((requestError) => setError(requestError.message || 'Unable to load records.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [token])
  useEffect(() => { if (type) setSelectedType(type) }, [type])

  const allRecords = useMemo(() => [
    ...expenses.map((record) => ({ ...record, recordType: 'expense' })),
    ...income.map((record) => ({ ...record, recordType: 'income' }))
  ], [expenses, income])
  const records = selectedType === 'expense' ? allRecords.filter((record) => record.recordType === 'expense') : selectedType === 'income' ? allRecords.filter((record) => record.recordType === 'income') : allRecords
  const filtered = useMemo(() => records.filter((record) => {
    const label = record.recordType === 'expense' ? record.category : record.source
    const searchable = `${record.description || ''} ${label || ''}`.toLowerCase()
    return (!query || searchable.includes(query.toLowerCase())) && (!category || label === category) && (!method || record.paymentMethod === method) && (!month || record.date?.startsWith(month))
  }).sort((first, second) => {
    if (sort === 'amount-desc') return Number(second.amount) - Number(first.amount)
    if (sort === 'amount-asc') return Number(first.amount) - Number(second.amount)
    if (sort === 'date-asc') return (first.date || '').localeCompare(second.date || '')
    return (second.date || '').localeCompare(first.date || '')
  }), [records, query, category, method, month, sort])

  const totalExpenses = expenses.reduce((sum, record) => sum + Number(record.amount || 0), 0)
  const totalIncome = income.reduce((sum, record) => sum + Number(record.amount || 0), 0)
  const heading = allMode ? 'Records' : typeLabel(type) + 's'
  const choices = selectedType === 'expense' ? expenseCategories : selectedType === 'income' ? incomeSources : []
  const openAdd = () => setModal({ type: allMode && selectedType === 'all' ? newType : selectedType })

  const save = async (payload) => {
    const recordType = modal.type
    const service = serviceFor(recordType)
    const requestPayload = { ...payload }
    delete requestPayload.recordType
    setSaving(true)
    try {
      const saved = modal.record ? await service.update(modal.record.id, requestPayload, token, logout) : await service.create(requestPayload, token, logout)
      if (recordType === 'expense') setExpenses((current) => modal.record ? current.map((item) => item.id === saved.id ? saved : item) : [saved, ...current])
      else setIncome((current) => modal.record ? current.map((item) => item.id === saved.id ? saved : item) : [saved, ...current])
      setModal(null)
      setNotice(`${typeLabel(recordType)} saved successfully.`)
      setTimeout(() => setNotice(''), 2800)
    } catch (saveError) {
      setModal((current) => ({ ...current, error: saveError.message || 'Unable to save this record.' }))
    } finally {
      setSaving(false)
    }
  }

  const remove = async (record) => {
    if (!window.confirm(`Delete this ${record.recordType} record?`)) return
    try {
      await serviceFor(record.recordType).remove(record.id, token, logout)
      if (record.recordType === 'expense') setExpenses((current) => current.filter((item) => item.id !== record.id))
      else setIncome((current) => current.filter((item) => item.id !== record.id))
      setNotice('Record deleted successfully.')
      setTimeout(() => setNotice(''), 2800)
    } catch (deleteError) {
      setError(deleteError.message || 'Unable to delete this record.')
    }
  }

  return <section className="records-page">
    <div className="page-heading"><div><p className="eyebrow">Money movement</p><h1>{heading}</h1></div><div className="records-heading-actions">{allMode && <select className="record-type-picker" value={newType} onChange={(event) => setNewType(event.target.value)} aria-label="New transaction type"><option value="expense">Expense</option><option value="income">Income</option></select>}<button className="primary-button add-button" onClick={openAdd}><Plus size={18} /> Add transaction</button></div></div>
    {notice && <div className="success-alert page-notice">{notice}</div>}
    <div className="record-summary-grid"><div><span>Total income</span><strong className="positive-value">{formatCurrency(totalIncome)}</strong></div><div><span>Total expenses</span><strong className="negative-value">{formatCurrency(totalExpenses)}</strong></div><div><span>Balance</span><strong>{formatCurrency(totalIncome - totalExpenses)}</strong></div></div>
    <div className="toolbar"><div className="search-box"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search descriptions or categories..." /></div>{allMode && <select value={selectedType} onChange={(event) => { setSelectedType(event.target.value); setCategory(''); setMethod('') }}><option value="all">All transactions</option><option value="income">Income</option><option value="expense">Expenses</option></select>}{selectedType !== 'all' && <select value={category} onChange={(event) => setCategory(event.target.value)}><option value="">All {selectedType === 'expense' ? 'categories' : 'sources'}</option>{choices.map((choice) => <option key={choice} value={choice}>{prettyLabel(choice)}</option>)}</select>}{selectedType === 'expense' && <select value={method} onChange={(event) => setMethod(event.target.value)}><option value="">All payment methods</option>{paymentMethods.map((item) => <option key={item} value={item}>{prettyLabel(item)}</option>)}</select>}<input type="month" value={month} onChange={(event) => setMonth(event.target.value)} /><select value={sort} onChange={(event) => setSort(event.target.value)}><option value="date-desc">Newest first</option><option value="date-asc">Oldest first</option><option value="amount-desc">Highest amount</option><option value="amount-asc">Lowest amount</option></select></div>
    {loading ? <LoadingState /> : error ? <ErrorState message={error} onRetry={load} /> : filtered.length === 0 ? <EmptyState title={allRecords.length ? 'No matching records' : 'No transactions yet'} text={allRecords.length ? 'Try adjusting your filters.' : 'Add your first income or expense to start tracking your finances.'} onAdd={openAdd} /> : <div className="table-wrap"><table><thead><tr><th>Date</th><th>Type</th><th>Category/source</th><th>Description</th><th className="amount-cell">Amount</th><th aria-label="Actions" /></tr></thead><tbody>{filtered.map((record) => { const isRecordExpense = record.recordType === 'expense'; return <tr key={`${record.recordType}-${record.id}`}><td data-label="Date">{dateLabel(record.date)}</td><td data-label="Type"><span className="record-type"><span className={`record-dot ${isRecordExpense ? 'expense-dot' : 'income-dot'}`} />{typeLabel(record.recordType)}</span></td><td data-label="Category/source">{prettyLabel(isRecordExpense ? record.category : record.source)}</td><td data-label="Description">{record.description || <span className="muted">No description</span>}</td><td data-label="Amount" className={`amount-cell ${isRecordExpense ? 'negative-value' : 'positive-value'}`}>{isRecordExpense ? '-' : '+'}{formatCurrency(record.amount)}</td><td><div className="row-actions"><button className="icon-button" onClick={() => setModal({ type: record.recordType, record })} aria-label={`Edit ${typeLabel(record.recordType).toLowerCase()}`}><Edit3 size={16} /></button><button className="icon-button danger-icon" onClick={() => remove(record)} aria-label={`Delete ${typeLabel(record.recordType).toLowerCase()}`}><Trash2 size={16} /></button></div></td></tr> })}</tbody></table></div>}
    {modal && <RecordModal type={modal.type} record={modal.record} error={modal.error} loading={saving} onClose={() => setModal(null)} onSubmit={save} />}
  </section>
}
