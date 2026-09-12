import type { ButtonHTMLAttributes, PropsWithChildren } from 'react'

type ChipProps = PropsWithChildren<ButtonHTMLAttributes<HTMLButtonElement> & {
  selected?: boolean
  variant?: 'filter' | 'choice'
}>

export function Chip({ children, selected = false, variant = 'filter', className = '', type = 'button', ...buttonProps }: ChipProps) {
  return (
    <button
      type={type}
      className={`ui-chip ui-chip--${variant} ${className}`}
      aria-pressed={selected}
      {...buttonProps}
    >
      {children}
    </button>
  )
}
