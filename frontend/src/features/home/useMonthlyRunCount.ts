import { useEffect, useState } from 'react'
import { homeService } from './homeService'

type MonthlyRunCountStatus = 'loading' | 'success' | 'error'

export function useMonthlyRunCount() {
  const [monthlyRunCount, setMonthlyRunCount] = useState<number | null>(null)
  const [monthlyRunCountStatus, setMonthlyRunCountStatus] = useState<MonthlyRunCountStatus>('loading')

  useEffect(() => {
    let active = true

    homeService.getMonthlyRunCount()
      .then((count) => {
        if (!active) return
        setMonthlyRunCount(count)
        setMonthlyRunCountStatus('success')
      })
      .catch(() => {
        if (!active) return
        setMonthlyRunCount(null)
        setMonthlyRunCountStatus('error')
      })

    return () => {
      active = false
    }
  }, [])

  return { monthlyRunCount, monthlyRunCountStatus }
}
