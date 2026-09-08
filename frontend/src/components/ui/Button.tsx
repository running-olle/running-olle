import type { ButtonHTMLAttributes, ReactNode } from 'react'

export type ButtonVariant = 'icon' | 'fab' | 'primary' | 'secondary' | 'tertiary' | 'ghost' | 'danger'
export type ButtonSize = 'sm' | 'md' | 'lg'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: ReactNode
  label?: string
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
  loading?: boolean
}

export function Button({
  icon,
  label,
  variant = 'icon',
  size = 'md',
  fullWidth = false,
  loading = false,
  className = '',
  type = 'button',
  children,
  disabled,
  ...buttonProps
}: ButtonProps) {
  const isIconOnly = variant === 'icon' || (variant === 'fab' && children == null)
  return (
    <button
      type={type}
      className={`ui-button ui-button--${variant} ${isIconOnly ? '' : `ui-button--${size}`} ${fullWidth ? 'ui-button--full' : ''} ${className}`}
      aria-label={label ?? (typeof children === 'string' ? children : undefined)}
      aria-busy={loading || undefined}
      disabled={disabled || loading}
      {...buttonProps}
    >
      {loading && <span className="ui-button__spinner" aria-hidden="true" />}
      <span className={`ui-button__content ${loading ? 'ui-button__content--loading' : ''}`} aria-hidden={isIconOnly || undefined}>
        {icon}
        {children}
      </span>
    </button>
  )
}
