import { Card, Chip, Badge, EmptyState, HorizontalScroller, Button } from '../../components/ui'
import type { Meetup, MeetupFilter } from './communityTypes'

export function MeetupList({
  meetups,
  activeFilter,
  onFilterChange,
  onOpenDetail,
}: {
  meetups: Meetup[]
  activeFilter: MeetupFilter
  onFilterChange: (filter: MeetupFilter) => void
  onOpenDetail: (meetup: Meetup) => void
}) {
  const filters: Array<{ key: MeetupFilter; label: string }> = [
    { key: 'all', label: '전체' },
    { key: 'recruiting', label: '모집 중' },
    { key: 'today', label: '오늘' },
    { key: 'thisWeek', label: '이번 주' },
    { key: 'coast', label: '해안' },
    { key: 'oreum', label: '오름' },
  ]

  return (
    <>
      <HorizontalScroller aria-label="번개 필터">
        {filters.map((filter) => (
          <Chip selected={activeFilter === filter.key}
            key={filter.key}
            type="button"
            onClick={() => onFilterChange(filter.key)}
            className="shrink-0"
          >
            {filter.label}
          </Chip>
        ))}
      </HorizontalScroller>

      <div className="mt-4 flex flex-col gap-3">
        {meetups.length === 0 ? (
          <EmptyState compact title="조건에 맞는 번개가 없습니다." />
        ) : null}

        {meetups.map((meetup) => {
          const acceptedCount = meetup.participantIds.length
          const myState = meetup.myParticipation
          const isClosed = meetup.status === 'closed' || acceptedCount >= meetup.maxParticipants
          const isCompleted = meetup.status === 'completed'
          const isCancelled = meetup.status === 'cancelled'
          const isInactive = isClosed || isCompleted || isCancelled
          const statusLabel = isCompleted
            ? '완료'
            : isCancelled
              ? '취소'
              : isClosed
                ? `마감 ${acceptedCount}/${meetup.maxParticipants}`
                : `모집중 ${acceptedCount}/${meetup.maxParticipants}`
          const statusTone = isCompleted || isCancelled ? 'danger' : isClosed ? 'neutral' : 'success'
          const actionLabel = isCompleted
            ? '완료'
            : isCancelled
              ? '취소'
              : myState === 'accepted'
                ? '채팅 보기'
                : meetup.joinMethod === 'instant'
                  ? '참여하기'
                  : myState === 'pending'
                    ? '대기중'
                    : isClosed
                      ? '마감'
                      : '요청하기'

          return (
            <Card as="article" shadow="none"
              key={meetup.id}
              className="community-card"
            >
              <button type="button" onClick={() => onOpenDetail(meetup)} className="block w-full text-left">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex gap-2">
                    <Badge variant="brand">번개</Badge>
                    <Badge variant={statusTone}>{statusLabel}</Badge>
                  </div>
                  {myState !== 'none' ? (
                    <Badge
                      variant={
                        myState === 'accepted'
                          ? 'success'
                          : myState === 'pending'
                            ? 'warning'
                            : 'danger'
                      }
                    >
                      {myState === 'accepted' ? '참여 확정' : myState === 'pending' ? '대기중' : '거절됨'}
                    </Badge>
                  ) : null}
                </div>

                <div className="mt-3 flex items-center gap-2">
                  <Badge variant={meetup.joinMethod === 'instant' ? 'info' : 'brand'}>
                    {meetup.joinMethod === 'instant' ? '즉시 참여' : '수락 후 참여'}
                  </Badge>
                  <span className="text-caption text-ink-secondary">{meetup.themeLabel}</span>
                </div>

                <div className="mt-3 text-body font-bold leading-6 text-ink">{meetup.title}</div>

                {meetup.course ? (
                  <div className="mt-3 inline-flex items-center gap-2 rounded-sm bg-surface-subtle px-3 py-2 text-caption text-ink-secondary">
                    <span>{meetup.course.icon}</span>
                    <span>
                      {meetup.course.name} · {meetup.course.distanceKm}km
                    </span>
                  </div>
                ) : null}

                <div className="mt-3 flex flex-wrap gap-x-3 gap-y-2 text-caption text-ink-secondary">
                  <span>일시 {meetup.scheduleLabel}</span>
                  <span>정원 {meetup.maxParticipants}명</span>
                  <span>페이스 {meetup.targetPaceLabel}</span>
                </div>
              </button>

              <div className="mt-4 flex items-center justify-between">
                <div className="flex items-center">
                  {meetup.participantIds.slice(0, 4).map((participantId, index) => (
                    <AvatarBubble
                      key={participantId}
                      index={index}
                      label={participantId === meetup.organizerId ? meetup.organizerAvatar : 'R'}
                    />
                  ))}
                  <span className="ml-3 text-caption text-ink-secondary">+{Math.max(acceptedCount - 1, 0)}명 참여</span>
                </div>
                <Button variant="primary" size="sm"
                  type="button"
                  onClick={() => onOpenDetail(meetup)}
                  className={`rounded-control px-4 py-2 text-label font-bold ${
                    isInactive ? 'bg-surface-muted text-ink-tertiary' : 'bg-brand-500 text-surface'
                  }`}
                >
                  {actionLabel}
                </Button>
              </div>
            </Card>
          )
        })}
      </div>
    </>
  )
}



function AvatarBubble({ index, label }: { index: number; label: string }) {
  return (
    <div
      className={`flex h-7 w-7 items-center justify-center rounded-full border-2 border-white bg-brand-500 text-caption text-surface ${
        index === 0 ? '' : '-ml-1.5'
      }`}
    >
      {label}
    </div>
  )
}
