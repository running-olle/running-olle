import type { HTMLAttributes, PropsWithChildren } from 'react'

type FullScreenPageProps = PropsWithChildren<HTMLAttributes<HTMLElement> & {
  scroll?: boolean
}>

export function FullScreenPage({ children, scroll = true, className = '', ...mainProps }: FullScreenPageProps) {
  return (
    <main className={`full-screen-page ${scroll ? 'full-screen-page--scroll' : 'full-screen-page--locked'} ${className}`} {...mainProps}>
      {children}
    </main>
  )
}
