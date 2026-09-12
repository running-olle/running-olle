import type { HTMLAttributes, ReactNode } from 'react'

type ListRowProps = HTMLAttributes<HTMLDivElement> & {
  leading?: ReactNode
  title: ReactNode
  description?: ReactNode
  trailing?: ReactNode
}

export function ListRow({ leading, title, description, trailing, className = '', ...rowProps }: ListRowProps) {
  return (
    <div className={`ui-list-row ${className}`} {...rowProps}>
      {leading && <span className="ui-list-row__leading">{leading}</span>}
      <div className="ui-list-row__content">
        <div className="ui-list-row__title">{title}</div>
        {description && <div className="ui-list-row__description">{description}</div>}
      </div>
      {trailing && <span className="ui-list-row__trailing">{trailing}</span>}
    </div>
  )
}
