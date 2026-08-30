import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, PointerEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { CourseBuilderMap } from '../../features/courseBuilder/CourseBuilderMap'
import { courseBuilderService } from '../../features/courseBuilder/courseBuilderService'
import { useCourseDraftStore } from '../../features/courseBuilder/courseDraftStore'
import { JEJU_CENTER, approximateWalkingMinutes, cleanDisplayText, difficultyLabel, formatDistanceKm, isInJejuBounds, kakaoSearchUrl } from '../../features/courseBuilder/courseBuilderUtils'
import { useRouteCalculation } from '../../features/courseBuilder/useRouteCalculation'
import type { CourseWaypointDraft, DraftRoute, LatLng, NearbyCategoryGroupCode, PlaceDetail, PlaceSearchResult } from '../../features/courseBuilder/types'
import { RunningIcon } from '../../features/running/RunningIcon'

type SearchStatus = 'idle' | 'loading' | 'success' | 'error'
type SheetSnap = 'peek' | 'full'

type NearbyCategory = {
  code: NearbyCategoryGroupCode
  label: string
  icon: 'camera' | 'coffee' | 'food' | 'store' | 'stay'
}

type NearbyResultsByCategory = Record<NearbyCategoryGroupCode, PlaceSearchResult[]>

const NEARBY_SEARCH_RADIUS_METERS = 1_500

const nearbyCategories: NearbyCategory[] = [
  { code: 'AT4', label: '관광지', icon: 'camera' },
  { code: 'CE7', label: '카페', icon: 'coffee' },
  { code: 'FD6', label: '맛집', icon: 'food' },
  { code: 'CS2', label: '편의점', icon: 'store' },
  { code: 'AD5', label: '숙소', icon: 'stay' },
]

function emptyNearbyResults(): NearbyResultsByCategory {
  return {
    AT4: [],
    CE7: [],
    FD6: [],
    CS2: [],
    AD5: [],
  }
}

type SheetControls = {
  snap: SheetSnap
  setSnap: (snap: SheetSnap) => void
  toggleSnap: () => void
  onPointerDown: (event: PointerEvent<HTMLElement>) => void
  onPointerUp: (event: PointerEvent<HTMLElement>) => void
}

function categoryBadgeClass(categoryGroupCode: string | null) {
  if (categoryGroupCode === 'AT4') return 'bg-[#E8F6E8] text-[#16833A]'
  if (categoryGroupCode === 'CE7') return 'bg-[#F6EEE7] text-[#8A5431]'
  if (categoryGroupCode === 'FD6') return 'bg-[#FFF0E5] text-[#E65E12]'
  if (categoryGroupCode === 'CS2') return 'bg-[#EAF3FF] text-[#2563EB]'
  if (categoryGroupCode === 'PK6') return 'bg-[#F1F5F9] text-[#475569]'
  return 'bg-[#F4F4F5] text-[#52525B]'
}

function categoryLabel(place: PlaceSearchResult | null, detail?: PlaceDetail | null) {
  if (place?.categoryGroupCode === 'AT4') return '관광지'
  if (place?.categoryGroupCode === 'CE7') return '카페'
  if (place?.categoryGroupCode === 'FD6') return '맛집'
  if (place?.categoryGroupCode === 'CS2') return '편의점'
  return detail?.categoryName || place?.categoryName || '장소'
}

function referencePoint(waypoints: ReturnType<typeof useCourseDraftStore.getState>['waypoints'], currentPosition: LatLng | null) {
  const lastWaypoint = waypoints.at(-1)
  if (lastWaypoint) return { lat: lastWaypoint.lat, lng: lastWaypoint.lng }
  return currentPosition
}

function walkingMinuteText(minutes: number | null) {
  return minutes === null ? '거리 계산 전' : `약 ${minutes}분`
}

function normalizeSearchText(value: string | null | undefined) {
  return (value ?? '').replace(/[^0-9A-Za-z가-힣]/g, '').toLowerCase()
}

