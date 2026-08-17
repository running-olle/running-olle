import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'

export function MyPage() {
  return (
    <Card>
      <SectionTitle icon="👤" title="마이" />
      <p className="mt-3 text-[14px] leading-6 text-[#594136]">내 러닝 기록과 프로필 화면을 연결할 준비가 된 라우트입니다.</p>
    </Card>
  )
}
