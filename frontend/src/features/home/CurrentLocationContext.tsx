import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getLocationErrorMessage } from '../running/runningUtils'
import { reverseGeocode, type Coordinates } from './currentLocationApi'

type LoadStatus = 'loading' | 'success' | 'error'

type CurrentLocation = {
  coordinates: Coordinates | null
  locationLabel: string
  locationStatus: LoadStatus
  locationError: string | null
  refreshLocation: () => void
}

const CurrentLocationContext = createContext<CurrentLocation | null>(null)

export function CurrentLocationProvider({ children }: { children: ReactNode }) {
  const [requestKey, setRequestKey] = useState(0)
  const [coordinates, setCoordinates] = useState<Coordinates | null>(null)
  const [locationLabel, setLocationLabel] = useState('현재 위치 확인 중…')
  const [locationStatus, setLocationStatus] = useState<LoadStatus>('loading')
  const [locationError, setLocationError] = useState<string | null>(null)
  const refreshLocation = useCallback(() => setRequestKey((key) => key + 1), [])

  useEffect(() => {
    let active = true

    setCoordinates(null)
    setLocationLabel('현재 위치 확인 중…')
    setLocationStatus('loading')
    setLocationError(null)

    if (!navigator.geolocation) {
      setLocationLabel('위치 정보를 사용할 수 없어요')
      setLocationStatus('error')
      setLocationError('이 브라우저는 현재 위치 확인을 지원하지 않아요.')
      return
    }

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        if (!active) return
        const currentCoordinates = { latitude: coords.latitude, longitude: coords.longitude }
        setCoordinates(currentCoordinates)

        reverseGeocode(currentCoordinates)
          .then((address) => {
            if (!active) return
            setLocationLabel(address)
            setLocationStatus('success')
          })
          .catch((error: unknown) => {
            if (!active) return
            setLocationLabel('현재 주소를 찾지 못했어요')
            setLocationStatus('error')
            setLocationError(error instanceof Error ? error.message : '현재 주소를 찾지 못했어요.')
          })
      },
      (error) => {
        if (!active) return
        const message = getLocationErrorMessage(error)
        setLocationLabel('현재 위치를 확인할 수 없어요')
        setLocationStatus('error')
        setLocationError(message)
      },
      { enableHighAccuracy: true, maximumAge: 300_000, timeout: 15_000 },
    )

    return () => {
      active = false
    }
  }, [requestKey])

  const value = useMemo(() => ({
    coordinates,
    locationLabel,
    locationStatus,
    locationError,
    refreshLocation,
  }), [coordinates, locationError, locationLabel, locationStatus, refreshLocation])

  return <CurrentLocationContext.Provider value={value}>{children}</CurrentLocationContext.Provider>
}

export function useCurrentLocation() {
  const context = useContext(CurrentLocationContext)
  if (!context) throw new Error('useCurrentLocation must be used inside CurrentLocationProvider.')
  return context
}
