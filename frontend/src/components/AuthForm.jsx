import { WalletCards } from 'lucide-react'

export function AuthShell({ eyebrow, title, children, footer }) { return <main className="auth-shell"><div className="auth-brand"><span className="brand-mark"><WalletCards size={20} /></span> Ledgerly</div><section className="auth-card"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{children}<div className="auth-footer">{footer}</div></section><p className="auth-caption">A calmer way to understand your money.</p></main> }
export function Field({ label, error, ...props }) { return <label className="field"><span>{label}</span><input {...props} />{error && <small>{error}</small>}</label> }
export function FormAlert({ children }) { return children ? <div className="form-alert" role="alert">{children}</div> : null }