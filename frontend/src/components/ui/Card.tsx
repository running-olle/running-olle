import type { PropsWithChildren } from 'react'

type CardShadow = 'card' | 'section' | 'none'
type CardPadding = 'none' | 'sm' | 'md' | 'lg'

type CardProps = PropsWithChildren<{
  className?: string
  padding?: CardPadding
  shadow?: CardShadow
}>

const paddingClassName: Record<CardPadding, string> = {
  none: '',
  sm: 'p-3',
  md: 'p-4',
  lg: 'p-5',
}

const shadowClassName: Record<CardShadow, string> = {
  card: 'shadow-[0px_4px_12px_rgba(0,0,0,0.05)]',
  section: 'shadow-[0px_4px_6px_rgba(0,0,0,0.05)]',
  none: '',
}

export function Card({ children, className = '', padding = 'md', shadow = 'card' }: CardProps) {
  return (
    <div
      className={`rounded-[16px] bg-white ${paddingClassName[padding]} ${shadowClassName[shadow]} ${className}`}
    >
      {children}
    </div>
  )
}
