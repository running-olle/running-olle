import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'
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
    <section>
      <SectionTitle icon="✨" title="오늘 맞춤 추천" />
      <div className="-mx-5 mt-5 flex gap-3 overflow-x-auto px-5 pb-2">
        {courses.map((course) => (
          <Card key={course.id} padding="none" className="w-60 shrink-0 overflow-hidden">
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
                <span>·</span>
                <span>약 {course.estimatedMinutes}분</span>
                <span className="ml-auto font-bold text-[#BFAC00]">★ {course.rating.toFixed(1)}</span>
              </div>
              <Badge variant={course.difficulty} className="mt-4">
                {difficultyLabel[course.difficulty]}
              </Badge>
            </div>
          </Card>
        ))}
      </div>
    </section>
  )
}
