import { useNavigate } from 'react-router-dom'
import { RunningIcon } from '../../features/running/RunningIcon'

const options = [
  {
    title: '코스 선택 달리기',
    description: <>저장된 코스 중<br />선택해서 달려요</>,
    icon: 'route' as const,
    action: '/running/courses',
  },
  {
    title: '코스 만들고 달리기',
    description: <>나만의 코스를 만들고<br />달려요</>,
    icon: 'make' as const,
    action: '/courses/create',
  },
  {
    title: '즉시 달리기',
    description: <>준비 없이 바로<br />달리기를 시작해요</>,
    icon: 'run' as const,
    action: '/running/free',
  },
]

export function RunningSelectPage() {
  const navigate = useNavigate()
  return (
    <section className="running-select-page">
      <div className="running-select-heading">
        <h1>러닝</h1>
        <p>어떤 방식으로 달려볼까요?</p>
      </div>
      <div className="running-option-list">
        {options.map((option) => (
          <button className="running-option-card" type="button" key={option.title} onClick={() => navigate(option.action)}>
            <span className={`running-option-icon running-option-icon-${option.icon}`}><RunningIcon name={option.icon} size={52} /></span>
            <span className="running-option-copy"><strong>{option.title}</strong><span>{option.description}</span></span>
            <RunningIcon name="chevron" size={26} />
          </button>
        ))}
      </div>
    </section>
  )
}
