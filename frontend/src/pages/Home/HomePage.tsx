import { PopularCourseRank } from '../../features/home/PopularCourseRank'
import { RecommendedCourseList } from '../../features/home/RecommendedCourseList'
import { RunningEventList } from '../../features/home/RunningEventList'
import { WeatherCard } from '../../features/home/WeatherCard'
import { events, popularCourses, recommendedCourses, weather } from '../../mocks/home'

export function HomePage() {
  return (
    <div className="flex flex-col gap-6">
      <WeatherCard weather={weather} />
      <RecommendedCourseList courses={recommendedCourses} />
      <PopularCourseRank courses={popularCourses} />
      <RunningEventList events={events} />
    </div>
  )
}
