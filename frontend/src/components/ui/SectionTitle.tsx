import type { ReactNode } from 'react'

type SectionTitleProps = {
  icon: ReactNode
  title: string
  className?: string
}

export function SectionTitle({ icon, title, className = '' }: SectionTitleProps) {
  return (
    <h2 className={`flex items-center gap-2 text-[20px] font-black leading-tight text-[#261912] ${className}`}>
      <span aria-hidden="true">{icon}</span>
      <span>{title}</span>
    </h2>
  )
}