function findBestSearchAnchor(keyword: string, places: PlaceSearchResult[]) {
  const normalizedKeyword = normalizeSearchText(keyword)
  if (!normalizedKeyword) return null

  const exactMatch = places.find((place) => normalizeSearchText(place.name) === normalizedKeyword)
  if (exactMatch) return exactMatch

  const tourismPrefixMatch = places.find((place) => (
    place.categoryGroupCode === 'AT4'
    && normalizeSearchText(place.name).startsWith(normalizedKeyword)
  ))
  if (tourismPrefixMatch) return tourismPrefixMatch

  const tourismContainMatch = places.find((place) => (
    place.categoryGroupCode === 'AT4'
    && normalizeSearchText(place.name).includes(normalizedKeyword)
  ))
  if (tourismContainMatch) return tourismContainMatch

  return places[0] ?? null
}

function isStrongSearchAnchor(keyword: string, place: PlaceSearchResult) {
  const normalizedKeyword = normalizeSearchText(keyword)
  const normalizedName = normalizeSearchText(place.name)
  return normalizedName === normalizedKeyword || (
    place.categoryGroupCode === 'AT4'
    && normalizedName.startsWith(normalizedKeyword)
  )
}

function useSheetControls(initialSnap: SheetSnap): SheetControls {
  const [snap, setSnap] = useState<SheetSnap>(initialSnap)
  const dragStartYRef = useRef<number | null>(null)
  const didDragRef = useRef(false)

  const toggleSnap = useCallback(() => {
    if (didDragRef.current) {
      didDragRef.current = false
      return
    }
    setSnap((currentSnap) => currentSnap === 'full' ? 'peek' : 'full')
  }, [])

  const onPointerDown = useCallback((event: PointerEvent<HTMLElement>) => {
    dragStartYRef.current = event.clientY
    event.currentTarget.setPointerCapture(event.pointerId)
  }, [])

  const onPointerUp = useCallback((event: PointerEvent<HTMLElement>) => {
    const dragStartY = dragStartYRef.current
    dragStartYRef.current = null
    if (dragStartY === null) return

    const dragDeltaY = event.clientY - dragStartY
    if (dragDeltaY < -24) {
      didDragRef.current = true
      setSnap('full')
      return
    }
    if (dragDeltaY > 24) {
      didDragRef.current = true
      setSnap('peek')
    }
  }, [])

  return { snap, setSnap, toggleSnap, onPointerDown, onPointerUp }
}

