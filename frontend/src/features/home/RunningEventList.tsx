import { Link } from 'react-router-dom'
import { Badge } from '../../components/ui/Badge'
import { Card } from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Feedback'
import { Icon } from '../../components/ui/Icon'
import { ListRow } from '../../components/ui/ListRow'
import { SectionHeader } from '../../components/ui/SectionHeader'
import type { TourismEvent } from './types'

type RunningEventListProps = {
  events: TourismEvent[]
  status: 'idle' | 'loading' | 'success' | 'error'
}

const monthFormatter = new Intl.DateTimeFormat('en-US', { month: 'short' })

function eventDay(date: string) {
  const eventDate = new Date(`${date}T00:00:00`)
  return {
    month: monthFormatter.format(eventDate).toUpperCase(),
    day: String(eventDate.getDate()).padStart(2, '0'),
  }
}

function ddayLabel(event: TourismEvent) {
  if (event.status === '진행 중') {
    return '진행 중'
  }
  if (event.status === '종료') {
    return `종료 D+${Math.abs(event.dday)}`
  }
  if (event.dday === 0) {
    return '오늘'
  }
  return event.dday > 0 ? `D-${event.dday}` : `D+${Math.abs(event.dday)}`
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
          <Link to="/events" className="inline-flex min-h-11 items-center text-label font-bold text-brand-700 no-underline">
            전체보기
          </Link>
        )}
      />
      <Card padding="none" shadow="none" className="mt-4 overflow-hidden border border-border-subtle">
        {isLoading && Array.from({ length: 2 }, (_, index) => (
          <div key={`event-loading-${index}`} className="flex min-h-24 items-center gap-3 border-t border-border-subtle px-4 py-3 first:border-t-0">
            <Skeleton width={64} height={64} className="shrink-0" />
            <div className="min-w-0 flex-1 space-y-2">
              <Skeleton width={72} height={18} rounded />
              <Skeleton width="75%" height={18} />
              <Skeleton width="55%" height={14} />
            </div>
          </div>
        ))}

        {!isLoading && status !== 'error' && events.map((event) => {
          const date = eventDay(event.eventStartDate)

          return (
            <Link
              key={event.id}
              to={`/events/${event.id}`}
              className="block border-t border-border-subtle text-inherit no-underline first:border-t-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-brand-500"
            >
              <ListRow
                className="min-h-24"
                leading={(
                  <span className="relative flex h-16 w-16 shrink-0 flex-col items-center justify-center overflow-hidden rounded-control bg-surface-subtle">
                    {event.thumbnailImageUrl ? (
                      <>
                        <img src={event.thumbnailImageUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
                        <span className="absolute inset-0 bg-surface/80" aria-hidden="true" />
                      </>
                    ) : null}
                    <span className="relative text-caption font-bold text-ink-secondary">{date.month}</span>
                    <strong className="relative mt-1 text-section-title font-extrabold tabular-nums text-brand-700">{date.day}</strong>
                  </span>
                )}
                title={event.title}
                description={(
                  <span className="block">
                    <span className="block truncate">{event.venueName || event.address || '장소 정보 확인 중'}</span>
                    <span className="mt-1 block text-ink-tertiary">정보 제공: {event.providerName}</span>
                  </span>
                )}
                trailing={(
                  <span className="flex flex-col items-end gap-2">
                    <Badge variant={event.runningRelated ? 'success' : 'category'}>{event.categoryLabel}</Badge>
                    <span className="inline-flex items-center gap-1 whitespace-nowrap text-caption font-bold text-ink-secondary">
                      {ddayLabel(event)}
                      <Icon name="chevronRight" size={16} />
                    </span>
                  </span>
                )}
              />
            </Link>
          )
        })}

        {!isLoading && status !== 'error' && events.length === 0 ? (
          <div className="px-5 py-8 text-center">
            <p className="text-body-sm font-bold text-ink">진행 중이거나 예정된 제주 행사가 없어요.</p>
            <p className="mt-1 text-caption text-ink-secondary">새 일정이 공개되면 이곳에 보여요.</p>
          </div>
        ) : null}

        {status === 'error' ? (
          <div className="bg-danger-subtle px-5 py-4 text-center" role="alert">
            <p className="text-body-sm font-bold text-danger">행사 정보를 불러오지 못했어요.</p>
            <p className="mt-1 text-caption text-ink-secondary">전체 행사 페이지에서 다시 확인해 주세요.</p>
          </div>
        ) : null}
      </Card>
    </section>
  )
}
