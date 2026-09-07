import { AlertCircle, LoaderCircle, Plus, SearchX } from 'lucide-react'

export function LoadingState() { return <div className="data-state"><LoaderCircle className="spin" size={26} /><p>Loading your data...</p></div> }
export function ErrorState({ message, onRetry }) { return <div className="data-state error-state"><AlertCircle size={26} /><p>{message}</p><button className="secondary-button" onClick={onRetry}>Try again</button></div> }
export function EmptyState({ title, text, onAdd }) { return <div className="data-state"><SearchX size={28} /><h2>{title}</h2><p>{text}</p>{onAdd && <button className="primary-button compact-button" onClick={onAdd}><Plus size={17} /> Add one</button>}</div> }