export function CourseBuilderPage() {
  const navigate = useNavigate()
  const waypoints = useCourseDraftStore((state) => state.waypoints)
  const selectedPlace = useCourseDraftStore((state) => state.selectedPlace)
  const selectedPlaceDetail = useCourseDraftStore((state) => state.selectedPlaceDetail)
  const draftRoute = useCourseDraftStore((state) => state.draftRoute)
  const routeStatus = useCourseDraftStore((state) => state.routeStatus)
  const routeError = useCourseDraftStore((state) => state.routeError)
  const setSelectedPlace = useCourseDraftStore((state) => state.setSelectedPlace)
  const setSelectedPlaceDetail = useCourseDraftStore((state) => state.setSelectedPlaceDetail)
  const addWaypoint = useCourseDraftStore((state) => state.addWaypoint)
  const removeWaypoint = useCourseDraftStore((state) => state.removeWaypoint)
  const resetDraft = useCourseDraftStore((state) => state.resetDraft)
  const [currentPosition, setCurrentPosition] = useState<LatLng | null>(null)
  const [keyword, setKeyword] = useState('')
  const [searchResults, setSearchResults] = useState<PlaceSearchResult[]>([])
  const [searchStatus, setSearchStatus] = useState<SearchStatus>('idle')
  const [searchError, setSearchError] = useState<string | null>(null)
  const [committedSearchAnchor, setCommittedSearchAnchor] = useState<PlaceSearchResult | null>(null)
  const [nearbyResults, setNearbyResults] = useState<NearbyResultsByCategory>(() => emptyNearbyResults())
  const [nearbyStatus, setNearbyStatus] = useState<SearchStatus>('idle')
  const [nearbyError, setNearbyError] = useState<string | null>(null)
  const [activeNearbyCategory, setActiveNearbyCategory] = useState<NearbyCategoryGroupCode | null>(null)
  const [detailStatus, setDetailStatus] = useState<SearchStatus>('idle')
  const [isOverviewExpanded, setIsOverviewExpanded] = useState(false)
  const searchRequestIdRef = useRef(0)
  const nearbyRequestIdRef = useRef(0)
  const detailSheetControls = useSheetControls('peek')
  const draftSheetControls = useSheetControls('peek')
  const setDetailSheetSnap = detailSheetControls.setSnap
  const setDraftSheetSnap = draftSheetControls.setSnap

  useRouteCalculation()

  useEffect(() => {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition((position) => {
      setCurrentPosition({
        lat: position.coords.latitude,
        lng: position.coords.longitude,
      })
    }, () => {
      setCurrentPosition(null)
    }, {
      enableHighAccuracy: true,
      maximumAge: 5_000,
      timeout: 12_000,
    })
  }, [])

  const jejuCurrentPosition = useMemo(() => (
    isInJejuBounds(currentPosition) ? currentPosition : null
  ), [currentPosition])

  const searchCenter = useMemo(() => {
    const lastWaypoint = waypoints.at(-1)
    if (lastWaypoint) return { lat: lastWaypoint.lat, lng: lastWaypoint.lng }
    return jejuCurrentPosition ?? JEJU_CENTER
  }, [jejuCurrentPosition, waypoints])

  const previousPoint = useMemo(() => referencePoint(waypoints, jejuCurrentPosition), [jejuCurrentPosition, waypoints])

  const resetNearbySearch = useCallback(() => {
    nearbyRequestIdRef.current += 1
    setCommittedSearchAnchor(null)
    setNearbyResults(emptyNearbyResults())
    setNearbyStatus('idle')
    setNearbyError(null)
    setActiveNearbyCategory(null)
  }, [])

  const executeSearch = useCallback(async (trimmedKeyword: string) => {
    if (!trimmedKeyword) return []

    const requestId = searchRequestIdRef.current + 1
    searchRequestIdRef.current = requestId
    setSearchStatus('loading')
    setSearchError(null)
    try {
      const places = await courseBuilderService.searchPlaces(trimmedKeyword, searchCenter.lat, searchCenter.lng, 5_000)
      if (searchRequestIdRef.current !== requestId) return []
      setSearchResults(places)
      setSearchStatus('success')
      return places
    } catch {
      if (searchRequestIdRef.current !== requestId) return []
      setSearchResults([])
      setSearchStatus('error')
      setSearchError('장소 검색에 실패했어요. 잠시 후 다시 시도해 주세요.')
      return []
    }
  }, [searchCenter.lat, searchCenter.lng])

  const loadNearbyResults = useCallback(async (anchor: PlaceSearchResult) => {
    const requestId = nearbyRequestIdRef.current + 1
    nearbyRequestIdRef.current = requestId
    setNearbyStatus('loading')
    setNearbyError(null)
    setNearbyResults(emptyNearbyResults())
    setActiveNearbyCategory(null)

    const entries = await Promise.all(nearbyCategories.map(async (category) => {
      try {
        const places = await courseBuilderService.searchNearbyPlaces(
          anchor.lat,
          anchor.lng,
          NEARBY_SEARCH_RADIUS_METERS,
          category.code,
        )
        return [category.code, places] as const
      } catch {
        return [category.code, []] as const
      }
    }))

    if (nearbyRequestIdRef.current !== requestId) return

    const nextResults = emptyNearbyResults()
    entries.forEach(([categoryCode, places]) => {
      nextResults[categoryCode] = places.filter((place) => place.kakaoPlaceId !== anchor.kakaoPlaceId)
    })
    setNearbyResults(nextResults)
    setNearbyStatus('success')
    if (entries.every(([, places]) => places.length === 0)) {
      setNearbyError('주변 장소를 찾지 못했어요.')
    }
  }, [])

  const handleSelectPlace = useCallback((
    place: PlaceSearchResult,
    options: { sheetSnap?: SheetSnap; keepNearbySearch?: boolean } = {},
  ) => {
    if (!options.keepNearbySearch) {
      resetNearbySearch()
    }
    setSelectedPlace(place)
    setSelectedPlaceDetail(null)
    setDetailStatus('loading')
    setIsOverviewExpanded(false)
    setDetailSheetSnap(options.sheetSnap ?? 'peek')
    setSearchStatus('idle')
    setSearchResults([])
    courseBuilderService.getPlaceDetail(place)
      .then((detail) => {
        setSelectedPlaceDetail(detail)
        setDetailStatus('success')
      })
      .catch(() => {
        setSelectedPlaceDetail({
          kakaoPlaceId: place.kakaoPlaceId,
          name: place.name,
          categoryName: place.categoryName,
          address: place.address,
          lat: place.lat,
          lng: place.lng,
          phone: null,
          kakaoPlaceUrl: null,
          tourApiMatched: false,
          tourContentId: null,
          tourContentTypeId: null,
          overview: null,
          firstImageUrl: null,
          useTime: null,
          tourDataRaw: null,
        })
        setDetailStatus('error')
      })
  }, [resetNearbySearch, setDetailSheetSnap, setSelectedPlace, setSelectedPlaceDetail])

  useEffect(() => {
    const trimmedKeyword = keyword.trim()
    if (trimmedKeyword.length < 2) {
      searchRequestIdRef.current += 1
      setSearchResults([])
      setSearchStatus('idle')
      setSearchError(null)
      return
    }

    const timerId = window.setTimeout(() => {
      if (committedSearchAnchor) return
      void executeSearch(trimmedKeyword)
    }, 350)
    return () => window.clearTimeout(timerId)
  }, [committedSearchAnchor, executeSearch, keyword])

  const handleKeywordChange = useCallback((value: string) => {
    setKeyword(value)
    setSelectedPlace(null)
    setSelectedPlaceDetail(null)
    resetNearbySearch()
  }, [resetNearbySearch, setSelectedPlace, setSelectedPlaceDetail])

  const handleSearch = useCallback(async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault()
    const trimmedKeyword = keyword.trim()
    if (!trimmedKeyword) return

    const places = await executeSearch(trimmedKeyword)
    const anchor = findBestSearchAnchor(trimmedKeyword, places)
    if (!anchor) return

    setCommittedSearchAnchor(anchor)
    setSearchStatus('idle')
    setSearchResults([])
    setActiveNearbyCategory(null)
    handleSelectPlace(anchor, {
      sheetSnap: isStrongSearchAnchor(trimmedKeyword, anchor) ? 'full' : 'peek',
      keepNearbySearch: true,
    })
    void loadNearbyResults(anchor)
  }, [executeSearch, handleSelectPlace, keyword, loadNearbyResults])

  const handleAddWaypoint = useCallback(() => {
    if (!selectedPlace || !selectedPlaceDetail) return
    addWaypoint(selectedPlace, selectedPlaceDetail)
    setKeyword('')
    setSearchStatus('idle')
    setSearchResults([])
    setSelectedPlace(null)
    setSelectedPlaceDetail(null)
    resetNearbySearch()
    setDraftSheetSnap('full')
  }, [addWaypoint, resetNearbySearch, selectedPlace, selectedPlaceDetail, setDraftSheetSnap, setSelectedPlace, setSelectedPlaceDetail])

  const handleClearSearch = useCallback(() => {
    searchRequestIdRef.current += 1
    setKeyword('')
    setSearchStatus('idle')
    setSearchResults([])
    setSearchError(null)
    setSelectedPlace(null)
    setSelectedPlaceDetail(null)
    resetNearbySearch()
  }, [resetNearbySearch, setSelectedPlace, setSelectedPlaceDetail])

  const handleMapPress = useCallback(() => {
    setSearchStatus('idle')
    setSearchResults([])
    setSearchError(null)
    setActiveNearbyCategory(null)
    if (selectedPlace) {
      setDetailSheetSnap('peek')
    }
  }, [selectedPlace, setDetailSheetSnap])

  const handleSelectedPlaceMarkerClick = useCallback(() => {
    setDetailSheetSnap('full')
  }, [setDetailSheetSnap])

  const handleNearbyCategorySelect = useCallback((categoryCode: NearbyCategoryGroupCode) => {
    setActiveNearbyCategory((currentCategoryCode) => currentCategoryCode === categoryCode ? null : categoryCode)
    setSearchStatus('idle')
    setSearchResults([])
    setSearchError(null)
    setDetailSheetSnap('peek')
  }, [setDetailSheetSnap])

  const handleCandidatePlaceClick = useCallback((place: PlaceSearchResult) => {
    handleSelectPlace(place, { sheetSnap: 'peek', keepNearbySearch: true })
  }, [handleSelectPlace])

  const activeNearbyPlaces = useMemo(() => (
    activeNearbyCategory ? nearbyResults[activeNearbyCategory] : []
  ), [activeNearbyCategory, nearbyResults])

  const activeNearbyCategoryLabel = useMemo(() => (
    nearbyCategories.find((category) => category.code === activeNearbyCategory)?.label ?? null
  ), [activeNearbyCategory])

  const isNearbyResultMode = Boolean(committedSearchAnchor && activeNearbyCategory)
  const displayedSearchPlaces = isNearbyResultMode ? activeNearbyPlaces : searchResults
  const displayedSearchStatus = isNearbyResultMode ? nearbyStatus : searchStatus
  const displayedSearchError = isNearbyResultMode ? nearbyError : searchError

  function handleSave() {
    if (waypoints.length < 2) {
      window.alert('코스를 저장하려면 경유지를 2개 이상 추가해 주세요.')
      return
    }
    if (!draftRoute) {
      window.alert('경로 계산이 끝난 뒤 저장할 수 있어요.')
      return
    }
    navigate('/courses/create/save')
  }

  return (
    <main className="course-builder-page">
      <CourseBuilderMap
        currentPosition={jejuCurrentPosition}
        waypoints={waypoints}
        draftRoute={draftRoute}
        selectedPlace={selectedPlace}
        candidatePlaces={activeNearbyPlaces}
        onMapPress={handleMapPress}
        onSelectedPlaceMarkerClick={handleSelectedPlaceMarkerClick}
        onCandidatePlaceClick={handleCandidatePlaceClick}
      />

      <header className="course-builder-header">
        <button type="button" aria-label="뒤로 가기" onClick={() => navigate('/running')}>
          <RunningIcon name="back" />
        </button>
        <strong>코스 만들기</strong>
        <button type="button" className="course-builder-save" onClick={handleSave}>저장</button>
      </header>

      <section className="course-builder-search-area" aria-label="장소 검색">
        <form className="course-builder-search" onSubmit={handleSearch}>
          <span aria-hidden="true">⌕</span>
          <input
            value={keyword}
            onChange={(event) => handleKeywordChange(event.target.value)}
            placeholder="관광지/맛집/숙소 검색"
            aria-label="관광지/맛집/숙소 검색"
          />
          {keyword && <button type="button" aria-label="검색어 지우기" onClick={handleClearSearch}>×</button>}
        </form>
        {committedSearchAnchor && (
          <NearbyCategoryRail
            categories={nearbyCategories}
            results={nearbyResults}
            status={nearbyStatus}
            activeCategory={activeNearbyCategory}
            anchor={committedSearchAnchor}
            onSelect={handleNearbyCategorySelect}
          />
        )}
        <SearchResultSheet
          currentPosition={previousPoint}
          places={displayedSearchPlaces}
          status={displayedSearchStatus}
          error={displayedSearchError}
          title={isNearbyResultMode && activeNearbyCategoryLabel ? `${activeNearbyCategoryLabel} 주변 장소` : undefined}
          onSelect={isNearbyResultMode ? handleCandidatePlaceClick : handleSelectPlace}
        />
      </section>

      {selectedPlace && selectedPlaceDetail ? (
        <WaypointDetailSheet
          place={selectedPlace}
          detail={selectedPlaceDetail}
          walkingMinutes={approximateWalkingMinutes(previousPoint, {
            lat: selectedPlaceDetail.lat,
            lng: selectedPlaceDetail.lng,
          })}
          isAdded={waypoints.some((waypoint) => waypoint.kakaoPlaceId === selectedPlace.kakaoPlaceId)}
          detailStatus={detailStatus}
          isOverviewExpanded={isOverviewExpanded}
          sheetControls={detailSheetControls}
          onToggleOverview={() => setIsOverviewExpanded((value) => !value)}
          onClose={() => {
            setSelectedPlace(null)
            setSelectedPlaceDetail(null)
            setDetailSheetSnap('peek')
          }}
          onAdd={handleAddWaypoint}
        />
      ) : (
        <CourseDraftBottomSheet
          waypoints={waypoints}
          draftRoute={draftRoute}
          routeStatus={routeStatus}
          routeError={routeError}
          sheetControls={draftSheetControls}
          onRemove={removeWaypoint}
          onReset={resetDraft}
        />
      )}
    </main>
  )
}

