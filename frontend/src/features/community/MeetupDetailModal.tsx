import type { ReactNode } from 'react'
import { KakaoPointMap } from '../map/KakaoPointMap'
import type { Meetup, ParticipationStatus } from './communityTypes'

export function MeetupDetailModal({
  meetup,
  myParticipation,
  onClose,
  onJoin,
  onOpenApplicants,
  onOpenInquiryChat,
  onOpenGroupChat,
  onEdit,
  onDelete,
  onShare,
  onOpenCourse,
}: {
  meetup: Meetup
  myParticipation: ParticipationStatus
  onClose: () => void
  onJoin: (meetup: Meetup) => void
  onOpenApplicants: (meetup: Meetup) => void
  onOpenInquiryChat: (meetup: Meetup) => void
  onOpenGroupChat: (meetup: Meetup) => void
  onEdit: (meetup: Meetup) => void
  onDelete: (meetup: Meetup) => void
  onShare: (meetup: Meetup) => void
  onOpenCourse: (courseId: string) => void
}) {
  const acceptedCount = meetup.participantIds.length
  const isOrganizer = meetup.isOrganizer
  const isClosed = meetup.status === 'closed' || acceptedCount >= meetup.maxParticipants
  const isCompleted = meetup.status === 'completed'
  const isCancelled = meetup.status === 'cancelled'
  const acceptedMembers = meetup.applicants.filter((item) => item.status === 'accepted')
  const pendingCount = meetup.applicants.filter((item) => item.status === 'pending').length
  const primaryAction = getPrimaryActionLabel(isOrganizer, myParticipation, meetup.joinMethod, meetup.status, isClosed)
  const actionHelper = getPrimaryActionHelper(isOrganizer, myParticipation, meetup.joinMethod, meetup.status, isClosed)
  const statusLabel = isCompleted
    ? '완료'
    : isCancelled
      ? '취소'
      : isClosed
        ? `마감 ${acceptedCount}/${meetup.maxParticipants}`
        : `모집중 ${acceptedCount}/${meetup.maxParticipants}`
  const statusTone = isCompleted || isCancelled ? 'danger' : isClosed ? 'neutral' : 'success'

  return (
    <div className="fixed inset-0 z-50 bg-[rgba(38,25,18,0.45)]" onClick={onClose}>
      <div className="mx-auto flex h-dvh max-w-[430px] flex-col bg-[#FFF8F6]" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-center gap-3 border-b border-[#E1BFB1] bg-[#FFF8F6] px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="flex h-[34px] w-[34px] items-center justify-center rounded-[10px] bg-[#F5F5F5] text-[17px] text-[#261912]"
          >
            ←
          </button>
          <div className="text-[17px] font-bold text-[#261912]">번개 상세</div>
          <div className="ml-auto flex gap-2">
            <button
              type="button"
              onClick={() => onShare(meetup)}
              className="flex h-8 w-8 items-center justify-center rounded-[8px] bg-[#F5F5F5] text-[14px] text-[#594136]"
            >
              ↗
            </button>
            {isOrganizer && !isCompleted && !isCancelled ? (
              <button
                type="button"
                onClick={() => onEdit(meetup)}
                className="flex h-8 items-center justify-center rounded-[8px] bg-[#F5F5F5] px-3 text-[12px] font-bold text-[#594136]"
              >
                수정
              </button>
            ) : null}
            {isOrganizer && !isCompleted ? (
              <button
                type="button"
                onClick={() => onDelete(meetup)}
                className="flex h-8 items-center justify-center rounded-[8px] bg-[#FFE8E5] px-3 text-[12px] font-bold text-[#B91C1C]"
              >
                삭제
              </button>
            ) : null}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto pb-[98px]">
          <div className="px-5 pt-4">
            <div className="flex flex-wrap items-center gap-2">
              <Badge tone="brand">번개</Badge>
              <Badge tone={statusTone}>{statusLabel}</Badge>
              <Badge tone={meetup.joinMethod === 'instant' ? 'info' : 'brand'}>
                {meetup.joinMethod === 'instant' ? '즉시 참여' : '수락 후 참여'}
              </Badge>
            </div>

            <div className="mt-3 text-[21px] font-black leading-[1.3] text-[#261912]">{meetup.title}</div>

            <div className="mt-3 flex items-center gap-2">
              <div
                className="flex h-8 w-8 items-center justify-center rounded-full text-[13px] text-white"
                style={{ backgroundImage: meetup.organizerGradient }}
              >
                {meetup.organizerAvatar}
              </div>
              <div className="text-[13px] text-[#8D7164]">
                {meetup.organizerName} · {meetup.createdAtLabel}
              </div>
            </div>
          </div>

          <div className="mx-5 mt-4 overflow-hidden rounded-[16px] bg-[#F7DDD3]">
            <KakaoPointMap
              lat={meetup.meetingLatitude}
              lng={meetup.meetingLongitude}
              label={meetup.locationLabel}
              className="h-[180px]"
            />
            <div className="px-4 py-3 text-[12px] font-normal text-[#8D7164]">
              {meetup.locationLabel} · {meetup.meetingLatitude.toFixed(4)}, {meetup.meetingLongitude.toFixed(4)}
            </div>
          </div>

          {meetup.course ? (
            <button
              type="button"
              onClick={() => onOpenCourse(meetup.course!.id)}
              className="mx-5 mt-4 flex items-center gap-3 rounded-[16px] bg-[#FFF5EE] px-4 py-4 text-left"
            >
              <div className="flex h-[42px] w-[42px] items-center justify-center rounded-[11px] bg-[#FF6F0F] text-[18px] text-white">
                {meetup.course.icon}
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate text-[14px] font-bold text-[#261912]">{meetup.course.name}</div>
                <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-[12px] text-[#8D7164]">
                  <span>{meetup.course.distanceKm}km</span>
                  <span>{meetup.course.durationMinutes}분</span>
                  <span>{meetup.course.difficultyLabel}</span>
                </div>
              </div>
              <span className="text-[12px] font-bold text-[#FF6F0F]">코스 보기</span>
            </button>
          ) : null}

          {meetup.joinMethod === 'approval' && !isCompleted && !isCancelled ? (
            <div className="mx-5 mt-4 rounded-[16px] bg-[#FFF5EE] px-4 py-4 text-[12px] leading-6 text-[#A04100]">
              이 번개는 방장 수락 후 참여가 확정됩니다. 요청이 승인되면 그룹 채팅에 자동 입장합니다.
            </div>
          ) : null}

          <div className="mx-5 mt-4 rounded-[16px] bg-white p-4">
            <div className="grid grid-cols-2 gap-3">
              <InfoItem label="일시" value={meetup.scheduleLabel} />
              <InfoItem label="목표 페이스" value={meetup.targetPaceLabel} />
              <InfoItem label="집결 장소" value={meetup.locationLabel} />
              <InfoItem label="정원" value={`${acceptedCount}/${meetup.maxParticipants}명`} />
            </div>
          </div>

          <div className="mx-5 mt-4 rounded-[16px] bg-white p-4">
            <div className="text-[15px] font-bold text-[#261912]">번개 소개</div>
            <p className="mt-2 text-[14px] leading-7 text-[#594136]">{meetup.description}</p>
          </div>

          <div className="mx-5 mt-4 rounded-[16px] bg-white p-4">
            <div className="flex items-center justify-between">
              <div className="text-[15px] font-bold text-[#261912]">참여 멤버</div>
              <div className="text-[12px] text-[#8D7164]">
                {acceptedCount}/{meetup.maxParticipants}
              </div>
            </div>

            <div className="mt-4 space-y-3">
              <MemberRow
                name={meetup.organizerName}
                avatar={meetup.organizerAvatar}
                gradient={meetup.organizerGradient}
                meta="방장"
                badge="호스트"
                badgeTone="brand"
              />
              {acceptedMembers.map((member) => (
                <MemberRow
                  key={member.id}
                  name={member.nickname}
                  avatar={member.avatar}
                  gradient={member.gradient}
                  meta={`누적 ${member.stats.totalDistanceKm}km · ${member.stats.averagePaceText}`}
                  badge="멤버"
                  badgeTone="neutral"
                />
              ))}
              {acceptedMembers.length === 0 ? (
                <div className="rounded-[12px] bg-[#FFF8F6] px-4 py-4 text-[12px] text-[#8D7164]">
                  아직 확정된 참여자가 없습니다.
                </div>
              ) : null}
            </div>
          </div>

          {!isOrganizer && myParticipation === 'pending' ? (
            <div className="mx-5 mt-4 rounded-[16px] bg-[#FFF5EE] p-4">
              <div className="text-[16px] font-black text-[#261912]">요청 대기 중</div>
              <div className="mt-2 text-[13px] leading-6 text-[#594136]">
                방장이 확인하면 참여 여부가 결정됩니다.
              </div>
            </div>
          ) : null}

          {myParticipation === 'rejected' ? (
            <div className="mx-5 mt-4 rounded-[16px] bg-[#FFF1EE] p-4">
              <div className="text-[16px] font-black text-[#261912]">참여가 거절됐습니다</div>
              <div className="mt-2 text-[13px] leading-6 text-[#594136]">
                필요하면 방장에게 문의해 조건을 다시 확인할 수 있습니다.
              </div>
            </div>
          ) : null}

          {isCompleted ? (
            <div className="mx-5 mt-4 rounded-[16px] bg-[#F5F5F5] p-4">
              <div className="text-[16px] font-black text-[#261912]">번개가 종료됐습니다</div>
              <div className="mt-2 text-[13px] leading-6 text-[#594136]">
                일정 시간이 지나 더 이상 참여 요청이나 승인 처리를 할 수 없습니다.
              </div>
            </div>
          ) : null}

          {isCancelled ? (
            <div className="mx-5 mt-4 rounded-[16px] bg-[#FFF1EE] p-4">
              <div className="text-[16px] font-black text-[#261912]">번개가 취소됐습니다</div>
              <div className="mt-2 text-[13px] leading-6 text-[#594136]">
                취소된 번개에는 새로 참여할 수 없습니다.
              </div>
            </div>
          ) : null}

          {isOrganizer && !isCompleted && !isCancelled ? (
            <div className="mx-5 mt-4 rounded-[16px] bg-white p-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-[15px] font-bold text-[#261912]">요청 관리</div>
                  <div className="mt-1 text-[12px] text-[#8D7164]">대기 중 {pendingCount}명</div>
                </div>
                <button
                  type="button"
                  onClick={() => onOpenApplicants(meetup)}
                  className="rounded-[10px] bg-[#FF6F0F] px-4 py-2 text-[13px] font-bold text-white"
                >
                  요청 보기
                </button>
              </div>
            </div>
          ) : null}
        </div>

        <div className="border-t border-[#E1BFB1] bg-white px-5 py-4">
          {!isOrganizer ? <div className="mb-3 text-[12px] text-[#8D7164]">{actionHelper}</div> : null}
          <div className="flex gap-3">
            {!isOrganizer ? (
              <button
                type="button"
                onClick={() => onOpenInquiryChat(meetup)}
                className="flex-1 rounded-[14px] bg-[#F5F5F5] px-4 py-4 text-[14px] font-semibold text-[#555]"
              >
                방장 문의
              </button>
            ) : null}
            <button
              type="button"
              onClick={() => (myParticipation === 'accepted' ? onOpenGroupChat(meetup) : onJoin(meetup))}
              disabled={shouldDisablePrimaryAction(isOrganizer, myParticipation, meetup.status, isClosed)}
              className={`rounded-[14px] px-4 py-4 text-[15px] font-bold ${
                shouldDisablePrimaryAction(isOrganizer, myParticipation, meetup.status, isClosed)
                  ? 'bg-[#F0F0F0] text-[#AAA]'
                  : 'bg-[#FF6F0F] text-white'
              } ${isOrganizer ? 'flex-1' : 'flex-[1.7]'}`}
            >
              {primaryAction}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function getPrimaryActionLabel(
  isOrganizer: boolean,
  myParticipation: ParticipationStatus,
  joinMethod: Meetup['joinMethod'],
  status: Meetup['status'],
  isClosed: boolean,
) {
  if (isOrganizer) return '운영 중'
  if (myParticipation === 'accepted') return '채팅 보기'
  if (status === 'completed') return '종료됨'
  if (status === 'cancelled') return '취소됨'
  if (myParticipation === 'pending') return '요청 대기중'
  if (myParticipation === 'rejected') return '참여 거절됨'
  if (isClosed) return '모집 마감'
  return joinMethod === 'instant' ? '참여하기' : '요청하기'
}

function shouldDisablePrimaryAction(
  isOrganizer: boolean,
  myParticipation: ParticipationStatus,
  status: Meetup['status'],
  isClosed: boolean,
) {
  return (
    isOrganizer ||
    myParticipation === 'pending' ||
    myParticipation === 'rejected' ||
    status === 'completed' ||
    status === 'cancelled' ||
    (isClosed && myParticipation !== 'accepted')
  )
}

function getPrimaryActionHelper(
  isOrganizer: boolean,
  myParticipation: ParticipationStatus,
  joinMethod: Meetup['joinMethod'],
  status: Meetup['status'],
  isClosed: boolean,
) {
  if (isOrganizer) return '방장은 신청자 관리와 현재 상태만 확인할 수 있습니다.'
  if (myParticipation === 'accepted') return '참여가 확정돼 그룹 채팅으로 이동할 수 있습니다.'
  if (status === 'completed') return '종료된 번개라 새 참여 요청이나 승인 처리를 할 수 없습니다.'
  if (status === 'cancelled') return '취소된 번개라 다시 참여를 열 수 없습니다.'
  if (myParticipation === 'pending') return '방장이 확인하면 참여가 확정됩니다.'
  if (myParticipation === 'rejected') return '현재 거절 상태입니다. 필요하면 방장에게 문의할 수 있습니다.'
  if (isClosed) return '정원이 가득 차 모집이 마감됐습니다.'
  return joinMethod === 'instant'
    ? '누르면 바로 참여가 확정되고 그룹 채팅에 입장합니다.'
    : '누르면 참여 요청이 접수되고 방장 확인 후 확정됩니다.'
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-[11px] text-[#B0B0B0]">{label}</div>
      <div className="mt-1 text-[14px] font-bold text-[#261912]">{value}</div>
    </div>
  )
}

function Badge({
  children,
  tone,
}: {
  children: ReactNode
  tone: 'brand' | 'success' | 'neutral' | 'info' | 'danger'
}) {
  const className =
    tone === 'brand'
      ? 'bg-[#FFF5EE] text-[#FF6F0F]'
      : tone === 'success'
        ? 'bg-[#E8F5E9] text-[#15803D]'
        : tone === 'info'
          ? 'bg-[#E8F5FE] text-[#1D4ED8]'
          : tone === 'danger'
            ? 'bg-[#FFE8E5] text-[#B91C1C]'
            : 'bg-[#F5F5F5] text-[#AAA]'

  return <span className={`rounded-full px-3 py-1 text-[11px] font-bold ${className}`}>{children}</span>
}

function MemberRow({
  name,
  avatar,
  gradient,
  meta,
  badge,
  badgeTone,
}: {
  name: string
  avatar: string
  gradient: string
  meta: string
  badge: string
  badgeTone: 'brand' | 'neutral'
}) {
  return (
    <div className="flex items-center gap-3">
      <div
        className="flex h-10 w-10 items-center justify-center rounded-full text-[14px] text-white"
        style={{ backgroundImage: gradient }}
      >
        {avatar}
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-[14px] font-bold text-[#261912]">{name}</div>
        <div className="mt-1 text-[12px] text-[#8D7164]">{meta}</div>
      </div>
      <span
        className={`rounded-full px-3 py-1 text-[11px] font-bold ${
          badgeTone === 'brand' ? 'bg-[#FFF5EE] text-[#FF6F0F]' : 'bg-[#F5F5F5] text-[#8D7164]'
        }`}
      >
        {badge}
      </span>
    </div>
  )
}
