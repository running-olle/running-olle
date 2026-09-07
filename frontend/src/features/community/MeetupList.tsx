import type { ReactNode } from 'react'
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
      <div className="mt-4 flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {filters.map((filter) => (
          <button
            key={filter.key}
            type="button"
            onClick={() => onFilterChange(filter.key)}
            className={`shrink-0 rounded-full border px-4 py-2 text-[12px] font-bold ${
              activeFilter === filter.key
                ? 'border-[#FF6F0F] bg-[#FF6F0F] text-white'
                : 'border-[#E1BFB1] bg-white text-[#594136]'
            }`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      <div className="mt-4 flex flex-col gap-3">
        {meetups.length === 0 ? (
          <div className="rounded-[18px] bg-white px-5 py-6 text-[13px] text-[#8D7164] shadow-[0px_2px_12px_rgba(0,0,0,0.06)]">
            조건에 맞는 번개가 없습니다.
          </div>
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
            <article
              key={meetup.id}
              className="rounded-[18px] bg-white p-4 text-left shadow-[0px_2px_12px_rgba(0,0,0,0.06)]"
            >
              <button type="button" onClick={() => onOpenDetail(meetup)} className="block w-full text-left">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex gap-2">
                    <Badge tone="brand">번개</Badge>
                    <Badge tone={statusTone}>{statusLabel}</Badge>
                  </div>
                  {myState !== 'none' ? (
                    <Badge
                      tone={
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
                  <Badge tone={meetup.joinMethod === 'instant' ? 'info' : 'brand'}>
                    {meetup.joinMethod === 'instant' ? '즉시 참여' : '수락 후 참여'}
                  </Badge>
                  <span className="text-[12px] text-[#8D7164]">{meetup.themeLabel}</span>
                </div>

                <div className="mt-3 text-[15px] font-bold leading-6 text-[#261912]">{meetup.title}</div>

                {meetup.course ? (
                  <div className="mt-3 inline-flex items-center gap-2 rounded-[8px] bg-[#F5F5F5] px-3 py-2 text-[12px] text-[#594136]">
                    <span>{meetup.course.icon}</span>
                    <span>
                      {meetup.course.name} · {meetup.course.distanceKm}km
                    </span>
                  </div>
                ) : null}

                <div className="mt-3 flex flex-wrap gap-x-3 gap-y-2 text-[12px] text-[#594136]">
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
                  <span className="ml-3 text-[12px] text-[#8D7164]">+{Math.max(acceptedCount - 1, 0)}명 참여</span>
                </div>
                <button
                  type="button"
                  onClick={() => onOpenDetail(meetup)}
                  className={`rounded-[10px] px-4 py-2 text-[13px] font-bold ${
                    isInactive ? 'bg-[#F0F0F0] text-[#AAA]' : 'bg-[#FF6F0F] text-white'
                  }`}
                >
                  {actionLabel}
                </button>
              </div>
            </article>
          )
        })}
      </div>
    </>
  )
}

function Badge({
  children,
  tone,
}: {
  children: ReactNode
  tone: 'brand' | 'success' | 'warning' | 'danger' | 'neutral' | 'info'
}) {
  const className =
    tone === 'brand'
      ? 'bg-[#FFF5EE] text-[#FF6F0F]'
      : tone === 'success'
        ? 'bg-[#E8F5E9] text-[#15803D]'
        : tone === 'warning'
          ? 'bg-[#FFF3E0] text-[#C2410C]'
          : tone === 'danger'
            ? 'bg-[#FFE8E5] text-[#B91C1C]'
            : tone === 'info'
              ? 'bg-[#E8F5FE] text-[#1D4ED8]'
              : 'bg-[#F5F5F5] text-[#AAA]'

  return <span className={`rounded-full px-3 py-1 text-[11px] font-bold ${className}`}>{children}</span>
}

function AvatarBubble({ index, label }: { index: number; label: string }) {
  return (
    <div
      className={`flex h-[26px] w-[26px] items-center justify-center rounded-full border-2 border-white bg-brand-500 text-[11px] text-white ${
        index === 0 ? '' : '-ml-1.5'
      }`}
    >
      {label}
    </div>
  )
}