type SearchResultSheetProps = {
  currentPosition: LatLng | null
  places: PlaceSearchResult[]
  status: SearchStatus
  error: string | null
  title?: string
  onSelect: (place: PlaceSearchResult) => void
}

type NearbyCategoryRailProps = {
  categories: NearbyCategory[]
  results: NearbyResultsByCategory
  status: SearchStatus
  activeCategory: NearbyCategoryGroupCode | null
  anchor: PlaceSearchResult
  onSelect: (categoryCode: NearbyCategoryGroupCode) => void
}

function NearbyCategoryRail({
  categories,
  results,
  status,
  activeCategory,
  anchor,
  onSelect,
}: NearbyCategoryRailProps) {
  return (
    <div className="course-nearby-panel" aria-label={`${anchor.name} 주변 장소`}>
      <div className="course-nearby-copy">
        <strong>{anchor.name} 주변</strong>
        <span>러닝 중 들르기 좋은 거점을 골라보세요.</span>
      </div>
      <div className="course-nearby-categories" role="list">
        {categories.map((category) => {
          const count = results[category.code].length
          const isActive = activeCategory === category.code
          return (
            <button
              key={category.code}
              type="button"
              className="course-nearby-chip"
              data-active={isActive}
              onClick={() => onSelect(category.code)}
            >
              <CategoryIcon name={category.icon} />
              <span>{category.label}</span>
              <small>{status === 'loading' ? '...' : count}</small>
            </button>
          )
        })}
      </div>
    </div>
  )
}

