import { Card } from '../../components/ui/Card'
import { Icon } from '../../components/ui/Icon'
import { ListRow } from '../../components/ui/ListRow'
import { MetaList } from '../../components/ui/MetaList'
import { SectionHeader } from '../../components/ui/SectionHeader'
import type { Difficulty, PopularCourse } from '../../mocks/home'
import { CourseThumbnail } from './CourseThumbnail'

type PopularCourseRankProps = {
  courses: PopularCourse[]
}

const difficultyLabel: Record<Difficulty, string> = {
  easy: '하',
  medium: '중',
}

export function PopularCourseRank({ courses }: PopularCourseRankProps) {
  return (
    <section aria-labelledby="popular-courses-title">
      <SectionHeader
        id="popular-courses-title"
        icon={<Icon name="trending" size={20} />}
        title="지금 인기 코스"
        description="제주 러너들이 최근 많이 찾고 있어요."
      />
      <Card padding="none" shadow="none" className="mt-4 overflow-hidden border border-border-subtle">
        <div>
          {courses.map((course) => (
            <ListRow
              key={course.id}
              leading={(
                <div className="flex items-center gap-3">
                  <strong className="w-5 text-center text-section-title font-extrabold tabular-nums text-brand-700">{course.rank}</strong>
                  <CourseThumbnail tone={course.imageTone} className="h-12 w-12" />
                </div>
              )}
              title={course.title}
              description={(
                <MetaList
                  ariaLabel={`${course.title} 요약`}
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
          ))}
        </div>
      </Card>
    </section>
  )
}
