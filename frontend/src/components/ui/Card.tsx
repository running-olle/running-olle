import type { PropsWithChildren } from 'react'

type CardShadow = 'card' | 'section' | 'none'
type CardPadding = 'none' | 'sm' | 'md' | 'lg'
type CardVariant = 'plain' | 'interactive' | 'media' | 'stat' | 'brand' | 'subtle' | 'accent'

type CardProps = PropsWithChildren<{
  as?: 'div' | 'article' | 'section'
  className?: string
  padding?: CardPadding
  shadow?: CardShadow
  variant?: CardVariant
}>

export function Card({ as: Component = 'div', children, className = '', padding = 'md', shadow = 'card', variant = 'plain' }: CardProps) {
  return (
    <Component
      className={`ui-card ui-card--${variant} ui-card--padding-${padding} ui-card--shadow-${shadow} ${className}`}
    >
      {children}
    </Component>
  )
}
