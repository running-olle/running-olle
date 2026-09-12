import { useEffect, useState } from 'react'
import { PopularCourseRank } from '../../features/home/PopularCourseRank'
import { RecommendedCourseList } from '../../features/home/RecommendedCourseList'
import { RunningEventList } from '../../features/home/RunningEventList'
import { WeatherCard } from '../../features/home/WeatherCard'
import { homeService } from '../../features/home/homeService'
import { toRecommendedCourseCardViewModel, type RecommendedCourseCardViewModel } from '../../features/home/homeViewModels'
import { useCurrentWeather } from '../../features/home/useCurrentWeather'
import { useTourismEventHighlights } from '../../features/home/useTourismEventHighlights'
import { popularCourses } from '../../mocks/home'

type OptionalPosition = {
  latitude: number
  longitude: number
} | null

export function HomePage() {
  const { refreshWeather, weather, weatherError, weatherStatus } = useCurrentWeather()
  const { events, status: eventStatus } = useTourismEventHighlights(3)
  const [recommendedCourses, setRecommendedCourses] = useState<RecommendedCourseCardViewModel[]>([])
  const [recommendationError, setRecommendationError] = useState<string | null>(null)
  const [loadingRecommendations, setLoadingRecommendations] = useState(true)

  useEffect(() => {
    let active = true

    const load = async () => {
      setLoadingRecommendations(true)
      setRecommendationError(null)
      try {
        const position = await getOptionalCurrentPosition()
        const recommendations = await homeService.getRecommendedCourses(
          position ? { latitude: position.latitude, longitude: position.longitude } : {}
        )
        if (!active) return
        setRecommendedCourses(recommendations.map(toRecommendedCourseCardViewModel))
      } catch {
        if (!active) return
        setRecommendationError('추천 코스를 불러오지 못했어요. 잠시 후 다시 시도해주세요.')
        setRecommendedCourses([])
      } finally {
        if (active) setLoadingRecommendations(false)
      }
    }

    void load()

    return () => {
      active = false
    }
  }, [])

  return (
    <div className="flex flex-col gap-6">
      <WeatherCard weather={weather} status={weatherStatus} errorMessage={weatherError} onRetry={refreshWeather} />
      <RecommendedCourseList
        courses={recommendedCourses}
        isLoading={loadingRecommendations}
        error={recommendationError}
      />
      <PopularCourseRank courses={popularCourses} />
      <RunningEventList events={events} status={eventStatus} />
    </div>
  )
}

function getOptionalCurrentPosition(): Promise<OptionalPosition> {
  if (typeof window === 'undefined' || !('geolocation' in navigator)) {
    return Promise.resolve(null)
  }

  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        })
      },
      () => resolve(null),
      {
        enableHighAccuracy: false,
        timeout: 2000,
        maximumAge: 300000,
      }
    )
  })
}
