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
    <div className="fixed inset-0 z-40 bg-[rgba(38,25,18,0.45)]" onClick={onClose}>
      <div
        className="mx-auto flex h-dvh max-w-[430px] flex-col bg-canvas"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center gap-3 border-b border-border-subtle bg-surface px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="flex h-[34px] w-[34px] items-center justify-center rounded-[10px] bg-[#F5F5F5] text-[17px]"
          >
            ←
          </button>
          <div className="text-[17px] font-bold text-[#261912]">요청 완료</div>
        </div>

        <div className="flex flex-1 flex-col items-center justify-center px-5 text-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-[linear-gradient(135deg,#FFF5EE,#FFDCBD)] text-[34px]">
            ✓
          </div>
          <div className="mt-5 text-[22px] font-black leading-[1.3] text-[#261912]">요청이 접수됐습니다</div>
          <div className="mt-2 text-[14px] leading-7 text-[#8D7164]">
            방장이 수락하면 채팅방에 초대됩니다.
            <br />
            결과는 알림으로 안내됩니다.
          </div>

          <div className="mt-8 w-full rounded-[18px] bg-[#FFF5EE] px-5 py-5 text-left">
            <div className="text-[16px] font-black text-[#261912]">{meetup.title}</div>
            <div className="mt-4 space-y-2 text-[13px]">
              <InfoRow label="일시" value={meetup.scheduleLabel} />
              <InfoRow label="방장" value={meetup.organizerName} />
              <InfoRow label="집결" value={meetup.locationLabel} />
            </div>
            <div className="mt-4 flex items-center justify-center gap-2 border-t border-[rgba(255,111,15,0.18)] pt-4">
              <span className="h-2 w-2 rounded-full bg-[#FF9A00]" />
              <span className="text-[13px] font-bold text-[#FF9A00]">방장 수락 대기 중</span>
            </div>
          </div>
        </div>

        <div className="border-t border-[#E1BFB1] bg-white px-5 py-4">
          <button
            type="button"
            onClick={() => onOpenInquiry(meetup)}
            className="mb-2 h-[52px] w-full rounded-[14px] bg-[#F5F5F5] text-[15px] font-semibold text-[#444]"
          >
            방장에게 문의하기
          </button>
          <button
            type="button"
            onClick={onClose}
            className="h-[52px] w-full rounded-[14px] bg-[#FF6F0F] text-[15px] font-bold text-white"
          >
            번개 목록으로 돌아가기
          </button>
        </div>
      </div>
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-[#8D7164]">{label}</span>
      <span className="font-semibold text-[#261912]">{value}</span>
    </div>
  )
}
