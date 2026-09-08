import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Button } from './Button'

type FabProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  icon: ReactNode
  label: string
  extended?: boolean
}

export function Fab({ icon, label, extended = false, className = '', ...buttonProps }: FabProps) {
  return (
    <Button
      variant="fab"
      icon={icon}
      label={label}
      className={`${extended ? 'ui-button--fab-extended' : ''} ${className}`}
      {...buttonProps}
    >
      {extended ? label : null}
    </Button>
  )
}
