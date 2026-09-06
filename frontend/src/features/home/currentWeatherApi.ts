import type { Coordinates } from './currentLocationApi'
import type { Weather } from './types'

type OpenMeteoResponse = {
  current?: {
    temperature_2m?: number
    weather_code?: number
    wind_speed_10m?: number
  }
}

const WEATHER_CONDITIONS: Record<number, string> = {
  0: '맑음', 1: '대체로 맑음', 2: '구름 조금', 3: '흐림', 45: '안개', 48: '서리 안개',
  51: '약한 이슬비', 53: '이슬비', 55: '강한 이슬비', 56: '약한 어는 이슬비', 57: '강한 어는 이슬비',
  61: '약한 비', 63: '비', 65: '강한 비', 66: '약한 어는 비', 67: '강한 어는 비',
  71: '약한 눈', 73: '눈', 75: '강한 눈', 77: '싸락눈', 80: '약한 소나기', 81: '소나기',
  82: '강한 소나기', 85: '약한 눈 소나기', 86: '강한 눈 소나기', 95: '뇌우',
  96: '우박을 동반한 뇌우', 99: '강한 우박을 동반한 뇌우',
}

function weatherHeadline(code: number, temperature: number, windSpeed: number) {
  if (code >= 51) return '비나 눈에 대비해 주세요'
  if (windSpeed >= 10) return '바람이 강해요, 주의해서 달려요'
  if (temperature >= 30) return '더운 날씨, 무리하지 마세요'
  if (temperature <= 0) return '추운 날씨, 준비 운동은 충분히'
  if (code === 45 || code === 48) return '안개가 있어 시야를 조심하세요'
  if (code <= 2) return '오늘 달리기 좋습니다'
  return '가볍게 달리기 좋은 날씨예요'
}

export async function fetchCurrentWeather(
  { latitude, longitude }: Coordinates,
  runningNowCount: number,
  signal: AbortSignal,
): Promise<Weather> {
  const params = new URLSearchParams({
    latitude: String(latitude), longitude: String(longitude),
    current: 'temperature_2m,weather_code,wind_speed_10m', wind_speed_unit: 'ms', timezone: 'auto',
  })
  const response = await fetch(`https://api.open-meteo.com/v1/forecast?${params}`, { signal })
  if (!response.ok) throw new Error('날씨 API 요청에 실패했어요.')

  const data = await response.json() as OpenMeteoResponse
  const temperature = data.current?.temperature_2m
  const weatherCode = data.current?.weather_code
  const windSpeed = data.current?.wind_speed_10m
  if (temperature === undefined || weatherCode === undefined || windSpeed === undefined) {
    throw new Error('현재 날씨 정보가 비어 있어요.')
  }

  return {
    headline: weatherHeadline(weatherCode, temperature, windSpeed),
    temperatureCelsius: Math.round(temperature),
    condition: WEATHER_CONDITIONS[weatherCode] ?? '날씨 정보 확인 중',
    windSpeedMeterPerSecond: Math.round(windSpeed * 10) / 10,
    runningNowCount,
  }
}
