# Frontend UI Audit

> 분석 범위: `frontend/` 전체. 이 문서는 2026-09-07 기준 정적 코드 감사 결과이며, 소스코드·기능·백엔드는 변경하지 않았다.

## 1. Frontend 기술 구조

### 핵심 스택

| 구분 | 현재 구조 | 근거 파일 |
| --- | --- | --- |
| Framework | React + TypeScript | `frontend/src/main.tsx`, `frontend/package.json` |
| Build tool | Vite, React plugin | `frontend/vite.config.ts` |
| PWA | `vite-plugin-pwa`, `registerType: 'autoUpdate'`, Workbox navigation fallback 제외 규칙, standalone manifest | `frontend/vite.config.ts` |
| Routing | `react-router-dom`의 `BrowserRouter`, `Routes`, 중첩 `Route`, `Navigate`, route state | `frontend/src/main.tsx`, `frontend/src/App.tsx` |
| HTTP | 공통 Axios 인스턴스 + 일부 외부 `fetch` | `frontend/src/api/axiosInstance.ts`, `frontend/src/features/home/currentWeatherApi.ts` |
| 상태 관리 | 대부분 컴포넌트 로컬 `useState`; 코스 제작 draft만 Zustand; 현재 위치는 React Context | `frontend/src/features/courseBuilder/courseDraftStore.ts`, `frontend/src/features/home/CurrentLocationContext.tsx` |
| CSS | Tailwind CSS v4 + 단일 대형 global CSS + 마이페이지 전용 CSS + 일부 inline style | `frontend/src/styles/global.css`, `frontend/src/pages/MyPage/mypage.css` |
| UI library | 별도 UI 컴포넌트 라이브러리 없음. 자체 `Button`, `Card`, `Badge`, `SectionTitle`만 존재 | `frontend/src/components/ui/` |
| Icon library | 별도 아이콘 패키지 없음. emoji/문자 기호, 직접 작성한 inline SVG가 혼재 | `frontend/src/features/running/RunningIcon.tsx`, `frontend/src/pages/MyPage/MyPage.tsx`, `frontend/src/components/layout/BottomNavigation.tsx` |

### Tailwind 적용 상태

- `@tailwindcss/vite`가 Vite plugin으로 등록되어 있고 `global.css`가 `@import "tailwindcss"`를 수행하므로 Tailwind는 실제 사용 중이다.
- 레이아웃, 홈, 커뮤니티 UI는 Tailwind utility class 비중이 높다. 다만 표준 scale보다 `text-[13px]`, `rounded-[16px]`, `bg-[#FF6F0F]`, 임의 shadow처럼 arbitrary value가 매우 많아 Tailwind가 곧 디자인 토큰 역할을 하지는 않는다.
- 러닝, 코스 제작/목록/상세, 알림은 `global.css`의 semantic class를 주로 사용한다.
- 마이페이지 전체는 `mypage.css`의 semantic class를 사용한다.
- CSS Modules, CSS-in-JS, Sass/Less는 사용하지 않는다.

### 라우팅과 레이아웃 경계

- `RequireAuth`가 토큰 확인과 `/users/me` 검증 후 로그인/온보딩 라우팅을 결정한다.
- 일반 인증 페이지는 `CurrentLocationProvider > AppLayout` 아래에서 공통 `Header`와 `BottomNavigation`을 쓴다.
- `/courses/create`, `/courses/create/save`, `/running/free`, `/running/live`, `/running/complete`는 `AppLayout` 바깥의 독립적인 전체화면 흐름이다.
- 마이페이지 라우트도 `AppLayout` 안에 있지만 `AppLayout`이 pathname으로 `/mypage`를 감지해 공통 `Header`를 숨기고, 마이페이지 자체 `PageHeader` 또는 `my-home-title`을 사용한다.
- 알 수 없는 URL은 모두 `/`로 redirect된다.

### PWA 설정 상태

- manifest의 이름, 설명, theme/background color, `display: standalone`, `start_url`은 설정되어 있다.
- `index.html`에는 기본 viewport와 theme-color가 있다.
- `public/`에는 `_redirects`와 로그인 이미지뿐이며 manifest 아이콘 파일/선언이 없다. 앱 아이콘, maskable icon, Apple touch icon, 별도 offline/fallback UI도 확인되지 않는다.

## 2. 페이지 및 라우트

### 인증 및 온보딩

| Route | Page | 역할과 주요 기능 | 주요 UI |
| --- | --- | --- | --- |
| `/login` | `LoginPage` | 카카오 OAuth 시작, query의 OAuth 오류 표시 | hero 이미지, 브랜드, 카카오 로그인 CTA, 오류 문구 |
| `/oauth/callback` | `OAuthCallbackPage` | hash의 access token/onboarding 상태 저장 후 이동 | 공통 spinner와 상태 문구 |
| `/onboarding` | `OnboardingPage` | 3단계 프로필·러닝 취향·약관/알림 설정, 닉네임 중복 검사, 사진 선택, 가입 저장 | 전용 header/progress, field, choice chip, toggle, 고정 footer CTA |

