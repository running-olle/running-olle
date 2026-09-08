import type { SVGProps } from 'react'

export type IconName =
  | 'arrowLeft'
  | 'arrowUp'
  | 'bell'
  | 'check'
  | 'heart'
  | 'share'
  | 'bookmark'
  | 'camera'
  | 'calendar'
  | 'chevronRight'
  | 'clock'
  | 'close'
  | 'community'
  | 'edit'
  | 'coffee'
  | 'course'
  | 'history'
  | 'chart'
  | 'food'
  | 'home'
  | 'landmark'
  | 'location'
  | 'minus'
  | 'plus'
  | 'route'
  | 'routeAdd'
  | 'run'
  | 'runners'
  | 'search'
  | 'settings'
  | 'sparkles'
  | 'star'
  | 'store'
  | 'trending'
  | 'user'
  | 'logout'
  | 'weather'
  | 'wind'

type IconProps = Omit<SVGProps<SVGSVGElement>, 'children' | 'name'> & {
  name: IconName
  size?: number
}

export function Icon({ name, size = 20, ...svgProps }: IconProps) {
  const commonProps = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
    ...svgProps,
  }

  if (name === 'bookmark') return <svg {...commonProps}><path d="M7 4.75A2.25 2.25 0 0 1 9.25 2.5h5.5A2.25 2.25 0 0 1 17 4.75v15.1a.65.65 0 0 1-1.02.53L12 17.6l-3.98 2.78A.65.65 0 0 1 7 19.85V4.75Z" /></svg>
  if (name === 'bell') return <svg {...commonProps}><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></svg>
  if (name === 'arrowLeft') return <svg {...commonProps}><path d="m11 5-7 7 7 7M4 12h16" /></svg>
  if (name === 'arrowUp') return <svg {...commonProps}><path d="m5 11 7-7 7 7M12 4v16" /></svg>
  if (name === 'check') return <svg {...commonProps}><path d="m5 12 4 4L19 6" /></svg>
  if (name === 'heart') return <svg {...commonProps}><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0l-1 1-1-1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" /></svg>
  if (name === 'share') return <svg {...commonProps}><path d="M12 15V3m-4 4 4-4 4 4M5 12v8h14v-8" /></svg>
  if (name === 'camera') return <svg {...commonProps}><path d="M5 8h3l1.5-2h5L16 8h3a2 2 0 0 1 2 2v8H3v-8a2 2 0 0 1 2-2Z" /><circle cx="12" cy="13" r="3" /></svg>
  if (name === 'calendar') return <svg {...commonProps}><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></svg>
  if (name === 'chevronRight') return <svg {...commonProps}><path d="m9 18 6-6-6-6" /></svg>
  if (name === 'clock') return <svg {...commonProps}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
  if (name === 'close') return <svg {...commonProps}><path d="m6 6 12 12M18 6 6 18" /></svg>
  if (name === 'community') return <svg {...commonProps}><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3 20v-1.5a6 6 0 0 1 12 0V20M15 14a5 5 0 0 1 6 4.9V20" /></svg>
  if (name === 'edit') return <svg {...commonProps}><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" /></svg>
  if (name === 'coffee') return <svg {...commonProps}><path d="M5 8h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5V8Zm11 2h2.3a2.7 2.7 0 1 1 0 5.4H16M7 21h10" /></svg>
  if (name === 'course') return <svg {...commonProps}><path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Z" /><path d="M9 3v15M15 6v15" /></svg>
  if (name === 'home') return <svg {...commonProps}><path d="m3 11 9-8 9 8" /><path d="M5 10v10h14V10M9 20v-6h6v6" /></svg>
  if (name === 'history') return <svg {...commonProps}><path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5M12 7v5l3 2" /></svg>
  if (name === 'chart') return <svg {...commonProps}><path d="M4 19V9M10 19V5M16 19v-7M22 19V2" /></svg>
  if (name === 'food') return <svg {...commonProps}><path d="M7 3v8M10 3v8M5 11h7M8.5 11v10M17 3v18M15 3c3 2.5 3 5.5 0 8" /></svg>
  if (name === 'landmark') return <svg {...commonProps}><path d="m3 9 9-5 9 5M5 10h14M6 10v8M10 10v8M14 10v8M18 10v8M4 18h16M3 21h18" /></svg>
  if (name === 'location') return <svg {...commonProps}><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></svg>
  if (name === 'minus') return <svg {...commonProps}><path d="M5 12h14" /></svg>
  if (name === 'plus') return <svg {...commonProps}><path d="M12 5v14M5 12h14" /></svg>
  if (name === 'route') return <svg {...commonProps}><circle cx="5" cy="18" r="2" /><circle cx="19" cy="6" r="2" /><path d="m7 17 4-5 3 2 3-6" /></svg>
  if (name === 'routeAdd') return <svg {...commonProps}><circle cx="5" cy="18" r="2" /><path d="m7 17 4-5 2 1.5" /><circle cx="17" cy="8" r="4" /><path d="M17 6v4M15 8h4" /></svg>
  if (name === 'run') return (
      <svg {...commonProps} viewBox="0 0 24 24">
        <g stroke="#ffffff" strokeWidth="0.6" strokeLinejoin="round">
          <path
              d="M18.5 6C19.8807 6 21 4.88071 21 3.5C21 2.11929 19.8807 1 18.5 1C17.1193 1 16 2.11929 16 3.5C16 4.88071 17.1193 6 18.5 6Z"
              fill="currentColor"
          />
          <path
              d="M9.49967 3.9377L7.47 5.20625C7.11268 5.42957 7 5.79894 7 6.19575C7 6.98119 7.86395 7.46003 8.53 7.04375L10.4185 5.86341C10.7689 5.64441 11.218 5.66348 11.5485 5.91141L13 7L9.29261 10.7074C9.09787 10.9021 8.91955 11.1126 8.75947 11.3367L6.94614 13.8754C6.683 14.2438 6.20519 14.3894 5.78129 14.2305L3.21008 13.2663C2.7942 13.1103 2.3257 13.2614 2.07933 13.631C1.76802 14.098 1.92419 14.7314 2.41688 15.0001L4.88909 16.3486C6.12269 17.0215 7.65806 16.7479 8.58338 15.6904L10.5 13.5L12.3001 16.0201C12.7307 16.623 12.7928 17.4144 12.4615 18.077L10.7236 21.5528C10.3912 22.2177 10.8746 23 11.618 23C12.0887 23 12.5417 22.9167 12.7764 22.4472L14.7476 18.5049C15.2149 17.5701 15.1622 16.4595 14.6083 15.5732L13 13L16 10L17.3722 10.9148C18.6066 11.7378 19.9731 11.6756 21.3162 11.2279C21.7813 11.0729 22 10.6447 22 10.1805C22 9.56252 21.4451 9.09248 20.8356 9.19407C20.1453 9.30911 19.1462 9.69488 18.6352 9.01366C16.9655 6.78731 14.9948 5.21933 12.5466 3.85922C11.5923 3.32907 10.4254 3.35913 9.49967 3.9377Z"
              fill="currentColor"
          />
        </g>
      </svg>
  );
  if (name === 'runners') return <svg {...commonProps}><circle cx="9" cy="7" r="3" /><path d="M3.5 20v-2a5.5 5.5 0 0 1 11 0v2M16 4.5a3 3 0 0 1 0 5.8M17 13a5 5 0 0 1 3.5 4.8V20" /></svg>
  if (name === 'search') return <svg {...commonProps}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
  if (name === 'settings') return <svg {...commonProps}><circle cx="12" cy="12" r="3" /><path d="M19 15a2 2 0 0 0 .4 2l-2.5 2.5a2 2 0 0 0-2-.4 2 2 0 0 0-1.2 1.8h-3.5A2 2 0 0 0 9 19a2 2 0 0 0-2 .4L4.5 17A2 2 0 0 0 5 15a2 2 0 0 0-1.8-1.2v-3.5A2 2 0 0 0 5 9a2 2 0 0 0-.4-2L7 4.5A2 2 0 0 0 9 5a2 2 0 0 0 1.2-1.8h3.5A2 2 0 0 0 15 5a2 2 0 0 0 2-.4L19.5 7A2 2 0 0 0 19 9a2 2 0 0 0 1.8 1.2v3.5A2 2 0 0 0 19 15Z" /></svg>
  if (name === 'sparkles') return <svg {...commonProps}><path d="m12 3 1.2 3.8L17 8l-3.8 1.2L12 13l-1.2-3.8L7 8l3.8-1.2L12 3ZM18.5 14l.7 2.3 2.3.7-2.3.7-.7 2.3-.7-2.3-2.3-.7 2.3-.7.7-2.3ZM5 13l.8 2.2L8 16l-2.2.8L5 19l-.8-2.2L2 16l2.2-.8L5 13Z" /></svg>
  if (name === 'star') return <svg {...commonProps}><path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-2.9-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z" /></svg>
  if (name === 'store') return <svg {...commonProps}><path d="M4 9h16l-1-5H5L4 9Zm1 0v11h14V9M8 20v-7h8v7" /></svg>
  if (name === 'trending') return <svg {...commonProps}><path d="m4 16 5-5 4 4 7-8" /><path d="M15 7h5v5" /></svg>
  if (name === 'user') return <svg {...commonProps}><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></svg>
  if (name === 'logout') return <svg {...commonProps}><path d="m10 17 5-5-5-5M15 12H3" /><path d="M15 3h6v18h-6" /></svg>
  if (name === 'weather') return <svg {...commonProps}><path d="M7 16a4 4 0 1 1 1-7.9A6 6 0 0 1 19 11a3 3 0 0 1-1 5H7Z" /><path d="M13 3v2M5.9 5.9l1.4 1.4M20.1 5.9l-1.4 1.4" /></svg>
  return <svg {...commonProps}><path d="M4 12h13" /><path d="m14 9 3 3-3 3" /><path d="M7 7c1-1 2-1 3 0M7 17c1-1 2-1 3 0" /></svg>
}
