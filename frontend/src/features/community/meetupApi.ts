import { axiosInstance } from '../../api/axiosInstance'
import type {
  Meetup,
  MeetupParticipant,
  MeetupStatus,
  MeetupTheme,
  ParticipationStatus,
} from './communityTypes'
import { normalizeThemeCode, themeLabels, type ThemeCode } from '../themes/themeCatalog'

type MeetupApiResponse = {
  id: string
  title: string
  description: string
  createdAt: string
  organizerId: string
  organizerName: string
  organizerProfileImageUrl: string | null
  themeCode: string | null
  themeLabel: string | null
  meetupDate: string
  meetingPlace: string
  meetingLatitude: number
  meetingLongitude: number
  maxParticipants: number
  targetPace: number | null
  joinMethod: 'INSTANT' | 'APPROVAL'
  status: 'RECRUITING' | 'CLOSED' | 'COMPLETED' | 'CANCELLED'
  course: null | {
    id: string
    name: string
    distanceKm: number
    durationMinutes: number
    difficulty: 'LOW' | 'MID' | 'HIGH'
    courseType: 'RUNNING_COURSE' | 'SPOT_COURSE'
  }
  participantIds: string[]
  participants: Array<{
    id: string
    nickname: string
    profileImageUrl: string | null
    status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
    stats: {
      totalDistanceKm: number
      averagePaceSeconds: number | null
      meetupCount: number
    }
  }>
  myParticipation: null | 'PENDING' | 'ACCEPTED' | 'REJECTED'
  organizer: boolean
}

export type MeetupCreatePayload = {
  title: string
  description: string
  meetupDate: string
  maxParticipants: number
  targetPace: number | null
  meetingPlace: string
  latitude: number
  longitude: number
  joinMethod: 'INSTANT' | 'APPROVAL'
  themeCode: ThemeCode
  courseId?: string | null
}

export type MeetupUpdatePayload = MeetupCreatePayload

export async function getMeetups() {
  const { data } = await axiosInstance.get<MeetupApiResponse[]>('/community/meetups')
  return data.map(mapMeetup)
}

export async function createMeetup(payload: MeetupCreatePayload) {
  const { data } = await axiosInstance.post<MeetupApiResponse>('/community/meetups', payload)
  return mapMeetup(data)
}

export async function updateMeetup(meetupId: string, payload: MeetupUpdatePayload) {
  const { data } = await axiosInstance.put<MeetupApiResponse>(`/community/meetups/${meetupId}`, payload)
  return mapMeetup(data)
}

export async function joinMeetup(meetupId: string) {
  const { data } = await axiosInstance.post<MeetupApiResponse>(`/community/meetups/${meetupId}/join`)
  return mapMeetup(data)
}

export async function acceptMeetupParticipant(meetupId: string, participantId: string) {
  const { data } = await axiosInstance.post<MeetupApiResponse>(
    `/community/meetups/${meetupId}/participants/${participantId}/accept`,
  )
  return mapMeetup(data)
}

export async function rejectMeetupParticipant(meetupId: string, participantId: string) {
  const { data } = await axiosInstance.post<MeetupApiResponse>(
    `/community/meetups/${meetupId}/participants/${participantId}/reject`,
  )
  return mapMeetup(data)
}

export async function deleteMeetup(meetupId: string) {
  await axiosInstance.delete(`/community/meetups/${meetupId}`)
}

