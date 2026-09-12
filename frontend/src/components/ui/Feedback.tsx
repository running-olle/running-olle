import type { CSSProperties, ReactNode } from 'react'
import { Button } from './Button'

type SkeletonProps = {
  width?: CSSProperties['width']
  height?: CSSProperties['height']
  className?: string
  rounded?: boolean
}

export function Skeleton({ width = '100%', height = 16, className = '', rounded = false }: SkeletonProps) {
  return <span className={`ui-skeleton ${rounded ? 'rounded-full' : ''} ${className}`} style={{ width, height }} aria-hidden="true" />
}

export function Spinner({ size = 'section', label = '불러오는 중' }: { size?: 'inline' | 'section' | 'page'; label?: string }) {
  return <span className={`ui-spinner ui-spinner--${size}`} role="status" aria-label={label} />
}

type EmptyStateProps = {
  title: ReactNode
  description?: ReactNode
  icon?: ReactNode
  action?: ReactNode
  compact?: boolean
  creation?: boolean
  className?: string
}

export function EmptyState({ title, description, icon, action, compact = false, creation = false, className = '' }: EmptyStateProps) {
  return (
    <section className={`ui-state ${compact ? 'ui-state--compact' : ''} ${creation ? 'ui-state--creation' : ''} ${className}`}>
      {icon && <span className="ui-state__icon" aria-hidden="true">{icon}</span>}
      <h2 className="ui-state__title">{title}</h2>
      {description && <p className="ui-state__description">{description}</p>}
      {action && <div className="ui-state__action">{action}</div>}
    </section>
  )
}

type ErrorStateProps = Omit<EmptyStateProps, 'action'> & {
  retryLabel?: string
  onRetry?: () => void
  action?: ReactNode
}

export function ErrorState({ retryLabel = '다시 시도', onRetry, action, className = '', ...stateProps }: ErrorStateProps) {
  const resolvedAction = action ?? (onRetry ? <Button variant="secondary" size="sm" onClick={onRetry}>{retryLabel}</Button> : undefined)
  return <EmptyState {...stateProps} action={resolvedAction} className={`ui-state--error ${className}`} />
}
