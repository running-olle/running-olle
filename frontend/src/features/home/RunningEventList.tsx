import { Card } from '../../components/ui/Card'
import { Icon } from '../../components/ui/Icon'
import { ListRow } from '../../components/ui/ListRow'
import { SectionHeader } from '../../components/ui/SectionHeader'
import type { RunningEvent } from '../../mocks/home'

type RunningEventListProps = {
  events: RunningEvent[]
}

export function RunningEventList({ events }: RunningEventListProps) {
  return (
    <section aria-labelledby="running-events-title">
      <SectionHeader
        id="running-events-title"
        icon={<Icon name="calendar" size={20} />}
        title="제주 러닝 행사"
        description="달력에 담아두고 싶은 가까운 행사예요."
      />
      <Card padding="none" shadow="none" className="mt-4 overflow-hidden border border-border-subtle">
        {events.map((event) => (
          <ListRow
            key={event.id}
            leading={(
              <span className="flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-control bg-surface-subtle">
                <span className="text-caption font-bold text-ink-secondary">{event.month}</span>
                <strong className="mt-1 text-section-title font-extrabold tabular-nums text-brand-700">{event.day}</strong>
              </span>
            )}
            title={event.title}
            description="제주에서 열리는 러닝 행사"
          />
        ))}
      </Card>
    </section>
  )
}
