import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { notificationApi, type InAppNotification } from './notificationApi'

export function BellIcon({ size = 22 }: { size?: number }) {
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>
}

export function NotificationCenter() {
  const navigate = useNavigate()
  const rootRef = useRef<HTMLDivElement>(null)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState<InAppNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = (showLoading = false) => {
    if (!localStorage.getItem('runningOlleAccessToken')) return Promise.resolve()
    if (showLoading) setLoading(true)
    return notificationApi.list().then((data) => {
      setItems(data.notifications); setUnreadCount(data.unreadCount); setError('')
    }).catch(() => { if (showLoading) setError('알림을 불러오지 못했어요.') }).finally(() => setLoading(false))
  }

  useEffect(() => {
    void load()
    const interval = window.setInterval(() => void load(), 30_000)
    const handleFocus = () => void load()
    window.addEventListener('focus', handleFocus)
    return () => { window.clearInterval(interval); window.removeEventListener('focus', handleFocus) }
  }, [])

  useEffect(() => {
    if (!open) return
    const outside = (event: MouseEvent) => { if (!rootRef.current?.contains(event.target as Node)) setOpen(false) }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('mousedown', outside); document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('mousedown', outside); document.removeEventListener('keydown', escape) }
  }, [open])

  const openNotification = async (notification: InAppNotification) => {
    if (!notification.read) {
      setItems((current) => current.map((item) => item.id === notification.id ? { ...item, read: true } : item))
      setUnreadCount((count) => Math.max(0, count - 1))
      await notificationApi.markRead(notification.id).catch(() => void load())
    }
    setOpen(false)
    if (notification.actionUrl) navigate(notification.actionUrl)
  }

  const markAllRead = async () => {
    setItems((current) => current.map((item) => ({ ...item, read: true }))); setUnreadCount(0)
    await notificationApi.markAllRead().catch(() => void load())
  }

  const clear = async () => {
    if (!window.confirm('알림을 모두 비울까요?')) return
    await notificationApi.clear(); setItems([]); setUnreadCount(0)
  }

  return <div className="notification-center" ref={rootRef}>
    <button className="notification-bell" type="button" aria-label={`알림${unreadCount ? `, 읽지 않은 알림 ${unreadCount}개` : ''}`} aria-expanded={open} onClick={() => { const next = !open; setOpen(next); if (next) void load(true) }}>
      <BellIcon/>{unreadCount > 0 ? <span>{unreadCount > 99 ? '99+' : unreadCount}</span> : null}
    </button>
    {open ? <section className="notification-panel" aria-label="알림 목록">
      <header><div><h2>알림</h2>{unreadCount > 0 ? <span>새 알림 {unreadCount}개</span> : null}</div><div>{unreadCount > 0 ? <button type="button" onClick={markAllRead}>모두 읽음</button> : null}{items.length > 0 ? <button type="button" onClick={clear}>비우기</button> : null}</div></header>
      <div className="notification-list">
        {loading && items.length === 0 ? <p className="notification-state">알림을 불러오는 중이에요.</p> : null}
        {!loading && error ? <p className="notification-state error">{error}</p> : null}
        {!loading && !error && items.length === 0 ? <div className="notification-empty"><BellIcon size={28}/><strong>새로운 알림이 없어요</strong><span>좋아요, 댓글, 번개 소식을 여기에 모아드려요.</span></div> : null}
        {items.map((notification) => <button className={`notification-item ${notification.read ? '' : 'unread'}`} type="button" key={notification.id} onClick={() => void openNotification(notification)}>
          <span className={`notification-type ${notification.type.startsWith('FEED_') ? 'social' : 'meetup'}`}>{notification.type.startsWith('FEED_') ? '♥' : '⚡'}</span>
          <span className="notification-copy"><strong>{notification.title}</strong><span>{notification.message}</span><time dateTime={notification.createdAt}>{relativeTime(notification.createdAt)}</time></span>
          {!notification.read ? <i aria-label="읽지 않음"/> : null}
        </button>)}
      </div>
    </section> : null}
  </div>
}

function relativeTime(value: string) {
  const date = new Date(value); const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000))
  if (seconds < 60) return '방금 전'; const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}분 전`; const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`; const days = Math.floor(hours / 24)
  if (days < 7) return `${days}일 전`
  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}
