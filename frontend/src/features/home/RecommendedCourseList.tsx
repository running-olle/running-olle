import { Link } from 'react-router-dom'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { HorizontalScroller } from '../../components/ui/HorizontalScroller'
import { Icon } from '../../components/ui/Icon'
import { MetaList } from '../../components/ui/MetaList'
import { SectionHeader } from '../../components/ui/SectionHeader'
import { Skeleton } from '../../components/ui/Feedback'
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
    <section aria-labelledby="recommended-courses-title">
      <SectionHeader
        id="recommended-courses-title"
        icon={<Icon name="sparkles" size={20} />}
        title="오늘 맞춤 추천"
        description="지금 가볍게 달리기 좋은 제주 코스예요."
      />
      {error ? (
        <div className="mt-4 rounded-control bg-danger-subtle px-4 py-3 text-caption leading-5 text-danger" role="alert">
          {error}
        </div>
      ) : null}
      <HorizontalScroller labelledBy="recommended-courses-title">
        {isLoading
          ? Array.from({ length: 3 }, (_, index) => (
              <Card key={`recommended-loading-${index}`} variant="media" padding="none" shadow="none" className="w-64 shrink-0 border border-border-subtle">
                <Skeleton height={128} className="block rounded-none" />
                <div className="space-y-3 p-4">
                  <Skeleton width={72} />
                  <Skeleton width="80%" height={20} />
                  <Skeleton width="100%" />
                  <Skeleton width="70%" />
                </div>
              </Card>
            ))
          : null}

        {!isLoading && courses.map((course) => (
          <Link key={course.id} to={`/courses/${course.id}`} className="block w-64 shrink-0">
            <Card variant="media" padding="none" shadow="none" className="h-full overflow-hidden border border-border-subtle">
              <div className="relative">
                <CourseThumbnail tone={course.imageTone} rounded={false} className="h-32" />
                <div className="absolute right-3 top-3">
                  <Badge variant="brand">{course.category}</Badge>
                </div>
              </div>
              <div className="p-4">
                <h3 className="line-clamp-2 text-card-title font-bold text-ink">{course.title}</h3>
                <MetaList
                  className="mt-3"
                  ariaLabel={`${course.title} 코스 정보`}
                  items={[
                    { icon: <Icon name="route" size={16} />, label: `${course.distanceKm.toFixed(1)}km` },
                    ...(course.distanceFromUserKm !== null
                      ? [{ icon: <Icon name="location" size={16} />, label: `${course.distanceFromUserKm.toFixed(1)}km 거리` }]
                      : []),
                  ]}
                />
                <div className="mt-4 flex items-center justify-between gap-2">
                  <Badge variant={difficultyVariant[course.difficulty]}>{difficultyLabel[course.difficulty]}</Badge>
                  {course.rating !== null ? (
                    <span className="inline-flex items-center gap-1 text-caption font-bold text-rating" aria-label={`평점 ${course.rating.toFixed(1)}`}>
                      <Icon name="star" size={16} />{course.rating.toFixed(1)}
                    </span>
                  ) : null}
                </div>
                <p className="mt-4 line-clamp-3 min-h-[3.75rem] text-caption leading-5 text-ink-secondary">
                  {course.recommendationReason}
                </p>
              </div>
            </Card>
          </Link>
        ))}

        {!isLoading && courses.length === 0 && !error ? (
          <Card shadow="none" className="w-full border border-border-subtle">
            <p className="text-caption leading-6 text-ink-secondary">추천할 만한 코스를 아직 찾지 못했어요.</p>
          </Card>
        ) : null}
      </HorizontalScroller>
    </section>
  )
}
