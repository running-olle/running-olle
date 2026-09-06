import { useCurrentLocation } from './CurrentLocationContext'

export function CurrentLocationLabel() {
  const { locationError, locationLabel, locationStatus, refreshLocation } = useCurrentLocation()

  return (
    <button
      type="button"
      className="max-w-[280px] truncate text-left"
      title={locationStatus === 'error' ? `${locationError ?? locationLabel} 다시 시도하려면 눌러 주세요.` : locationLabel}
      aria-label={locationStatus === 'error' ? `${locationError ?? locationLabel} 현재 위치 다시 찾기` : `현재 위치 ${locationLabel}`}
      onClick={locationStatus === 'error' ? refreshLocation : undefined}
    >
      {locationLabel}
    </button>
  )
}
