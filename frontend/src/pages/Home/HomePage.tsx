import { PopularCourseRank } from '../../features/home/PopularCourseRank'
import { RecommendedCourseList } from '../../features/home/RecommendedCourseList'
import { RunningEventList } from '../../features/home/RunningEventList'
import { WeatherCard } from '../../features/home/WeatherCard'
import { useCurrentWeather } from '../../features/home/useCurrentWeather'
import { events, popularCourses, recommendedCourses } from '../../mocks/home'

export function HomePage() {
  const { refreshWeather, weather, weatherError, weatherStatus } = useCurrentWeather()

  return (
    <div className="flex flex-col gap-8 pb-2">
      <WeatherCard weather={weather} status={weatherStatus} errorMessage={weatherError} onRetry={refreshWeather} />
      <RecommendedCourseList courses={recommendedCourses} />
      <PopularCourseRank courses={popularCourses} />
      <RunningEventList events={events} />
    </div>
  )
}
