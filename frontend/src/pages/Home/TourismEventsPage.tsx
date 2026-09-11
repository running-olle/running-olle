import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { tourismEventApi } from '../../features/home/tourismEventApi'
import type { TourismEvent, TourismEventStatus, TourismEventTypeFilter } from '../../features/home/types'

type EventFilterChip = {
  label: string
  type: TourismEventTypeFilter
}

type StatusFilterChip = {
  label: string
  status: TourismEventStatus
}

const typeFilters: EventFilterChip[] = [
  { label: '전체', type: 'ALL' },
  { label: '러닝·마라톤', type: 'RUNNING' },
  { label: '축제', type: 'FESTIVAL' },
  { label: '공연·행사', type: 'PERFORMANCE' },
]

const statusFilters: StatusFilterChip[] = [
  { label: '전체 일정', status: 'ALL' },
  { label: '진행 중', status: 'ONGOING' },
  { label: '예정', status: 'UPCOMING' },
]

const dateFormatter = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })

function formatDateRange(event: TourismEvent) {
  const startDate = dateFormatter.format(new Date(`${event.eventStartDate}T00:00:00`))
  const endDate = dateFormatter.format(new Date(`${event.eventEndDate}T00:00:00`))
  return startDate === endDate ? startDate : `${startDate} ~ ${endDate}`
}

function ddayLabel(event: TourismEvent) {
  if (event.status === '진행 중') {
    return '진행 중'
  }
  if (event.dday === 0) {
    return '오늘 시작'
  }
  return event.dday > 0 ? `D-${event.dday}` : `D+${Math.abs(event.dday)}`
}

function statusClassName(event: TourismEvent) {
  if (event.status === '진행 중') {
    return 'bg-[#E1F5E8] text-[#168847]'
  }
  return 'bg-[#FFF0E8] text-[#A04100]'
}

export function TourismEventsPage() {
  const [events, setEvents] = useState<TourismEvent[]>([])
  const [type, setType] = useState<TourismEventTypeFilter>('ALL')
  const [status, setStatus] = useState<TourismEventStatus>('ALL')
  const [keyword, setKeyword] = useState('')
  const [submittedKeyword, setSubmittedKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  const loadEvents = useCallback(async () => {
    setLoading(true)
    setFailed(false)
    try {
      const data = await tourismEventApi.getEvents({ type, status, keyword: submittedKeyword.trim() })
      setEvents(data)
    } catch {
      setEvents([])
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }, [status, submittedKeyword, type])

  useEffect(() => {
    void loadEvents()
  }, [loadEvents])

  const summary = useMemo(() => {
    const runningCount = events.filter((event) => event.runningRelated).length
    return { totalCount: events.length, runningCount }
  }, [events])

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmittedKeyword(keyword)
  }

  return (
    <div className="flex flex-col gap-5">
      <section>
        <p className="text-[14px] font-black text-[#FF6F0F]">TourAPI Jeju Events</p>
        <h1 className="mt-2 text-[34px] font-black leading-tight text-[#261912]">제주 행사</h1>
        <p className="mt-3 text-[16px] font-semibold leading-relaxed text-[#6B5347]">
          한국관광공사가 제공하는 제주 행사 중 러닝과 함께 즐기기 좋은 일정을 모았어요.
        </p>
      </section>

      <form onSubmit={submitSearch} className="flex min-h-[50px] items-center gap-3 rounded-full bg-white px-4 shadow-[0px_4px_12px_rgba(0,0,0,0.05)]">
        <span className="text-[18px]" aria-hidden="true">🔎</span>
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          className="min-w-0 flex-1 bg-transparent text-[14px] font-bold text-[#261912] outline-none placeholder:text-[#B3A49D]"
          placeholder="마라톤, 트레일런, 축제명 검색"
        />
        <button type="submit" className="shrink-0 text-[13px] font-black text-[#FF6F0F]">검색</button>
      </form>

      <Card className="grid grid-cols-2 divide-x divide-[#F1DED6] text-center" shadow="section">
        <div>
          <span className="text-[11px] font-bold text-[#8D7164]">표시 중인 행사</span>
          <strong className="mt-1 block text-[24px] font-black text-[#111827]">{summary.totalCount}개</strong>
        </div>
        <div>
          <span className="text-[11px] font-bold text-[#8D7164]">러닝 관련</span>
          <strong className="mt-1 block text-[24px] font-black text-[#111827]">{summary.runningCount}개</strong>
        </div>
      </Card>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {typeFilters.map((filter) => (
          <button
            key={filter.type}
            type="button"
            onClick={() => setType(filter.type)}
            className={`h-11 shrink-0 rounded-full px-5 text-[13px] font-black ${type === filter.type ? 'bg-[#FF6F0F] text-white' : 'border border-[#E1BFB1] bg-white text-[#594136]'}`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {statusFilters.map((filter) => (
          <button
            key={filter.status}
            type="button"
            onClick={() => setStatus(filter.status)}
            className={`h-9 shrink-0 rounded-full px-4 text-[12px] font-black ${status === filter.status ? 'bg-[#261912] text-white' : 'bg-[#FFF0E8] text-[#6B5347]'}`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {loading && (
        <div className="space-y-3">
          {Array.from({ length: 4 }, (_, index) => (
            <Card key={index} className="h-36 animate-pulse bg-white" />
          ))}
        </div>
      )}

      {!loading && failed && (
        <Card className="text-center">
          <p className="text-[14px] font-black text-[#A04100]">행사 정보를 불러오지 못했어요.</p>
          <button type="button" onClick={loadEvents} className="mt-3 rounded-full bg-[#FF6F0F] px-5 py-2 text-[12px] font-black text-white">다시 시도</button>
        </Card>
      )}

      {!loading && !failed && events.length === 0 && (
        <Card className="text-center">
          <p className="text-[14px] font-black text-[#261912]">진행 중이거나 예정된 제주 행사가 없어요.</p>
          <p className="mt-1 text-[12px] text-[#8D7164]">새 일정이 공개되면 다시 보여드릴게요.</p>
        </Card>
      )}

      {!loading && !failed && events.length > 0 && (
        <div className="space-y-4">
          {events.map((event) => (
            <Link key={event.id} to={`/events/${event.id}`} className="block text-inherit no-underline">
              <Card padding="none" className="overflow-hidden">
                {event.firstImageUrl && <img src={event.firstImageUrl} alt="" className="h-40 w-full object-cover" />}
                <div className="p-5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={`rounded-full px-3 py-1 text-[11px] font-black ${event.runningRelated ? 'bg-[#E1F5E8] text-[#168847]' : 'bg-[#FFF0E8] text-[#A04100]'}`}>{event.categoryLabel}</span>
                    <span className={`rounded-full px-3 py-1 text-[11px] font-black ${statusClassName(event)}`}>{ddayLabel(event)}</span>
                  </div>
                  <h2 className="mt-3 text-[20px] font-black leading-snug text-[#111827]">{event.title}</h2>
                  <p className="mt-2 text-[13px] font-bold text-[#594136]">{formatDateRange(event)}</p>
                  <p className="mt-1 text-[12px] font-semibold text-[#6B7280]">{event.venueName || event.address || '장소 정보 확인 중'}</p>
                  {event.overview && <p className="mt-4 line-clamp-3 text-[13px] leading-relaxed text-[#4B5563]">{event.overview}</p>}
                  <p className="mt-4 text-[11px] font-black text-[#168847]">정보 제공: {event.providerName}</p>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
