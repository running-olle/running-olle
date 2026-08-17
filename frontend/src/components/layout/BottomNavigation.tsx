import { NavLink, useLocation } from 'react-router-dom'

type NavigationItem = {
  label: string
  path: string
  icon: string
}

const navigationItems: NavigationItem[] = [
  { label: '홈', path: '/', icon: '⌂' },
  { label: '코스', path: '/courses', icon: '◇' },
  { label: '커뮤니티', path: '/community', icon: '♙' },
  { label: '마이', path: '/mypage', icon: '♙' },
]

function getIsActive(currentPath: string, itemPath: string) {
  return itemPath === '/' ? currentPath === '/' : currentPath.startsWith(itemPath)
}

export function BottomNavigation() {
  const { pathname } = useLocation()

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-30 h-[83px] border-t border-[#E1BFB1] bg-white shadow-[0px_10px_15px_-3px_rgba(0,0,0,0.1),0px_4px_6px_-4px_rgba(0,0,0,0.1)]">
      <div className="mx-auto grid h-full max-w-[430px] grid-cols-5 items-center px-5">
        {navigationItems.slice(0, 2).map((item) => {
          const isActive = getIsActive(pathname, item.path)

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={`flex h-full flex-col items-center justify-center gap-1 text-[12px] font-bold ${
                isActive ? 'text-[#A04100]' : 'text-[#594136]'
              }`}
              aria-label={item.label}
            >
              <span className="text-[28px] leading-none">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          )
        })}

        <button
          type="button"
          className="-mt-8 mx-auto flex h-[62px] w-16 items-center justify-center rounded-full bg-[linear-gradient(135deg,#FF6F0F_0%,#FD934C_100%)] text-[34px] leading-none text-white drop-shadow-[0px_4px_6px_rgba(0,0,0,0.2)]"
          aria-label="러닝 시작"
        >
          ♟
        </button>

        {navigationItems.slice(2).map((item) => {
          const isActive = getIsActive(pathname, item.path)

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={`flex h-full flex-col items-center justify-center gap-1 text-[12px] font-bold ${
                isActive ? 'text-[#A04100]' : 'text-[#594136]'
              }`}
              aria-label={item.label}
            >
              <span className="text-[28px] leading-none">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          )
        })}
      </div>
    </nav>
  )
}
