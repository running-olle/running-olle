import type { TourismEvent } from './types'

const compactDateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
  weekday: 'short',
})

const detailDateFormatter = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'short',
})

function toLocalDate(value: string) {
  return new Date(`${value}T00:00:00`)
}

export function getTourismEventDateParts(value: string) {
  const date = toLocalDate(value)
  return {
    month: `${date.getMonth() + 1}월`,
    day: String(date.getDate()).padStart(2, '0'),
  }
}

export function formatTourismEventDateRange(event: TourismEvent, includeYear = false) {
  const formatter = includeYear ? detailDateFormatter : compactDateFormatter
  const startDate = formatter.format(toLocalDate(event.eventStartDate))
  const endDate = formatter.format(toLocalDate(event.eventEndDate))

  return startDate === endDate ? startDate : `${startDate} ~ ${endDate}`
}

export function getTourismEventDdayLabel(event: TourismEvent) {
  if (event.status === '진행 중') return '진행 중'
  if (event.status === '종료') return `종료 D+${Math.abs(event.dday)}`
  if (event.dday === 0) return '오늘 시작'
  return event.dday > 0 ? `D-${event.dday}` : `D+${Math.abs(event.dday)}`
}

export function getTourismEventLocation(event: TourismEvent) {
  return event.venueName || event.address || '장소 정보 확인 중'
}

export function getTourismEventImage(event: TourismEvent) {
  return event.thumbnailImageUrl || event.firstImageUrl
}