### 홈·코스·커뮤니티·러닝

| Route | Page | 역할과 주요 기능 | 주요 UI |
| --- | --- | --- | --- |
| `/` | `HomePage` | 현재 위치 기반 날씨 표시; 추천/인기 코스와 행사 노출. 코스/행사 데이터는 현재 mock | 공통 header, `WeatherCard`, 가로 추천 카드, 인기 순위 카드, 행사 카드 |
| `/courses` | `CoursesPage` → `CourseListView` | 공개 코스 검색/필터/북마크, 상세 진입, 코스 만들기 진입 | 검색 input, filter chips, 지도 thumbnail card, bookmark, 상세/생성 action |
| `/courses/:courseId` | `CourseDetailPage` | 코스 상세 조회, 북마크, 지도/경유지/통계, 코스 러닝 시작 | 지도, 상태 badge, 작성자 card, stat card, 소개 modal, 고정 CTA |
| `/courses/create` | `CourseBuilderPage` | 위치 기준 장소 검색/주변 탐색, 장소 상세, 경유지 구성, 경로 계산 | 전체화면 지도, 상단 검색/header, 지도 marker, 주변 장소 panel, 장소 상세/draft bottom sheet |
| `/courses/create/save` | `CourseSaveDetailPage` | draft 요약, 이름/소개/유형/테마/태그/공개 여부 입력, 저장 및 후속 이동 | 전용 header, summary, form section/chips/toggle, 경유지 목록, 고정 CTA, 완료 bottom sheet |
| `/community` | `CommunityPage` | 피드·번개·채팅 탭, 필터, 게시물 CRUD/댓글/좋아요, 번개 CRUD·참여 승인, 채팅 및 realtime | tab/filter, feed card, meetup list, chat list, 작성/상세/참여/지원자/채팅 fullscreen overlay, 코스 preview modal |
| `/running` | `RunningSelectPage` | 코스 선택/코스 제작/즉시 달리기 분기 | 3개 대형 option card |
| `/running/courses` | `RunningCourseSelectPage` → `CourseListView` | 저장/제작 코스 검색·필터 후 러닝 시작 | 코스 summary, 검색, filter, 코스 card와 실행 CTA |
| `/running/free` | `FreeRunReadyPage` | GPS 1회 조회, 선택 코스 상세/지도 표시, 3초 countdown 후 live 진입 | 전체화면 지도, 전용 header, 위치 재조회, 위치 상태 card, 고정 시작 CTA, countdown overlay |
| `/running/live` | `LiveRunningPage` | GPS watch, 경로/거리/시간/pace 계산, pause/lock/photo/end/save | 전체화면 지도, 실시간 stat/status, 주변 장소 범례, control dock, screen lock, 종료 bottom sheet |
| `/running/complete` | `RunningCompletePage` | 기록 결과 표시, 자유 러닝 경로의 코스 저장, 홈/코스 이동 | 완료 hero, 경로 지도, stat, 동기화 안내, 코스 저장 modal, CTA |

### 마이페이지

| Route | Page | 역할과 주요 기능 | 주요 UI |
| --- | --- | --- | --- |
| `/mypage` | `MyPage` | dashboard/profile/누적 통계, 하위 메뉴 진입 | 자체 title/notification, avatar/profile, stat cards, menu list |
| `/mypage/history` | `RunningHistoryPage` | 최근 완주와 방문 장소 요약 | `PageHeader`, `RunCard`, `VisitCard`, loading/empty |
| `/mypage/history/all` | `CompletedRunsPage` | 전체 완주 기록 요약 및 유형 필터 | summary cards, chips, run list |
| `/mypage/history/visits` | `VisitedPlacesPage` | 방문 장소 전체 목록 | filter chips, visit cards, 외부 Kakao Map link |
| `/mypage/history/:recordId` | `RunningRecordDetailPage` | 기록 상세, 계획/실제 경로 비교, 통계와 코스 연결 | 자체 header, hero, `CourseRouteMap`, legend, stat/detail cards, waypoint list |
| `/mypage/bookmarks` | `BookmarksPage` → `CourseListView` | 생성/저장 코스 조회, 북마크 해제, 본인 코스 삭제/시작 | 공통 마이 header + 코스 목록 UI |
| `/mypage/reports` | `ReportsPage` | 여행별/전체 러닝 통계 | tab, trip hero, stat grid, report card, CTA |
| `/mypage/trips` | `TripsPage` | 여행 목록 조회와 생성 진입 | info box, trip list/empty, header action |
| `/mypage/trips/new` | `TripCreatePage` | 여행명/지역/기간 생성 | form fields, date pair, placeholder, submit/error |
| `/mypage/settings` | `SettingsPage` | 계정/프로필/알림 진입, 로그아웃/회원 탈퇴 | setting groups, menu rows, native confirm |
| `/mypage/settings/account` | `AccountPage` | 카카오 계정/가입일/상태 표시 | account panel, status pill, 외부 관리 link |
| `/mypage/settings/profile` | `ProfileEditPage` | 프로필 조회·이미지 업로드/삭제·취향 편집·저장 | avatar picker, fields, chips, error/submit |
| `/mypage/settings/notifications` | `NotificationPage` | 알림 설정 조회와 즉시 toggle 저장 | info box, toggle rows, save toast |

