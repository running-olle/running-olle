import { axiosInstance } from '../../api/axiosInstance'
import type { ChatMessage, ChatRoom } from './communityTypes'

export type ChatRoomApiResponse = {
  id: string
  roomType: 'MEETUP_GROUP' | 'DIRECT_INQUIRY'
  meetupId: string | null
  title: string
  subtitle: string
  iconLabel: string
  unreadCount: number
  lastMessage: string
  lastMessageAt: string
  participantCount: number
  messages: Array<{
    id: string
    senderId: string
    senderName: string
    senderProfileImageUrl: string | null
    content: string
    createdAt: string
    mine: boolean
    system: boolean
  }>
}

export async function getChatRooms() {
  const { data } = await axiosInstance.get<ChatRoomApiResponse[]>('/community/chats')
  return data.map(mapChatRoom)
}

export async function getChatRoom(roomId: string) {
  const { data } = await axiosInstance.get<ChatRoomApiResponse>(`/community/chats/${roomId}`)
  return mapChatRoom(data)
}

export async function sendChatMessage(roomId: string, content: string) {
  const { data } = await axiosInstance.post<ChatRoomApiResponse>(`/community/chats/${roomId}/messages`, { content })
  return mapChatRoom(data)
}

export async function deleteChatMessage(roomId: string, messageId: string) {
  const { data } = await axiosInstance.delete<ChatRoomApiResponse>(`/community/chats/${roomId}/messages/${messageId}`)
  return mapChatRoom(data)
}

export async function createInquiryRoom(meetupId: string) {
  const { data } = await axiosInstance.post<ChatRoomApiResponse>('/community/chats/inquiry', { meetupId })
  return mapChatRoom(data)
}

export function mapChatRoom(source: ChatRoomApiResponse): ChatRoom {
  return {
    id: source.id,
    type: source.roomType === 'MEETUP_GROUP' ? 'group' : 'inquiry',
    meetupId: source.meetupId ?? '',
    title: source.title,
    subtitle: source.subtitle,
    icon: source.iconLabel,
    gradient: buildGradient(source.roomType),
    unreadCount: source.unreadCount,
    lastMessage: source.lastMessage,
    lastMessageAt: formatRelativeTime(source.lastMessageAt),
    participantCountLabel: String(source.participantCount),
    activeDot: source.roomType === 'MEETUP_GROUP',
    messages: source.messages.map(mapMessage),
  }
}

function mapMessage(source: ChatRoomApiResponse['messages'][number]): ChatMessage {
  if (source.system) {
    return {
      id: source.id,
      senderId: source.senderId,
      senderName: source.senderName,
      senderAvatar: '',
      senderProfileImageUrl: null,
      senderGradient: '',
      content: source.content,
      sentAtLabel: formatTime(source.createdAt),
      mine: false,
      system: true,
    }
  }

  return {
    id: source.id,
    senderId: source.senderId,
    senderName: source.senderName,
    senderAvatar: source.senderName.slice(0, 1).toUpperCase(),
    senderProfileImageUrl: source.senderProfileImageUrl,
    senderGradient: source.mine
      ? 'linear-gradient(135deg,#3B82F6,#93C5FD)'
      : 'linear-gradient(135deg,#FF6F0F,#FF954E)',
    content: source.content,
    sentAtLabel: formatTime(source.createdAt),
    mine: source.mine,
  }
}

function buildGradient(roomType: ChatRoomApiResponse['roomType']) {
  return roomType === 'MEETUP_GROUP'
    ? 'linear-gradient(135deg,#FF6F0F,#FF954E)'
    : 'linear-gradient(135deg,#8B5CF6,#C4B5FD)'
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatRelativeTime(value: string) {
  const date = new Date(value)
  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.max(1, Math.floor(diffMs / 60000))

  if (diffMinutes < 60) return `${diffMinutes}분 전`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffHours < 48) return '어제'

  return `${Math.floor(diffHours / 24)}일 전`
}
