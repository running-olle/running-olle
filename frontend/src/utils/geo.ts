import type { GeoPoint, RouteProgress } from '../models/running'

const EARTH_RADIUS_METERS = 6_371_000

function toRadians(degrees: number) {
  return degrees * Math.PI / 180
}

export function distanceBetween(from: GeoPoint, to: GeoPoint) {
  const latitudeDelta = toRadians(to.latitude - from.latitude)
  const longitudeDelta = toRadians(to.longitude - from.longitude)
  const fromLatitude = toRadians(from.latitude)
  const toLatitude = toRadians(to.latitude)
  const haversine = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.sin(longitudeDelta / 2) ** 2

  return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine))
}

export function calculateRouteProgress(route: GeoPoint[], current: GeoPoint | null): RouteProgress {
  if (!current || route.length < 2) {
    return { percent: 0, distanceFromRouteMeters: 0, completedPath: [] }
  }

  const referenceLatitude = toRadians(current.latitude)
  const metersPerLatitudeDegree = 111_320
  const metersPerLongitudeDegree = 111_320 * Math.cos(referenceLatitude)
  const routeSegmentLengths = route.slice(0, -1).map((point, index) => distanceBetween(point, route[index + 1]))
  const totalLength = routeSegmentLengths.reduce((sum, length) => sum + length, 0)
  let nearestDistance = Number.POSITIVE_INFINITY
  let nearestSegmentIndex = 0
  let nearestRatio = 0
  let nearestPoint = route[0]

  route.slice(0, -1).forEach((segmentStart, index) => {
    const segmentEnd = route[index + 1]
    const startX = (segmentStart.longitude - current.longitude) * metersPerLongitudeDegree
    const startY = (segmentStart.latitude - current.latitude) * metersPerLatitudeDegree
    const endX = (segmentEnd.longitude - current.longitude) * metersPerLongitudeDegree
    const endY = (segmentEnd.latitude - current.latitude) * metersPerLatitudeDegree
    const segmentX = endX - startX
    const segmentY = endY - startY
    const segmentLengthSquared = segmentX ** 2 + segmentY ** 2
    const ratio = segmentLengthSquared === 0
      ? 0
      : Math.max(0, Math.min(1, -(startX * segmentX + startY * segmentY) / segmentLengthSquared))
    const projectedX = startX + ratio * segmentX
    const projectedY = startY + ratio * segmentY
    const projectedDistance = Math.hypot(projectedX, projectedY)

    if (projectedDistance < nearestDistance) {
      nearestDistance = projectedDistance
      nearestSegmentIndex = index
      nearestRatio = ratio
      nearestPoint = {
        latitude: segmentStart.latitude + (segmentEnd.latitude - segmentStart.latitude) * ratio,
        longitude: segmentStart.longitude + (segmentEnd.longitude - segmentStart.longitude) * ratio,
      }
    }
  })

  const completedLength = routeSegmentLengths.slice(0, nearestSegmentIndex).reduce((sum, length) => sum + length, 0)
    + routeSegmentLengths[nearestSegmentIndex] * nearestRatio

  return {
    percent: Math.max(0, Math.min(100, totalLength ? completedLength / totalLength * 100 : 0)),
    distanceFromRouteMeters: nearestDistance,
    completedPath: [...route.slice(0, nearestSegmentIndex + 1), nearestPoint],
  }
}

export function formatDuration(totalSeconds: number) {
  const hours = Math.floor(totalSeconds / 3_600)
  const minutes = Math.floor(totalSeconds % 3_600 / 60)
  const seconds = totalSeconds % 60
  return hours > 0
    ? `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    : `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function formatDistance(distanceMeters: number) {
  return distanceMeters < 1_000 ? `${Math.round(distanceMeters)} m` : `${(distanceMeters / 1_000).toFixed(2)} km`
}

export function formatPace(minutesPerKilometer: number | null) {
  if (!minutesPerKilometer || !Number.isFinite(minutesPerKilometer)) return '--:-- /km'
  const roundedSeconds = Math.round(minutesPerKilometer * 60)
  const minutes = Math.floor(roundedSeconds / 60)
  const seconds = roundedSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')} /km`
}
