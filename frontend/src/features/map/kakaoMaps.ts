export type KakaoLatLng = {
  getLat(): number
  getLng(): number
}

export type KakaoMap = {
  relayout(): void
  panTo(position: KakaoLatLng): void
  setCenter(position: KakaoLatLng): void
  setBounds(bounds: KakaoLatLngBounds): void
  getLevel(): number
  setLevel(level: number): void
}

export type KakaoLatLngBounds = {
  extend(position: KakaoLatLng): void
}

export type KakaoPolyline = {
  setMap(map: KakaoMap | null): void
  setPath(path: KakaoLatLng[]): void
}

export type KakaoCustomOverlay = {
  setMap(map: KakaoMap | null): void
  setPosition(position: KakaoLatLng): void
}

export type KakaoCircle = {
  setPosition(position: KakaoLatLng): void
  setRadius(radius: number): void
}

type KakaoRegionCodeResult = {
  address_name: string
  region_type: 'B' | 'H'
}

type KakaoGeocoder = {
  coord2RegionCode(
    longitude: number,
    latitude: number,
    callback: (result: KakaoRegionCodeResult[], status: string) => void,
  ): void
}

type KakaoMapOptions = {
  center: KakaoLatLng
  level: number
}

type KakaoPolylineOptions = {
  map: KakaoMap
  path: KakaoLatLng[]
  strokeWeight: number
  strokeColor: string
  strokeOpacity: number
  strokeStyle: string
}

type KakaoCustomOverlayOptions = {
  map: KakaoMap
  position: KakaoLatLng
  content: string | HTMLElement
  zIndex?: number
  yAnchor?: number
}

type KakaoCircleOptions = {
  map: KakaoMap
  center: KakaoLatLng
  radius: number
  strokeWeight: number
  strokeColor: string
  strokeOpacity: number
  fillColor: string
  fillOpacity: number
}

export type KakaoMapsNamespace = {
  load(callback: () => void): void
  LatLng: new (lat: number, lng: number) => KakaoLatLng
  LatLngBounds: new () => KakaoLatLngBounds
  Map: new (container: HTMLElement, options: KakaoMapOptions) => KakaoMap
  Polyline: new (options: KakaoPolylineOptions) => KakaoPolyline
  CustomOverlay: new (options: KakaoCustomOverlayOptions) => KakaoCustomOverlay
  Circle: new (options: KakaoCircleOptions) => KakaoCircle
  services: {
    Status: { OK: string }
    Geocoder: new () => KakaoGeocoder
  }
  event: {
    addListener(target: object, type: string, handler: () => void): void
  }
}

declare global {
  interface Window {
    kakao?: {
      maps: KakaoMapsNamespace
    }
  }
}

let kakaoMapLoader: Promise<void> | null = null

export function getKakaoMapAppKey() {
  const candidates = [
    import.meta.env.VITE_KAKAO_MAP_APP_KEY,
    import.meta.env.VITE_MAP_API_KEY,
  ]

  return candidates.find((value) => isUsableKakaoMapKey(value)) ?? ''
}

function isUsableKakaoMapKey(value: string | undefined) {
  if (!value) return false
  const trimmedValue = value.trim()
  if (!trimmedValue) return false
  return !/키|KEY|placeholder|실제|카카오/i.test(trimmedValue)
}

export function loadKakaoMapSdk(appKey: string) {
  if (window.kakao?.maps?.services) return Promise.resolve()
  if (kakaoMapLoader) return kakaoMapLoader

  kakaoMapLoader = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false&libraries=services`
    script.async = true
    script.onload = () => window.kakao?.maps
      ? window.kakao.maps.load(resolve)
      : reject(new Error('카카오맵 SDK를 불러오지 못했어요.'))
    script.onerror = () => reject(new Error('카카오맵 연결에 실패했어요.'))
    document.head.appendChild(script)
  })

  return kakaoMapLoader
}
