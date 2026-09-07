import { useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { courseService } from '../course/courseService'
import type { CourseListItem } from '../course/types'
import { courseBuilderService } from '../courseBuilder/courseBuilderService'
import { difficultyLabel, formatDistanceKm, JEJU_CENTER } from '../courseBuilder/courseBuilderUtils'
import type { PlaceSearchResult } from '../courseBuilder/types'
import type { Meetup, MeetupTheme } from './communityTypes'
import type { MeetupCreatePayload } from './meetupApi'

type SearchStatus = 'idle' | 'loading' | 'success' | 'error'

export function MeetupComposer({
  editingMeetup,
  onClose,
  onSubmit,
}: {
  editingMeetup?: Meetup | null
  onClose: () => void
  onSubmit: (payload: MeetupCreatePayload) => void | Promise<void>
}) {
  const isEditMode = !!editingMeetup
  const [title, setTitle] = useState(editingMeetup?.title ?? '')
  const [description, setDescription] = useState(editingMeetup?.description ?? '')
  const [meetupDate, setMeetupDate] = useState(() =>
    editingMeetup?.meetupDate ? toDateTimeLocal(editingMeetup.meetupDate) : defaultMeetupDateTimeLocal(),
  )
  const [meetingPlace, setMeetingPlace] = useState(editingMeetup?.locationLabel ?? '')
  const [placeKeyword, setPlaceKeyword] = useState(editingMeetup?.locationLabel ?? '')
  const [latitude, setLatitude] = useState<number | null>(editingMeetup?.meetingLatitude ?? null)
  const [longitude, setLongitude] = useState<number | null>(editingMeetup?.meetingLongitude ?? null)
  const [placeResults, setPlaceResults] = useState<PlaceSearchResult[]>([])
  const [placeStatus, setPlaceStatus] = useState<SearchStatus>('idle')
  const [maxParticipants, setMaxParticipants] = useState(String(editingMeetup?.maxParticipants ?? 6))
  const [targetPaceLabel, setTargetPaceLabel] = useState(
    editingMeetup?.targetPaceValue != null ? formatPaceValue(editingMeetup.targetPaceValue) : `6'30"`,
  )
  const [joinMethod, setJoinMethod] = useState<'INSTANT' | 'APPROVAL'>(
    editingMeetup?.joinMethod === 'approval' ? 'APPROVAL' : 'INSTANT',
  )
  const [theme, setTheme] = useState<MeetupTheme>(editingMeetup?.theme ?? 'coast')
  const [courseOptions, setCourseOptions] = useState<CourseListItem[]>([])
  const [courseKeyword, setCourseKeyword] = useState('')
  const [selectedCourseId, setSelectedCourseId] = useState(editingMeetup?.course?.id ?? '')
  const [courseLoading, setCourseLoading] = useState(true)
  const [courseError, setCourseError] = useState('')
  const [error, setError] = useState('')
  const placeRequestIdRef = useRef(0)

  const titleText = useMemo(() => (isEditMode ? '번개 수정' : '번개 만들기'), [isEditMode])

  useEffect(() => {
    let active = true
    setCourseLoading(true)
    setCourseError('')

    courseService.getCourses({ filter: 'ALL', scope: 'AVAILABLE' })
      .then((courses) => {
        if (active) setCourseOptions(courses)
      })
      .catch(() => {
        if (active) {
          setCourseOptions([])
          setCourseError('코스 목록을 불러오지 못했습니다.')
        }
      })
      .finally(() => {
        if (active) setCourseLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    const trimmedKeyword = placeKeyword.trim()
    if (trimmedKeyword.length < 2 || (trimmedKeyword === meetingPlace && latitude !== null && longitude !== null)) {
      placeRequestIdRef.current += 1
      setPlaceResults([])
      setPlaceStatus('idle')
      return
    }

    const requestId = placeRequestIdRef.current + 1
    placeRequestIdRef.current = requestId
    const timerId = window.setTimeout(() => {
      setPlaceStatus('loading')
      courseBuilderService.searchPlaces(trimmedKeyword, JEJU_CENTER.lat, JEJU_CENTER.lng, 20_000)
        .then((places) => {
          if (placeRequestIdRef.current !== requestId) return
          setPlaceResults(places)
          setPlaceStatus('success')
        })
        .catch(() => {
          if (placeRequestIdRef.current !== requestId) return
          setPlaceResults([])
          setPlaceStatus('error')
        })
    }, 350)

    return () => window.clearTimeout(timerId)
  }, [latitude, longitude, meetingPlace, placeKeyword])

  const selectedCourse = useMemo(
    () => courseOptions.find((course) => course.id === selectedCourseId) ?? null,
    [courseOptions, selectedCourseId],
  )

  const filteredCourses = useMemo(() => {
    const keyword = courseKeyword.trim().toLowerCase()
    const source = keyword
      ? courseOptions.filter((course) =>
          [course.name, course.description ?? '', ...course.waypointNames]
            .some((value) => value.toLowerCase().includes(keyword)),
        )
      : courseOptions
    return source.slice(0, 8)
  }, [courseKeyword, courseOptions])

  const submit = () => {
    if (!title.trim() || !description.trim()) {
      setError('제목과 설명을 입력해 주세요.')
      return
    }

    if (!meetingPlace.trim() || latitude === null || longitude === null) {
      setError('집결 장소를 검색해서 선택해 주세요.')
      return
    }

    if (new Date(meetupDate).getTime() <= Date.now()) {
      setError('번개 일시는 현재 시각 이후로 선택해 주세요.')
      return
    }

    const parsedMax = Number(maxParticipants)
    if (Number.isNaN(parsedMax) || parsedMax < 2) {
      setError('최대 인원은 2명 이상이어야 합니다.')
      return
    }

    onSubmit({
      title: title.trim(),
      description: description.trim(),
      meetupDate,
      maxParticipants: parsedMax,
      targetPace: parsePace(targetPaceLabel),
      meetingPlace: meetingPlace.trim(),
      latitude,
      longitude,
      joinMethod,
      themeCode: theme,
      courseId: selectedCourseId || null,
    })
  }

  const handlePlaceKeywordChange = (value: string) => {
    setPlaceKeyword(value)
    if (value.trim() !== meetingPlace) {
      setMeetingPlace('')
      setLatitude(null)
      setLongitude(null)
    }
  }

  const selectPlace = (place: PlaceSearchResult) => {
    setMeetingPlace(place.name)
    setPlaceKeyword(place.name)
    setLatitude(place.lat)
    setLongitude(place.lng)
    setPlaceResults([])
    setPlaceStatus('idle')
  }

  return (
    <div className="fixed inset-0 z-40 bg-[rgba(38,25,18,0.45)]">
      <div className="mx-auto flex h-dvh max-w-[430px] flex-col bg-canvas">
        <div className="flex items-center justify-between border-b border-border-subtle bg-surface px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-[#E1BFB1] px-4 py-2 text-[13px] font-bold text-[#594136]"
          >
            취소
          </button>
          <strong className="text-[16px] font-bold text-[#261912]">{titleText}</strong>
          <button
            type="button"
            onClick={submit}
            className="rounded-full bg-[#FF6F0F] px-4 py-2 text-[13px] font-bold text-white"
          >
            저장
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-5">
          <Field label="제목">
            <input value={title} onChange={(event) => setTitle(event.target.value)} className={inputClassName} />
          </Field>
          <Field label="설명">
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className={`${inputClassName} min-h-[120px] py-3`}
            />
          </Field>
          <Field label="일시">
            <input
              type="datetime-local"
              value={meetupDate}
              onChange={(event) => setMeetupDate(event.target.value)}
              className={inputClassName}
            />
          </Field>
          <Field label="집결 장소">
            <div className="relative">
              <input
                value={placeKeyword}
                onChange={(event) => handlePlaceKeywordChange(event.target.value)}
                className={inputClassName}
                placeholder="장소명 검색"
              />
              {placeKeyword ? (
                <button
                  type="button"
                  onClick={() => handlePlaceKeywordChange('')}
                  className="absolute right-3 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full bg-[#F5F5F5] text-[16px] font-bold text-[#8D7164]"
                  aria-label="집결 장소 검색어 지우기"
                >
                  ×
                </button>
              ) : null}
            </div>
            <PlaceSearchResults places={placeResults} status={placeStatus} onSelect={selectPlace} />
            {meetingPlace && latitude !== null && longitude !== null ? (
              <div className="mt-3 rounded-[12px] bg-white px-4 py-3 text-[12px] text-[#594136]">
                <strong className="block text-[13px] text-[#261912]">{meetingPlace}</strong>
                <span>
                  {latitude.toFixed(5)}, {longitude.toFixed(5)}
                </span>
              </div>
            ) : null}
          </Field>
          <Field label="연계 코스">
            <input
              value={courseKeyword}
              onChange={(event) => setCourseKeyword(event.target.value)}
              className={inputClassName}
              placeholder="코스명 또는 경유지 검색"
            />
            <div className="mt-3 space-y-2">
              <button
                type="button"
                onClick={() => setSelectedCourseId('')}
                className={`w-full rounded-[12px] border px-4 py-3 text-left text-[13px] font-bold ${
                  selectedCourseId
                    ? 'border-[#E1BFB1] bg-white text-[#594136]'
                    : 'border-[#FF6F0F] bg-[#FFF1EA] text-[#A04100]'
                }`}
              >
                코스 없이 만들기
              </button>
              {courseLoading ? <StateText>코스를 불러오는 중입니다.</StateText> : null}
              {!courseLoading && courseError ? <StateText>{courseError}</StateText> : null}
              {!courseLoading && !courseError && filteredCourses.length === 0 ? (
                <StateText>조건에 맞는 코스가 없습니다.</StateText>
              ) : null}
              {!courseLoading && !courseError
                ? filteredCourses.map((course) => (
                    <CourseOptionButton
                      key={course.id}
                      course={course}
                      active={course.id === selectedCourseId}
                      onClick={() => setSelectedCourseId(course.id)}
                    />
                  ))
                : null}
            </div>
            {!selectedCourse && editingMeetup?.course && selectedCourseId === editingMeetup.course.id ? (
              <div className="mt-3 rounded-[12px] bg-white px-4 py-3 text-[12px] text-[#594136]">
                <strong className="block text-[13px] text-[#261912]">{editingMeetup.course.name}</strong>
                <span>{editingMeetup.course.distanceKm}km · {editingMeetup.course.durationMinutes}분</span>
              </div>
            ) : null}
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="최대 인원">
              <input value={maxParticipants} onChange={(event) => setMaxParticipants(event.target.value)} className={inputClassName} />
            </Field>
            <Field label="목표 페이스">
              <input value={targetPaceLabel} onChange={(event) => setTargetPaceLabel(event.target.value)} className={inputClassName} />
            </Field>
          </div>
          <Field label="참여 방식">
            <div className="grid grid-cols-2 gap-3">
              <ChipButton active={joinMethod === 'INSTANT'} onClick={() => setJoinMethod('INSTANT')}>
                즉시 참여
              </ChipButton>
              <ChipButton active={joinMethod === 'APPROVAL'} onClick={() => setJoinMethod('APPROVAL')}>
                수락 후 참여
              </ChipButton>
            </div>
          </Field>
          <Field label="테마">
            <div className="flex flex-wrap gap-2">
              {(['coast', 'forest', 'oreum', 'photo', 'food'] as MeetupTheme[]).map((item) => (
                <ChipButton key={item} active={theme === item} onClick={() => setTheme(item)}>
                  {themeToLabel(item)}
                </ChipButton>
              ))}
            </div>
          </Field>
          {error ? (
            <div className="mt-4 rounded-[12px] bg-[#FFF1EE] px-4 py-3 text-[12px] text-[#B91C1C]">{error}</div>
          ) : null}
        </div>
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="mt-4">
      <div className="mb-2 text-[13px] font-bold text-[#261912]">{label}</div>
      {children}
    </div>
  )
}

function PlaceSearchResults({
  places,
  status,
  onSelect,
}: {
  places: PlaceSearchResult[]
  status: SearchStatus
  onSelect: (place: PlaceSearchResult) => void
}) {
  if (status === 'idle') return null

  return (
    <div className="mt-2 max-h-[240px] overflow-y-auto rounded-[14px] bg-white shadow-[0px_6px_18px_rgba(0,0,0,0.08)]">
      {status === 'loading' ? <StateText>장소를 검색하는 중입니다.</StateText> : null}
      {status === 'error' ? <StateText>장소 검색에 실패했습니다.</StateText> : null}
      {status === 'success' && places.length === 0 ? <StateText>검색 결과가 없습니다.</StateText> : null}
      {status === 'success'
        ? places.map((place) => (
            <button
              key={place.kakaoPlaceId}
              type="button"
              onClick={() => onSelect(place)}
              className="block w-full border-b border-[#F3F4F6] px-4 py-3 text-left last:border-b-0"
            >
              <span className="rounded-full bg-[#FFF5EE] px-2.5 py-1 text-[11px] font-bold text-[#FF6F0F]">
                {placeCategoryLabel(place)}
              </span>
              <strong className="mt-2 block text-[13px] font-black text-[#261912]">{place.name}</strong>
              <small className="mt-1 block text-[12px] leading-5 text-[#8D7164]">
                {place.address || place.categoryName || '주소 정보 없음'}
              </small>
            </button>
          ))
        : null}
    </div>
  )
}

function CourseOptionButton({
  course,
  active,
  onClick,
}: {
  course: CourseListItem
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full rounded-[12px] border px-4 py-3 text-left ${
        active ? 'border-[#FF6F0F] bg-[#FFF1EA]' : 'border-[#E1BFB1] bg-white'
      }`}
    >
      <div className="flex items-center gap-2">
        <span className="rounded-full bg-[#F5F5F5] px-2.5 py-1 text-[11px] font-bold text-[#594136]">
          {course.courseType === 'SPOT_COURSE' ? '스팟' : '러닝'}
        </span>
        <strong className="min-w-0 flex-1 truncate text-[13px] font-black text-[#261912]">{course.name}</strong>
      </div>
      <div className="mt-2 text-[12px] text-[#8D7164]">
        {formatDistanceKm(course.distanceKm)}km · {course.estimatedDurationMinutes}분 · 난이도 {difficultyLabel(course.difficulty)}
      </div>
      {course.waypointNames.length > 0 ? (
        <div className="mt-1 truncate text-[12px] text-[#8D7164]">{course.waypointNames.join(' > ')}</div>
      ) : null}
    </button>
  )
}

function ChipButton({
  active,
  children,
  onClick,
}: {
  active: boolean
  children: ReactNode
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-[12px] border px-4 py-3 text-[13px] font-bold ${
        active ? 'border-[#FF6F0F] bg-[#FFF1EA] text-[#A04100]' : 'border-[#E1BFB1] bg-white text-[#594136]'
      }`}
    >
      {children}
    </button>
  )
}

function StateText({ children }: { children: ReactNode }) {
  return <div className="rounded-[12px] bg-white px-4 py-3 text-[12px] text-[#8D7164]">{children}</div>
}

function placeCategoryLabel(place: PlaceSearchResult) {
  if (place.categoryGroupCode === 'AT4') return '관광지'
  if (place.categoryGroupCode === 'CE7') return '카페'
  if (place.categoryGroupCode === 'FD6') return '맛집'
  if (place.categoryGroupCode === 'CS2') return '편의점'
  return place.categoryName || '장소'
}

function themeToLabel(theme: MeetupTheme) {
  switch (theme) {
    case 'forest':
      return '숲길'
    case 'oreum':
      return '오름'
    case 'photo':
      return '포토'
    case 'food':
      return '맛집'
    default:
      return '해안'
  }
}

function parsePace(value: string) {
  const match = value.match(/(\d+)\D+(\d{1,2})/)
  if (!match) {
    return null
  }
  const minutes = Number(match[1])
  const seconds = Number(match[2])
  if (Number.isNaN(minutes) || Number.isNaN(seconds)) {
    return null
  }
  return minutes + seconds / 60
}

function formatPaceValue(value: number) {
  const totalSeconds = Math.round(value * 60)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}'${String(seconds).padStart(2, '0')}"`
}

function defaultMeetupDateTimeLocal() {
  const date = new Date(Date.now() + 60 * 60 * 1_000)
  date.setSeconds(0, 0)
  return formatDateTimeLocal(date)
}

function toDateTimeLocal(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 16)
  }
  return formatDateTimeLocal(date)
}

function formatDateTimeLocal(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}T${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const inputClassName =
  'h-12 w-full rounded-[12px] border border-[#E1BFB1] bg-white px-4 text-[13px] text-[#261912] outline-none'