개발 모드에만 `/dev/mypage`, `/dev/mypage/history`, `/dev/mypage/history/visits`가 추가된다. 인증 없이 마이 UI를 확인하기 위한 route이며 production build에서는 생성되지 않는다.

## 3. 현재 공통 컴포넌트

### 프로젝트 전역으로 공개된 공통 컴포넌트

| 컴포넌트 | 파일 | 현재 사용 범위 / 한계 |
| --- | --- | --- |
| `AppLayout` | `frontend/src/components/layout/AppLayout.tsx` | 일반 인증 화면의 scroll container, header, bottom nav를 조합. pathname 조건이 마이페이지 레이아웃 정책까지 직접 알고 있음 |
| `Header` | `frontend/src/components/layout/Header.tsx` | 위치 label과 `NotificationCenter` slot. 일반 탭 화면 전용 |
| `BottomNavigation` | `frontend/src/components/layout/BottomNavigation.tsx` | 홈/코스/러닝/커뮤니티/마이 5칸. 중앙 러닝 CTA 포함 |
| `Button` | `frontend/src/components/ui/Button.tsx` | `icon`/`fab`/`primary` variant가 있으나 현재 실제 호출처가 없음 |
| `Card` | `frontend/src/components/ui/Card.tsx` | 홈 추천/인기/행사에서만 사용 |
| `Badge` | `frontend/src/components/ui/Badge.tsx` | 홈 추천에서만 사용 |
| `SectionTitle` | `frontend/src/components/ui/SectionTitle.tsx` | 홈 섹션에서만 사용 |
| `RequireAuth` | `frontend/src/components/auth/RequireAuth.tsx` | 인증/온보딩 route guard와 인증 확인 loading UI 담당 |

### feature/page 내부의 사실상 공통 컴포넌트

- 코스: `CourseListView`, 내부 `CourseListCard`, `CourseRouteMap`, `CourseRouteThumbnail`.
- 지도: `KakaoPointMap`, `CourseBuilderMap`, `FreeRunningMap`.
- 러닝: `RunningIcon`.
- 커뮤니티: `FeedPostCard`, `ChatList`, `MeetupList`와 여러 composer/detail/modal 컴포넌트.
- 마이페이지: `PageHeader`, `Empty`, `Loading`, `Menu`, `RunCard`, `VisitCard`, `Choice`, `ToggleGroup`가 있지만 모두 `MyPage.tsx` 내부 private 구현이다.
- 커뮤니티의 `MeetupList`와 `MeetupDetailModal`에는 각각 별도의 private `Badge`가 중복 구현되어 있다.

### 요청 예시 기준의 존재 여부

- Header: 전역 `Header`, 마이페이지 `PageHeader`, 러닝/코스 제작/저장/커뮤니티 overlay별 header가 별도 존재.
- BottomNavigation: 존재하지만 safe-area 대응 없음.
- Button/Card/Badge: 존재하나 적용 범위가 매우 좁고 대부분 화면은 자체 class로 다시 구현.
- Input: 전역 공통 컴포넌트 없음.
- Modal/BottomSheet: 전역 공통 컴포넌트 없음. 화면별 backdrop/sheet가 반복 구현됨.
- Loading/Empty/Error: 전역 공통 컴포넌트 없음. `.spinner`만 사실상 공유하고 문맥별 wrapper가 반복됨.

## 4. 스타일링 구조

### `frontend/src/styles/global.css`

- Tailwind import와 reset 수준의 root/body/button 규칙을 포함한다.
- 동시에 인증, 온보딩, 러닝, 코스 제작, 코스 저장, 코스 목록, 코스 상세, 알림까지 617줄의 페이지별 CSS가 한 파일에 모여 있다.
- class naming은 semantic하지만 모든 selector가 global scope라 이름 충돌과 영향 범위 확인이 어렵다.
- `#root > main`, `#root > main`이 아닌 `AppLayout` 내부 main, 독립 full-screen main 등 화면 셸 규칙이 서로 다르다.

### `frontend/src/pages/MyPage/mypage.css`

- 마이페이지와 러닝 기록 상세 스타일을 별도 파일로 관리한다.
- 파일은 줄바꿈이 매우 적은 압축형 형태라 selector 탐색, diff review, 토큰 치환이 어렵다.
- 동일한 `.spinner`, `.form-error`, `.course-detail-waypoints` 같은 global class에 기대거나 global selector와 조합한다.

### Tailwind class

