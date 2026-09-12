import type { ReactNode } from 'react'

export type MetaItem = {
  icon?: ReactNode
  label: ReactNode
}

type MetaListProps = {
  items: MetaItem[]
  className?: string
  ariaLabel?: string
}

export function MetaList({ items, className = '', ariaLabel }: MetaListProps) {
  return (
    <ul className={`ui-meta-list ${className}`} aria-label={ariaLabel}>
      {items.map((item, index) => (
        <li key={index} className="ui-meta-list__item">
          {item.icon && <span className="ui-meta-list__icon" aria-hidden="true">{item.icon}</span>}
          <span>{item.label}</span>
        </li>
      ))}
    </ul>
  )
}
