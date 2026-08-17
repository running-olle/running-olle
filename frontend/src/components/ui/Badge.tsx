import type { PropsWithChildren } from 'react'

type BadgeVariant = 'category' | 'easy' | 'medium' | 'spot' | 'neutral'

type BadgeProps = PropsWithChildren<{
  variant?: BadgeVariant
  className?: string
}>

const variantClassName: Record<BadgeVariant, string> = {
  category: 'bg-[#FF6F0F] text-[#261912]',
  easy: 'bg-[rgba(74,222,128,0.1)] text-[#15803D]',
  medium: 'bg-[rgba(251,146,60,0.1)] text-[#C2410C]',
  spot: 'bg-[#DBEAFE] text-[#2563EB]',
  neutral: 'bg-[#F7DDD3] text-[#594136]',
}

export function Badge({ children, variant = 'neutral', className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex h-7 items-center rounded-full px-3 text-[12px] font-bold leading-none ${variantClassName[variant]} ${className}`}
    >
      {children}
    </span>
  )
}
