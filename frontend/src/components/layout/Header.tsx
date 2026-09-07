import type { ReactNode } from 'react'
import { NotificationCenter } from '../../features/notifications/NotificationCenter'

export type HeaderProps = {
  leftSlot?: ReactNode
  rightSlot?: ReactNode
}

export type AppHeaderProps = {
  leading?: ReactNode
  title?: ReactNode
  trailing?: ReactNode
  variant?: 'root' | 'detail' | 'fullscreen'
  className?: string
}

export function AppHeader({ leading, title, trailing, variant = 'detail', className = '' }: AppHeaderProps) {
  return (
    <header className={`app-header app-header--${variant} ${className}`}>
      <div className="app-header__inner">
        <div className="app-header__leading">{leading}</div>
        <h1 className="app-header__title">{title}</h1>
        <div className="app-header__trailing">{trailing}</div>
      </div>
    </header>
  )
}

export function Header({
  leftSlot = <span>제주 제주시 구좌읍</span>,
  rightSlot = <NotificationCenter />,
}: HeaderProps) {
  return <AppHeader variant="root" leading={leftSlot} trailing={rightSlot} />
}
