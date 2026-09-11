import { PopularCourseRank } from '../../features/home/PopularCourseRank'
import { RecommendedCourseList } from '../../features/home/RecommendedCourseList'
import { RunningEventList } from '../../features/home/RunningEventList'
import { WeatherCard } from '../../features/home/WeatherCard'
import { useCurrentWeather } from '../../features/home/useCurrentWeather'
import { useTourismEventHighlights } from '../../features/home/useTourismEventHighlights'
import { popularCourses, recommendedCourses } from '../../mocks/home'

export function HomePage() {
  const { refreshWeather, weather, weatherError, weatherStatus } = useCurrentWeather()
  const { events, status: eventStatus } = useTourismEventHighlights(3)

  return (
    <div className="flex flex-col gap-6">
      <WeatherCard weather={weather} status={weatherStatus} errorMessage={weatherError} onRetry={refreshWeather} />
      <RecommendedCourseList courses={recommendedCourses} />
      <PopularCourseRank courses={popularCourses} />
      <RunningEventList events={events} status={eventStatus} />
    </div>
  )
}
