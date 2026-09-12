import type { PropsWithChildren } from 'react'

export type BadgeVariant = 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'category' | 'easy' | 'medium' | 'spot' | 'neutral'

type BadgeProps = PropsWithChildren<{
  variant?: BadgeVariant
  className?: string
}>

export function Badge({ children, variant = 'neutral', className = '' }: BadgeProps) {
  return (
    <span className={`ui-badge ui-badge--${variant} ${className}`}>
      {children}
    </span>
  )
}
