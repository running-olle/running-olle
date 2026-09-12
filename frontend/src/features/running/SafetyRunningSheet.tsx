import { useMemo, useState } from 'react'
import { getNearbySafetyPlaces } from './safetyService'
import type { SafetyNearbyPlace, SafetyPlaceType } from './safetyTypes'
import type { GeoPoint } from './types'

type SafetyRunningSheetProps = {
  position: GeoPoint | null
  courseName?: string | null
  onPlacesLoaded?: (places: SafetyNearbyPlace[]) => void
  showPlacesOnMap: boolean
  onShowPlacesOnMapChange: (show: boolean) => void
  onClose: () => void
}

const TYPE_LABELS: Record<SafetyPlaceType, string> = {
  hospital: '병원',
  pharmacy: '약국',
  convenience_store: '편의점',
  toilet: '화장실',
}

export function SafetyRunningSheet({
  position,
  courseName,
  onPlacesLoaded,
  showPlacesOnMap,
  onShowPlacesOnMapChange,
  onClose,
}: SafetyRunningSheetProps) {
  const [places, setPlaces] = useState<SafetyNearbyPlace[] | null>(null)
  const [loadingPlaces, setLoadingPlaces] = useState(false)
  const [placesError, setPlacesError] = useState<string | null>(null)

  const shareText = useMemo(() => {
    if (!position) return ''
    const mapUrl = kakaoMapUrl(position)
    return [
      '러닝올레 안심 위치 공유',
      courseName ? `코스: ${courseName}` : null,
      `현재 위치: ${mapUrl}`,
      `좌표: ${position.latitude.toFixed(6)}, ${position.longitude.toFixed(6)}`,
    ].filter(Boolean).join('\n')
  }, [courseName, position])

  const loadPlaces = async () => {
    if (!position || loadingPlaces) return
    setLoadingPlaces(true)
    setPlacesError(null)
    try {
      const nextPlaces = await getNearbySafetyPlaces(position.latitude, position.longitude)
      setPlaces(nextPlaces)
      onPlacesLoaded?.(nextPlaces)
    } catch {
      setPlacesError('주변 도움 시설을 불러오지 못했습니다.')
    } finally {
      setLoadingPlaces(false)
    }
  }

  const shareLocation = async () => {
    if (!position) return
    try {
      if (navigator.share) {
        await navigator.share({
          title: '러닝올레 안심 위치',
          text: shareText,
          url: kakaoMapUrl(position),
        })
        return
      }
      await navigator.clipboard.writeText(shareText)
    } catch {
      await navigator.clipboard.writeText(shareText)
    }
  }

  const groupedPlaces = useMemo(() => {
    const groups = new Map<SafetyPlaceType, SafetyNearbyPlace[]>()
    for (const place of places ?? []) {
      groups.set(place.type, [...(groups.get(place.type) ?? []), place])
    }
    return groups
  }, [places])

  return (
    <div className="safety-sheet-backdrop" onClick={onClose}>
      <section className="safety-sheet" role="dialog" aria-modal="true" aria-labelledby="safety-title" onClick={(event) => event.stopPropagation()}>
        <span className="sheet-handle" />
        <div className="safety-sheet-header">
          <div>
            <span>안심 러닝</span>
            <h2 id="safety-title">도움이 필요할 때</h2>
          </div>
          <button type="button" aria-label="닫기" onClick={onClose}>×</button>
        </div>

        <div className="safety-location-card">
          <strong>{position ? '현재 위치 확인됨' : '현재 위치 확인 중'}</strong>
          <p>{position ? '필요할 때 119 연결, 위치 공유, 주변 도움 시설 확인을 사용할 수 있습니다.' : 'GPS 위치를 확인한 뒤 사용할 수 있습니다.'}</p>
        </div>

        <div className="safety-actions">
          <a className="safety-call-button" href="tel:119">119 전화</a>
          <button type="button" onClick={shareLocation} disabled={!position}>내 위치 공유</button>
        </div>

        <button className="safety-load-places" type="button" onClick={loadPlaces} disabled={!position || loadingPlaces}>
          {loadingPlaces ? '주변 시설 확인 중' : places ? '주변 도움 시설 새로고침' : '주변 도움 시설 보기'}
        </button>
        {places && places.length > 0 && (
          <button
            className={`safety-map-toggle ${showPlacesOnMap ? 'is-on' : ''}`}
            type="button"
            onClick={() => onShowPlacesOnMapChange(!showPlacesOnMap)}
          >
            {showPlacesOnMap ? '지도에서 도움 시설 숨기기' : '지도에 도움 시설 표시'}
          </button>
        )}

        {placesError && <p className="safety-error">{placesError}</p>}
        {places && (
          <div className="safety-place-list">
            {places.length === 0 ? (
              <p className="safety-empty">가까운 도움 시설을 찾지 못했습니다.</p>
            ) : (
              (Object.keys(TYPE_LABELS) as SafetyPlaceType[]).map((type) => {
                const items = groupedPlaces.get(type) ?? []
                if (items.length === 0) return null
                return (
                  <section key={type}>
                    <h3>{TYPE_LABELS[type]}</h3>
                    {items.map((place) => (
                      <a key={`${place.type}-${place.name}-${place.lat}-${place.lng}`} href={place.placeUrl ?? kakaoMapUrl({ latitude: place.lat, longitude: place.lng })} target="_blank" rel="noreferrer">
                        <strong>{place.name}</strong>
                        <span>{formatDistance(place.distanceMeters)} · {place.address ?? place.categoryName ?? '위치 정보 확인'}</span>
                      </a>
                    ))}
                  </section>
                )
              })
            )}
          </div>
        )}
      </section>
    </div>
  )
}

function kakaoMapUrl(position: Pick<GeoPoint, 'latitude' | 'longitude'>) {
  return `https://map.kakao.com/link/map/현재위치,${position.latitude},${position.longitude}`
}

function formatDistance(value: number | null) {
  if (value == null || Number.isNaN(value)) return '거리 확인 중'
  if (value >= 1000) return `${(value / 1000).toFixed(1)}km`
  return `${value}m`
}
