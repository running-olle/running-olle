import { getKakaoMapAppKey, loadKakaoMapSdk } from '../map/kakaoMaps'

export type Coordinates = {
  latitude: number
  longitude: number
}

export async function reverseGeocode({ latitude, longitude }: Coordinates) {
  const appKey = getKakaoMapAppKey()
  if (!appKey) throw new Error('카카오 지도 키가 설정되지 않았어요.')

  await loadKakaoMapSdk(appKey)
  const maps = window.kakao?.maps
  if (!maps?.services) throw new Error('카카오 주소 서비스를 불러오지 못했어요.')

  return new Promise<string>((resolve, reject) => {
    const geocoder = new maps.services.Geocoder()
    geocoder.coord2RegionCode(longitude, latitude, (result, status) => {
      if (status !== maps.services.Status.OK) {
        reject(new Error('현재 주소를 찾지 못했어요.'))
        return
      }

      const region = result.find(({ region_type }) => region_type === 'H') ?? result[0]
      if (!region?.address_name) {
        reject(new Error('현재 주소를 찾지 못했어요.'))
        return
      }
      resolve(region.address_name)
    })
  })
}
