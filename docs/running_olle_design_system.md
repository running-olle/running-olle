# 러닝올레 (Running Olle) 디자인 시스템

> 근거: Figma 홈 화면(node `118:156`) 분석 결과 기반.
> 표기 규칙: **[확정]** = Figma에서 직접 확인된 값 / **[유추]** = 홈 화면에는 없지만 시스템 일관성을 위해 확장·제안한 값 (팀 확인 필요).

---

## 1. 색상 토큰 (Color Tokens)

### 1-1. Base / Surface [확정]
| 토큰명 | 값 | 용도 |
|---|---|---|
| `color-bg-base` | `#FFF8F6` | 앱 기본 배경, Header/BottomNav/Card 배경 |
| `color-surface-muted` | `#F7DDD3` | 이미지 placeholder, 썸네일 배경 |
| `color-border` | `#E1BFB1` | Header 하단 보더, BottomNav 상단 보더 |

### 1-2. Text [확정]
| 토큰명 | 값 | 용도 |
|---|---|---|
| `color-text-primary` | `#261912` | 제목, 본문 핵심 텍스트 |
| `color-text-secondary` | `#594136` | 메타 정보, 비활성 네비 라벨 |

### 1-3. Brand / Accent [확정]
| 토큰명 | 값 | 용도 |
|---|---|---|
| `color-brand-gradient-start` | `#FF6F0F` | 브랜드 그라데이션 시작 (135deg) |
| `color-brand-gradient-end` | `#FD934C` | 브랜드 그라데이션 끝 (135deg) |
| `color-brand-accent-1` | `#A04100` | 활성 네비 라벨, 랭킹 1위, 참여인원 텍스트, 날짜(일) |
| `color-brand-accent-2` | `#994700` | 랭킹 2위 숫자 |
| `color-brand-accent-3` | `#8D7164` | 랭킹 3위 숫자 |

### 1-4. Semantic / Status [확정 + 유추]
| 토큰명 | 값 | 용도 | 근거 |
|---|---|---|---|
| `color-success` | `#4ade80` | 활동중 표시 dot | 확정 |
| `color-success-text` | `#15803D` | 난이도-하 텍스트 | 확정 |
| `color-success-bg` | `rgba(74,222,128,0.1)` | 난이도-하 배경 | 확정 |
| `color-warning-text` | `#C2410C` | 난이도-중 텍스트 | 확정 |
| `color-warning-bg` | `rgba(251,146,60,0.1)` | 난이도-중 배경 | 확정 |
| `color-rating` | `#BFAC00` | 별점 텍스트 | 확정 |
| `color-danger-text` | `#B91C1C` | 난이도-상 텍스트 | **유추** (하/중 톤 매핑 기준 red-700 제안) |
| `color-danger-bg` | `rgba(239,68,68,0.1)` | 난이도-상 배경 | **유추** |
| `color-info-text` | `#1D4ED8` | 정보성 배지(추후 알림/공지 등) | **유추** |
| `color-info-bg` | `rgba(59,130,246,0.1)` | 정보성 배지 배경 | **유추** |

### 1-5. Overlay / On-Brand [확정]
| 토큰명 | 값 | 용도 |
|---|---|---|
| `color-on-brand` | `#FFFFFF` | 그라데이션 카드 위 텍스트 |
| `color-on-brand-muted` | `rgba(255,255,255,0.9)` | 그라데이션 카드 보조 텍스트 |
| `color-overlay-pill` | `rgba(255,255,255,0.2)` | 그라데이션 카드 내 pill 배경 (backdrop-blur 2px 동반) |

### Tailwind config 매핑 예시
```js
// tailwind.config.js
colors: {
  bg: { base: '#FFF8F6', muted: '#F7DDD3' },
  border: { DEFAULT: '#E1BFB1' },
  text: { primary: '#261912', secondary: '#594136' },
  brand: {
    accent1: '#A04100',
    accent2: '#994700',
    accent3: '#8D7164',
    from: '#FF6F0F',
    to: '#FD934C',
  },
  success: { DEFAULT: '#4ade80', text: '#15803D', bg: 'rgba(74,222,128,0.1)' },
  warning: { text: '#C2410C', bg: 'rgba(251,146,60,0.1)' },
  danger: { text: '#B91C1C', bg: 'rgba(239,68,68,0.1)' }, // 유추
  rating: '#BFAC00',
}
```

---

## 2. Typography 토큰

폰트 패밀리: **Noto Sans KR** [확정] — Regular / Bold / Black(=ExtraBold) 3가지 굵기만 홈 화면에서 확인됨.

