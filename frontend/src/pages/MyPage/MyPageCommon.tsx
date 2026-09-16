import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppHeader } from '../../components/layout/Header'
import { Icon, IconButton, Spinner } from '../../components/ui'

export function MyPageHeader({ title, action, onBack }: { title: string; action?: ReactNode; onBack?: () => void }) {
  const navigate = useNavigate()
  return <AppHeader className="my-header" title={title} leading={<IconButton icon={<Icon name="arrowLeft" size={22}/>} label="뒤로" onClick={onBack ?? (() => navigate(-1))}/>} trailing={action}/>
}

export function MyPageLoading({ label = '기록을 불러오는 중…' }: { label?: string }) {
  return <div className="my-loading"><Spinner label={label}/><span>{label}</span></div>
}
