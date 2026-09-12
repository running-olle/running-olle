import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

type ToastProps = {
  open: boolean
  message: ReactNode
  tone?: 'neutral' | 'success' | 'error'
  actionLabel?: string
  onAction?: () => void
  onClose?: () => void
  duration?: number
  className?: string
}

export function Toast({ open, message, tone = 'neutral', actionLabel, onAction, onClose, duration = 2500, className = '' }: ToastProps) {
  useEffect(() => {
    if (!open || !onClose || duration <= 0) return
    const timer = window.setTimeout(onClose, duration)
    return () => window.clearTimeout(timer)
  }, [duration, onClose, open])

  if (!open) return null

  return createPortal(
    <div className={`ui-toast ui-toast--${tone} ${className}`} role={tone === 'error' ? 'alert' : 'status'} aria-live={tone === 'error' ? 'assertive' : 'polite'}>
      <span className="ui-toast__message">{message}</span>
      {actionLabel && onAction && <button type="button" className="ui-toast__action" onClick={onAction}>{actionLabel}</button>}
    </div>,
    document.body,
  )
}
