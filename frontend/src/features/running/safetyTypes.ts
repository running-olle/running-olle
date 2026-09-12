export type SafetyPlaceType = 'hospital' | 'pharmacy' | 'convenience_store' | 'toilet'

export type SafetyNearbyPlace = {
  type: SafetyPlaceType
  name: string
  categoryName: string | null
  address: string | null
  lat: number
  lng: number
  distanceMeters: number | null
  phone: string | null
  placeUrl: string | null
}
