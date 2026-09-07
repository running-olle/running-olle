import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Button } from './Button'

type FabProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  icon: ReactNode
  label: string
}

export function Fab({ icon, label, ...buttonProps }: FabProps) {
  return <Button variant="fab" icon={icon} label={label} {...buttonProps} />
}
