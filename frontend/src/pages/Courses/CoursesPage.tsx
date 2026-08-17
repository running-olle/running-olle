import { Card } from '../../components/ui/Card'
import { SectionTitle } from '../../components/ui/SectionTitle'

export function CoursesPage() {
  return (
    <Card>
      <SectionTitle icon="🗺️" title="코스" />
      <p className="mt-3 text-[14px] leading-6 text-[#594136]">코스 목록 화면을 연결할 준비가 된 라우트입니다.</p>
    </Card>
  )
}
