import { Icon, Button, ProfileAvatar } from '../../components/ui'
import { FullScreenPage } from '../../components/layout/FullScreenPage'
import { useEffect, useRef, useState } from 'react'
import { connectChatRoomRealtime } from './chatRealtime'
import type { ChatRoom, Meetup } from './communityTypes'

export function ChatRoomModal({
  chatRoom,
  meetup,
  onClose,
  onSendMessage,
  onDeleteMessage,
  onRealtimeRoom,
  onShare,
}: {
  chatRoom: ChatRoom
  meetup: Meetup | undefined
  onClose: () => void
  onSendMessage: (chatRoomId: string, content: string) => void
  onDeleteMessage: (chatRoomId: string, messageId: string) => void
  onRealtimeRoom: (chatRoom: ChatRoom) => void
  onShare: (chatRoom: ChatRoom) => void
}) {
  const [message, setMessage] = useState('')
  const [messageMenuId, setMessageMenuId] = useState<string | null>(null)
  const realtimeHandlerRef = useRef(onRealtimeRoom)
  const longPressTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const longPressStartRef = useRef<{ x: number; y: number } | null>(null)

  useEffect(() => {
    realtimeHandlerRef.current = onRealtimeRoom
  }, [onRealtimeRoom])

  useEffect(() => {
    return connectChatRoomRealtime(chatRoom.id, (room) => realtimeHandlerRef.current(room))
  }, [chatRoom.id])

  useEffect(() => {
    const closeMenu = () => setMessageMenuId(null)
    const handlePointerDown = (event: PointerEvent) => {
      if (event.target instanceof Element && event.target.closest('[data-message-menu]')) return
      closeMenu()
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') closeMenu()
    }

    document.addEventListener('pointerdown', handlePointerDown, true)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown, true)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [])

  useEffect(() => {
    setMessageMenuId(null)
  }, [chatRoom.id])

  useEffect(() => () => {
    if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current)
  }, [])

  const clearLongPress = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current)
      longPressTimerRef.current = null
    }
    longPressStartRef.current = null
  }

  const startLongPress = (event: React.PointerEvent, messageId: string) => {
    if (event.pointerType === 'mouse') return
    clearLongPress()
    longPressStartRef.current = { x: event.clientX, y: event.clientY }
    longPressTimerRef.current = setTimeout(() => {
      setMessageMenuId(messageId)
      longPressTimerRef.current = null
    }, 550)
  }

  const handleLongPressMove = (event: React.PointerEvent) => {
    const start = longPressStartRef.current
    if (!start || Math.hypot(event.clientX - start.x, event.clientY - start.y) < 10) return
    clearLongPress()
  }

  const send = () => {
    if (!message.trim()) {
      return
    }
    onSendMessage(chatRoom.id, message.trim())
    setMessage('')
  }

  const handleMessageKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey || !isDesktopBrowser()) {
      return
    }

    event.preventDefault()
    send()
  }

  const contextLabel = meetup?.course
    ? `${meetup.course.name} · ${meetup.course.distanceKm}km`
    : meetup
      ? `${meetup.title} 문의`
      : chatRoom.subtitle

  const headerMeta =
    chatRoom.type === 'group'
      ? `번개 채팅 · 참여 ${chatRoom.participantCountLabel ?? '-'} · ${meetup?.scheduleLabel ?? ''}`
      : chatRoom.subtitle

  return (
    <div className="community-backdrop" onClick={onClose}>
      <FullScreenPage
        scroll={false} role="dialog" aria-modal="true" aria-label="채팅방" className="community-dialog"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="community-dialog-header">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="sm"
              type="button"
              onClick={onClose}
              className="flex h-11 w-11 items-center justify-center rounded-control bg-surface-subtle text-app-title"
              aria-label="닫기"
            >
              <Icon name="arrowLeft" />
            </Button>
            <div
              className={`flex h-9 w-9 items-center justify-center rounded-control text-surface ${
                chatRoom.type === 'group' ? 'community-chat-icon-group' : 'community-chat-icon-inquiry'
              }`}
              aria-hidden="true"
            >
              <Icon name="lightning" size={20} fill="currentColor" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-card-title font-bold text-ink">{chatRoom.title}</div>
              <div className="mt-0.5 truncate text-caption text-ink-secondary">{headerMeta}</div>
            </div>
            <Button variant="ghost" size="sm"
              type="button"
              onClick={() => onShare(chatRoom)}
              className="flex h-8 w-8 items-center justify-center rounded-sm bg-surface-subtle text-card-title text-ink-secondary"
              aria-label="공유"
            >
              <Icon name="share" />
            </Button>
          </div>
        </div>

        <div className="mx-4 mt-3 flex items-center justify-between rounded-control bg-surface-subtle px-4 py-3">
          <div className="truncate pr-3 text-caption font-semibold text-brand-500">{contextLabel}</div>
          <div className="shrink-0 text-caption text-ink-secondary">{meetup?.scheduleLabel ?? ''}</div>
        </div>

        <div className="community-dialog-body flex-1 overflow-y-auto px-4 py-3">
          {chatRoom.messages.map((item) =>
            item.system ? (
              <div key={item.id} className="mb-4 text-center">
                <span className="inline-block rounded-full bg-surface-muted px-4 py-1.5 text-caption text-ink-secondary">
                  {item.content}
                </span>
              </div>
            ) : (
              <div key={item.id} className={`mb-4 flex gap-2 ${item.mine ? 'flex-row-reverse' : ''}`}>
                <ProfileAvatar
                  name={item.senderName}
                  imageUrl={item.senderProfileImageUrl}
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-label text-surface"
                  fallbackStyle={{ backgroundImage: item.senderGradient }}
                />
                <div
                  className={`community-message flex flex-col ${
                    item.mine ? 'items-end text-right' : 'items-start'
                  }`}
                >
                  {!item.mine ? <div className="mb-1 text-caption text-ink-secondary">{item.senderName}</div> : null}
                  <div className={`community-message-line ${item.mine ? 'is-mine' : ''}`}>
                    <div
                      className="community-message-bubble-wrap"
                      data-message-menu
                      onContextMenu={item.mine ? (event) => {
                        event.preventDefault()
                        event.stopPropagation()
                        setMessageMenuId(item.id)
                      } : undefined}
                      onPointerDown={item.mine ? (event) => startLongPress(event, item.id) : undefined}
                      onPointerMove={item.mine ? handleLongPressMove : undefined}
                      onPointerUp={item.mine ? clearLongPress : undefined}
                      onPointerCancel={item.mine ? clearLongPress : undefined}
                      onClick={item.mine && messageMenuId === item.id ? (event) => event.stopPropagation() : undefined}
                    >
                      <div
                        className={`w-fit max-w-full break-words whitespace-pre-wrap px-4 py-3 text-left text-label leading-6 ${
                          item.mine
                            ? 'rounded-md bg-brand-500 text-surface'
                            : 'rounded-md bg-surface-muted text-ink'
                        }`}
                        tabIndex={item.mine ? 0 : undefined}
                        aria-haspopup={item.mine ? 'menu' : undefined}
                        aria-expanded={item.mine ? messageMenuId === item.id : undefined}
                        aria-label={item.mine ? `${item.content}. 메시지 메뉴` : undefined}
                        title={item.mine ? '우클릭하거나 길게 눌러 삭제' : undefined}
                        onKeyDown={item.mine ? (event) => {
                          if (event.key === 'ContextMenu' || (event.shiftKey && event.key === 'F10')) {
                            event.preventDefault()
                            setMessageMenuId(item.id)
                          }
                        } : undefined}
                      >
                        {item.content}
                      </div>
                      {item.mine && messageMenuId === item.id ? (
                        <div className="community-message-menu" role="menu" onClick={(event) => event.stopPropagation()}>
                          <Button
                            variant="danger"
                            size="sm"
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setMessageMenuId(null)
                              onDeleteMessage(chatRoom.id, item.id)
                            }}
                          >
                            삭제
                          </Button>
                        </div>
                      ) : null}
                    </div>
                    <span className="community-message-time">{item.sentAtLabel}</span>
                  </div>
                </div>
              </div>
            ),
          )}
        </div>

        <div className="community-dialog-footer">
          <div className="flex items-end gap-2">
            {chatRoom.type === 'group' ? (
              <Button variant="ghost" size="sm"
                type="button"
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-subtle text-app-title"
                aria-label="추가 기능"
              >
                +
              </Button>
            ) : null}
            <textarea aria-label="메시지를 입력하세요."
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={handleMessageKeyDown}
              placeholder="메시지를 입력하세요."
              rows={1}
              className="ui-textarea min-h-10 flex-1 resize-none rounded-lg border border-border-subtle bg-surface px-4 py-2.5 text-body-sm leading-5 outline-none"
            />
            <Button variant="primary" size="sm"
              type="button"
              onClick={send}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand-500 text-app-title text-surface"
              aria-label="전송"
            >
              <Icon name="arrowUp" />
            </Button>
          </div>
        </div>
      </FullScreenPage>
    </div>
  )
}

function isDesktopBrowser() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false
  }

  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}
