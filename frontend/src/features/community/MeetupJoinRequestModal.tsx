import { Icon, Button } from '../../components/ui'
import { FullScreenPage } from '../../components/layout/FullScreenPage'
import type { Meetup } from './communityTypes'

export function MeetupJoinRequestModal({
  meetup,
  onClose,
  onOpenInquiry,
}: {
  meetup: Meetup
  onClose: () => void
  onOpenInquiry: (meetup: Meetup) => void
}) {
  return (
    <div className="community-backdrop" onClick={onClose}>
      <FullScreenPage
        scroll={false} role="dialog" aria-modal="true" aria-label="참여 요청 완료" className="community-dialog"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="community-dialog-header">
          <Button variant="ghost" size="sm"
            type="button"
            onClick={onClose}
            className="flex h-11 w-11 items-center justify-center rounded-control bg-surface-subtle text-app-title"
           aria-label="닫기">
            <Icon name="arrowLeft" />
          </Button>
          <div className="text-app-title font-bold text-ink">요청 완료</div>
        </div>

        <div className="community-request-body flex flex-1 flex-col items-center px-5 text-center">
          <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-success-subtle text-success">
            <Icon name="check" />
          </div>
          <div className="mt-5 text-section-title font-extrabold leading-relaxed text-ink">요청이 접수됐습니다</div>
          <div className="mt-2 text-body-sm leading-7 text-ink-secondary">
            방장이 수락하면 채팅방에 초대됩니다.
            <br />
            결과는 알림으로 안내됩니다.
          </div>

          <div className="mt-8 w-full rounded-md bg-surface-subtle px-5 py-5 text-left">
            <div className="text-card-title font-extrabold text-ink">{meetup.title}</div>
            <div className="mt-4 space-y-2 text-label">
              <InfoRow label="일시" value={meetup.scheduleLabel} />
              <InfoRow label="방장" value={meetup.organizerName} />
              <InfoRow label="집결" value={meetup.locationLabel} />
            </div>
            <div className="mt-4 flex items-center justify-center gap-2 border-t border-border-subtle pt-4">
              <span className="h-2 w-2 rounded-full bg-warning" />
              <span className="text-label font-bold text-warning">방장 수락 대기 중</span>
            </div>
          </div>
        </div>

        <div className="community-dialog-footer">
          <Button variant="ghost" size="sm"
            type="button"
            onClick={() => onOpenInquiry(meetup)}
            className="mb-2 h-13 w-full rounded-control bg-surface-subtle text-body font-semibold text-ink-secondary"
          >
            방장에게 문의하기
          </Button>
          <Button variant="primary" size="sm"
            type="button"
            onClick={onClose}
            className="h-13 w-full rounded-control bg-brand-500 text-body font-bold text-surface"
          >
            번개 목록으로 돌아가기
          </Button>
        </div>
      </FullScreenPage>
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-ink-secondary">{label}</span>
      <span className="font-semibold text-ink">{value}</span>
    </div>
  )
}
