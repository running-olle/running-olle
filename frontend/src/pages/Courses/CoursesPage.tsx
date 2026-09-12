import { useNavigate } from 'react-router-dom'
import { Fab, Icon } from '../../components/ui'
import { CourseListView } from '../../features/course/CourseListView'

export function CoursesPage() {
  const navigate = useNavigate()

  return (
    <>
      <CourseListView
        scope="AVAILABLE"
        emptyTitle="공개된 코스가 아직 없어요"
        emptyDescription="다른 러너가 공개한 코스가 생기면 여기서 탐색할 수 있어요."
        createActionLabel="코스 만들기"
        createdBadgeLabel="내 공개 코스"
        showHeader={false}
        showCreatedFilter={false}
        showBookmarkedFilter
        showSummary={false}
        showSearch
        showDescription={false}
        compactCards
        showStartAction={false}
        showBookmarkAction
      />
      <div className="floating-action-clearance" aria-hidden="true" />
      <Fab
        extended
        className="page-action-fab"
        icon={<Icon name="plus" />}
        label="코스 만들기"
        onClick={() => navigate('/courses/create')}
      />
    </>
  )
}
