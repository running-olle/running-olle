import type { ThemeCode } from '../themes/themeCatalog'

export type CommunityTab = 'feed' | 'meetup' | 'chat'

export type MeetupFilter = 'all' | 'recruiting' | 'today' | 'thisWeek' | 'coast' | 'oreum'
export type MeetupJoinMethod = 'instant' | 'approval'
export type MeetupStatus = 'recruiting' | 'closed' | 'completed' | 'cancelled'
export type ParticipationStatus = 'none' | 'pending' | 'accepted' | 'rejected'

export type MeetupTheme = ThemeCode

export type MemberStats = {
  totalDistanceKm: number
  averagePaceText: string
  meetupCount: number
}

export type MeetupParticipant = {
  id: string
  nickname: string
  avatar: string
  gradient: string
  role: 'organizer' | 'member' | 'applicant'
  status: 'pending' | 'accepted' | 'rejected'
  stats: MemberStats
}

export type MeetupCourseSummary = {
  id: string
  name: string
  distanceKm: number
  durationMinutes: number
  difficultyLabel: string
  icon: string
}

export type Meetup = {
  id: string
  title: string
  description: string
  createdAtLabel: string
  organizerId: string
  organizerName: string
  organizerAvatar: string
  organizerGradient: string
  theme: MeetupTheme
  themeLabel: string
  meetupDate: string
  scheduleLabel: string
  dateKey: string
  locationLabel: string
  meetingPointLabel: string
  meetingLatitude: number
  meetingLongitude: number
  maxParticipants: number
  targetPaceLabel: string
  targetPaceValue: number | null
  joinMethod: MeetupJoinMethod
  status: MeetupStatus
  course: MeetupCourseSummary | null
  participantIds: string[]
  applicants: MeetupParticipant[]
  myParticipation: ParticipationStatus
  isOrganizer: boolean
}

export type ChatRoomType = 'group' | 'inquiry'

export type ChatMessage = {
  id: string
  senderId: string | null
  senderName: string
  senderAvatar: string
  senderGradient: string
  content: string
  sentAtLabel: string
  mine: boolean
  system?: boolean
}

export type ChatRoom = {
  id: string
  type: ChatRoomType
  meetupId: string
  title: string
  subtitle: string
  icon: string
  gradient: string
  unreadCount: number
  lastMessage: string
  lastMessageAt: string
  participantCountLabel?: string
  activeDot?: boolean
  messages: ChatMessage[]
}
