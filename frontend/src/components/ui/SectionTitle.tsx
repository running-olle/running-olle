import type { ReactNode } from 'react'
import { SectionHeader } from './SectionHeader'

type SectionTitleProps = {
  icon: ReactNode
  title: string
  className?: string
}

export function SectionTitle({ icon, title, className = '' }: SectionTitleProps) {
  return <SectionHeader icon={icon} title={title} className={className} />
}
