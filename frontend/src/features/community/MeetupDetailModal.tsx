import { Icon, Badge, Button } from '../../components/ui'
import { FullScreenPage } from '../../components/layout/FullScreenPage'
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
    <div className="community-backdrop" onClick={onClose}>
      <FullScreenPage scroll={false} role="dialog" aria-modal="true" aria-label="번개 상세" className="community-dialog" onClick={(event) => event.stopPropagation()}>
        <div className="community-dialog-header">
          <Button variant="ghost" size="sm"
            type="button"
            onClick={onClose}
            className="flex h-11 w-11 items-center justify-center rounded-control bg-surface-subtle text-app-title text-ink"
           aria-label="닫기">
            <Icon name="arrowLeft" />
          </Button>
          <div className="text-app-title font-bold text-ink">번개 상세</div>
          <div className="ml-auto flex gap-2">
            <Button variant="ghost" size="sm"
              type="button"
              onClick={() => onShare(meetup)}
              className="flex h-8 w-8 items-center justify-center rounded-sm bg-surface-subtle text-body-sm text-ink-secondary"
            >
              <Icon name="share" />
            </Button>
            {isOrganizer && !isCompleted && !isCancelled ? (
              <Button variant="ghost" size="sm"
                type="button"
                onClick={() => onEdit(meetup)}
                className="flex h-8 items-center justify-center rounded-sm bg-surface-subtle px-3 text-caption font-bold text-ink-secondary"
              >
                수정
              </Button>
            ) : null}
            {isOrganizer && !isCompleted ? (
              <Button variant="danger" size="sm"
                type="button"
                onClick={() => onDelete(meetup)}
                className="flex h-8 items-center justify-center rounded-sm bg-danger-subtle px-3 text-caption font-bold text-danger"
              >
                삭제
              </Button>
            ) : null}
          </div>
        </div>

        <div className="community-dialog-body flex-1 overflow-y-auto pb-6">
          <div className="px-5 pt-4">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="brand">번개</Badge>
              <Badge variant={statusTone}>{statusLabel}</Badge>
              <Badge variant={meetup.joinMethod === 'instant' ? 'info' : 'brand'}>
                {meetup.joinMethod === 'instant' ? '즉시 참여' : '수락 후 참여'}
              </Badge>
            </div>

            <div className="mt-3 text-section-title font-extrabold leading-relaxed text-ink">{meetup.title}</div>

            <div className="mt-3 flex items-center gap-2">
              <div
                className="flex h-8 w-8 items-center justify-center rounded-full text-label text-surface"
                style={{ backgroundImage: meetup.organizerGradient }}
              >
                {meetup.organizerAvatar}
              </div>
              <div className="text-label text-ink-secondary">
                {meetup.organizerName} · {meetup.createdAtLabel}
              </div>
            </div>
          </div>

          <div className="mx-5 mt-4 overflow-hidden rounded-md bg-surface-subtle">
            <KakaoPointMap
              lat={meetup.meetingLatitude}
              lng={meetup.meetingLongitude}
              label={meetup.locationLabel}
              className="h-[180px]"
            />
            <div className="px-4 py-3 text-caption font-normal text-ink-secondary">
              {meetup.locationLabel} · {meetup.meetingLatitude.toFixed(4)}, {meetup.meetingLongitude.toFixed(4)}
            </div>
          </div>

          {meetup.course ? (
            <button
              type="button"
              onClick={() => onOpenCourse(meetup.course!.id)}
              className="mx-5 mt-4 flex items-center gap-3 rounded-md bg-surface-subtle px-4 py-4 text-left"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-control bg-brand-500 text-app-title text-surface">
                {meetup.course.icon}
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate text-body-sm font-bold text-ink">{meetup.course.name}</div>
                <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-caption text-ink-secondary">
                  <span>{meetup.course.distanceKm}km</span>
                  <span>{meetup.course.durationMinutes}분</span>
                  <span>{meetup.course.difficultyLabel}</span>
                </div>
              </div>
              <span className="text-caption font-bold text-brand-500">코스 보기</span>
            </button>
          ) : null}

          {meetup.joinMethod === 'approval' && !isCompleted && !isCancelled ? (
            <div className="mx-5 mt-4 rounded-md bg-surface-subtle px-4 py-4 text-caption leading-6 text-brand-700">
              이 번개는 방장 수락 후 참여가 확정됩니다. 요청이 승인되면 그룹 채팅에 자동 입장합니다.
            </div>
          ) : null}

          <div className="mx-5 mt-4 rounded-md bg-surface p-4">
            <div className="grid grid-cols-2 gap-3">
              <InfoItem label="일시" value={meetup.scheduleLabel} />
              <InfoItem label="목표 페이스" value={meetup.targetPaceLabel} />
              <InfoItem label="집결 장소" value={meetup.locationLabel} />
              <InfoItem label="정원" value={`${acceptedCount}/${meetup.maxParticipants}명`} />
            </div>
          </div>

          <div className="mx-5 mt-4 rounded-md bg-surface p-4">
            <div className="text-body font-bold text-ink">번개 소개</div>
            <p className="mt-2 text-body-sm leading-7 text-ink-secondary">{meetup.description}</p>
          </div>

          <div className="mx-5 mt-4 rounded-md bg-surface p-4">
            <div className="flex items-center justify-between">
              <div className="text-body font-bold text-ink">참여 멤버</div>
              <div className="text-caption text-ink-secondary">
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
                <div className="rounded-control bg-surface-subtle px-4 py-4 text-caption text-ink-tertiary">
                  아직 확정된 참여자가 없습니다.
                </div>
              ) : null}
            </div>
          </div>

          {!isOrganizer && myParticipation === 'pending' ? (
            <div className="mx-5 mt-4 rounded-md bg-surface-subtle p-4">
              <div className="text-card-title font-extrabold text-ink">요청 대기 중</div>
              <div className="mt-2 text-label leading-6 text-ink-secondary">
                방장이 확인하면 참여 여부가 결정됩니다.
              </div>
            </div>
          ) : null}

          {myParticipation === 'rejected' ? (
            <div className="mx-5 mt-4 rounded-md bg-danger-subtle p-4">
              <div className="text-card-title font-extrabold text-ink">참여가 거절됐습니다</div>
              <div className="mt-2 text-label leading-6 text-ink-secondary">
                필요하면 방장에게 문의해 조건을 다시 확인할 수 있습니다.
              </div>
            </div>
          ) : null}

          {isCompleted ? (
            <div className="mx-5 mt-4 rounded-md bg-surface-subtle p-4">
              <div className="text-card-title font-extrabold text-ink">번개가 종료됐습니다</div>
              <div className="mt-2 text-label leading-6 text-ink-secondary">
                일정 시간이 지나 더 이상 참여 요청이나 승인 처리를 할 수 없습니다.
              </div>
            </div>
          ) : null}

          {isCancelled ? (
            <div className="mx-5 mt-4 rounded-md bg-danger-subtle p-4">
              <div className="text-card-title font-extrabold text-ink">번개가 취소됐습니다</div>
              <div className="mt-2 text-label leading-6 text-ink-secondary">
                취소된 번개에는 새로 참여할 수 없습니다.
              </div>
            </div>
          ) : null}

          {isOrganizer && !isCompleted && !isCancelled ? (
            <div className="mx-5 mt-4 rounded-md bg-surface p-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-body font-bold text-ink">요청 관리</div>
                  <div className="mt-1 text-caption text-ink-secondary">대기 중 {pendingCount}명</div>
                </div>
                <Button variant="primary" size="sm"
                  type="button"
                  onClick={() => onOpenApplicants(meetup)}
                  className="rounded-control bg-brand-500 px-4 py-2 text-label font-bold text-surface"
                >
                  요청 보기
                </Button>
              </div>
            </div>
          ) : null}
        </div>

        <div className="community-dialog-footer">
          {!isOrganizer ? <div className="mb-3 text-caption text-ink-secondary">{actionHelper}</div> : null}
          <div className="flex gap-3">
            {!isOrganizer ? (
              <Button variant="ghost" size="sm"
                type="button"
                onClick={() => onOpenInquiryChat(meetup)}
                className="flex-1 rounded-control bg-surface-subtle px-4 py-4 text-body-sm font-semibold text-ink-secondary"
              >
                방장 문의
              </Button>
            ) : null}
            <Button variant="primary" size="sm"
              type="button"
              onClick={() => (myParticipation === 'accepted' ? onOpenGroupChat(meetup) : onJoin(meetup))}
              disabled={shouldDisablePrimaryAction(isOrganizer, myParticipation, meetup.status, isClosed)}
              className={`rounded-control px-4 py-4 text-body font-bold ${
                shouldDisablePrimaryAction(isOrganizer, myParticipation, meetup.status, isClosed)
                  ? 'bg-surface-muted text-ink-tertiary'
                  : 'bg-brand-500 text-surface'
              } ${isOrganizer ? 'flex-1' : 'flex-[1.7]'}`}
            >
              {primaryAction}
            </Button>
          </div>
        </div>
      </FullScreenPage>
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
      <div className="text-caption text-ink-secondary">{label}</div>
      <div className="mt-1 text-body-sm font-bold text-ink">{value}</div>
    </div>
  )
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
        className="flex h-10 w-10 items-center justify-center rounded-full text-body-sm text-surface"
        style={{ backgroundImage: gradient }}
      >
        {avatar}
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-body-sm font-bold text-ink">{name}</div>
        <div className="mt-1 text-caption text-ink-secondary">{meta}</div>
      </div>
      <span
        className={`rounded-full px-3 py-1 text-caption font-bold ${
          badgeTone === 'brand' ? 'bg-surface-subtle text-brand-500' : 'bg-surface-subtle text-ink-secondary'
        }`}
      >
        {badge}
      </span>
    </div>
  )
}
