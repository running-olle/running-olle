import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { HorizontalScroller } from '../../components/ui/HorizontalScroller'
import { Icon } from '../../components/ui/Icon'
import { MetaList } from '../../components/ui/MetaList'
import { SectionHeader } from '../../components/ui/SectionHeader'
import type { Difficulty, RecommendedCourse } from '../../mocks/home'
import { CourseThumbnail } from './CourseThumbnail'

type RecommendedCourseListProps = {
  courses: RecommendedCourse[]
}

const difficultyLabel: Record<Difficulty, string> = {
  easy: '난이도 하',
  medium: '난이도 중',
}

export function RecommendedCourseList({ courses }: RecommendedCourseListProps) {
  return (
    <section aria-labelledby="recommended-courses-title">
      <SectionHeader
        id="recommended-courses-title"
        icon={<Icon name="sparkles" size={20} />}
        title="오늘 맞춤 추천"
        description="지금 가볍게 달리기 좋은 제주 코스예요."
      />
      <HorizontalScroller labelledBy="recommended-courses-title">
        {courses.map((course) => (
          <Card key={course.id} variant="media" padding="none" shadow="none" className="w-64 shrink-0 border border-border-subtle">
            <div className="relative">
              <CourseThumbnail tone={course.imageTone} rounded={false} className="h-32" />
              <div className="absolute right-3 top-3">
                <Badge variant="brand">{course.category}</Badge>
              </div>
            </div>
            <div className="p-4">
              <p className="flex items-center gap-1 truncate text-caption text-ink-tertiary"><Icon name="location" size={16} />{course.location}</p>
              <h3 className="mt-2 line-clamp-2 text-card-title font-bold text-ink">{course.title}</h3>
              <MetaList
                className="mt-3"
                ariaLabel={`${course.title} 코스 정보`}
                items={[
                  { icon: <Icon name="route" size={16} />, label: `${course.distanceKm.toFixed(1)}km` },
                  { icon: <Icon name="clock" size={16} />, label: `약 ${course.estimatedMinutes}분` },
                ]}
              />
              <div className="mt-4 flex items-center justify-between gap-2">
                <Badge variant={course.difficulty}>{difficultyLabel[course.difficulty]}</Badge>
                <span className="inline-flex items-center gap-1 text-caption font-bold text-rating" aria-label={`평점 ${course.rating.toFixed(1)}, 후기 ${course.ratingCount}개`}>
                  <Icon name="star" size={16} />{course.rating.toFixed(1)} <span className="font-normal text-ink-tertiary">({course.ratingCount})</span>
                </span>
              </div>
            </div>
          </Card>
        ))}
      </HorizontalScroller>
    </section>
  )
}
