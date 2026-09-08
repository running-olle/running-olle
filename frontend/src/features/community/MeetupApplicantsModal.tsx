import { Icon, Button } from '../../components/ui'
import { FullScreenPage } from '../../components/layout/FullScreenPage'
import type { Meetup } from './communityTypes'

export function MeetupApplicantsModal({
  meetup,
  onClose,
  onAccept,
  onReject,
}: {
  meetup: Meetup
  onClose: () => void
  onAccept: (meetupId: string, participantId: string) => void
  onReject: (meetupId: string, participantId: string) => void
}) {
  const pendingApplicants = meetup.applicants.filter((item) => item.status === 'pending')
  const acceptedApplicants = meetup.applicants.filter((item) => item.status === 'accepted')

  return (
    <div className="community-backdrop" onClick={onClose}>
      <FullScreenPage scroll={false} role="dialog" aria-modal="true" aria-label="신청자 관리" className="community-dialog" onClick={(event) => event.stopPropagation()}>
        <div className="community-dialog-header">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="sm"
              type="button"
              onClick={onClose}
              className="flex h-11 w-11 items-center justify-center rounded-control bg-surface-subtle text-app-title"
             aria-label="닫기">
              <Icon name="arrowLeft" />
            </Button>
            <div className="text-app-title font-bold text-ink">요청자 관리</div>
          </div>
        </div>

        <div className="border-b border-border-subtle bg-surface px-5 py-4">
          <div className="text-body-sm font-bold text-ink">{meetup.title}</div>
          <div className="mt-1 text-caption text-ink-secondary">
            {meetup.scheduleLabel} · 모집 {meetup.participantIds.length}/{meetup.maxParticipants}
          </div>
        </div>

        <div className="community-dialog-body flex-1 overflow-y-auto bg-surface-subtle px-5 py-4">
          <div className="mb-3 text-label font-bold text-ink-secondary">대기 중인 요청 {pendingApplicants.length}명</div>

          {pendingApplicants.length === 0 ? (
            <div className="rounded-md bg-surface p-4 text-caption text-ink-secondary">현재 대기 중인 요청이 없습니다.</div>
          ) : null}

          {pendingApplicants.map((applicant) => (
            <div key={applicant.id} className="mb-3 rounded-md bg-surface p-4">
              <div className="flex items-center gap-3">
                <div
                  className="flex h-10 w-10 items-center justify-center rounded-full text-body-sm text-surface"
                  style={{ backgroundImage: applicant.gradient }}
                >
                  {applicant.avatar}
                </div>
                <div className="flex-1">
                  <div className="text-body-sm font-bold text-ink">{applicant.nickname}</div>
                  <div className="mt-1 text-caption text-ink-secondary">참여 요청 대기</div>
                </div>
              </div>
              <div className="mt-4 grid grid-cols-3 rounded-control border border-border-subtle">
                <StatItem label="누적 거리" value={`${applicant.stats.totalDistanceKm}km`} />
                <StatItem label="평균 페이스" value={applicant.stats.averagePaceText} />
                <StatItem label="번개 참여" value={`${applicant.stats.meetupCount}회`} />
              </div>
              <div className="mt-4 flex gap-2">
                <Button variant="danger" size="sm"
                  type="button"
                  onClick={() => onReject(meetup.id, applicant.id)}
                  className="flex-1 rounded-control bg-danger-subtle py-3 text-body-sm font-bold text-danger"
                >
                  거절
                </Button>
                <Button variant="primary" size="sm"
                  type="button"
                  onClick={() => onAccept(meetup.id, applicant.id)}
                  className="flex-[1.7] rounded-control bg-brand-500 py-3 text-body-sm font-bold text-surface"
                >
                  수락하기
                </Button>
              </div>
            </div>
          ))}

          <div className="mt-4 text-label font-bold text-ink-secondary">확정 멤버 {acceptedApplicants.length}명</div>
          <div className="mt-2 rounded-md bg-surface p-4">
            {acceptedApplicants.length === 0 ? (
              <div className="text-caption text-ink-secondary">아직 확정된 멤버가 없습니다.</div>
            ) : (
              acceptedApplicants.map((member) => (
                <div key={member.id} className="flex items-center gap-3 py-2">
                  <div
                    className="flex h-10 w-10 items-center justify-center rounded-full text-body-sm text-surface"
                    style={{ backgroundImage: member.gradient }}
                  >
                    {member.avatar}
                  </div>
                  <div className="flex-1">
                    <div className="text-body-sm font-bold text-ink">{member.nickname}</div>
                    <div className="mt-1 text-caption text-ink-secondary">참여 확정</div>
                  </div>
                  <span className="rounded-full bg-surface-subtle px-3 py-1 text-caption font-bold text-brand-500">멤버</span>
                </div>
              ))
            )}
          </div>
        </div>
      </FullScreenPage>
    </div>
  )
}

function StatItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-r border-border-subtle px-2 py-3 text-center last:border-r-0">
      <div className="text-body-sm font-bold text-ink">{value}</div>
      <div className="mt-1 text-caption text-ink-secondary">{label}</div>
    </div>
  )
}
