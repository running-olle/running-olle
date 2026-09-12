export type Weather = {
  headline: string
  temperatureCelsius: number
  condition: string
  windSpeedMeterPerSecond: number
}

export type TourismEventStatus = 'ALL' | 'ONGOING' | 'UPCOMING'

export type TourismEventTypeFilter = 'ALL' | 'RUNNING' | 'FESTIVAL' | 'PERFORMANCE'

export type TourismEvent = {
  id: string
  contentId: string
  contentTypeId: string
  title: string
  address: string | null
  detailAddress: string | null
  venueName: string | null
  organizer: string | null
  tel: string | null
  homepage: string | null
  eventStartDate: string
  eventEndDate: string
  status: string
  dday: number
  categoryLabel: string
  runningRelated: boolean
  runningScore: number
  lat: number | null
  lng: number | null
  firstImageUrl: string | null
  thumbnailImageUrl: string | null
  overview: string | null
  providerName: string
  sourceUrl: string | null
}