- `AppLayout`, `Header`, `BottomNavigation`, 홈 feature, 커뮤니티 feature에서 집중적으로 사용한다.
- 색상/폰트/반경/shadow를 arbitrary value로 직접 작성한다. 예: `bg-[#FFF8F6]`, `text-[12px]`, `rounded-[16px]`, `shadow-[0px_4px_12px_rgba(...)]`.
- 동일 색상이라도 대소문자, 3/6자리 hex, opacity 표기, rgba 표기가 혼재한다.

### Inline style

- 주 사용 목적은 서버 이미지 URL/gradient 주입이다: avatar, feed image, trip/visit/run thumbnail, chat gradient.
- 동적 위치 UI로 `ScreenLock` drag transform이 inline style을 사용한다.
- `CourseRouteMap`에는 `plannedRouteStyle={{ strokeWeight: 5 }}`처럼 지도 SDK 표현 설정도 prop object로 전달된다.
- 동적 데이터에 필요한 inline style과 정적 디자인 값이 섞여 있으므로, 리디자인 시 전자는 보존하고 후자만 토큰화 대상으로 구분해야 한다.

## 5. 현재 디자인 시스템 적용 현황

### 존재하는 기반

- 기본 글꼴 stack: `Pretendard`, `Noto Sans KR`, system-ui.
- 반복되는 핵심 팔레트는 있다: orange `#FF6F0F`/`#FD934C`, dark brown `#261912`, secondary brown `#594136`, warm background `#FFF8F6`, border `#E1BFB1`.
- 최대 모바일 canvas 폭 `430px`, pill/rounded card, orange gradient CTA라는 시각적 방향도 반복된다.
- 작은 UI abstraction으로 `Button`, `Card`, `Badge`, `SectionTitle`가 시작되어 있다.

### 부재하거나 불완전한 부분

- `:root`에 CSS custom property가 한 개도 없으며 color/spacing/type/radius/shadow/z-index/motion token이 없다.
- Tailwind theme 확장 또는 별도 theme config가 없다. 모든 브랜드 값이 arbitrary class 또는 CSS literal이다.
- typography scale과 line-height 규칙이 정의되어 있지 않다.
- component state 규격(default/pressed/focus/disabled/loading/error)이 통합되어 있지 않다.
- icon size/stroke/visual style 규격이 없다.
- surface/card, form control, chip/badge, modal/sheet, toast, empty/error/loading의 전역 primitive가 없다.
- 다크 모드나 사용자 theme 구조는 없다.

결론적으로 현재는 “반복되는 브랜드 값”은 있으나, 이를 단일 출처에서 강제하는 디자인 시스템은 적용되지 않은 상태다.

## 6. 디자인 일관성 문제

### 하드코딩된 시각 값

- TSX/CSS 전체에서 단순 문자열 발생 기준 `#fff` 약 110회, `#594136` 94회, `#ff6f0f` 93회, `#261912` 78회, `#8d7164` 55회가 반복된다. 토큰이 없어 색 변경 시 광범위한 수동 수정이 필요하다.
- 유사 목적의 orange가 `#FF6F0F`, `#FF671B`, `#FF6418`, `#FF7E20`, `#FD934C`, `#A04100` 등으로 분기된다.
- neutral/background도 warm brown 계열과 Slate 계열(`#111827`, `#374151`, `#6B7280`, `#F7F8FC`)이 화면별로 섞인다. 코스 저장/상세는 Slate, 홈/러닝은 warm brown, 커뮤니티와 마이는 양쪽을 혼용한다.
- success/error/info 색도 공통 semantic token 없이 각 컴포넌트가 직접 정한다.

### Typography

- 9px부터 36px 이상까지 다수의 개별 크기가 존재하며, 특히 9–13px 텍스트가 광범위하다. 같은 보조 문구도 9/10/11/12/13/14px로 달라진다.
- `font-bold`, `font-black`, 700/800/900이 혼재하고 같은 제목 계층도 17/18/20/21/23/24/28px 등으로 다르다.
- `Header` 15px, 마이 `PageHeader` 18px, 러닝 flow header 17px, 코스 save header 16px 등 동일한 app bar title 역할이 다르다.

### Spacing, radius, shadow

- 화면 기본 horizontal padding이 14/16/18/20/22/26px로 분산된다.
- gap도 2–25px 사이 literal 조합이 많아 4px 또는 8px 기반 scale이 보이지 않는다.
- radius는 5/8/9/10/11/12/13/14/15/16/18/20/22/24/26/28/30/34px와 99/999px가 혼재한다. 카드만 보아도 `Card` 16px, my stat 20px, menu 22px, course card 18px이다.
- card shadow가 `0 4px 12px`를 중심으로 opacity .04/.05/.06/.08이 각각 존재하고, sheet/panel shadow도 화면별 literal이다.

### 같은 역할의 서로 다른 UI

