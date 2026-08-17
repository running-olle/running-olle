import type { ButtonHTMLAttributes, ReactNode } from 'react'

type ButtonVariant = 'icon' | 'fab' | 'primary'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: ReactNode
  label?: string
  variant?: ButtonVariant
}

const variantClassName: Record<ButtonVariant, string> = {
  icon: 'h-10 w-10 bg-[#F5F5F5]/80 text-[#594136]',
  fab: 'h-[62px] w-16 bg-[linear-gradient(135deg,#FF6F0F_0%,#FD934C_100%)] text-white drop-shadow-[0px_4px_6px_rgba(0,0,0,0.2)]',
  primary:
    'h-14 min-w-36 bg-[linear-gradient(135deg,#FF6F0F_0%,#FD934C_100%)] px-6 text-[16px] font-bold text-white shadow-[0px_4px_12px_rgba(0,0,0,0.08)]',
}

export function Button({
  icon,
  label,
  variant = 'icon',
  className = '',
  type = 'button',
  children,
  ...buttonProps
}: ButtonProps) {
  return (
    <button
      type={type}
      className={`inline-flex shrink-0 items-center justify-center gap-2 rounded-full border-0 ${variantClassName[variant]} ${className}`}
      aria-label={label}
      {...buttonProps}
    >
      {icon}
      {children}
    </button>
  )
}
