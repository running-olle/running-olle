import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Button } from './Button'

type IconButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  icon: ReactNode
  label: string
}

export function IconButton({ icon, label, ...buttonProps }: IconButtonProps) {
  return <Button variant="icon" icon={icon} label={label} {...buttonProps} />
}
