import type { FormEvent } from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export function CourseReviewWorkspacePage() {
  const navigate = useNavigate()
  const [courseId, setCourseId] = useState('')

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!courseId.trim()) {
      return
    }

    navigate(`/courses/${courseId.trim()}`)
  }

  return (
    <main className="min-h-screen bg-[#fff7f1] text-[#2f1c11]">
      <div className="mx-auto flex min-h-screen w-full max-w-[460px] flex-col px-4 pb-8 pt-5">
        <section className="rounded-[32px] bg-[linear-gradient(135deg,#ff8a3d_0%,#ff9f58_50%,#ffd6b1_100%)] px-5 py-6 text-white shadow-[0_24px_60px_rgba(242,124,42,0.28)]">
          <p className="text-sm font-medium text-white/78">Running Olle</p>
          <h1 className="mt-2 text-[30px] font-semibold leading-tight">
            코스 후기 PWA 작업 화면
          </h1>
          <p className="mt-3 text-sm leading-6 text-white/88">
            실제 앱에서는 코스 상세에서 진입하지만, 지금은 개발 중이라 코스 ID를
            입력해 바로 후기 화면으로 이동합니다.
          </p>
        </section>

        <section className="mt-5 rounded-[28px] bg-white px-5 py-5 shadow-[0_18px_44px_rgba(77,46,23,0.08)] ring-1 ring-[#f0ddd0]">
          <p className="text-sm font-semibold text-[#3b2517]">코스 진입</p>
          <p className="mt-2 text-sm leading-6 text-[#8a6a56]">
            실제 코스 UUID를 넣으면 모바일 후기 화면으로 바로 이동합니다.
          </p>

          <form className="mt-5" onSubmit={handleSubmit}>
            <label className="block">
              <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#a47959]">
                courseId
              </span>
              <input
                value={courseId}
                onChange={(event) => setCourseId(event.target.value)}
                placeholder="예: 7a9c3f7e-9c51-4fb2-a63a-3f8d8b7bfe21"
                className="w-full rounded-2xl border border-[#eeded2] bg-[#fffdfa] px-4 py-3 text-sm text-[#3d281a] outline-none transition focus:border-[#ff9c5a] focus:ring-4 focus:ring-[#ffd9bc]"
              />
            </label>

            <button
              type="submit"
              className="mt-5 inline-flex h-12 w-full items-center justify-center rounded-full bg-[#ff7e33] px-5 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(255,126,51,0.28)]"
            >
              후기 화면 열기
            </button>
          </form>
        </section>

        <section className="mt-4 rounded-[24px] bg-white px-5 py-5 shadow-[0_18px_44px_rgba(77,46,23,0.08)] ring-1 ring-[#f0ddd0]">
          <p className="text-sm font-semibold text-[#3b2517]">이번 화면에 포함된 것</p>
          <ul className="mt-3 space-y-2 text-sm leading-6 text-[#5f4433]">
            <li>평균 평점과 후기 수 요약</li>
            <li>후기 카드 리스트</li>
            <li>모바일 바텀시트 기반 작성 / 수정</li>
            <li>개발용 액세스 토큰 저장 패널</li>
          </ul>
        </section>
      </div>
    </main>
  )
}
