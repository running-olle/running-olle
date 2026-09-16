import type { CourseImageTone } from '../../mocks/home'

type CourseThumbnailProps = {
  tone: CourseImageTone
  imageUrl?: string | null
  className?: string
  rounded?: boolean
}

export function CourseThumbnail({ tone, imageUrl, className = '', rounded = true }: CourseThumbnailProps) {
  return (
    <div
      className={`course-thumbnail course-thumbnail--${tone} ${imageUrl ? 'course-thumbnail--image' : ''} ${rounded ? '' : 'course-thumbnail--square'} ${className}`}
      aria-hidden="true"
    >
      {imageUrl ? <img src={imageUrl} alt="" className="h-full w-full object-cover" /> : null}
    </div>
  )
}
