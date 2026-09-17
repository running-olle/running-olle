import { axiosInstance } from '../../api/axiosInstance'

export const THEME_CODES = ['COAST', 'FOREST', 'OREUM', 'FOOD', 'PHOTO', 'TRADITION', 'URBAN'] as const
export type ThemeCode = typeof THEME_CODES[number]

export type ThemeOption = {
  id: string
  code: ThemeCode
  name: string
}

export const themeLabels: Record<ThemeCode, string> = {
  COAST: '해안',
  FOREST: '숲길',
  OREUM: '오름',
  FOOD: '맛집',
  PHOTO: '포토',
  TRADITION: '전통',
  URBAN: '도심',
}

export const MEETUP_THEME_CODES: ThemeCode[] = ['COAST', 'FOREST', 'OREUM', 'PHOTO', 'FOOD']

export function isThemeCode(value: string | null | undefined): boolean {
  return THEME_CODES.includes(value?.trim().toUpperCase() as ThemeCode)
}

export function normalizeThemeCode(value: string | null | undefined): ThemeCode {
  const normalized = value?.trim().toUpperCase()
  return THEME_CODES.includes(normalized as ThemeCode) ? normalized as ThemeCode : 'COAST'
}

export const themeCatalogService = {
  list: () => axiosInstance.get<ThemeOption[]>('/themes').then(({ data }) => data),
}
