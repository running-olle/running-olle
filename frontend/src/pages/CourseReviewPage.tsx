import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ACCESS_TOKEN_KEY } from '../api/axiosInstance'
import { getCourseReviews, issueTestToken } from '../api/courseReviews'
import { CourseReviewSection } from '../components/review/CourseReviewSection'
import type { CourseReviewListResponse } from '../types/courseReview'

const DEFAULT_KAKAO_ID = 'test-kakao-user-1'

export function CourseReviewPage() {
  const navigate = useNavigate()
  const { courseId = '' } = useParams()
  const [accessToken, setAccessToken] = useState(
    () => localStorage.getItem(ACCESS_TOKEN_KEY) ?? '',
  )
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isIssuingToken, setIsIssuingToken] = useState(false)
  const [isToolsOpen, setIsToolsOpen] = useState(false)
  const [kakaoId, setKakaoId] = useState(DEFAULT_KAKAO_ID)
  const [reviewsState, setReviewsState] = useState<CourseReviewListResponse | null>(
    null,
  )

  const courseMeta = useMemo(
    () => ({
      difficulty: '보통',
      distance: '8.2km',
      location: '제주 애월 해안',
      subtitle: '후기 중심으로 읽는 코스 상세',
      title: '제주 해안 러닝 코스',
      vibe: '해안 도로 · 노을 뷰',
    }),
    [],
  )

  const loadReviews = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage(null)

    try {
      const response = await getCourseReviews(courseId)
      setReviewsState(response)
    } catch (error) {
      setReviewsState(null)
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }, [courseId])

  useEffect(() => {
    void loadReviews()
  }, [loadReviews])

  const myReview = reviewsState?.reviews.find((review) => review.isMine) ?? null
  const ratingAvg = formatRating(reviewsState?.ratingAvg)
  const reviewCount = reviewsState?.reviewCount ?? 0

  const handleIssueTestToken = async () => {
    setIsIssuingToken(true)

    try {
      const response = await issueTestToken({ kakaoId: kakaoId.trim() })
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
      setAccessToken(response.accessToken)
      await loadReviews()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsIssuingToken(false)
    }
  }

  return (
    <main className="min-h-screen bg-[#fff7f1] text-[#2f1c11]">
      <div className="mx-auto flex min-h-screen w-full max-w-[460px] flex-col px-4 pb-[112px] pt-4">
        <header className="sticky top-0 z-20 -mx-4 bg-[#fff7f1]/92 px-4 pb-4 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="flex h-11 w-11 items-center justify-center rounded-full bg-white text-lg text-[#7d5438] shadow-[0_10px_24px_rgba(92,52,24,0.08)] ring-1 ring-[#f0ddd0]"
              aria-label="뒤로가기"
            >
              ←
            </button>

            <div className="rounded-full bg-white px-4 py-2 text-xs font-semibold text-[#9b704d] shadow-[0_10px_24px_rgba(92,52,24,0.08)] ring-1 ring-[#f0ddd0]">
              코스 후기
            </div>

            <button
              type="button"
              onClick={() => setIsToolsOpen((prev) => !prev)}
              className="flex h-11 w-11 items-center justify-center rounded-full bg-white text-sm font-semibold text-[#7d5438] shadow-[0_10px_24px_rgba(92,52,24,0.08)] ring-1 ring-[#f0ddd0]"
              aria-label="개발 도구"
            >
              ⋯
            </button>
          </div>
        </header>

        <section className="overflow-hidden rounded-[28px] bg-[linear-gradient(135deg,#ff8a3d_0%,#ff9f58_48%,#ffd6b1_100%)] px-5 pb-5 pt-6 text-white shadow-[0_24px_60px_rgba(242,124,42,0.28)]">
          <p className="text-sm font-medium text-white/78">{courseMeta.location}</p>
          <h1 className="mt-2 text-[28px] font-semibold leading-tight">
            {courseMeta.title}
          </h1>
          <p className="mt-2 text-sm text-white/84">{courseMeta.subtitle}</p>

          <div className="mt-5 grid grid-cols-3 gap-3">
            <HeroMetric label="거리" value={courseMeta.distance} />
            <HeroMetric label="난이도" value={courseMeta.difficulty} />
            <HeroMetric label="분위기" value={courseMeta.vibe} />
          </div>

          <div className="mt-5 rounded-[24px] bg-white/16 px-4 py-4 backdrop-blur-sm">
            <div className="flex items-end justify-between gap-4">
              <div>
                <p className="text-xs font-medium uppercase tracking-[0.14em] text-white/72">
                  Review Snapshot
                </p>
                <div className="mt-2 flex items-end gap-2">
                  <span className="text-[34px] font-semibold leading-none">
                    {ratingAvg}
                  </span>
                  <span className="pb-1 text-sm text-white/82">
                    후기 {reviewCount}개
                  </span>
                </div>
              </div>

              <div className="rounded-full bg-white/18 px-3 py-1.5 text-xs font-semibold text-white">
                {myReview ? '내 후기 있음' : '첫 후기 가능'}
              </div>
            </div>
          </div>
        </section>

        {isToolsOpen ? (
          <section className="mt-4 rounded-[24px] bg-white px-4 py-4 shadow-[0_14px_34px_rgba(77,46,23,0.08)] ring-1 ring-[#f0ddd0]">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-[#3b2517]">개발 도구</p>
                <p className="mt-1 text-xs leading-5 text-[#8b6b57]">
                  Postman 없이 이 화면에서 테스트 토큰을 발급하고 바로 저장할 수
                  있습니다.
                </p>
              </div>
              <Link
                to="/"
                className="rounded-full bg-[#fff4eb] px-3 py-2 text-xs font-semibold text-[#c76524]"
              >
                코스 변경
              </Link>
            </div>

            <label className="mt-4 block">
              <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#a47959]">
                Kakao ID
              </span>
              <input
                value={kakaoId}
                onChange={(event) => setKakaoId(event.target.value)}
                placeholder="예: test-kakao-user-1"
                className="w-full rounded-2xl border border-[#eeded2] bg-[#fffdfa] px-4 py-3 text-sm text-[#3d281a] outline-none transition focus:border-[#ff9c5a] focus:ring-4 focus:ring-[#ffd9bc]"
              />
            </label>

            <div className="mt-3 flex gap-2">
              <button
                type="button"
                onClick={() => void handleIssueTestToken()}
                disabled={isIssuingToken || !kakaoId.trim()}
                className="inline-flex h-10 items-center justify-center rounded-full bg-[#ff7e33] px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-[#f6b184]"
              >
                {isIssuingToken ? '발급 중...' : '테스트 토큰 발급'}
              </button>
              <button
                type="button"
                onClick={() => {
                  localStorage.removeItem(ACCESS_TOKEN_KEY)
                  setAccessToken('')
                  void loadReviews()
                }}
                className="inline-flex h-10 items-center justify-center rounded-full border border-[#ead4c5] px-4 text-sm font-semibold text-[#855c42]"
              >
                토큰 제거
              </button>
            </div>

            <label className="mt-4 block">
              <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#a47959]">
                Access Token
              </span>
              <textarea
                rows={4}
                value={accessToken}
                onChange={(event) => setAccessToken(event.target.value)}
                placeholder="발급된 access token"
                className="w-full resize-none rounded-2xl border border-[#eeded2] bg-[#fffdfa] px-4 py-3 text-sm leading-6 text-[#3d281a] outline-none transition focus:border-[#ff9c5a] focus:ring-4 focus:ring-[#ffd9bc]"
              />
            </label>

            <button
              type="button"
              onClick={() => {
                localStorage.setItem(ACCESS_TOKEN_KEY, accessToken.trim())
                void loadReviews()
              }}
              disabled={!accessToken.trim()}
              className="mt-3 inline-flex h-10 items-center justify-center rounded-full border border-[#ead4c5] px-4 text-sm font-semibold text-[#855c42] disabled:cursor-not-allowed disabled:opacity-50"
            >
              현재 토큰 다시 저장
            </button>
          </section>
        ) : null}

        <section className="mt-4 flex flex-col gap-4">
          <div className="rounded-[24px] bg-white px-4 py-4 shadow-[0_14px_34px_rgba(77,46,23,0.08)] ring-1 ring-[#f0ddd0]">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-[#3b2517]">이 코스의 후기</p>
                <p className="mt-1 text-sm leading-6 text-[#8a6a56]">
                  실제 완주 기록을 바탕으로 남긴 후기만 모아 보여줍니다.
                </p>
              </div>
              <div className="rounded-full bg-[#fff4eb] px-3 py-1.5 text-xs font-semibold text-[#c76524]">
                {courseId.slice(0, 8)}
              </div>
            </div>
          </div>

          <CourseReviewSection
            courseId={courseId}
            errorMessage={errorMessage}
            isLoading={isLoading}
            onRefresh={loadReviews}
            reviewsState={reviewsState}
            showPrimaryAction={false}
          />
        </section>
      </div>

      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[460px] bg-[linear-gradient(180deg,rgba(255,247,241,0)_0%,#fff7f1_24%,#fff7f1_100%)] px-4 pb-5 pt-8">
        <div className="rounded-[28px] bg-white/96 px-4 py-3 shadow-[0_20px_45px_rgba(58,33,16,0.12)] ring-1 ring-[#f0ddd0] backdrop-blur-md">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#a47959]">
                Quick Action
              </p>
              <p className="mt-1 text-sm text-[#6d4d39]">
                {myReview
                  ? '수정은 기존 카드에서, 새 기록으로는 새 후기를 남길 수 있습니다.'
                  : '완주 기록이 있다면 바로 후기를 남길 수 있습니다.'}
              </p>
            </div>
            <button
              type="button"
              onClick={() => {
                const trigger = document.querySelector<HTMLButtonElement>(
                  '[data-review-create-trigger="true"]',
                )
                trigger?.click()
              }}
              className="inline-flex h-12 shrink-0 items-center justify-center rounded-full bg-[#ff7e33] px-5 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(255,126,51,0.3)]"
            >
              후기 작성
            </button>
          </div>
        </div>
      </div>
    </main>
  )
}

function HeroMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[20px] bg-white/16 px-3 py-3 backdrop-blur-sm">
      <p className="text-[11px] font-medium text-white/72">{label}</p>
      <p className="mt-1 text-sm font-semibold leading-5 text-white">{value}</p>
    </div>
  )
}

function extractErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = Reflect.get(error, 'response')

    if (typeof response === 'object' && response !== null) {
      if ('status' in response) {
        const status = Reflect.get(response, 'status')

        if (status === 401) {
          return '인증이 필요합니다. 개발 도구에서 테스트 토큰을 발급한 뒤 다시 시도하세요.'
        }

        if (status === 404) {
          return '사용자 또는 후기 데이터를 찾지 못했습니다. kakaoId와 코스 ID를 확인하세요.'
        }
      }

      if ('data' in response) {
        const data = Reflect.get(response, 'data')

        if (typeof data === 'string' && data.trim()) {
          return data
        }

        if (
          typeof data === 'object' &&
          data !== null &&
          'message' in data &&
          typeof Reflect.get(data, 'message') === 'string'
        ) {
          return Reflect.get(data, 'message') as string
        }
      }
    }
  }

  return '후기 데이터를 불러오지 못했습니다. 서버 연결과 응답 형식을 확인하세요.'
}

function formatRating(value?: number | string) {
  if (typeof value === 'number') {
    return value.toFixed(2)
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? value : parsed.toFixed(2)
  }

  return '0.00'
}
