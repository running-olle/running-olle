import { useCallback, useEffect, useState } from 'react'
import { tourismEventApi } from './tourismEventApi'
import type { TourismEvent } from './types'

type TourismEventHighlightsStatus = 'idle' | 'loading' | 'success' | 'error'

export function useTourismEventHighlights(limit = 3) {
  const [events, setEvents] = useState<TourismEvent[]>([])
  const [status, setStatus] = useState<TourismEventHighlightsStatus>('idle')

  const refresh = useCallback(async () => {
    setStatus('loading')
    try {
      const data = await tourismEventApi.getHighlights(limit)
      setEvents(data)
      setStatus('success')
    } catch {
      setEvents([])
      setStatus('error')
    }
  }, [limit])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return { events, status, refresh }
}
