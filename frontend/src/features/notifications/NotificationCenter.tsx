import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, EmptyState, Icon, Spinner } from '../../components/ui'
import { notificationApi, type InAppNotification } from './notificationApi'

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
    <button className="notification-bell" data-unread={unreadCount > 0 || undefined} type="button" aria-label={`알림${unreadCount ? `, 읽지 않은 알림 ${unreadCount}개` : ''}`} aria-haspopup="dialog" aria-controls="notification-panel" aria-expanded={open} onClick={() => { const next = !open; setOpen(next); if (next) void load(true) }}>
      <Icon name="bell" size={24}/>{unreadCount > 0 ? <span className="notification-badge" aria-hidden="true">{unreadCount > 99 ? '99+' : unreadCount}</span> : null}
    </button>
    {open ? <section id="notification-panel" className="notification-panel" role="dialog" aria-modal="false" aria-labelledby="notification-panel-title">
      <header className="notification-panel-header">
        <div className="notification-panel-heading"><h2 id="notification-panel-title">알림</h2>{unreadCount > 0 ? <span>읽지 않음 {unreadCount}개</span> : <span>모든 소식을 확인했어요</span>}</div>
        <div className="notification-panel-actions">{unreadCount > 0 ? <Button variant="ghost" size="sm" onClick={markAllRead}>모두 읽음</Button> : null}{items.length > 0 ? <Button variant="ghost" size="sm" className="notification-clear" onClick={clear}>비우기</Button> : null}</div>
      </header>
      <div className="notification-list">
        {loading && items.length === 0 ? <div className="notification-state" role="status"><Spinner size="section" label="알림을 불러오는 중"/><p>알림을 불러오는 중이에요.</p></div> : null}
        {!loading && error ? <p className="notification-state error">{error}</p> : null}
        {!loading && !error && items.length === 0 ? <EmptyState compact className="notification-empty" icon={<Icon name="bell" size={28}/>} title="아직 도착한 알림이 없어요" description="좋아요, 댓글 소식을 이곳에 모아드릴게요."/> : null}
        {items.map((notification) => <NotificationItem key={notification.id} notification={notification} onOpen={openNotification}/>)}
      </div>
    </section> : null}
  </div>
}

function NotificationItem({ notification, onOpen }: { notification: InAppNotification; onOpen: (notification: InAppNotification) => Promise<void> }) {
  const isSocial = notification.type.startsWith('FEED_')
  return <button className={`notification-item ${notification.read ? '' : 'unread'}`} type="button" onClick={() => void onOpen(notification)}>
    <span className={`notification-type ${isSocial ? 'social' : 'meetup'}`} aria-hidden="true"><Icon name={isSocial ? 'heart' : 'runners'} size={20}/></span>
    <span className="notification-copy">
      <span className="notification-meta"><span>{isSocial ? '커뮤니티' : '같이 달리기'}</span><time dateTime={notification.createdAt}>{relativeTime(notification.createdAt)}</time></span>
      <strong>{notification.title}</strong>
      <span className="notification-message">{notification.message}</span>
    </span>
    {!notification.read ? <span className="notification-unread"><span className="sr-only">읽지 않음</span></span> : null}
  </button>
}

function relativeTime(value: string) {
  const date = new Date(value); const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000))
  if (seconds < 60) return '방금 전'; const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}분 전`; const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`; const days = Math.floor(hours / 24)
  if (days < 7) return `${days}일 전`
  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}
