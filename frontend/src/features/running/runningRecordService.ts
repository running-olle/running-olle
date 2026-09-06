import { axiosInstance } from '../../api/axiosInstance'
import type { GeoPoint, RunningMode, SavedRunningRecord } from './types'

const RECORDS_KEY = 'runningOlleRunningRecords'

type RecordInput = Omit<SavedRunningRecord, 'localId' | 'serverId' | 'syncStatus'>

type CreateRunningRecordRequest = {
  route: Array<{ latitude: number; longitude: number }>
  totalDistanceMeters: number
  totalDurationSeconds: number
  averagePace: number | null
  calories: number
  startedAt: string
  endedAt: string
  courseId: string | null
  runningMode: RunningMode
}

function readRecords(): SavedRunningRecord[] {
  try {
    return JSON.parse(localStorage.getItem(RECORDS_KEY) ?? '[]') as SavedRunningRecord[]
  } catch {
    return []
  }
}

function writeRecord(record: SavedRunningRecord) {
  const records = readRecords()
  const index = records.findIndex(({ localId }) => localId === record.localId)
  if (index >= 0) records[index] = record
  else records.unshift(record)
  localStorage.setItem(RECORDS_KEY, JSON.stringify(records.slice(0, 50)))
}

function routeForApi(route: GeoPoint[]) {
  return route.map(({ latitude, longitude }) => ({ latitude, longitude }))
}

export async function saveRunningRecord(input: RecordInput) {
  const pending: SavedRunningRecord = {
    ...input,
    localId: crypto.randomUUID(),
    syncStatus: 'pending',
  }
  writeRecord(pending)

  try {
    const body: CreateRunningRecordRequest = {
      route: routeForApi(input.route),
      totalDistanceMeters: input.distanceMeters,
      totalDurationSeconds: input.durationSeconds,
      averagePace: input.averagePace,
      calories: input.calories,
      startedAt: input.startedAt,
      endedAt: input.endedAt,
      courseId: input.courseId,
      runningMode: input.runningMode,
    }

    const { data } = await axiosInstance.post<{ id: string }>('/running-records', body)
    const synced = { ...pending, serverId: data.id, syncStatus: 'synced' as const }
    writeRecord(synced)
    return synced
  } catch {
    return pending
  }
}

export function saveRunningRouteAsCourse(
  recordId: string,
  input: { name: string; description?: string | null; isPublic: boolean },
) {
  return axiosInstance.post<{ courseId: string }>(`/running-records/${recordId}/course`, {
    name: input.name,
    description: input.description ?? null,
    isPublic: input.isPublic,
  }).then(({ data }) => data)
}
