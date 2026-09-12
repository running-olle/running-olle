import type { HTMLAttributes, PropsWithChildren } from 'react'

type PageContainerProps = PropsWithChildren<HTMLAttributes<HTMLElement> & {
  withHeader?: boolean
  withBottomNavigation?: boolean
  flush?: boolean
}>

export function PageContainer({
  children,
  withHeader = true,
  withBottomNavigation = true,
  flush = false,
  className = '',
  ...mainProps
}: PageContainerProps) {
  return (
    <main
      className={`page-container ${withHeader ? 'page-container--with-header' : ''} ${withBottomNavigation ? 'page-container--with-navigation' : ''} ${flush ? 'page-container--flush' : ''} ${className}`}
      {...mainProps}
    >
      {children}
    </main>
  )
}
