import type { ButtonHTMLAttributes, ReactNode } from 'react'

type SwitchProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'onChange' | 'onClick' | 'role'> & {
  checked: boolean
  label: ReactNode
  description?: ReactNode
  onCheckedChange: (checked: boolean) => void
}

export function Switch({
  checked,
  label,
  description,
  onCheckedChange,
  className = '',
  disabled,
  type = 'button',
  ...buttonProps
}: SwitchProps) {
  return (
    <button
      type={type}
      role="switch"
      aria-checked={checked}
      className={`ui-switch-row ${className}`}
      disabled={disabled}
      onClick={() => onCheckedChange(!checked)}
      {...buttonProps}
    >
      <span className="ui-switch-row__copy">
        <strong>{label}</strong>
        {description && <small>{description}</small>}
      </span>
      <span className="ui-switch" aria-hidden="true"><span /></span>
    </button>
  )
}
