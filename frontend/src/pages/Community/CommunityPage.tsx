import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'

export function CommunityPage() {
  return (
    <Card>
      <SectionTitle icon="👥" title="커뮤니티" />
      <p className="mt-3 text-[14px] leading-6 text-[#594136]">러너 커뮤니티 화면을 연결할 준비가 된 라우트입니다.</p>
    </Card>
  )
}
