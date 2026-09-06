import { useCallback, useEffect, useState } from 'react'
import { weather as fallbackWeather } from '../../mocks/home'
import { fetchCurrentWeather } from './currentWeatherApi'
import { useCurrentLocation } from './CurrentLocationContext'
import type { Weather } from './types'

type WeatherStatus = 'loading' | 'success' | 'error'

export function useCurrentWeather() {
  const { coordinates, locationError, locationStatus, refreshLocation } = useCurrentLocation()
  const [requestKey, setRequestKey] = useState(0)
  const [weather, setWeather] = useState<Weather | null>(null)
  const [weatherStatus, setWeatherStatus] = useState<WeatherStatus>('loading')
  const [weatherError, setWeatherError] = useState<string | null>(null)
  const refreshWeather = useCallback(() => {
    if (coordinates) setRequestKey((key) => key + 1)
    else refreshLocation()
  }, [coordinates, refreshLocation])

  useEffect(() => {
    const controller = new AbortController()
    setWeather(null)
    setWeatherError(null)

    if (!coordinates) {
      setWeatherStatus(locationStatus === 'error' ? 'error' : 'loading')
      if (locationStatus === 'error') setWeatherError(locationError)
      return () => controller.abort()
    }

    setWeatherStatus('loading')
    fetchCurrentWeather(coordinates, fallbackWeather.runningNowCount, controller.signal)
      .then((currentWeather) => {
        setWeather(currentWeather)
        setWeatherStatus('success')
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setWeatherStatus('error')
        setWeatherError(error instanceof Error ? error.message : '현재 날씨를 불러오지 못했어요.')
      })

    return () => controller.abort()
  }, [coordinates, locationError, locationStatus, requestKey])

  return { weather, weatherStatus, weatherError, refreshWeather }
}
