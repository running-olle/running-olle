import { Link } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'
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
  return (
    <section>
      <div className="flex items-center justify-between">
        <SectionTitle icon="📅" title="제주 행사" />
        <Link to="/events" className="text-[12px] font-extrabold text-[#A04100] no-underline">전체보기</Link>
      </div>
      <div className="mt-5 space-y-3">
        {status === 'loading' && Array.from({ length: 2 }, (_, index) => (
          <Card key={index} padding="none" className="flex min-h-[96px] animate-pulse overflow-hidden" shadow="section">
            <div className="w-24 shrink-0 bg-[#F7DDD3]" />
            <div className="flex flex-1 flex-col justify-center gap-3 px-5">
              <span className="h-3 w-16 rounded bg-[#F7DDD3]" />
              <span className="h-4 w-44 rounded bg-[#F7DDD3]" />
            </div>
          </Card>
        ))}
        {status !== 'loading' && events.map((event) => {
          const date = eventDay(event.eventStartDate)
          return (
            <Link key={event.id} to={`/events/${event.id}`} className="block text-inherit no-underline">
              <Card padding="none" className="flex min-h-[98px] overflow-hidden" shadow="section">
                <div className="relative flex w-28 shrink-0 flex-col items-center justify-center overflow-hidden bg-[#F7DDD3]">
                  {event.thumbnailImageUrl && (
                    <>
                      <img src={event.thumbnailImageUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
                      <span className="absolute inset-0 bg-[#F7DDD3]/75" aria-hidden="true" />
                    </>
                  )}
                  <span className="relative text-[13px] font-black leading-none text-[#594136]">{date.month}</span>
                  <span className="relative mt-2 text-[22px] font-black leading-none text-[#A04100]">{date.day}</span>
                </div>
                <div className="flex min-w-0 flex-1 flex-col justify-center px-5">
                  <div className="mb-2 flex items-center gap-2">
                    <span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${event.runningRelated ? 'bg-[#E1F5E8] text-[#168847]' : 'bg-[#FFF0E8] text-[#A04100]'}`}>{event.categoryLabel}</span>
                    <span className="text-[10px] font-bold text-[#8D7164]">{ddayLabel(event)}</span>
                  </div>
                  <h3 className="truncate text-[16px] font-black text-[#261912]">{event.title}</h3>
                  <p className="mt-1 truncate text-[11px] font-semibold text-[#6B7280]">{event.venueName || event.address || '장소 정보 확인 중'}</p>
                  <p className="mt-1 text-[10px] font-bold text-[#168847]">정보 제공: {event.providerName}</p>
                </div>
              </Card>
            </Link>
          )
        })}
        {status !== 'loading' && events.length === 0 && (
          <Card className="text-center" shadow="section">
            <p className="text-[13px] font-bold text-[#594136]">진행 중이거나 예정된 제주 행사가 없어요.</p>
            <p className="mt-1 text-[11px] text-[#8D7164]">새 일정이 공개되면 이곳에 보여요.</p>
          </Card>
        )}
        {status === 'error' && (
          <p className="text-center text-[11px] font-semibold text-[#A04100]">행사 정보를 불러오지 못했어요.</p>
        )}
      </div>
    </section>
  )
}