function CategoryIcon({ name }: { name: NearbyCategory['icon'] }) {
  if (name === 'coffee') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 8h11v5a5 5 0 0 1-5 5H10a5 5 0 0 1-5-5V8Zm11 2h2.3a2.7 2.7 0 1 1 0 5.4H16" />
      </svg>
    )
  }
  if (name === 'food') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M7 3v8M10 3v8M5 11h7M8.5 11v10M17 3v18M15 3c3 2.5 3 5.5 0 8" />
      </svg>
    )
  }
  if (name === 'store') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 9h16l-1-5H5L4 9Zm1 0v11h14V9M8 20v-7h8v7" />
      </svg>
    )
  }
  if (name === 'stay') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 11h16v8M4 19V7M20 19v-5a3 3 0 0 0-3-3H4M8 11V8h4a2 2 0 0 1 2 2v1" />
      </svg>
    )
  }
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M7 5h10v14H7V5Zm2 4h6M9 13h6" />
    </svg>
  )
}

function SearchResultSheet({ currentPosition, places, status, error, title, onSelect }: SearchResultSheetProps) {
  if (status === 'idle') return null

  return (
    <div className="course-search-results">
      {title && status === 'success' && <p className="course-search-title">{title}</p>}
      {status === 'loading' && <p className="course-search-state">검색 중이에요…</p>}
      {status === 'error' && <p className="course-search-state">{error}</p>}
      {status === 'success' && places.length === 0 && <p className="course-search-state">검색 결과가 없어요.</p>}
      {status === 'success' && places.map((place) => {
        const minutes = approximateWalkingMinutes(currentPosition, { lat: place.lat, lng: place.lng })
        return (
          <button key={place.kakaoPlaceId} type="button" className="course-search-result-item" onClick={() => onSelect(place)}>
            <span className={`course-category-badge ${categoryBadgeClass(place.categoryGroupCode)}`}>
              {categoryLabel(place)}
            </span>
            <strong>{place.name}</strong>
            <small>{place.address || place.categoryName || '주소 정보 없음'}</small>
            <em>{walkingMinuteText(minutes)}</em>
          </button>
        )
      })}
    </div>
  )
}

