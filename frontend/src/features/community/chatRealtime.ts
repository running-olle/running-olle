import { mapChatRoom, type ChatRoomApiResponse } from './chatApi'
import type { ChatRoom } from './communityTypes'

type ChatRealtimeRoomEnvelope = {
  type: 'room_snapshot'
  room: ChatRoomApiResponse
}

type ChatRealtimeListEnvelope = {
  type: 'chat_list_snapshot'
  rooms: ChatRoomApiResponse[]
}

type ChatRealtimeListRoomUpdateEnvelope = {
  type: 'chat_list_room_update'
  room: ChatRoomApiResponse
}

export function connectChatRoomRealtime(roomId: string, onRoom: (room: ChatRoom) => void) {
  const token = localStorage.getItem('runningOlleAccessToken')
  if (!token) {
    return () => {}
  }

  const websocketUrl = buildWebSocketUrl(roomId, token)
  const socket = new WebSocket(websocketUrl)

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(String(event.data)) as ChatRealtimeRoomEnvelope
      if (payload.type !== 'room_snapshot' || !payload.room) {
        return
      }
      onRoom(mapChatRoom(payload.room))
    } catch {
      // Ignore malformed realtime messages.
    }
  }

  return () => {
    if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
      socket.close()
    }
  }
}

export function connectChatListRealtime(
  onRooms: (rooms: ChatRoom[]) => void,
  onRoomUpdate: (room: ChatRoom) => void,
) {
  const token = localStorage.getItem('runningOlleAccessToken')
  if (!token) {
    return () => {}
  }

  const websocketUrl = buildWebSocketBaseUrl(token)
  const socket = new WebSocket(websocketUrl)

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(String(event.data)) as ChatRealtimeListEnvelope | ChatRealtimeListRoomUpdateEnvelope
      if (payload.type === 'chat_list_snapshot' && Array.isArray(payload.rooms)) {
        onRooms(payload.rooms.map(mapChatRoom))
        return
      }
      if (payload.type === 'chat_list_room_update' && payload.room) {
        onRoomUpdate(mapChatRoom(payload.room))
      }
    } catch {
      // Ignore malformed realtime messages.
    }
  }

  return () => {
    if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
      socket.close()
    }
  }
}

function buildWebSocketUrl(roomId: string, token: string) {
  const url = new URL(resolveApiBaseUrl(), window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `/ws/community/chats/${roomId}`
  url.searchParams.set('token', token)
  return url.toString()
}

function buildWebSocketBaseUrl(token: string) {
  const url = new URL(resolveApiBaseUrl(), window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = '/ws/community/chat-list'
  url.searchParams.set('token', token)
  return url.toString()
}

function resolveApiBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
}
