import { Link } from 'react-router-dom'
import { Skeleton } from '../../components/ui/Feedback'
import { Icon } from '../../components/ui/Icon'
import { SectionHeader } from '../../components/ui/SectionHeader'
import { TourismEventCard } from './TourismEventCard'
import type { TourismEvent } from './types'

type RunningEventListProps = {
  events: TourismEvent[]
  status: 'idle' | 'loading' | 'success' | 'error'
}

export function RunningEventList({ events, status }: RunningEventListProps) {
  const isLoading = status === 'idle' || status === 'loading'

  return (
    <section aria-labelledby="running-events-title">
      <SectionHeader
        id="running-events-title"
        icon={<Icon name="calendar" size={20} />}
        title="제주 행사"
        description="달력에 담아두고 싶은 가까운 행사예요."
        action={(
          <Link to="/events" className="tourism-events-more">
            <span>전체보기</span>
            <Icon name="chevronRight" size={16} />
          </Link>
        )}
      />
      <div className="tourism-event-highlight-list">
        {isLoading && Array.from({ length: 2 }, (_, index) => (
          <div key={`event-loading-${index}`} className="tourism-event-highlight-skeleton">
            <Skeleton width={76} height={76} className="shrink-0" />
            <div className="tourism-event-highlight-skeleton__copy">
              <Skeleton width={108} height={24} rounded />
              <Skeleton width="82%" height={20} />
              <Skeleton width="66%" height={16} />
            </div>
          </div>
        ))}

        {!isLoading && status !== 'error' && events.map((event) => (
          <TourismEventCard key={event.id} event={event} variant="compact" />
        ))}

        {!isLoading && status !== 'error' && events.length === 0 ? (
          <div className="tourism-event-highlight-state">
            <Icon name="calendar" size={24} />
            <p>진행 중이거나 예정된 제주 행사가 없어요.</p>
            <span>새 일정이 공개되면 이곳에 보여요.</span>
          </div>
        ) : null}

        {status === 'error' ? (
          <div className="tourism-event-highlight-state tourism-event-highlight-state--error" role="alert">
            <Icon name="calendar" size={24} />
            <p>행사 정보를 불러오지 못했어요.</p>
            <span>전체 행사 페이지에서 다시 확인해 주세요.</span>
          </div>
        ) : null}
      </div>
    </section>
  )
}
