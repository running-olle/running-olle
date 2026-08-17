import type { Weather } from '../../mocks/home'

type WeatherCardProps = {
  weather: Weather
}

export function WeatherCard({ weather }: WeatherCardProps) {
  return (
    <section className="relative overflow-hidden rounded-[16px] bg-[linear-gradient(135deg,#FF6F0F_0%,#FD934C_100%)] p-5 shadow-[0px_4px_12px_rgba(0,0,0,0.05)]">
      <div className="relative z-10">
        <p className="text-[16px] font-bold leading-tight text-white">{weather.headline}</p>
        <div className="mt-2 flex items-end gap-4">
          <p className="text-[26px] font-black leading-none text-white">{weather.temperatureCelsius}°C</p>
          <p className="pb-0.5 text-[14px] font-normal leading-none text-white/90">
            {weather.condition} · 바람 {weather.windSpeedMeterPerSecond}m/s
          </p>
        </div>
        <div className="mt-6 inline-flex h-10 items-center gap-2 rounded-full bg-white/20 px-4 text-[14px] font-bold text-white">
          <span className="h-2.5 w-2.5 rounded-full bg-[#4ADE80]" />
          <span>지금 달리는 러너 {weather.runningNowCount}명</span>
        </div>
      </div>
      <div className="absolute -bottom-6 -right-3 h-28 w-28 rounded-full border-[12px] border-white/20" />
      <div className="absolute bottom-10 right-8 h-10 w-1 rounded-full bg-white/20" />
      <div className="absolute bottom-4 right-24 h-1 w-10 rounded-full bg-white/20" />
    </section>
  )
}