- Header: `Header`, 마이 `PageHeader`, `onboarding-header`, `running-flow-header`, `course-builder-header`, `course-save-header`, 커뮤니티 fullscreen overlay header가 각기 다른 높이·padding·색·back behavior를 가진다.
- Primary CTA: 공통 `Button`은 사용되지 않고 `onboarding-footer button`, `.free-ready-footer button`, `.complete-home-button`, `.course-add-button`, `.course-save-footer button`, `.submit-button`, 커뮤니티 action이 모두 별도 구현이다. pill과 rounded rectangle도 혼재한다.
- Input: onboarding `.field`, course save field, course builder search, community composer field, mypage form이 높이·border·focus·error 표현을 따로 갖는다.
- Chip/Badge: 전역 `Badge`, onboarding `Choice`, course filters/chips, mypage `.chips`, community filter와 private `Badge` 2종이 중복된다.
- Toggle: onboarding `.toggle`, course `.course-public-toggle`, running `.running-course-public-toggle`, mypage `.toggle-group` toggle이 각각 별도 DOM/CSS 구조다.
- Modal/Sheet: 코스 소개/preview/save, 러닝 종료/save, 커뮤니티 composer/detail/chat/meetup 계열이 backdrop, header, close, scroll, radius, z-index를 독립 구현한다.
- Loading/Empty/Error: `RequireAuth`, course list/detail/save, community `StateBox`, 마이 `Loading`/`Empty`, 지도 fallback이 동일 상태를 서로 다른 구조와 문구 스타일로 표시한다. 빈 상태 영역은 고정 UI를 제외한 실제 가용 영역을 기준으로 가로·세로 중앙 정렬하고, 동일한 상·하 padding과 요소 간 gap을 사용해야 한다. 첫 요소의 margin으로 콘텐츠 묶음이 중심에서 밀리지 않도록 한다.
- Icon: bottom nav 문자 기호, 여러 emoji, `RunningIcon`, `MyPage`의 local SVG path map, 각 feature의 개별 SVG가 혼재한다.

### 중복/제한된 추상화

- `Button`은 정의되어 있으나 사용되지 않아 실제 버튼 일관성에 기여하지 않는다.
- `Card`, `Badge`, `SectionTitle`은 홈 전용에 가깝다.
- `MeetupList`와 `MeetupDetailModal`은 유사 Badge를 각 파일에서 중복 구현한다.
- 마이페이지 `PageHeader`, `Empty`, `Loading`, `Choice`, `ToggleGroup`는 다른 영역에서 재사용할 수 없는 private 함수다.
- `CourseDetailPage`와 `CoursePreviewModal`은 코스 타입/난이도 label, bookmark icon/action, 상세 정보 표현을 상당 부분 반복한다.

## 7. 공통화 후보

아래 후보는 기능 로직을 옮기기 전에 “표현 primitive와 상태 contract”부터 추출하는 것이 안전하다.

1. **App shell primitives**: `AppViewport`, safe-area 대응 `TopAppBar`, `BottomNavigation`, scroll content, fullscreen flow shell.
2. **Button family**: primary/secondary/tertiary/danger/icon/FAB과 size, full-width, loading/disabled 상태. 기존 `Button` API는 실제 화면 요구를 포함하도록 재정의할 필요가 있다.
3. **Form controls**: `TextField`, `TextArea`, `SearchField`, `DateField`, `FieldLabel`, helper/error/count text.
4. **Selection controls**: filter chip, choice chip, semantic `Badge`, `Switch`/toggle, segmented tab.
5. **Surface primitives**: card, list row, stat card, info box, notice/error box.
6. **Overlay primitives**: center modal, bottom sheet, fullscreen dialog, backdrop, sheet handle, overlay app bar. focus/scroll/escape contract도 함께 규격화해야 한다.
7. **Async state**: page/section loading, empty, error, retry UI. `.spinner`만 공유하는 현재 구조를 대체할 수 있다.
8. **Navigation/feedback**: back header, toast, confirm dialog, fixed bottom action bar.
9. **Media primitives**: avatar, thumbnail, image fallback, gradient placeholder.
10. **Domain presentation**: `CourseBadge`, `CourseStats`, `CourseCard`, bookmark action, route map frame; `RunStats`; `MeetupBadge`/participant avatar.
11. **Icon system**: 현재 inline SVG path를 한 registry로 모으고 emoji/문자 기호 사용 범위를 명시.
12. **Design tokens**: semantic color, typography, spacing, radius, elevation, z-index, motion, viewport width, header/nav 높이, safe-area inset.

## 8. 페이지별 리디자인 주의사항

### 인증/온보딩

- `LoginPage`의 CTA는 `window.location.href`로 OAuth URL을 직접 연다. 버튼을 link처럼 교체하더라도 redirect 동작과 query의 `oauth_error` 표시를 보존해야 한다.
- `OAuthCallbackPage`는 화면이 단순하지만 hash parsing, localStorage 저장, onboarding 분기와 결합되어 있다. loading 화면 교체 시 effect lifecycle을 건드리지 않아야 한다.
- `OnboardingPage`는 3단계 state, 단계별 validation, 350ms 닉네임 debounce, 3MB 이미지 제한/FileReader, 필수 약관, 최종 API payload가 한 파일에 있다. step DOM을 분리할 때 form state와 `stepValid` 조건을 그대로 유지해야 한다.
- onboarding footer는 fixed인데 bottom safe-area를 반영하지 않는다. safe-area를 추가할 때 콘텐츠 `padding-bottom: 108px`도 함께 계산해야 겹침이 생기지 않는다.

