import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Icon } from '../../components/ui/Icon'
import type { Weather } from './types'

type WeatherCardProps = {
  weather: Weather | null
  status: 'loading' | 'success' | 'error'
  errorMessage?: string | null
  onRetry: () => void
  monthlyRunCount: number | null
  monthlyRunCountStatus: 'loading' | 'success' | 'error'
}

export function WeatherCard({
  weather,
  status,
  errorMessage,
  onRetry,
  monthlyRunCount,
  monthlyRunCountStatus,
}: WeatherCardProps) {
  const isReady = status === 'success' && weather

  return (
    <Card variant="brand" shadow="none" padding="md">
      <section aria-live="polite" aria-label="오늘의 러닝 날씨">
        <h2 className="text-card-title font-bold text-white">
          {isReady ? weather.headline : status === 'loading' ? '현재 날씨를 확인하고 있어요' : '날씨를 불러오지 못했어요'}
        </h2>

        <div className="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1">
          <strong className="text-page-title font-extrabold tabular-nums text-white">
            {isReady ? `${weather.temperatureCelsius}°C` : '--°C'}
          </strong>
          <span className="text-label text-white/90">
            {isReady ? `${weather.condition} · 바람 ${weather.windSpeedMeterPerSecond}m/s` : status === 'loading' ? '위치 기반 날씨 조회 중' : errorMessage}
          </span>
        </div>

        <div className="mt-3 flex flex-wrap items-end justify-between gap-x-3">
          <div className="inline-flex min-h-10 items-center gap-2 rounded-full bg-white/15 px-4 text-label font-bold text-white">
            <Icon name="calendar" size={16} />
            <span>
              이번달 러닝 횟수{' '}
              <strong className="tabular-nums">
                {monthlyRunCountStatus === 'success' ? monthlyRunCount ?? 0 : '--'}회
              </strong>
            </span>
          </div>

          {status === 'error' ? (
            <Button type="button" variant="secondary" size="sm" onClick={onRetry}>다시 시도</Button>
          ) : null}

          <a className="ml-auto inline-flex min-h-11 shrink-0 items-end pb-1 text-legal text-white/80 underline underline-offset-2" href="https://open-meteo.com/" target="_blank" rel="noreferrer">
            날씨 데이터: Open-Meteo
          </a>
        </div>

      </section>
    </Card>
  )
}
