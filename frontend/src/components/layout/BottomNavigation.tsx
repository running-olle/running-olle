import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { Fab } from '../ui/Fab'
import { Icon, type IconName } from '../ui/Icon'

type NavigationItem = {
  label: string
  path: string
  icon: IconName
}

const navigationItems: NavigationItem[] = [
  { label: '홈', path: '/', icon: 'home' },
  { label: '코스', path: '/courses', icon: 'course' },
  { label: '커뮤니티', path: '/community', icon: 'community' },
  { label: '마이', path: '/mypage', icon: 'user' },
]

function getIsActive(currentPath: string, itemPath: string) {
  return itemPath === '/' ? currentPath === '/' : currentPath.startsWith(itemPath)
}

export function BottomNavigation() {
  const { pathname } = useLocation()
  const navigate = useNavigate()

  return (
    <nav className="bottom-navigation" aria-label="주요 메뉴">
      <div className="bottom-navigation__inner">
        {navigationItems.slice(0, 2).map((item) => {
          const isActive = getIsActive(pathname, item.path)

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className="bottom-navigation__item"
              aria-current={isActive ? 'page' : undefined}
              aria-label={item.label}
            >
              <span className="bottom-navigation__icon"><Icon name={item.icon} size={24} /></span>
              <span className="bottom-navigation__label">{item.label}</span>
            </NavLink>
          )
        })}

        <div className="bottom-navigation__fab-slot">
          <Fab
            className={`bottom-navigation__fab ${pathname.startsWith('/running') ? 'bottom-navigation__fab--active' : ''}`}
            icon={<Icon name="run" size={24} />}
            label="러닝 방식 선택"
            onClick={() => navigate('/running')}
          />
          <span className="bottom-navigation__fab-label">러닝</span>
        </div>

        {navigationItems.slice(2).map((item) => {
          const isActive = getIsActive(pathname, item.path)

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className="bottom-navigation__item"
              aria-current={isActive ? 'page' : undefined}
              aria-label={item.label}
            >
              <span className="bottom-navigation__icon"><Icon name={item.icon} size={24} /></span>
              <span className="bottom-navigation__label">{item.label}</span>
            </NavLink>
          )
        })}
      </div>
    </nav>
  )
}
