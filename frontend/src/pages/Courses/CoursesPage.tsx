import { CourseListView } from '../../features/course/CourseListView'

export function CoursesPage() {
  return (
    <CourseListView
      scope="AVAILABLE"
      title="코스 탐색"
      subtitle="러너들이 공개한 제주 코스를 둘러보고, 뛰고 싶은 코스는 저장해두세요."
      emptyTitle="공개된 코스가 아직 없어요"
      emptyDescription="다른 러너가 공개한 코스가 생기면 여기서 탐색할 수 있어요."
      createActionLabel="코스 만들기"
      createdBadgeLabel="내 공개 코스"
      showCreatedFilter={false}
      showSummary={false}
      showStartAction={false}
      showBookmarkAction
    />
  )
}
