import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'
import type { Difficulty, PopularCourse } from '../../mocks/home'
import { CourseThumbnail } from './CourseThumbnail'

type PopularCourseRankProps = {
  courses: PopularCourse[]
}

const difficultyLabel: Record<Difficulty, string> = {
  easy: '하',
  medium: '중',
}

const rankClassName: Record<number, string> = {
  1: 'text-[#A04100]',
  2: 'text-[#A04100]',
  3: 'text-[#8B7468]',
}

export function PopularCourseRank({ courses }: PopularCourseRankProps) {
  return (
    <section>
      <Card padding="md" shadow="section">
        <SectionTitle icon="🔥" title="지금 인기 코스" />
        <div className="mt-5 space-y-5">
          {courses.map((course) => (
            <div key={course.id} className="grid grid-cols-[40px_48px_1fr_auto] items-center gap-3">
              <div className={`text-center text-[20px] font-black ${rankClassName[course.rank] ?? 'text-[#594136]'}`}>
                {course.rank}
              </div>
              <CourseThumbnail tone={course.imageTone} className="h-12 w-12" />
              <div className="min-w-0">
                <h3 className="truncate text-[15px] font-bold leading-tight text-[#261912]">{course.title}</h3>
                <p className="mt-1 text-[12px] leading-none text-[#594136]">
                  {course.distanceKm.toFixed(1)}km · 난이도 {difficultyLabel[course.difficulty]}
                </p>
              </div>
              <div className="whitespace-nowrap text-[16px] font-bold text-[#A04100]">🏃 {course.participantCount}명</div>
            </div>
          ))}
        </div>
      </Card>
    </section>
  )
}