| 토큰명 | size / line-height | weight | 확인 상태 | 용도 예시 |
|---|---|---|---|---|
| `text-display` | 26px / 26px | Black | 확정 | 온도 등 강조 숫자 |
| `text-h1` | 20px / 28px | Black | 확정 | 랭킹 번호 |
| `text-h2` | 16px / 24px | Bold | 확정 | 섹션 제목, 카드 제목 |
| `text-h3` | 15px / 28px | Bold | 확정 | Header 위치명 |
| `text-body` | 14px / 20px | Bold / Regular | 확정 | 리스트 제목, 날씨 설명 |
| `text-caption` | 12px / 16px | Bold / Regular | 확정 | 메타정보, 배지, 라벨 |
| `text-tiny` | 10px / 15px | Bold / Regular | 확정 | 난이도 배지, 보조 메타 |
| `text-h4` | 18px / 26px | Bold | **유추** | 상세 페이지 등 26/20 사이 중간 위계 필요 시 |
| `letter-spacing-tight` | -0.52px | - | 확정 (display에만 적용) | 큰 숫자 강조 시 |

> 참고: 홈 화면에는 Medium/SemiBold 굵기가 없습니다. 다른 화면에서 필요해지면 임의로 추가하지 말고 먼저 확인이 필요합니다.

---

## 3. Spacing Scale

4px 배수 체계로 재구성 (Figma 실측값 기반) [확정 값 위주로 스케일화]:

| 토큰명 | 값 | 확인 상태 |
|---|---|---|
| `space-1` | 4px | 확정 (카드 내부 텍스트 gap) |
| `space-2` | 8px | 확정 (배지 padding, 리스트 row gap 일부) |
| `space-3` | 12px | 확정 (카드 간 gap, 리스트 아이템 gap) |
| `space-4` | 16px | 확정 (섹션/카드 내부 padding) |
| `space-5` | 20px | 확정 (화면 좌우 패딩, 배너 padding) |
| `space-6` | 24px | 확정 (섹션 간 gap) |
| `space-8` | 32px | **유추** (대형 섹션 구분용, 필요 시) |

**레이아웃 상수** [확정]
- 화면 좌우 패딩: `20px`
- 섹션 간 세로 간격: `24px`
- Header 높이: `56px`
- BottomNav 높이: `83px`
- 스크롤 영역 하단 여백(BottomNav 겹침 방지): `82px`

---

## 4. Radius Scale [확정]

| 토큰명 | 값 | 용도 |
|---|---|---|
| `radius-sm` | 8px | 썸네일, 리스트 row 배경 |
| `radius-md` | 16px | 카드, 섹션 컨테이너 |
| `radius-full` | 9999px | pill 배지, 원형 버튼, FAB, 아바타 |

---

## 5. Shadow [확정]

| 토큰명 | 값 | 용도 |
|---|---|---|
| `shadow-card` | `0px 4px 12px rgba(0,0,0,0.05)` | 카드형 컴포넌트 (추천 코스 카드, 행사 카드, WeatherCard) |
| `shadow-section` | `0px 4px 6px rgba(0,0,0,0.05)` | 섹션 컨테이너 (인기 코스 리스트) |
| `shadow-nav` | `0px 10px 15px -3px rgba(0,0,0,0.1), 0px 4px 6px -4px rgba(0,0,0,0.1)` | BottomNavigation (위쪽 방향 그림자) |
| `shadow-fab` | `drop-shadow(0px 4px 6px rgba(0,0,0,0.2))` | FAB 버튼 |

---

## 6. Button Variant

> 홈 화면에서 확인 가능한 버튼은 **아이콘 버튼(알림)**, **네비게이션 탭 버튼**, **FAB** 3종뿐입니다. 텍스트형 Primary/Secondary 버튼은 화면에 없어 브랜드 컬러 기준으로 **유추** 제안합니다. 실제 도입 전 디자이너 확인 권장합니다.

| Variant | 배경 | 텍스트/아이콘 색 | Radius | Shadow | 확인 상태 |
|---|---|---|---|---|---|
| `icon` (Header 알림 버튼) | `#F5F5F5` (opacity 80%) | `#261912` 계열 아이콘 | `radius-full` | 없음 | 확정 |
| `fab` (러닝 시작) | gradient `#FF6F0F→#FD934C`, 135deg | white 아이콘 | `radius-full` | `shadow-fab` | 확정 |
| `nav-tab-active` | 없음(투명) | 아이콘 + `color-brand-accent-1`, bold 12px | - | - | 확정 |
| `nav-tab-inactive` | 없음(투명) | 아이콘 + `color-text-secondary`, bold 12px | - | - | 확정 |
| `primary` (텍스트 버튼) | gradient `#FF6F0F→#FD934C` | white, bold 14px | `radius-md` (또는 full) | `shadow-card` | **유추** |
| `secondary` (텍스트 버튼) | `#FFF8F6` + border `#E1BFB1` | `color-text-primary` | `radius-md` | 없음 | **유추** |
| `ghost` | 투명 | `color-brand-accent-1` | - | - | **유추** |
| `disabled` | `#F5F5F5` | `#B0A69E` | 동일 | 없음 | **유추** |

---

## 7. Card Variant

