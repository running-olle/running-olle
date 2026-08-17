type ReviewSummaryPanelProps = {
  hasMyReview: boolean
  onWriteReview: () => void
  ratingAvg: string
  reviewCount: number
  showActionButton?: boolean
}

export function ReviewSummaryPanel({
  hasMyReview,
  onWriteReview,
  ratingAvg,
  reviewCount,
  showActionButton = true,
}: ReviewSummaryPanelProps) {
  return (
    <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-sm font-medium text-[#a47858]">Course Reviews</p>
        <div className="mt-2 flex flex-wrap items-end gap-3">
          <div className="text-[36px] font-semibold leading-none text-[#2f1c11]">
            {ratingAvg}
          </div>
          <div className="pb-1 text-sm text-[#8e6a53]">후기 {reviewCount}개</div>
        </div>
        <p className="mt-3 text-sm leading-6 text-[#866854]">
          러닝 기록을 바탕으로 코스 체감 난이도와 풍경, 다시 뛰고 싶은 이유를
          빠르게 확인할 수 있습니다.
        </p>
      </div>

      {showActionButton ? (
        <button
          type="button"
          onClick={onWriteReview}
          className="inline-flex h-11 items-center justify-center rounded-full bg-[#ff7e33] px-5 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(255,126,51,0.28)] transition hover:bg-[#f06e20]"
        >
          {hasMyReview ? '다른 기록으로 후기 작성' : '후기 작성'}
        </button>
      ) : null}
    </div>
  )
}
