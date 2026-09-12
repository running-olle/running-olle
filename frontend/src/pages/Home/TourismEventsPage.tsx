import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Chip, EmptyState, ErrorState, Icon, Skeleton } from '../../components/ui'
import { TourismEventCard } from '../../features/home/TourismEventCard'
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
    <div className="tourism-events-page">
      <section className="tourism-events-intro" aria-labelledby="tourism-events-title">
        <h1 id="tourism-events-title">러닝 전후로<br />즐길 제주 행사</h1>
        <p>러닝·마라톤부터 축제와 공연까지, 제주에서 열리는 가까운 일정을 한곳에 모았어요.</p>
      </section>

      <form onSubmit={submitSearch} className="tourism-events-search" role="search">
        <Icon name="search" size={20} />
        <label className="sr-only" htmlFor="tourism-event-keyword">제주 행사 검색</label>
        <input
          id="tourism-event-keyword"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="마라톤, 트레일런, 축제명 검색"
        />
        <Button type="submit" variant="ghost" size="sm" className="tourism-events-search__action">검색</Button>
      </form>

      <Card as="section" variant="subtle" shadow="none" className="tourism-events-summary" aria-label="행사 검색 결과 요약">
        <div>
          <span>검색 결과</span>
          <strong>{summary.totalCount}<small>개</small></strong>
        </div>
        <div>
          <span>러닝 관련</span>
          <strong>{summary.runningCount}<small>개</small></strong>
        </div>
      </Card>

      <section className="tourism-events-filters" aria-label="행사 필터">
        <fieldset>
          <legend>행사 종류</legend>
          <div className="tourism-events-chip-row">
            {typeFilters.map((filter) => (
              <Chip key={filter.type} selected={type === filter.type} onClick={() => setType(filter.type)}>
                {filter.label}
              </Chip>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend>진행 상태</legend>
          <div className="tourism-events-chip-row">
            {statusFilters.map((filter) => (
              <Chip key={filter.status} selected={status === filter.status} onClick={() => setStatus(filter.status)}>
                {filter.label}
              </Chip>
            ))}
          </div>
        </fieldset>
      </section>

      {loading && (
        <div className="tourism-events-grid" role="status" aria-label="행사 목록을 불러오는 중">
          {Array.from({ length: 3 }, (_, index) => (
            <Card key={index} padding="none" className="tourism-event-catalog-skeleton">
              <Skeleton width="100%" height={196} />
              <div>
                <Skeleton width={132} height={24} rounded />
                <Skeleton width="78%" height={24} />
                <Skeleton width="92%" height={18} />
                <Skeleton width="65%" height={18} />
              </div>
            </Card>
          ))}
        </div>
      )}

      {!loading && failed && (
        <ErrorState
          icon={<Icon name="calendar" size={26} />}
          title="행사 정보를 불러오지 못했어요"
          description="잠시 후 다시 시도해 주세요. 선택한 필터는 그대로 유지돼요."
          onRetry={loadEvents}
        />
      )}

      {!loading && !failed && events.length === 0 && (
        <EmptyState
          creation
          icon={<Icon name="search" size={26} />}
          title="조건에 맞는 행사가 없어요"
          description="검색어나 필터를 바꾸면 다른 제주 일정을 찾을 수 있어요."
        />
      )}

      {!loading && !failed && events.length > 0 && (
        <section aria-labelledby="tourism-events-results-title">
          <div className="tourism-events-results-heading">
            <h2 id="tourism-events-results-title">행사 목록</h2>
            <span>{summary.totalCount}개의 일정</span>
          </div>
          <div className="tourism-events-grid">
            {events.map((event) => <TourismEventCard key={event.id} event={event} />)}
          </div>
        </section>
      )}
    </div>
  )
}