### 홈

- `HomePage`의 날씨만 실데이터이고 추천/인기/행사는 `frontend/src/mocks/home.ts`를 사용한다. 리디자인 시 카드 interaction이 실제 route/action을 가진다고 가정하면 안 된다.
- 위치는 `CurrentLocationProvider`, 날씨는 `useCurrentWeather`가 연결한다. header location retry/status와 weather retry/status를 시각적으로 통합해도 context 책임은 유지해야 한다.
- 추천 목록은 의도적인 horizontal scroll이며 `-mx-5`가 `AppLayout` padding에 의존한다.

### 코스 탐색/선택/북마크

- `CourseListView` 하나가 scope와 다수의 boolean prop으로 `/courses`, `/running/courses`, `/mypage/bookmarks`의 기능 차이를 만든다. 카드 분리 시 `scope`, filter, search debounce, 북마크/삭제, start route state 조건을 잃기 쉽다.
- 카드마다 `CourseRouteMap`을 렌더링하므로 목록 layout/visibility 변경은 지도 relayout 및 성능에 영향을 줄 수 있다.
- `/running/courses`에서 시작할 때 전달하는 `courseId`, `courseName`, `runningMode` route state는 러닝 준비 화면의 필수 입력이다.

### 코스 상세

- 조회/북마크 optimistic-like local update, creator 여부, 공개 여부, fixed footer CTA, 소개 modal, 러닝 route state가 한 컴포넌트에 결합되어 있다.
- fixed footer는 bottom nav 위 `83px`을 전제로 `bottom: calc(83px + safe-area...)`를 사용한다. nav 높이를 바꾸면 반드시 함께 조정해야 한다.
- `CoursePreviewModal`은 별도 구현이므로 상세 페이지 리디자인만 변경하면 커뮤니티 preview와 시각/동작이 갈라질 수 있다.

### 코스 제작/저장

- `CourseBuilderPage`는 가장 위험도가 높은 화면이다. Kakao map, GPS, 장소 검색 request race 방지 id, 주변 카테고리, 장소 상세, draggable sheet snap, waypoint Zustand, 500ms 경로 계산, 저장 route 전환이 동일 화면에 있다.
- 지도 위 header/search/results/nearby panel/place sheet/draft sheet의 z-index와 pointer 영역이 맞물린다. DOM 순서나 overlay 면적 변경 시 지도 조작 또는 선택 event가 막힐 수 있다.
- `/courses/create/save`는 Zustand draft가 없으면 제작 화면으로 즉시 redirect한다. 새 레이아웃에서 reload/직접 진입용 빈 화면을 임의로 추가하면 현재 흐름과 달라진다.
- 저장 성공 modal의 세 CTA는 모두 draft reset 시점과 목적지가 다르다. 시각적으로 합쳐도 handler를 합치면 안 된다.

### 커뮤니티

- `CommunityPage`는 700줄 이상이며 피드/번개/채팅의 fetch, filtering, modal orchestration, optimistic mutation, realtime 연결과 UI가 집중되어 있다.
- URL search의 `tab`은 초기 tab에만 반영된다. 탭 UI 교체 시 기존 deep-link 해석을 보존해야 한다.
- 여러 fullscreen overlay가 430px app canvas를 만들고 각각 독립 scroll container를 쓴다. 공통 modal로 바꿀 때 overlay 간 전환(상세 → 편집, 번개 → 채팅, 참여 완료 → 문의)을 보존해야 한다.
- feed image/upload, comments, likes, meetup 신청/승인/거절/삭제, chat send/delete/realtime은 버튼 위치보다 handler와 entity patch 순서가 중요하다.
- 여러 overlay는 `aria-modal`/dialog semantics, focus trap, body scroll lock 적용이 일관되지 않다. 리디자인에서 공통 overlay primitive로 정리할 주요 대상이다.

### 러닝

- `FreeRunReadyPage`는 GPS 성공 전 시작 버튼이 disabled이며 countdown 0에서 route state를 만들어 `/running/live`로 이동한다. CTA 리디자인 시 이 gate와 3초 timer cleanup을 보존해야 한다.
- `LiveRunningPage`는 500ms timer, `watchPosition`, 정확도/구간 거리 필터, pause 누적 시간 refs, route 기록, 화면 잠금 gesture, 카메라 input, 종료 저장을 한 컴포넌트에서 처리한다. 시각 영역만 우선 분리하고 effect/ref/state machine은 그대로 두는 것이 안전하다.
- live 화면은 `startPosition` route state가 없으면 준비 화면으로 redirect한다. 새 진입 버튼이 이를 누락하면 러닝이 시작되지 않는다.
- `ScreenLock`은 pointer capture, 72px swipe 또는 1.2초 hold로 해제한다. 버튼 구조/CSS transform/touch-action 변경 시 gesture가 손상될 수 있다.
- `RunningCompletePage`는 navigation state의 record가 없으면 redirect한다. free run의 서버 sync 성공 여부에 따라 “경로를 코스로 저장” 가능 여부가 달라진다.

