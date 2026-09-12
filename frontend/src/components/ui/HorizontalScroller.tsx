import type { HTMLAttributes, PropsWithChildren } from 'react'

type HorizontalScrollerProps = PropsWithChildren<HTMLAttributes<HTMLDivElement> & {
  labelledBy?: string
}>

export function HorizontalScroller({ children, labelledBy, className = '', ...scrollProps }: HorizontalScrollerProps) {
  return (
    <div
      className={`ui-horizontal-scroller ${className}`}
      role="region"
      aria-labelledby={labelledBy}
      tabIndex={0}
      {...scrollProps}
    >
      {children}
    </div>
  )
}
