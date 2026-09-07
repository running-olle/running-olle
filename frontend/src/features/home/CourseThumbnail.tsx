import type { CourseImageTone } from '../../mocks/home'

type CourseThumbnailProps = {
  tone: CourseImageTone
  className?: string
  rounded?: boolean
}

export function CourseThumbnail({ tone, className = '', rounded = true }: CourseThumbnailProps) {
  return (
    <div className={`course-thumbnail course-thumbnail--${tone} ${rounded ? '' : 'course-thumbnail--square'} ${className}`} aria-hidden="true" />
  )
}
