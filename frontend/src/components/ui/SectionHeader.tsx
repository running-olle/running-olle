import type { ReactNode } from 'react'

type SectionHeaderProps = {
  title: ReactNode
  icon?: ReactNode
  description?: ReactNode
  action?: ReactNode
  className?: string
  id?: string
}

export function SectionHeader({ title, icon, description, action, className = '', id }: SectionHeaderProps) {
  return (
    <div className={`ui-section-header ${className}`}>
      <div className="ui-section-header__main">
        {icon && <span className="ui-section-header__icon" aria-hidden="true">{icon}</span>}
        <div className="ui-section-header__copy">
          <h2 className="ui-section-header__title" id={id}>{title}</h2>
          {description && <p className="ui-section-header__description">{description}</p>}
        </div>
      </div>
      {action && <div className="ui-section-header__action">{action}</div>}
    </div>
  )
}
