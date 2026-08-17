import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'
import type { RunningEvent } from '../../mocks/home'

type RunningEventListProps = {
  events: RunningEvent[]
}

export function RunningEventList({ events }: RunningEventListProps) {
  return (
    <section>
      <SectionTitle icon="📅" title="제주 러닝 행사" />
      <div className="mt-5 space-y-3">
        {events.map((event) => (
          <Card key={event.id} padding="none" className="flex min-h-[88px] overflow-hidden" shadow="section">
            <div className="flex w-28 shrink-0 flex-col items-center justify-center bg-[#F7DDD3]">
              <span className="text-[14px] font-bold leading-none text-[#594136]">{event.month}</span>
              <span className="mt-2 text-[20px] font-black leading-none text-[#A04100]">{event.day}</span>
            </div>
            <div className="flex min-w-0 flex-1 items-center px-5">
              <h3 className="truncate text-[16px] font-bold text-[#261912]">{event.title}</h3>
            </div>
          </Card>
        ))}
      </div>
    </section>
  )
}
