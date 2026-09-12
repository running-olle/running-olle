import { axiosInstance } from '../../api/axiosInstance'
import type { SafetyNearbyPlace } from './safetyTypes'

export async function getNearbySafetyPlaces(latitude: number, longitude: number, radius = 1500) {
  const { data } = await axiosInstance.get<SafetyNearbyPlace[]>('/safety/nearby', {
    params: { lat: latitude, lng: longitude, radius },
  })
  return data
}
