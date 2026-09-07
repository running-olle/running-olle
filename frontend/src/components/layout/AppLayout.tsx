import { Outlet, useLocation } from 'react-router-dom'
import { BottomNavigation } from './BottomNavigation'
import { Header, type HeaderProps } from './Header'
import { PageContainer } from './PageContainer'

export function AppLayout({ leftSlot, rightSlot }: HeaderProps) {
  const { pathname } = useLocation()
  const isMyPage = pathname.startsWith('/mypage') || pathname.startsWith('/dev/mypage')
  return (
    <div className="app-viewport">
      {!isMyPage && <Header leftSlot={leftSlot} rightSlot={rightSlot} />}
      <PageContainer withHeader={!isMyPage} withBottomNavigation flush={isMyPage}>
        <Outlet />
      </PageContainer>
      <BottomNavigation />
    </div>
  )
}