### 마이페이지

- 여러 route component와 private UI helper가 `MyPage.tsx` 한 파일에 압축되어 있다. 리디자인 전에 route별 view를 분리하더라도 service call/fallback semantics를 보존해야 한다.
- 여러 조회 실패가 실제 error와 empty를 구분하지 않고 `[]` 또는 `EMPTY_*`로 fallback한다. UI 감사 범위에서는 이 의미를 바꾸지 말고 현재와 동일하게 표시해야 한다.
- 프로필 편집은 load/upload/delete/save 상태와 hidden file input/ref가 결합되어 있다.
- 알림 toggle은 즉시 local update 후 API 실패 시 rollback하고, 성공 toast를 1.5초 timer로 숨긴다.
- 설정의 탈퇴는 native `confirm` 후 API 호출, logout 순서다. 커스텀 confirm으로 교체 시 동일한 비동기 순서를 보존해야 한다.

## 9. 기능 로직과 UI가 결합된 위험 영역

| 위험도 | 파일 | 결합된 로직 | 리디자인 시 보존 경계 |
| --- | --- | --- | --- |
| 매우 높음 | `frontend/src/pages/Running/LiveRunningPage.tsx` | GPS watch, 거리 필터, timer/ref 기반 pause, route 기록, photo input, save API/local fallback, routing, pointer gesture | effects/refs/phase 전이와 control handler를 view 변경과 분리 |
| 매우 높음 | `frontend/src/pages/Courses/CourseBuilderPage.tsx` | GPS, Kakao map, search/nearby API, request race 제어, sheet drag, Zustand draft, route calculation, routing | map/panel pointer·z-index·snap contract와 draft mutation 순서 유지 |
| 매우 높음 | `frontend/src/pages/Community/CommunityPage.tsx` | 3개 기능 영역의 API/realtime/filter/entity patch/modal orchestration | tab별 state와 overlay 전환 handler를 presentational UI 밖의 controller로 유지 |
| 높음 | `frontend/src/features/community/FeedComposer.tsx` | option API, image upload, file input, create/update payload, submit state | file/selection state와 submit payload 유지 |
| 높음 | `frontend/src/features/community/MeetupComposer.tsx` | 장소 검색 debounce/race, course API, validation, create/update payload | place coordinates/course selection과 field UI 분리 |
| 높음 | `frontend/src/features/course/CourseListView.tsx` | search debounce, fetch, bookmark/remove/delete/start, 다중 route 변형 | boolean prop 조합과 action visibility 규칙 유지 |
| 높음 | `frontend/src/pages/Auth/OnboardingPage.tsx` | multi-step form, nickname timer/API, FileReader, validation, submit route | step state와 validation/payload 계약 유지 |
| 높음 | `frontend/src/pages/Running/FreeRunReadyPage.tsx` | GPS, course fetch, countdown, route state 생성 | GPS gate와 countdown/navigation effect 유지 |
| 높음 | `frontend/src/pages/Courses/CourseSaveDetailPage.tsx` | Zustand draft guard, metadata API, create payload, reset/navigation | draft 유효성, reset 시점, 3개 성공 action 유지 |
| 높음 | `frontend/src/pages/Courses/CourseDetailPage.tsx` | fetch, bookmark mutation, modal, run navigation state | bookmark disabled/creator 조건과 route state 유지 |
| 중간 | `frontend/src/features/notifications/NotificationCenter.tsx` | 30초 polling, outside click, mark read/all/clear, route navigation | polling cleanup과 panel action semantics 유지 |
| 중간 | `frontend/src/features/home/CurrentLocationContext.tsx`, `useCurrentWeather.ts` | GPS 1회 조회, reverse geocode, 외부 weather fetch/abort, retry | provider 범위 및 status/error/retry contract 유지 |
| 중간 | `frontend/src/pages/MyPage/MyPage.tsx` | 다수 route의 service call, forms, upload, logout/withdraw, notification rollback timer | route별 controller를 추출하되 fallback 동작 유지 |
| 중간 | `frontend/src/pages/MyPage/RunningRecordDetailPage.tsx` | route param fetch, 계획/기록 경로 선택 규칙 | 지도에 전달하는 planned/recorded path 조건 유지 |
| 중간 | `frontend/src/components/auth/RequireAuth.tsx` | localStorage token, session API, onboarding redirect | loading UI만 교체하고 guard 분기는 변경 금지 |

