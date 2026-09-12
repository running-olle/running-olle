import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { tourismEventApi } from '../../features/home/tourismEventApi'
import type { TourismEvent } from '../../features/home/types'

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'short',
})

function formatDateRange(event: TourismEvent) {
  const startDate = dateFormatter.format(new Date(`${event.eventStartDate}T00:00:00`))
  const endDate = dateFormatter.format(new Date(`${event.eventEndDate}T00:00:00`))
  return startDate === endDate ? startDate : `${startDate} ~ ${endDate}`
}

function ddayLabel(event: TourismEvent) {
  if (event.status === '진행 중') {
    return '진행 중'
  }
  if (event.status === '종료') {
    return `종료 D+${Math.abs(event.dday)}`
  }
  if (event.dday === 0) {
    return '오늘 시작'
  }
  return `D-${event.dday}`
}

function cleanExternalUrl(value: string | null) {
  if (!value) return null
  const hrefMatch = value.match(/href=["']([^"']+)["']/i)
  const candidate = (hrefMatch?.[1] ?? value).replace(/<[^>]+>/g, '').trim()
  if (!candidate.startsWith('http://') && !candidate.startsWith('https://')) {
    return null
  }
  return candidate
}

export function TourismEventDetailPage() {
  const navigate = useNavigate()
  const { eventId } = useParams()
  const [event, setEvent] = useState<TourismEvent | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (!eventId) return
    let ignore = false
    setLoading(true)
    setFailed(false)

    tourismEventApi.getEvent(eventId)
      .then((data) => {
        if (!ignore) setEvent(data)
      })
      .catch(() => {
        if (!ignore) {
          setEvent(null)
          setFailed(true)
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [eventId])

  if (loading) {
    return (
      <section className="flex min-h-[55vh] items-center justify-center">
        <Card className="w-full text-center">
          <div className="mx-auto h-7 w-7 animate-spin rounded-full border-4 border-[#FFE4D6] border-t-[#FF6F0F]" />
          <p className="mt-4 text-[13px] font-bold text-[#6B5347]">행사 정보를 불러오는 중이에요</p>
        </Card>
      </section>
    )
  }

  if (failed || !event) {
    return (
      <section className="flex min-h-[55vh] items-center justify-center">
        <Card className="w-full text-center">
          <strong className="text-[17px] font-black text-[#261912]">행사를 찾을 수 없어요</strong>
          <p className="mt-2 text-[13px] text-[#8D7164]">삭제되었거나 볼 수 없는 행사일 수 있어요.</p>
          <Link to="/events" className="mt-5 inline-flex h-11 items-center rounded-full bg-[#FF6F0F] px-5 text-[13px] font-black text-white no-underline">
            제주 행사로 돌아가기
          </Link>
        </Card>
      </section>
    )
  }

  const homepageUrl = cleanExternalUrl(event.homepage) ?? cleanExternalUrl(event.sourceUrl)
  const kakaoMapUrl = event.lat && event.lng
    ? `https://map.kakao.com/link/map/${encodeURIComponent(event.title)},${event.lat},${event.lng}`
    : null

  return (
    <section className="flex flex-col gap-5">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="flex h-11 w-11 items-center justify-center rounded-full bg-white text-[24px] font-black text-[#261912] shadow-[0px_4px_14px_rgba(0,0,0,0.06)]"
        aria-label="뒤로"
      >
        ←
      </button>

      <section>
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full bg-[#FFF0E8] px-3 py-1 text-[11px] font-black text-[#A04100]">{event.categoryLabel}</span>
          <span className="rounded-full bg-[#E1F5E8] px-3 py-1 text-[11px] font-black text-[#168847]">{ddayLabel(event)}</span>
        </div>
        <h1 className="mt-3 text-[30px] font-black leading-tight text-[#261912]">{event.title}</h1>
        <p className="mt-3 text-[15px] font-bold leading-relaxed text-[#6B5347]">{formatDateRange(event)}</p>
      </section>

      {event.firstImageUrl && (
        <div className="overflow-hidden rounded-[28px] bg-[#F7F1EE] shadow-[0px_14px_28px_rgba(89,65,54,0.12)]">
          <img
            src={event.firstImageUrl}
            alt=""
            className="max-h-[72vh] w-full object-contain"
          />
        </div>
      )}

      <Card className="space-y-4">
        <InfoRow label="장소" value={event.venueName || event.address || '장소 정보 확인 중'} />
        {event.address && <InfoRow label="주소" value={`${event.address}${event.detailAddress ? ` ${event.detailAddress}` : ''}`} />}
        {event.organizer && <InfoRow label="주최" value={event.organizer} />}
        {event.tel && <InfoRow label="문의" value={event.tel} />}
      </Card>

      {event.overview && (
        <Card>
          <h2 className="text-[18px] font-black text-[#261912]">행사 소개</h2>
          <p className="mt-3 whitespace-pre-line text-[14px] font-medium leading-7 text-[#4B5563]">{event.overview}</p>
        </Card>
      )}

      <div className="grid grid-cols-2 gap-3">
        {homepageUrl && (
          <a
            href={homepageUrl}
            target="_blank"
            rel="noreferrer"
            className="flex h-12 items-center justify-center rounded-2xl bg-[#261912] text-[13px] font-black text-white no-underline"
          >
            공식 페이지
          </a>
        )}
        {kakaoMapUrl && (
          <a
            href={kakaoMapUrl}
            target="_blank"
            rel="noreferrer"
            className="flex h-12 items-center justify-center rounded-2xl bg-[#FF6F0F] text-[13px] font-black text-white no-underline"
          >
            지도에서 보기
          </a>
        )}
      </div>

      <p className="pb-3 text-center text-[11px] font-black text-[#168847]">정보 제공: {event.providerName}</p>
    </section>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span className="text-[11px] font-black text-[#A04100]">{label}</span>
      <p className="mt-1 text-[15px] font-bold leading-relaxed text-[#261912]">{value}</p>
    </div>
  )
}
