import type { Weather } from './types'

type WeatherCardProps = {
  weather: Weather | null
  status: 'loading' | 'success' | 'error'
  errorMessage?: string | null
  onRetry: () => void
}

export function WeatherCard({ weather, status, errorMessage, onRetry }: WeatherCardProps) {
  const isReady = status === 'success' && weather

  return (
    <section
      className="relative overflow-hidden rounded-[16px] bg-[linear-gradient(135deg,#FF6F0F_0%,#FD934C_100%)] p-5 shadow-[0px_4px_12px_rgba(0,0,0,0.05)]"
      aria-live="polite"
    >
      <div className="relative z-10">
        <p className="text-[16px] font-bold leading-tight text-white">
          {isReady ? weather.headline : status === 'loading' ? '현재 날씨를 확인하고 있어요' : '날씨를 불러오지 못했어요'}
        </p>
        <div className="mt-2 flex items-end gap-4">
          <p className="text-[26px] font-black leading-none text-white">{isReady ? `${weather.temperatureCelsius}°C` : '--°C'}</p>
          <p className="pb-0.5 text-[14px] font-normal leading-none text-white/90">
            {isReady ? `${weather.condition} · 바람 ${weather.windSpeedMeterPerSecond}m/s` : status === 'loading' ? '위치 기반 날씨 조회 중' : errorMessage}
          </p>
        </div>
        {isReady ? (
          <div className="mt-6 inline-flex h-10 items-center gap-2 rounded-full bg-white/20 px-4 text-[14px] font-bold text-white">
            <span className="h-2.5 w-2.5 rounded-full bg-[#4ADE80]" />
            <span>지금 달리는 러너 {weather.runningNowCount}명</span>
          </div>
        ) : status === 'error' ? (
          <button type="button" className="mt-6 h-10 rounded-full bg-white/20 px-4 text-[14px] font-bold text-white" onClick={onRetry}>
            다시 시도
          </button>
        ) : <div className="mt-6 h-10" />}
      </div>
      <a
        className="absolute bottom-2 right-3 z-20 text-[9px] text-white/70 underline underline-offset-2"
        href="https://open-meteo.com/"
        target="_blank"
        rel="noreferrer"
      >
        날씨 데이터: Open-Meteo
      </a>
      <div className="absolute -bottom-6 -right-3 h-28 w-28 rounded-full border-[12px] border-white/20" />
      <div className="absolute bottom-10 right-8 h-10 w-1 rounded-full bg-white/20" />
      <div className="absolute bottom-4 right-24 h-1 w-10 rounded-full bg-white/20" />
    </section>
  )
}
