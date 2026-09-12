import { axiosInstance } from '../../api/axiosInstance'
import type { TourismEvent, TourismEventStatus, TourismEventTypeFilter } from './types'

type GetTourismEventsParams = {
  type: TourismEventTypeFilter
  status: TourismEventStatus
  keyword?: string
}

export const tourismEventApi = {
  getHighlights(limit = 3) {
    return axiosInstance.get<TourismEvent[]>('/tourism-events/highlights', {
      params: { limit },
    }).then(({ data }) => data)
  },

  getEvent(eventId: string) {
    return axiosInstance.get<TourismEvent>(`/tourism-events/${eventId}`)
      .then(({ data }) => data)
  },

  getEvents({ type, status, keyword }: GetTourismEventsParams) {
    return axiosInstance.get<TourismEvent[]>('/tourism-events', {
      params: { type, status, keyword: keyword || undefined },
    }).then(({ data }) => data)
  },
}