type WaypointDetailSheetProps = {
  place: PlaceSearchResult
  detail: PlaceDetail
  walkingMinutes: number | null
  isAdded: boolean
  detailStatus: SearchStatus
  isOverviewExpanded: boolean
  sheetControls: SheetControls
  onToggleOverview: () => void
  onClose: () => void
  onAdd: () => void
}

function WaypointDetailSheet({
  place,
  detail,
  walkingMinutes,
  isAdded,
  detailStatus,
  isOverviewExpanded,
  sheetControls,
  onToggleOverview,
  onClose,
  onAdd,
}: WaypointDetailSheetProps) {
  const isTourism = place.categoryGroupCode === 'AT4'
  const isTourApiMatched = isTourism && detail.tourApiMatched
  const overview = cleanDisplayText(detail.overview)
  const useTime = cleanDisplayText(detail.useTime)
  const kakaoPlaceUrl = detail.kakaoPlaceUrl ?? kakaoSearchUrl(detail.name)
  const shouldClampOverview = Boolean(overview && overview.length > 96)

  return (
    <section
      className="course-place-detail-sheet"
      data-snap={sheetControls.snap}
    >
      <button
        type="button"
        className="course-sheet-handle"
        aria-label={sheetControls.snap === 'full' ? '상세 정보 접기' : '상세 정보 펼치기'}
        onClick={sheetControls.toggleSnap}
        onPointerDown={sheetControls.onPointerDown}
        onPointerUp={sheetControls.onPointerUp}
      />
      <div className="course-place-detail-header">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className={`course-category-badge ${categoryBadgeClass(place.categoryGroupCode)}`}>
              {categoryLabel(place, detail)}
            </span>
            {isTourApiMatched && <span className="course-source-badge">한국관광공사</span>}
          </div>
          <h2>{detail.name}</h2>
          <p>{detail.address || '주소 정보 없음'}</p>
        </div>
        <button type="button" aria-label="상세 닫기" onClick={onClose}>×</button>
      </div>

      <div className="course-place-meta">
        <span>{walkingMinuteText(walkingMinutes)}</span>
        {detail.phone && <span>{detail.phone}</span>}
      </div>

      <div className="course-place-detail-body">
        {isTourApiMatched && (
          <div className="course-tour-card">
            {detail.firstImageUrl && <img src={detail.firstImageUrl} alt="" />}
            {useTime && (
              <dl className="course-detail-info-list">
                <div>
                  <dt>이용 시간</dt>
                  <dd>{useTime}</dd>
                </div>
              </dl>
            )}
            {overview && (
              <div>
                <p className={isOverviewExpanded ? '' : 'line-clamp-3'}>{overview}</p>
                {shouldClampOverview && (
                  <button type="button" onClick={onToggleOverview}>
                    {isOverviewExpanded ? '접기' : '더보기'}
                  </button>
                )}
              </div>
            )}
            <div className="course-detail-actions">
              <a href={kakaoPlaceUrl} target="_blank" rel="noreferrer">카카오맵에서 보기</a>
            </div>
            <em>정보 제공: 한국관광공사</em>
          </div>
        )}

        {isTourism && !detail.tourApiMatched && (
          <div className="course-tour-fallback">
            <strong>상세 정보를 준비 중인 장소예요</strong>
            <p>한국관광공사 데이터와 아직 매칭되지 않았어요.</p>
            <a href={kakaoPlaceUrl} target="_blank" rel="noreferrer">카카오맵에서 보기</a>
          </div>
        )}

        {!isTourism && (
          <div className="course-kakao-only-card">
            <strong>{detail.categoryName || place.categoryName || '카카오 장소 정보'}</strong>
            <dl className="course-detail-info-list">
              <div>
                <dt>주소</dt>
                <dd>{detail.address || '주소 정보 없음'}</dd>
              </div>
              {detail.phone && (
                <div>
                  <dt>전화</dt>
                  <dd>{detail.phone}</dd>
                </div>
              )}
            </dl>
            <a href={kakaoPlaceUrl} target="_blank" rel="noreferrer">카카오맵에서 보기</a>
          </div>
        )}

        {detailStatus === 'error' && <p className="course-detail-notice">카카오 상세 조회가 불안정해서 기본 정보만 표시해요.</p>}
      </div>

      <button type="button" className="course-add-button" disabled={isAdded} onClick={onAdd}>
        {isAdded ? '이미 추가된 장소' : '+ 코스에 추가하기'}
      </button>
    </section>
  )
}