function mapMeetup(source: MeetupApiResponse): Meetup {
  const theme = normalizeThemeCode(source.themeCode)

  return {
    id: source.id,
    title: source.title,
    description: source.description,
    createdAtLabel: formatRelativeTime(source.createdAt),
    organizerId: source.organizerId,
    organizerName: source.organizerName,
    organizerAvatar: source.organizerName.slice(0, 1).toUpperCase(),
    organizerGradient: buildGradient(theme),
    theme,
    themeLabel: source.themeLabel ?? themeLabels[theme],
    meetupDate: source.meetupDate,
    scheduleLabel: formatSchedule(source.meetupDate),
    dateKey: source.meetupDate.slice(0, 10),
    locationLabel: source.meetingPlace,
    meetingPointLabel: `${source.meetingLatitude.toFixed(4)}, ${source.meetingLongitude.toFixed(4)}`,
    meetingLatitude: source.meetingLatitude,
    meetingLongitude: source.meetingLongitude,
    maxParticipants: source.maxParticipants,
    targetPaceLabel: formatPace(source.targetPace),
    targetPaceValue: source.targetPace,
    joinMethod: source.joinMethod === 'INSTANT' ? 'instant' : 'approval',
    status: normalizeStatus(source.status),
    course: source.course
      ? {
          id: source.course.id,
          name: source.course.name,
          distanceKm: source.course.distanceKm,
          durationMinutes: source.course.durationMinutes,
          difficultyLabel: mapDifficulty(source.course.difficulty),
          icon: source.course.courseType === 'SPOT_COURSE' ? 'S' : 'R',
        }
      : null,
    participantIds: source.participantIds,
    applicants: source.participants.map((participant) => mapParticipant(participant, theme)),
    myParticipation: normalizeParticipation(source.myParticipation),
    isOrganizer: source.organizer,
  }
}

function mapParticipant(
  participant: MeetupApiResponse['participants'][number],
  theme: MeetupTheme,
): MeetupParticipant {
  const status =
    participant.status === 'ACCEPTED' ? 'accepted' : participant.status === 'REJECTED' ? 'rejected' : 'pending'

  return {
    id: participant.id,
    nickname: participant.nickname,
    avatar: participant.nickname.slice(0, 1).toUpperCase(),
    gradient: buildGradient(theme),
    role: status === 'accepted' ? 'member' : 'applicant',
    status,
    stats: {
      totalDistanceKm: participant.stats.totalDistanceKm,
      averagePaceText: formatPaceSeconds(participant.stats.averagePaceSeconds),
      meetupCount: participant.stats.meetupCount,
    },
  }
}

function normalizeStatus(value: MeetupApiResponse['status']): MeetupStatus {
  switch (value) {
    case 'CLOSED':
      return 'closed'
    case 'COMPLETED':
      return 'completed'
    case 'CANCELLED':
      return 'cancelled'
    default:
      return 'recruiting'
  }
}

function normalizeParticipation(value: MeetupApiResponse['myParticipation']): ParticipationStatus {
  switch (value) {
    case 'PENDING':
      return 'pending'
    case 'ACCEPTED':
      return 'accepted'
    case 'REJECTED':
      return 'rejected'
    default:
      return 'none'
  }
}

function buildGradient(theme: MeetupTheme) {
  switch (theme) {
    case 'FOREST':
      return 'linear-gradient(135deg,#34C759,#86EFAC)'
    case 'OREUM':
      return 'linear-gradient(135deg,#14B8A6,#67E8F9)'
    case 'PHOTO':
      return 'linear-gradient(135deg,#3B82F6,#93C5FD)'
    case 'FOOD':
      return 'linear-gradient(135deg,#F97316,#FDBA74)'
    case 'TRADITION':
      return 'linear-gradient(135deg,#8B5E3C,#D6A77A)'
    case 'URBAN':
      return 'linear-gradient(135deg,#475569,#94A3B8)'
    default:
      return 'linear-gradient(135deg,#FF6F0F,#FF954E)'
  }
}

function mapDifficulty(value: 'LOW' | 'MID' | 'HIGH') {
  switch (value) {
    case 'LOW':
      return '쉬움'
    case 'MID':
      return '보통'
    case 'HIGH':
      return '어려움'
  }
}

function formatRelativeTime(value: string) {
  const date = new Date(value)
  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.max(1, Math.floor(diffMs / 60000))

  if (diffMinutes < 60) return `${diffMinutes}분 전`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours}시간 전`
  if (diffHours < 48) return '1일 전'
  return `${Math.floor(diffHours / 24)}일 전`
}

function formatSchedule(value: string) {
  const date = new Date(value)
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    weekday: 'short',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date)
}

function formatPace(value: number | null) {
  if (value == null || Number.isNaN(value)) {
    return '페이스 자유'
  }

  const totalSeconds = Math.round(value * 60)
  return formatPaceSeconds(totalSeconds)
}

function formatPaceSeconds(value: number | null) {
  if (value == null || Number.isNaN(value)) {
    return '페이스 자유'
  }

  const minutes = Math.floor(value / 60)
  const seconds = value % 60
  return `${minutes}'${String(seconds).padStart(2, '0')}" /km`
}