| Variant | 특징 | 근거 |
|---|---|---|
| `card-course` (추천 코스 카드) | 폭 240px, 썸네일 128px + 우상단 카테고리 배지 + 본문(제목/메타/별점/난이도), `radius-md`, `shadow-card` | 확정 |
| `card-rank-row` (인기 코스 랭킹 행) | 순위 숫자 + 48×48 썸네일(`radius-sm`) + 텍스트 + 참여인원, 배경 투명(부모 섹션이 `shadow-section` 보유) | 확정 |
| `card-event` (행사 카드) | 좌측 날짜 블록(`bg-surface-muted`) + 우측 제목, `radius-md`, `shadow-card` | 확정 |
| `card-weather` (배너형) | 그라데이션 배경, 장식 아이콘, `radius-md`, `shadow-card`, on-brand 텍스트 | 확정 |
| `card-plain` (기본형) | `bg-base`, `radius-md`, `shadow-card`, padding `space-4` — 위 variant들의 공통 베이스 | **유추** (공통 쉘로 추상화 제안) |

---

## 8. Input Variant

> **홈 화면에는 입력 필드(텍스트 인풋, 검색창, 셀렉트 등)가 전혀 없습니다.** 아래는 컬러/보더/라운드 토큰을 기준으로 최소한의 형태만 **유추** 제안한 것이며, 실제 입력 컴포넌트가 필요한 화면(예: 검색, 프로필 수정, 후기 작성)의 Figma가 확보되면 반드시 재검증해야 합니다.

| Variant | 배경 | 보더 | Radius | 텍스트 색 | 확인 상태 |
|---|---|---|---|---|---|
| `input-default` | `#FFFFFF` | `1px solid #E1BFB1` | `radius-sm` | `color-text-primary` | **유추** |
| `input-focus` | `#FFFFFF` | `1px solid color-brand-accent-1` | `radius-sm` | `color-text-primary` | **유추** |
| `input-error` | `#FFFFFF` | `1px solid color-danger-text` | `radius-sm` | `color-text-primary` | **유추** |
| `input-disabled` | `#F5F5F5` | `1px solid #E1BFB1` | `radius-sm` | `#B0A69E` | **유추** |
| placeholder 색 | - | - | - | `color-text-secondary` | **유추** |

---

## 9. Navigation 규칙

### BottomNavigation [확정]
- 항상 화면 하단 고정 (`fixed`/`sticky bottom-0`), 앱 전역 공통 (러닝 진행 중 같은 몰입 화면 제외 여부는 별도 확인 필요)
- 5개 탭: 홈 / 코스 / **러닝(FAB, 중앙 강조)** / 커뮤니티 / 마이
- 활성 탭: 아이콘+라벨 색 `color-brand-accent-1`, 아이콘 `scale(0.95)`
- 비활성 탭: `color-text-secondary`
- 중앙 러닝 탭은 다른 탭과 달리 **FAB 스타일**(그라데이션 원형, 위로 튀어나옴, 그림자)로 시각적 우선순위를 가짐 → 탭 이동이 아니라 "러닝 시작" 액션으로 동작할 가능성 높음 (라우팅 vs 모달/플로우 진입 여부는 기획 확인 필요)
- 라우트와 활성 탭 매핑은 React Router의 현재 pathname 기준으로 판단

### Header [확정 + 유추]
- 페이지 상단 고정, 높이 56px, 배경 `color-bg-base`, 하단 보더 `color-border`
- 좌측 슬롯: 페이지별 타이틀/위치 텍스트 등 커스터마이즈 가능 [확정: 홈 화면 기준]
- 우측 슬롯: 아이콘 버튼 0~n개 (홈은 알림 1개) [확정 1개 사례, n개 확장은 **유추**]
- 상태바 여백(46px)은 실제 웹앱에서는 제외, PWA 환경에서 `env(safe-area-inset-top)` 등으로 대체 고려 **[유추]**

---

## 10. 페이지 레이아웃 규칙

- **뷰포트 기준**: 390px 고정 폭 모바일 프레임 (breakpoint 변형 없음, 모바일 퍼스트) [확정: Figma에 다른 폭 프레임 없음]
- **전체 구조**: `Header(56px, fixed top)` + `Main(scrollable)` + `BottomNavigation(83px, fixed bottom)` — 3단 고정 레이아웃 [확정]
- **Main 콘텐츠 영역**:
    - 좌우 패딩 `space-5`(20px)
    - 상단 패딩 `space-4`(16px)
    - 하단 패딩 `82px` (BottomNav에 가려지지 않도록)
    - 섹션 간 세로 gap `space-6`(24px)
    - `overflow-y: auto`
- **섹션 내부 구조**: `SectionTitle` → 콘텐츠(카드 리스트/그리드) 순서, 카드/리스트 간 gap은 `space-3`(12px) 기준 [확정]
- **가로 스크롤 영역**(추천 코스 등): 카드 폭 고정(240px) + `overflow-x: auto`, 스크롤바 숨김 처리 권장 [확정 구조 + 스크롤바 스타일은 **유추**]
- **최대 폭 제한**: PWA를 데스크톱 브라우저에서도 열람할 경우, 콘텐츠를 390~430px로 제한하고 중앙 정렬하는 것을 권장 [**유추**, Figma에 명시 없음 — 팀 확인 필요]