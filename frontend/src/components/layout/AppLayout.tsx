import { Outlet, useLocation } from 'react-router-dom'
import { CurrentLocationProvider } from '../../features/home/CurrentLocationContext'
import { CurrentLocationLabel } from '../../features/home/CurrentLocationLabel'
import { BottomNavigation } from './BottomNavigation'
import { Header, type HeaderProps } from './Header'
import { PageContainer } from './PageContainer'

type AppLayoutProps = Pick<HeaderProps, 'rightSlot'>

function getHeaderTitle(pathname: string) {
  if (pathname === '/courses') return '코스'
  if (pathname.startsWith('/courses/')) return '코스 상세'
  if (pathname === '/community') return '커뮤니티'
  return 'Running Olle'
}

function usesCurrentLocation(pathname: string) {
  return pathname === '/' || pathname === '/running' || pathname.startsWith('/running/')
}

export function AppLayout({ rightSlot }: AppLayoutProps) {
  const { pathname } = useLocation()
  const isMyPage = pathname.startsWith('/mypage') || pathname.startsWith('/dev/mypage')
  const layout = (leftSlot: HeaderProps['leftSlot']) => (
    <div className="app-viewport">
      {!isMyPage && <Header leftSlot={leftSlot} rightSlot={rightSlot} />}
      <PageContainer withHeader={!isMyPage} withBottomNavigation flush={isMyPage}>
        <Outlet />
      </PageContainer>
      <BottomNavigation />
    </div>
  )

  if (usesCurrentLocation(pathname)) {
    return (
      <CurrentLocationProvider>
        {layout(<CurrentLocationLabel />)}
      </CurrentLocationProvider>
    )
  }

  return layout(getHeaderTitle(pathname))
}
