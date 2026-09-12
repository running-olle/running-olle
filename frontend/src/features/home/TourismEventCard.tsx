import { Link } from 'react-router-dom'
import { Badge, Card, Icon, MetaList, type BadgeVariant } from '../../components/ui'
import {
  formatTourismEventDateRange,
  getTourismEventDateParts,
  getTourismEventDdayLabel,
  getTourismEventImage,
  getTourismEventLocation,
} from './tourismEventPresentation'
import type { TourismEvent } from './types'

type TourismEventCardProps = {
  event: TourismEvent
  variant?: 'compact' | 'catalog'
}

function statusBadgeVariant(event: TourismEvent): BadgeVariant {
  if (event.status === '진행 중') return 'success'
  if (event.status === '종료') return 'neutral'
  return 'warning'
}

export function TourismEventBadges({ event, className = '' }: { event: TourismEvent; className?: string }) {
  return (
    <div className={`tourism-event-badges ${className}`}>
      <Badge variant={event.runningRelated ? 'brand' : 'neutral'}>{event.categoryLabel}</Badge>
      <Badge variant={statusBadgeVariant(event)}>{getTourismEventDdayLabel(event)}</Badge>
    </div>
  )
}

export function TourismEventCard({ event, variant = 'catalog' }: TourismEventCardProps) {
  const imageUrl = getTourismEventImage(event)
  const dateParts = getTourismEventDateParts(event.eventStartDate)
  const compact = variant === 'compact'

  return (
    <Link
      to={`/events/${event.id}`}
      className={`tourism-event-link tourism-event-link--${variant}`}
    >
      <Card
        as="article"
        padding="none"
        shadow={compact ? 'none' : 'card'}
        variant="interactive"
        className={`tourism-event-card tourism-event-card--${variant}`}
      >
        <div className="tourism-event-card__media" data-has-image={Boolean(imageUrl)}>
          {imageUrl ? <img src={imageUrl} alt="" loading="lazy" /> : <Icon name="calendar" size={compact ? 24 : 32} />}
          {compact ? (
            <span className="tourism-event-card__date">
              <span>{dateParts.month}</span>
              <strong>{dateParts.day}</strong>
            </span>
          ) : (
            <TourismEventBadges event={event} className="tourism-event-card__media-badges" />
          )}
        </div>

        <div className="tourism-event-card__body">
          {compact && <TourismEventBadges event={event} />}
          <h3>{event.title}</h3>
          <MetaList
            ariaLabel="행사 일정과 장소"
            className="tourism-event-card__meta"
            items={[
              {
                icon: <Icon name="calendar" size={15} />,
                label: <time dateTime={event.eventStartDate}>{formatTourismEventDateRange(event)}</time>,
              },
              {
                icon: <Icon name="location" size={15} />,
                label: getTourismEventLocation(event),
              },
            ]}
          />
          <p className="tourism-event-card__source">정보 제공 · {event.providerName}</p>
        </div>

        {compact && (
          <span className="tourism-event-card__chevron" aria-hidden="true">
            <Icon name="chevronRight" size={20} />
          </span>
        )}
      </Card>
    </Link>
  )
}
