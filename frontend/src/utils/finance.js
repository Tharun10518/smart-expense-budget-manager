export const CURRENCY = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })
export const formatCurrency = (value) => CURRENCY.format(Number(value || 0))
export const prettyLabel = (value = '') => value.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
export const sum = (items) => items.reduce((total, item) => total + Number(item.amount || 0), 0)
export const monthKey = (date) => date?.slice(0, 7)
export const dateLabel = (date) => date ? new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'