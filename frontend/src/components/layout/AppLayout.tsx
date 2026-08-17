import { Outlet } from 'react-router-dom'
import { BottomNavigation } from './BottomNavigation'
import { Header, type HeaderProps } from './Header'

export function AppLayout({ leftSlot, rightSlot }: HeaderProps) {
  return (
    <div className="h-dvh overflow-hidden bg-[#FFF8F6] text-[#261912]">
      <Header leftSlot={leftSlot} rightSlot={rightSlot} />
      <main className="mx-auto h-full max-w-[430px] overflow-y-auto px-5 pb-[107px] pt-20">
        <Outlet />
      </main>
      <BottomNavigation />
    </div>
  )
}
