import type { ReactNode } from 'react'
import { Button } from '../ui/Button'

export type HeaderProps = {
  leftSlot?: ReactNode
  rightSlot?: ReactNode
}

export function Header({
  leftSlot = <span>📍 제주시 구좌읍</span>,
  rightSlot = <Button icon={<span className="text-[20px] leading-none">🔔</span>} label="알림" />,
}: HeaderProps) {
  return (
    <header className="fixed left-0 right-0 top-0 z-30 h-14 border-b border-[#E1BFB1] bg-[#FFF8F6]">
      <div className="mx-auto flex h-full max-w-[430px] items-center justify-between px-5">
        <div className="text-[15px] font-bold leading-none text-[#261912]">{leftSlot}</div>
        <div className="flex items-center justify-end">{rightSlot}</div>
      </div>
    </header>
  )
}