type CourseDraftBottomSheetProps = {
  waypoints: CourseWaypointDraft[]
  draftRoute: DraftRoute | null
  routeStatus: 'idle' | 'loading' | 'success' | 'error'
  routeError: string | null
  sheetControls: SheetControls
  onRemove: (orderIndex: number) => void
  onReset: () => void
}

function CourseDraftBottomSheet({
  waypoints,
  draftRoute,
  routeStatus,
  routeError,
  sheetControls,
  onRemove,
  onReset,
}: CourseDraftBottomSheetProps) {
  const distanceKm = draftRoute ? formatDistanceKm(draftRoute.distanceKm) : '0'
  const estimatedMinutes = draftRoute?.estimatedDurationMinutes ?? 0
  const elevationGainM = draftRoute ? Math.round(draftRoute.elevationGainM) : 0

  return (
    <section
      className="course-draft-bottom-sheet"
      data-snap={sheetControls.snap}
    >
      <button
        type="button"
        className="course-sheet-handle"
        aria-label={sheetControls.snap === 'full' ? '코스 초안 접기' : '코스 초안 펼치기'}
        onClick={sheetControls.toggleSnap}
        onPointerDown={sheetControls.onPointerDown}
        onPointerUp={sheetControls.onPointerUp}
      />
      <div className="course-draft-stats">
        <Stat label="총 거리" value={distanceKm} unit="km" />
        <Stat label="예상 시간" value={String(estimatedMinutes)} unit="분" />
        <Stat label="누적 고도" value={String(elevationGainM)} unit="m" />
      </div>
      {draftRoute?.surface && (
        <div className="course-surface-row">
          <span>포장 {Math.round(draftRoute.surface.asphaltPct)}%</span>
          <span>흙길 {Math.round(draftRoute.surface.dirtPct)}%</span>
          <span>계단 {Math.round(draftRoute.surface.stairsPct)}%</span>
        </div>
      )}
      {routeStatus === 'loading' && <p className="course-route-status">경로를 다시 계산하고 있어요…</p>}
      {routeError && <p className="course-route-status is-error">{routeError}</p>}
      {draftRoute && <p className="course-route-status">난이도 {difficultyLabel(draftRoute.suggestedDifficulty)}</p>}
      <div className="course-waypoint-list">
        {waypoints.length === 0 && (
          <p className="course-empty-waypoints">검색해서 러닝 코스에 넣을 장소를 추가해 주세요.</p>
        )}
        {waypoints.map((waypoint, index) => (
          <div key={`${waypoint.kakaoPlaceId}-${waypoint.orderIndex}`} className="course-waypoint-item">
            <span>{index + 1}</span>
            <div>
              <strong>{waypoint.name}</strong>
              {waypoint.categoryName && <small>{waypoint.categoryName}</small>}
            </div>
            <button type="button" aria-label={`${waypoint.name} 삭제`} onClick={() => onRemove(waypoint.orderIndex)}>×</button>
          </div>
        ))}
      </div>
      {waypoints.length > 0 && (
        <button type="button" className="course-reset-button" onClick={onReset}>경유지 모두 지우기</button>
      )}
    </section>
  )
}

function Stat({ label, value, unit }: { label: string; value: string; unit: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}<small>{unit}</small></strong>
    </div>
  )
}
