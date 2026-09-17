import { useEffect, useState } from 'react'
import { homeService, type HomePopularCourse } from './homeService'

type PopularCoursesStatus = 'loading' | 'success' | 'error'

export function usePopularCourses() {
  const [courses, setCourses] = useState<HomePopularCourse[]>([])
  const [status, setStatus] = useState<PopularCoursesStatus>('loading')

  useEffect(() => {
    let active = true

    homeService.getPopularCourses()
      .then((popularCourses) => {
        if (!active) return
        setCourses(popularCourses)
        setStatus('success')
      })
      .catch(() => {
        if (!active) return
        setCourses([])
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [])

  return { courses, status }
}
