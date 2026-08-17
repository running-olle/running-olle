import type { CourseImageTone } from '../../mocks/home'

type CourseThumbnailProps = {
  tone: CourseImageTone
  className?: string
}

const toneClassName: Record<CourseImageTone, string> = {
  beach:
    'bg-[radial-gradient(circle_at_70%_28%,#2F8AA7_0_18%,transparent_19%),linear-gradient(160deg,#D9F0F5_0_32%,#4CB3C7_33%_54%,#F8E7C3_55%_72%,#78A96F_73%_100%)]',
  oreum:
    'bg-[radial-gradient(circle_at_62%_42%,#376B45_0_18%,transparent_19%),linear-gradient(145deg,#86D0E9_0_36%,#1E6F95_37%_58%,#124A5F_59%_100%)]',
  forest:
    'bg-[radial-gradient(circle_at_35%_35%,#A7D39A_0_18%,transparent_19%),linear-gradient(145deg,#E2F2D5_0_28%,#6DAA6B_29%_62%,#2E5D3C_63%_100%)]',
}

export function CourseThumbnail({ tone, className = '' }: CourseThumbnailProps) {
  return (
    <div
      className={`relative overflow-hidden rounded-[8px] bg-[#F7DDD3] ${toneClassName[tone]} ${className}`}
      aria-hidden="true"
    >
      <div className="absolute bottom-0 left-0 right-0 h-1/3 bg-white/15" />
    </div>
  )
}
