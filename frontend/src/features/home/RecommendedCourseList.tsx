import { Link } from 'react-router-dom'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'
import { CourseThumbnail } from './CourseThumbnail'
import type { RecommendedCourseCardViewModel } from './homeViewModels'

type RecommendedCourseListProps = {
  courses: RecommendedCourseCardViewModel[]
  isLoading?: boolean
  error?: string | null
}

const difficultyLabel: Record<RecommendedCourseCardViewModel['difficulty'], string> = {
  LOW: '쉬움',
  MID: '보통',
  HIGH: '어려움',
}

const difficultyVariant: Record<RecommendedCourseCardViewModel['difficulty'], 'easy' | 'medium' | 'neutral'> = {
  LOW: 'easy',
  MID: 'medium',
  HIGH: 'neutral',
}

export function RecommendedCourseList({
  courses,
  isLoading = false,
  error = null,
}: RecommendedCourseListProps) {
  return (
    <section>
      <SectionTitle icon="✨" title="오늘의 맞춤 추천" />
      {error ? (
        <div className="mt-5 rounded-[16px] bg-[#FFF1EE] px-4 py-4 text-[13px] leading-6 text-[#B91C1C]">
          {error}
        </div>
      ) : null}
      <div className="-mx-5 mt-5 flex gap-3 overflow-x-auto px-5 pb-2">
        {isLoading
          ? Array.from({ length: 3 }, (_, index) => (
              <Card key={`recommended-loading-${index}`} padding="none" className="w-60 shrink-0 overflow-hidden">
                <div className="h-32 animate-pulse bg-[#F7DDD3]" />
                <div className="space-y-3 p-4">
                  <div className="h-4 w-20 animate-pulse rounded-full bg-[#F7DDD3]" />
                  <div className="h-5 w-40 animate-pulse rounded-full bg-[#F7DDD3]" />
                  <div className="h-4 w-full animate-pulse rounded-full bg-[#F7DDD3]" />
                  <div className="h-4 w-5/6 animate-pulse rounded-full bg-[#F7DDD3]" />
                </div>
              </Card>
            ))
          : null}

        {!isLoading &&
          courses.map((course) => (
            <Link key={course.id} to={`/courses/${course.id}`} className="block w-60 shrink-0">
              <Card padding="none" className="h-full overflow-hidden">
                <div className="relative">
                  <CourseThumbnail tone={course.imageTone} className="h-32 rounded-b-none rounded-t-[16px]" />
                  <div className="absolute right-3 top-3">
                    <Badge variant="category">{course.category}</Badge>
                  </div>
                </div>
                <div className="p-4">
                  <h3 className="line-clamp-2 text-[16px] font-bold leading-snug text-[#261912]">{course.title}</h3>
                  <div className="mt-3 flex items-center gap-2 text-[14px] text-[#594136]">
                    <span>{course.distanceKm.toFixed(1)}km</span>
                    {course.distanceFromUserKm !== null ? (
                      <>
                        <span>•</span>
                        <span>{course.distanceFromUserKm.toFixed(1)}km 거리</span>
                      </>
                    ) : null}
                    {course.rating !== null ? (
                      <span className="ml-auto font-bold text-[#BFAC00]">★ {course.rating.toFixed(1)}</span>
                    ) : null}
                  </div>
                  <Badge variant={difficultyVariant[course.difficulty]} className="mt-4">
                    {difficultyLabel[course.difficulty]}
                  </Badge>
                  <p className="mt-4 min-h-[3.75rem] line-clamp-3 text-[12px] leading-5 text-[#594136]">
                    {course.recommendationReason}
                  </p>
                </div>
              </Card>
            </Link>
          ))}

        {!isLoading && courses.length === 0 && !error ? (
          <Card className="w-full" shadow="none">
            <p className="text-[13px] leading-6 text-[#8D7164]">추천할 만한 코스를 아직 찾지 못했어요.</p>
          </Card>
        ) : null}
      </div>
    </section>
  )
}
