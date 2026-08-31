import { CourseListView } from '../../features/course/CourseListView'

export function RunningCourseSelectPage() {
  return (
    <CourseListView
      scope="LIBRARY"
      title="코스 선택 달리기"
      subtitle="저장한 코스와 내가 만든 코스 중에서 오늘 달릴 제주 코스를 고르세요."
      emptyTitle="달릴 코스가 아직 없어요"
      emptyDescription="코스를 만들거나 공개 코스를 저장하면 여기서 선택해서 달릴 수 있어요."
      createActionLabel="새 코스"
      showStartAction
    />
  )
}
