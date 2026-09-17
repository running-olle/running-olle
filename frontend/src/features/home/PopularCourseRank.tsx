import { Link } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Feedback'
import { Icon } from '../../components/ui/Icon'
import { ListRow } from '../../components/ui/ListRow'
import { MetaList } from '../../components/ui/MetaList'
import { SectionHeader } from '../../components/ui/SectionHeader'
import type { CourseImageTone } from '../../mocks/home'
import { CourseThumbnail } from './CourseThumbnail'
import type { HomePopularCourse, HomeRecommendedCourseDifficulty } from './homeService'

type PopularCourseRankProps = {
  courses: HomePopularCourse[]
  status: 'loading' | 'success' | 'error'
}

const difficultyLabel: Record<HomeRecommendedCourseDifficulty, string> = {
  LOW: '하',
  MID: '중',
  HIGH: '상',
}

const difficultyTone: Record<HomeRecommendedCourseDifficulty, CourseImageTone> = {
  LOW: 'forest',
  MID: 'beach',
  HIGH: 'oreum',
}

export function PopularCourseRank({ courses, status }: PopularCourseRankProps) {
  return (
    <section aria-labelledby="popular-courses-title">
      <SectionHeader
        id="popular-courses-title"
        icon={<Icon name="trending" size={20} />}
        title="지금 인기 코스"
        description="최근 30일 동안 많은 러너가 달린 코스예요."
      />
      <Card padding="none" shadow="none" className="mt-4 overflow-hidden border border-border-subtle">
        <div>
          {status === 'loading'
            ? Array.from({ length: 3 }, (_, index) => (
                <ListRow
                  key={`popular-course-loading-${index}`}
                  leading={<Skeleton width={80} height={48} />}
                  title={<Skeleton width="55%" height={18} />}
                  description={<Skeleton width="35%" />}
                  trailing={<Skeleton width={44} />}
                />
              ))
            : null}

          {status === 'success' && courses.map((course) => (
            <Link
              key={course.courseId}
              to={`/courses/${course.courseId}`}
              className="popular-course-link"
              aria-label={`${course.courseName} 코스 상세 보기`}
            >
              <ListRow
                leading={(
                  <div className="flex items-center gap-3">
                    <strong className="w-5 text-center text-section-title font-extrabold tabular-nums text-brand-700">{course.rank}</strong>
                    <CourseThumbnail
                      tone={difficultyTone[course.difficulty]}
                      imageUrl={course.thumbnailImageUrl}
                      className="h-12 w-12"
                    />
                  </div>
                )}
                title={course.courseName}
                description={(
                  <MetaList
                    ariaLabel={`${course.courseName} 요약`}
                    items={[
                      { icon: <Icon name="route" size={16} />, label: `${course.distanceKm.toFixed(1)}km` },
                      { label: `난이도 ${difficultyLabel[course.difficulty]}` },
                    ]}
                  />
                )}
                trailing={(
                  <span className="inline-flex items-center gap-1 whitespace-nowrap text-label font-bold text-brand-700">
                    <Icon name="runners" size={16} />{course.participantCount}명
                  </span>
                )}
              />
            </Link>
          ))}

          {status === 'success' && courses.length === 0 ? (
            <p className="px-4 py-8 text-center text-caption leading-6 text-ink-secondary">
              최근 30일 동안 달린 코스가 아직 없어요.
            </p>
          ) : null}

          {status === 'error' ? (
            <p className="px-4 py-8 text-center text-caption leading-6 text-danger" role="alert">
              인기 코스를 불러오지 못했어요. 잠시 후 다시 시도해주세요.
            </p>
          ) : null}
        </div>
      </Card>
    </section>
  )
}
