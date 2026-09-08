import { Card, EmptyState, SectionHeader } from '../../components/ui'
import type { ChatRoom } from './communityTypes'

export function ChatList({
  groupChats,
  inquiryChats,
  onOpenChat,
}: {
  groupChats: ChatRoom[]
  inquiryChats: ChatRoom[]
  onOpenChat: (chat: ChatRoom) => void
}) {
  return (
    <>
      <SectionHeader className="mt-6" title="번개 채팅" description={`참여 중인 번개 채팅방 ${groupChats.length}개`} />

      <Card className="community-chat-list mt-4" padding="none" shadow="none">
        {groupChats.length === 0 ? (
          <EmptyState compact title="참여 중인 그룹 채팅방이 없습니다." />
        ) : (
          groupChats.map((chat) => <ChatRow key={chat.id} chat={chat} onOpen={onOpenChat} />)
        )}
      </Card>

      <SectionHeader className="mt-6" title="1:1 문의" />
      <Card className="community-chat-list mt-4" padding="none" shadow="none">
        {inquiryChats.length === 0 ? (
          <EmptyState compact title="문의 채팅이 없습니다." />
        ) : (
          inquiryChats.map((chat) => <ChatRow key={chat.id} chat={chat} onOpen={onOpenChat} />)
        )}
      </Card>
    </>
  )
}

function ChatRow({ chat, onOpen }: { chat: ChatRoom; onOpen: (chat: ChatRoom) => void }) {
  return (
    <button
      type="button"
      onClick={() => onOpen(chat)}
      className="flex w-full items-center gap-3 border-b border-border-subtle px-5 py-4 text-left last:border-b-0"
    >
      <div className="relative">
        <div
          className={`flex items-center justify-center ${
            chat.type === 'group' ? 'h-12 w-12 rounded-control' : 'h-12 w-12 rounded-full'
          } text-section-title text-surface`}
          style={{ backgroundImage: chat.gradient }}
        >
          {chat.icon}
        </div>
        {chat.activeDot ? (
          <div className="absolute bottom-0.5 right-0.5 h-3 w-3 rounded-full border-2 border-white bg-success" />
        ) : null}
      </div>
      <div className="min-w-0 flex-1">
        <div className="truncate text-body font-bold text-ink">{chat.title}</div>
        <div className="mt-1 truncate text-label text-ink-secondary">{chat.lastMessage}</div>
      </div>
      <div className="text-right">
        <div className="text-caption text-ink-secondary">{chat.lastMessageAt}</div>
        {chat.unreadCount > 0 ? (
          <div className="ml-auto mt-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-500 px-1 text-caption font-bold text-surface">
            {chat.unreadCount}
          </div>
        ) : null}
      </div>
    </button>
  )
}
