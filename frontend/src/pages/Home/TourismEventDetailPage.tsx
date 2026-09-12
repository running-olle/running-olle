import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Card, ErrorState, Icon, IconButton, Spinner } from '../../components/ui'
import { TourismEventBadges } from '../../features/home/TourismEventCard'
import { tourismEventApi } from '../../features/home/tourismEventApi'
import { formatTourismEventDateRange, getTourismEventLocation } from '../../features/home/tourismEventPresentation'
import type { TourismEvent } from '../../features/home/types'

function cleanExternalUrl(value: string | null) {
  if (!value) return null
  const hrefMatch = value.match(/href=["']([^"']+)["']/i)
  const candidate = (hrefMatch?.[1] ?? value).replace(/<[^>]+>/g, '').trim()
  if (!candidate.startsWith('http://') && !candidate.startsWith('https://')) return null
  return candidate
}

export function TourismEventDetailPage() {
  const navigate = useNavigate()
  const { eventId } = useParams()
  const [event, setEvent] = useState<TourismEvent | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (!eventId) return
    let ignore = false
    setLoading(true)
    setFailed(false)

    tourismEventApi.getEvent(eventId)
      .then((data) => {
        if (!ignore) setEvent(data)
      })
      .catch(() => {
        if (!ignore) {
          setEvent(null)
          setFailed(true)
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [eventId])

  if (loading) {
    return (
      <section className="tourism-event-detail-state" aria-label="행사 정보를 불러오는 중">
        <Spinner size="page" label="행사 정보를 불러오는 중" />
        <p>행사 정보를 불러오는 중이에요</p>
      </section>
    )
  }

  if (failed || !event) {
    return (
      <ErrorState
        className="tourism-event-detail-state"
        icon={<Icon name="calendar" size={26} />}
        title="행사를 찾을 수 없어요"
        description="삭제되었거나 지금은 볼 수 없는 행사일 수 있어요."
        action={<Link to="/events" className="ui-button ui-button--secondary ui-button--sm">제주 행사로 돌아가기</Link>}
      />
    )
  }

  const homepageUrl = cleanExternalUrl(event.homepage) ?? cleanExternalUrl(event.sourceUrl)
  const kakaoMapUrl = event.lat && event.lng
    ? `https://map.kakao.com/link/map/${encodeURIComponent(event.title)},${event.lat},${event.lng}`
    : null
  const actionCount = Number(Boolean(homepageUrl)) + Number(Boolean(kakaoMapUrl))

  return (
    <article className="tourism-event-detail">
      <IconButton
        icon={<Icon name="arrowLeft" size={22} />}
        label="이전 화면으로"
        onClick={() => navigate(-1)}
        className="tourism-event-detail__back"
      />

      <header className="tourism-event-detail__header">
        <TourismEventBadges event={event} />
        <h1>{event.title}</h1>
        <p>
          <Icon name="calendar" size={18} />
          <time dateTime={event.eventStartDate}>{formatTourismEventDateRange(event, true)}</time>
        </p>
      </header>

      <figure className="tourism-event-detail__hero" data-has-image={Boolean(event.firstImageUrl)}>
        {event.firstImageUrl ? (
          <img src={event.firstImageUrl} alt="" />
        ) : (
          <div>
            <Icon name="calendar" size={40} />
            <span>제주 행사</span>
          </div>
        )}
      </figure>

      <Card as="section" shadow="none" className="tourism-event-detail__info">
        <h2>행사 정보</h2>
        <dl>
          <InfoRow icon={<Icon name="location" size={19} />} label="장소" value={getTourismEventLocation(event)} />
          {event.address && (
            <InfoRow
              icon={<Icon name="course" size={19} />}
              label="주소"
              value={`${event.address}${event.detailAddress ? ` ${event.detailAddress}` : ''}`}
            />
          )}
          {event.organizer && <InfoRow icon={<Icon name="runners" size={19} />} label="주최" value={event.organizer} />}
          {event.tel && <InfoRow icon={<Icon name="phone" size={19} />} label="문의" value={event.tel} />}
        </dl>
      </Card>

      {event.overview && (
        <Card as="section" shadow="none" className="tourism-event-detail__overview">
          <h2>행사 소개</h2>
          <p>{event.overview}</p>
        </Card>
      )}

      {actionCount > 0 && (
        <div className="tourism-event-detail__actions" data-count={actionCount}>
          {homepageUrl && (
            <a href={homepageUrl} target="_blank" rel="noreferrer" className="ui-button ui-button--secondary ui-button--md">
              <Icon name="externalLink" size={18} />
              공식 페이지
            </a>
          )}
          {kakaoMapUrl && (
            <a href={kakaoMapUrl} target="_blank" rel="noreferrer" className="ui-button ui-button--primary ui-button--md">
              <Icon name="location" size={18} />
              지도에서 보기
            </a>
          )}
        </div>
      )}

      <footer className="tourism-event-detail__source">
        <span>정보 제공</span>
        <strong>{event.providerName}</strong>
      </footer>
    </article>
  )
}

function InfoRow({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="tourism-event-detail__info-row">
      <dt>
        <span>{icon}</span>
        {label}
      </dt>
      <dd>{value}</dd>
    </div>
  )
}