추가로 `CourseRouteMap`, `CourseBuilderMap`, `FreeRunningMap`, `KakaoPointMap`은 지도 SDK lifecycle과 container 크기에 민감하다. 지도 wrapper의 display/height/visibility가 바뀌면 SDK `relayout`, bounds, marker/polylines가 깨질 수 있으므로 순수 장식 컴포넌트처럼 다루면 안 된다.

## 10. 권장 리디자인 순서

기능 변경 없이 UI를 전면 교체한다는 전제의 순서다.

1. **시각 기준 동결 및 token 정의**: 현재 route별 상태(default/loading/empty/error/disabled/modal/open)를 목록화하고 color/type/spacing/radius/elevation/z-index/safe-area token을 먼저 확정한다.
2. **앱 셸 정리**: 430px viewport, `TopAppBar`, safe-area, scroll container, `BottomNavigation`, fullscreen flow shell을 통합한다. 이 단계에서 콘텐츠 상·하단 inset 계약을 확정한다.
3. **저위험 primitive 구축**: icon, Button, Card/Surface, Badge/Chip, Input, Switch, Tabs, AsyncState, Toast를 만들고 기존 로직 없이 Story/example 상태로 검증한다.
4. **정적·조회 중심 화면 적용**: 홈 → 러닝 선택 → 계정 정보/설정 메뉴 → 여행/리포트/히스토리 순으로 적용한다. 데이터 fetch handler는 유지하고 presentational markup부터 교체한다.
5. **공통 코스 표현 통합**: `CourseListView`, 상세, 커뮤니티 preview, 마이 북마크에서 card/badge/stats/bookmark/map frame을 공유한다. 지도 container 크기 회귀를 별도 확인한다.
6. **Form과 onboarding 적용**: form field/chip/switch/bottom action을 통합한 뒤 온보딩, 여행 생성, 프로필, 코스 저장 화면에 적용한다. validation과 submit handler는 그대로 유지한다.
7. **Overlay 체계 통합**: modal/bottom sheet/fullscreen dialog primitive를 만든 후 코스/러닝의 단순 overlay부터 적용하고, 마지막에 커뮤니티의 다중 overlay 전환을 옮긴다.
8. **고위험 지도 제작 화면 적용**: `CourseBuilderPage`는 검색/장소 상세/draft sheet를 각각 view 단위로 분리하되 Zustand와 지도 event contract는 유지한다.
9. **고위험 실시간 러닝 적용**: `LiveRunningPage`는 timer/GPS/phase/ref 로직을 controller로 보존한 상태에서 stat panel/control/screen lock/end sheet만 교체한다.
10. **PWA 모바일 QA**: standalone iOS/Android에서 safe-area, keyboard+form, 320–430px 폭, orientation/resize, install icon/theme, offline navigation, fixed overlay, nested scroll, touch target, GPS permission/error 상태를 route별로 점검한다.

### 모바일 PWA 관점에서 우선 해결할 항목

- `index.html` viewport에 `viewport-fit=cover`가 없어 iOS에서 선언된 `env(safe-area-inset-*)`가 기대대로 동작하지 않을 수 있다.
- 공통 `Header`와 `BottomNavigation`, 마이 `PageHeader`, onboarding footer에는 safe-area 반영이 없거나 일관되지 않다. 반면 러닝/코스 일부만 개별 적용한다.
- BottomNavigation은 고정 `83px`이고 AppLayout content는 `pb-[107px]`, 코스 상세 CTA는 `83px`을 직접 참조한다. 하나를 변경하면 다른 화면의 겹침이 발생한다.
- app header는 56px인데 main top padding은 80px이며, notification panel top은 62px이다. 공통 dimension token이 없어 위치가 어긋날 가능성이 있다.
- 다수의 30/36/40px icon button, 36px chip, 9–11px text가 있어 모바일 touch target과 가독성이 부족하다. 최소 44×44px interaction 영역을 기준으로 재검토해야 한다.
- `AppLayout`은 `h-dvh overflow-hidden` 안에 `main h-full overflow-y-auto`를 두며, 커뮤니티 fullscreen dialog와 지도 bottom sheet도 별도 내부 scroll을 만든다. keyboard 등장, overscroll, dialog open 시 scroll ownership이 복잡하다.
- 가로 필터/추천 목록은 의도적 horizontal scroll이지만 scrollbar/edge affordance가 화면별로 다르고, 긴 텍스트·날짜 input·5열 bottom nav의 320–360px 폭 검증이 필요하다.
- full-screen 지도 화면은 `overflow: hidden`과 absolute/fixed 요소가 많다. 작은 높이, landscape, 주소창 축소, 키보드가 열린 경우 검색 결과나 CTA가 가려질 수 있다.
- manifest icon/maskable icon 및 Apple touch icon이 없고, standalone 상태의 status-bar/splash 관련 메타도 없다.
- 별도 offline/error shell이 없어 service worker cache miss나 API offline 상태가 각 페이지의 제각각인 오류 UI로 드러난다.
