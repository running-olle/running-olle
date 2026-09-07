# Running Olle Design System v2

> 기준 자료: `docs/running_olle_design_system.md`, `docs/frontend-ui-audit.md`  
> 목표: 홈 Figma에서 확인된 러닝올레의 따뜻한 제주·러닝 정체성을 유지하면서, 현재 존재하는 모든 화면과 모바일 PWA 상태를 하나의 일관된 시스템으로 확장한다.  
> 상태 표기: **[유지]** 기존 규칙을 그대로 채택 · **[수정]** 기존 규칙을 서비스 전체 기준으로 조정 · **[추가]** 기존 문서에 없던 규칙을 정의

이 문서는 특정 서비스의 화면을 복제하지 않는다. 정보 위계, 충분한 여백, 명확한 CTA, 모바일 탐색 밀도, 러닝 수치의 즉시 판독성이라는 원칙만 적용한다. 시각 방향은 white에 가까운 neutral canvas와 surface를 기본으로 하고, orange는 행동과 상태를 강조할 때만 제한적으로 사용하는 깔끔한 앱 UI다.

---

## 0. v1 검토 요약

| 기존 영역 | 판정 | v2 결정 | 이유 |
| --- | --- | --- | --- |
| Warm base `#FFF8F6` | **[수정]** | neutral canvas `#F8F9FA`로 교체 | 넓은 주황 기운을 제거하고 orange accent와 콘텐츠 대비를 명확히 하기 위함 |
| Card도 base와 같은 배경 | **[수정]** | canvas `#F8F9FA`, surface `#FFFFFF`, subtle `#F4F5F7`로 분리 | card와 section을 border 또는 매우 약한 neutral 면으로 구분하기 위함 |
| Brand gradient `#FF6F0F → #FD934C` | **[수정]** | FAB와 예외적인 핵심 CTA에만 허용 | 큰 hero/card 배경에 사용하면 브랜드 색이 콘텐츠보다 먼저 보이기 때문 |
| Brown text palette | **[수정]** | neutral ink palette로 교체 | white/neutral 기반 화면에서 대비와 정보 위계를 더 명료하게 만들기 위함 |
| Noto Sans KR 단독 | **[수정]** | Pretendard 우선, Noto Sans KR fallback | 실제 global CSS와 일치시키고 숫자·한글 밀도가 높은 모바일 화면의 가독성 확보 |
| 9–10px tiny text | **[수정]** | 일반 UI 최소 12px, 예외적 legal/source만 11px | 실제 프로젝트에서 9–11px가 과다하며 모바일 가독성이 낮음 |
| 4px spacing scale | **[유지]** | 4px 기반 scale을 확장 | 기존 브랜드 화면과 현재 구현을 무리 없이 수렴 가능 |
| Radius 8/16/full | **[수정]** | 8/12/16/20/24/full 단계로 확장 | input, card, large surface, bottom sheet의 역할을 구분하기 위함 |
| 다수의 card shadow | **[수정]** | border 우선, elevation 0–3으로 제한 | 깔끔한 앱 UI와 정보 밀도를 위해 불필요한 부유감을 축소 |
| 56px Header | **[수정]** | 56px content height + top safe-area | standalone PWA에서 상태바와 콘텐츠 충돌 방지 |
| 83px BottomNavigation | **[수정]** | 64px content height + bottom safe-area | 기기별 inset을 높이에 포함하고 본문 padding과 단일 토큰으로 연결 |
| 390px 고정 frame | **[수정]** | 320–430px fluid, desktop에서 max 430px | 실제 구현의 430px canvas와 소형 모바일 대응을 모두 수용 |
| Button/Card/Input 기본안 | **[수정]** | 실제 전체 페이지 상태와 variant를 반영해 재정의 | 홈 외 화면에서 서로 다른 구현이 다수 존재 |
| Modal/Toast/State/Map/Z-index | **[추가]** | 전역 component와 layer contract 정의 | 현재 프로젝트에 실제 사용되지만 v1에 규칙이 없음 |

### 시스템 원칙

1. 한 화면의 강한 orange CTA는 원칙적으로 하나만 둔다.
2. 제목, 핵심 숫자, 다음 행동 순으로 시선이 이동해야 한다.
3. spacing과 typography로 먼저 위계를 만들고, border와 shadow는 필요한 경우에만 쓴다.
4. 모든 interactive target은 보이는 크기와 무관하게 최소 `44×44px` hit area를 보장한다.
5. 일반 본문은 12px 미만으로 내려가지 않는다.
6. 상태는 색만으로 전달하지 않고 label, icon, 형태 중 하나를 함께 사용한다.
7. app shell, fullscreen flow, map flow는 scroll owner와 safe-area owner를 각각 하나만 가진다.
8. 디자인 값은 CSS variable을 단일 원본으로 두고 Tailwind semantic alias와 component variant가 이를 참조한다. TSX/CSS에 raw hex, 임의 radius, 임의 shadow를 새로 추가하지 않는다.

---

## 1. Color

### 1.1 Brand and neutral

| Token | Value | 상태 | 용도 |
| --- | --- | --- | --- |
| `--color-brand-500` | `#FF6F0F` | **[유지]** | primary action, active indicator, 중요한 running accent |
| `--color-brand-400` | `#FD934C` | **[유지]** | 제한적 gradient end, illustration accent |
| `--color-brand-700` | `#A04100` | **[유지]** | 밝은 배경 위 brand text, active navigation label |
| `--color-brand-050` | `#FFF4EC` | **[수정]** | selected chip, 작은 icon 배경, 제한적인 accent surface |
| `--color-canvas` | `#F8F9FA` | **[수정]** | 앱 전체 기본 background. 넓은 면적에 사용하는 최상위 neutral |
| `--color-surface-subtle` | `#F4F5F7` | **[수정]** | group background, secondary section, skeleton base |
| `--color-surface` | `#FFFFFF` | **[유지]** | card, list, input, modal, sheet, header/navigation |
| `--color-surface-muted` | `#EEF0F2` | **[수정]** | disabled/placeholder 등 더 명확한 neutral 면. 브랜드 표현 용도로 사용하지 않음 |
| `--color-map-fallback` | `#E6E8EC` | **[추가]** | 지도 loading/fallback 전용 neutral |
| `--color-ink` | `#191F28` | **[수정]** | 제목, 핵심 값, 주요 본문 |
| `--color-ink-secondary` | `#4E5968` | **[수정]** | 설명, metadata, inactive navigation |
| `--color-ink-tertiary` | `#8B95A1` | **[수정]** | placeholder, timestamp, 낮은 우선순위 metadata |
| `--color-ink-disabled` | `#B0B8C1` | **[수정]** | disabled text/icon |
| `--color-border-subtle` | `#E5E8EB` | **[수정]** | card/list divider, 조용한 경계 |
| `--color-border-default` | `#D1D6DB` | **[수정]** | input, selected 구분, 강한 구조 경계 |
| `--color-overlay` | `rgba(25, 31, 40, 0.45)` | **[수정]** | modal/sheet backdrop |
| `--color-scrim-strong` | `rgba(16, 20, 28, 0.86)` | **[추가]** | live running screen lock 등 몰입형 차단 layer |

### 1.2 Semantic

| Token | Value | 상태 | 용도 |
| --- | --- | --- | --- |
| `--color-success` | `#168847` | **[수정]** | success text/icon. v1의 밝은 `#4ADE80`은 dot/indicator로만 사용 |
| `--color-success-indicator` | `#4ADE80` | **[유지]** | live dot, 작은 상태 indicator |
| `--color-success-subtle` | `#E8F7EC` | **[수정]** | success badge/notice 배경. 불투명 surface로 통일 |
| `--color-warning` | `#C2410C` | **[유지]** | warning text/icon, 난이도 중 |
| `--color-warning-subtle` | `#FFF0DF` | **[수정]** | warning badge/notice |
| `--color-danger` | `#B91C1C` | **[유지]** | destructive/error text/icon |
| `--color-danger-subtle` | `#FFF0EE` | **[수정]** | error background |
| `--color-info` | `#2563EB` | **[수정]** | link/map/info 상태. 실제 코드 사용값으로 정리 |
| `--color-info-subtle` | `#DBEAFE` | **[수정]** | info badge/notice |
| `--color-rating` | `#BFAC00` | **[유지]** | 별점. 본문/CTA에는 사용하지 않음 |

### 1.3 Color usage rules

- **[수정]** 기본 화면은 `neutral canvas → white surface → content`의 3단 위계를 사용한다. 앱 shell과 page background에 brand tint를 사용하지 않는다.
- **[수정]** primary button은 solid `brand-500`이 기본이다. gradient는 FAB 또는 한 화면의 유일한 핵심 CTA에만 허용하며 card, section, header, page background에는 사용하지 않는다.
- **[추가]** orange는 primary CTA, FAB, 활성 navigation, 주요 icon, 일부 badge/chip, 강조 text와 작은 `brand-050` accent surface에만 사용한다.
- **[추가]** `brand-050`도 화면의 큰 section이나 연속된 card 전체에 반복 사용하지 않는다. 넓은 보조 면은 `surface-subtle`을 사용한다.
- **[수정]** text와 neutral surface는 `ink` 계열과 neutral background token으로 수렴한다. 지도 자체와 지도 fallback은 예외다.
- **[추가]** `brand-700`은 밝은 배경 위 text용이다. `brand-500` 위 text는 항상 white를 사용한다.
- **[추가]** WCAG AA 대비를 목표로 하며, caption이라도 저대비 text를 장식적으로 흐리게 만들지 않는다.

---

## 2. Typography

### 2.1 Font family and weight

- **[수정]** 기본 font family: `Pretendard, "Noto Sans KR", system-ui, sans-serif`.
- **[추가]** 허용 weight: 400 Regular, 600 SemiBold, 700 Bold, 800 ExtraBold. 900/Black은 running 핵심 수치 또는 제한된 display에만 사용한다.
- **[추가]** 거리·시간·페이스·통계 숫자는 `font-variant-numeric: tabular-nums`를 적용한다. 숫자와 단위는 별도 위계로 구성하되 한 덩어리로 읽혀야 한다.
- **[추가]** 한글 기본 letter-spacing은 `-0.01em`, 24px 이상 제목/숫자는 `-0.02em`; caption에는 음수 자간을 쓰지 않는다.

### 2.2 Type scale

| Token | Size / line-height | Weight | 상태 | 대표 용도 |
| --- | --- | --- | --- | --- |
| `--text-running-display` | `36px / 40px` | 800–900 | **[추가]** | live 거리/시간/페이스, countdown |
| `--text-display` | `28px / 34px` | 800 | **[수정]** | 완료 수치, weather 온도, 핵심 dashboard 값 |
| `--text-page-title` | `24px / 32px` | 800 | **[추가]** | 화면 본문 최상위 제목 |
| `--text-section-title` | `20px / 28px` | 800 | **[수정]** | section header. v1 h1과 역할 정리 |
| `--text-app-title` | `18px / 24px` | 700 | **[수정]** | AppHeader 중앙 title |
| `--text-card-title` | `16px / 24px` | 700 | **[유지]** | card/list 주요 제목 |
| `--text-body` | `15px / 22px` | 400/600 | **[수정]** | 일반 본문과 주요 설명 |
| `--text-body-sm` | `14px / 20px` | 400/600 | **[유지]** | compact list, form text |
| `--text-label` | `13px / 18px` | 600/700 | **[추가]** | field label, button small, chip |
| `--text-caption` | `12px / 18px` | 400/600 | **[수정]** | metadata, badge, timestamp |
| `--text-legal` | `11px / 16px` | 400 | **[수정]** | 약관/출처처럼 보조적인 비상호작용 정보만 허용 |

### 2.3 Typography rules

- **[수정]** v1의 10px `text-tiny`는 폐기한다. 9–10px는 icon 내부 장식 외 UI text로 사용하지 않는다.
- **[추가]** 제목은 한 화면에서 최대 3단계(`page → section → card`)만 사용한다.
- **[추가]** CTA label은 14–16px/700, navigation label은 12px/600으로 고정한다.
- **[추가]** 숫자 stat은 숫자를 가장 크게, 단위를 caption 또는 label 크기로 두며 단위만 색을 지나치게 흐리지 않는다.
- **[추가]** 긴 제목은 list에서는 1줄 ellipsis, card에서는 최대 2줄, detail에서는 줄 수 제한 없이 표시한다.

---

## 3. Spacing

### 3.1 Scale

| Token | Value | 상태 | 용도 |
| --- | --- | --- | --- |
| `--space-0` | `0` | **[추가]** | reset |
| `--space-0-5` | `2px` | **[추가]** | indicator/optical adjustment만 허용 |
| `--space-1` | `4px` | **[유지]** | icon-text tight gap |
| `--space-2` | `8px` | **[유지]** | metadata/chip 내부 gap |
| `--space-3` | `12px` | **[유지]** | list/card gap |
| `--space-4` | `16px` | **[유지]** | compact surface padding |
| `--space-5` | `20px` | **[유지]** | 화면 좌우 gutter, standard card padding |
| `--space-6` | `24px` | **[유지]** | section gap |
| `--space-8` | `32px` | **[유지]** | 큰 section 구분 |
| `--space-10` | `40px` | **[추가]** | empty state 상하 여백 |
| `--space-12` | `48px` | **[추가]** | 몰입/완료 화면의 큰 구분 |

### 3.2 Layout spacing rules

- **[유지]** 표준 화면 좌우 gutter는 20px이다. 폭 360px 이하에서는 16px까지 줄일 수 있다.
- **[수정]** section 간 기본 간격은 24px, 서로 다른 정보 그룹은 32px이다. 현재의 임의 25/26/28/30px은 가까운 token으로 수렴한다.
- **[추가]** card 내부는 16px compact, 20px standard, 24px hero/detail 중 하나만 사용한다.
- **[추가]** vertical list gap은 12px, divider list는 gap 없이 row padding으로 밀도를 조절한다.
- **[추가]** fixed 요소와 본문 사이 여백은 개별 숫자가 아니라 `--app-header-total-height`, `--bottom-nav-total-height`, `--bottom-action-total-height`를 사용한다.

---

## 4. Radius

| Token | Value | 상태 | 용도 |
| --- | --- | --- | --- |
| `--radius-sm` | `8px` | **[유지]** | 작은 badge, thumbnail 내부 요소 |
| `--radius-control` | `12px` | **[추가]** | input, small button, compact control |
| `--radius-md` | `16px` | **[유지]** | 일반 card, primary button, notice |
| `--radius-lg` | `20px` | **[추가]** | 큰 card, menu group, map card |
| `--radius-sheet` | `24px` | **[추가]** | bottom sheet 상단, large modal |
| `--radius-full` | `9999px` | **[유지]** | chip, avatar, circular icon button/FAB |

- **[수정]** 기존 코드의 9/10/11/13/14/15/18/22/26/28/30/34px radius는 위 역할 기반 scale로 수렴한다.
- **[추가]** button을 습관적으로 pill로 만들지 않는다. 주요 CTA는 16px, chip과 compact inline action만 full radius다.
- **[추가]** 중첩 surface는 바깥 radius보다 안쪽 radius가 같거나 작아야 한다.

---

## 5. Shadow

| Token | Value | 상태 | 용도 |
| --- | --- | --- | --- |
| `--shadow-none` | `none` | **[추가]** | 기본 surface, divider list |
| `--shadow-1` | `0 2px 8px rgba(25,31,40,0.06)` | **[수정]** | interactive card, floating map control |
| `--shadow-2` | `0 6px 18px rgba(25,31,40,0.10)` | **[수정]** | popover, elevated map card, FAB |
| `--shadow-3` | `0 16px 40px rgba(25,31,40,0.18)` | **[수정]** | modal/panel |
| `--shadow-top` | `0 -4px 16px rgba(25,31,40,0.08)` | **[수정]** | bottom navigation/action bar/sheet |
| `--shadow-navigation` | `0 -2px 8px rgba(25,31,40,0.04)` | **[추가]** | BottomNavigation 전용 최소 elevation |
| `--shadow-fab` | `0 4px 12px rgba(25,31,40,0.14)` | **[추가]** | navigation 중앙 FAB |

- **[수정]** v1의 `shadow-card`, `shadow-section`은 `shadow-1`로 통합한다. 정적 card는 우선 `border-subtle` 또는 배경 차이만 사용하고, 눌러 이동하는 card에만 shadow를 허용한다.
- **[수정]** BottomNavigation의 기존 아래 방향 복합 shadow는 `shadow-top`으로 바꾼다.
- **[유지]** FAB는 일반 card보다 한 단계 높은 elevation을 유지한다.
- **[추가]** 한 surface에 border와 강한 shadow를 동시에 사용하지 않는다.

---

## 6. Button

### 6.1 Size

| Size | Height | Horizontal padding | Text | Icon | 상태 |
| --- | --- | --- | --- | --- | --- |
| `sm` | 40px, hit area 44px | 14px | 13px/700 | 18px | **[추가]** |
| `md` | 48px | 18px | 14px/700 | 20px | **[추가]** |
| `lg` | 52px | 20px | 16px/700 | 22px | **[수정]** |
| `icon` | 44×44px | 0 | accessible label 필수 | 20–24px | **[수정]** |

### 6.2 Variant

| Variant | 규칙 | 상태 |
| --- | --- | --- |
| `primary` | solid `brand-500`, white text, radius-md, shadow 없음 또는 floating일 때 shadow-1 | **[수정]** |
| `secondary` | surface, `border-default`, primary text | **[수정]** |
| `tertiary` | `brand-050`, `brand-700`, border 없음 | **[추가]** |
| `ghost` | transparent, primary 또는 brand-700 text | **[유지]** |
| `danger` | danger-bg/danger text; 최종 destructive confirm만 solid danger 허용 | **[추가]** |
| `icon` | surface/subtle background 선택, 원형 또는 radius-control | **[수정]** |
| `fab` | 56×56px, brand gradient, white icon, radius-full, shadow-2 | **[수정]** |

### 6.3 State and priority

- **[추가]** 한 화면의 bottom action에서 primary 1개, secondary 최대 1개를 원칙으로 한다. 3개 이상이면 vertical action list 또는 overflow action으로 바꾼다.
- **[추가]** pressed는 밝기 변화 또는 `scale(.98)` 중 하나만 사용하고, 120ms 이내로 끝낸다.
- **[추가]** focus-visible은 `2px brand-500` ring + `2px` offset을 사용한다.
- **[수정]** disabled는 opacity만 낮추지 않고 `bg-subtle`, `text-disabled`, shadow none을 사용한다. `aria-disabled` 또는 native `disabled`를 함께 적용한다.
- **[추가]** loading 중에는 폭이 변하지 않으며 label을 유지하거나 동일 폭의 “저장 중…”과 spinner를 함께 쓴다.
- **[추가]** camera, GPS, lock, pause, stop처럼 안전에 관련된 control은 icon만으로 보여도 accessible name과 48px 이상의 target을 보장한다.

---

## 7. Input / Textarea

### 7.1 Base

- **[수정]** 높이 48px(`md`) 또는 52px(`lg`), padding `0 14px`, surface background, `1px border-default`, radius-control.
- **[수정]** 입력 text는 15px/22px, placeholder는 `text-tertiary`; 16px 미만 입력이 iOS zoom을 유발하는 환경에서는 form control text를 16px로 올린다.
- **[추가]** label은 13px/700, input 위 8px 간격. helper/error/count는 12px/18px이며 input 아래 6px 간격.
- **[추가]** textarea는 최소 112px, padding 14px, vertical resize 허용 여부를 화면별로 명시한다.

### 7.2 State

| State | Border / background | 보조 표현 | 상태 |
| --- | --- | --- | --- |
| default | border-default / surface | 없음 | **[수정]** |
| hover | border-default 유지 | desktop pointer에서만 surface 변화 가능 | **[추가]** |
| focus | `2px brand-500` 또는 1px border + focus ring | label primary | **[수정]** |
| filled | default와 동일 | clear action 선택 | **[추가]** |
| error | danger / surface | error icon + message | **[수정]** |
| success | success / surface | 짧은 검증 message | **[추가]** |
| disabled | border-subtle / bg-subtle | text-disabled | **[수정]** |
| loading | 입력 유지 | 우측 18px spinner, 중복 요청 방지 | **[추가]** |

### 7.3 Specialized fields

- **[추가]** `SearchField`: 48px, 좌측 search icon, 입력값이 있을 때 우측 44px clear target. 결과 overlay와 input 사이 8px.
- **[추가]** `FileField/ImagePicker`: native input은 숨길 수 있으나 trigger, 선택 상태, 제거 action이 keyboard/accessibility로 동작해야 한다.
- **[추가]** `DateField`: 320px 화면에서 두 필드를 억지로 2열에 두지 않는다. 유효 폭이 부족하면 1열로 전환한다.
- **[추가]** character counter는 제한에 가까울 때만 강조하며 11px 이하로 축소하지 않는다.

---

## 8. Card / List

### 8.1 Surface variants

| Variant | 규칙 | 상태 |
| --- | --- | --- |
| `card-plain` | surface, radius-md, padding 16/20, border-subtle 또는 shadow-none | **[수정]** |
| `card-interactive` | card-plain + shadow-1 또는 pressed surface; 전체 card가 하나의 target | **[추가]** |
| `card-media` | image/map + body, radius-lg, overflow hidden | **[수정]** |
| `card-stat` | label → large number → unit, surface, radius-md | **[추가]** |
| `card-brand` | solid brand-500 + white on-brand text. 보조 text는 white 80–90% 범위이며 넓은 dashboard/section 용도로 반복 사용 금지 | **[수정]** |
| `list-row` | min-height 64px, padding 12–16px, divider-subtle | **[추가]** |
| `menu-group` | surface, radius-lg, 내부 row는 divider로 구분 | **[추가]** |

### 8.2 Existing home variants

- **[유지]** `card-course`: 240px 가로 scroll card 구조, 128px media, title/meta/badge 위계.
- **[유지]** `card-rank-row`: rank + 48px thumbnail + course info + participant count.
- **[유지]** `card-event`: 날짜 block + title의 간단한 row 구조.
- **[수정]** `card-weather`: compact한 solid brand-500 surface에 white text를 사용한다. headline → 온도/날씨 → 실시간 러너 순으로 구성하고 출처는 우측 하단 legal text 11px로 표시한다.

### 8.3 Rules

- **[추가]** card 안에 별도 CTA가 두 개 이상이면 card 전체 clickable 처리를 피하고 action 영역을 명확히 분리한다.
- **[추가]** course/run/meetup list는 title, 핵심 metadata 1줄, 상태/행동 순으로 밀도를 통일한다.
- **[추가]** 지도 thumbnail은 고정 aspect 또는 명시적 높이를 가지며 loading/fallback에서도 같은 크기를 유지한다.
- **[추가]** 빈 image는 브랜드 placeholder token과 일관된 icon을 사용하며 emoji를 기본 fallback으로 사용하지 않는다.

---

## 9. Badge / Chip / Tag

### 9.1 Role separation

- **[추가]** `Badge`: 읽기 전용 상태/분류. 높이 24px 또는 28px, 12px/700, radius-full.
- **[추가]** `FilterChip`: 목록 필터용 button. 높이 36px, hit area 44px, padding 0 14px.
- **[추가]** `ChoiceChip`: form 단일/복수 선택. 높이 44px, padding 0 16px.
- **[추가]** `Tag`: 콘텐츠 metadata. 읽기 전용이며 제거 가능할 때만 44px remove target을 제공한다.

### 9.2 State and tone

- **[수정]** active filter는 solid brand-500/white, inactive는 surface/border-default/text-secondary.
- **[추가]** selected form choice는 brand-050, brand-700 text, 2px brand-500 border를 사용해 filter와 역할을 구분한다.
- **[유지]** semantic tone은 success/warning/danger/info/neutral을 사용한다.
- **[추가]** course type, difficulty, visibility, bookmark, meetup status는 동일 `Badge` primitive의 tone만 바꾼다. 각 feature의 private Badge를 만들지 않는다.
- **[추가]** badge에 emoji를 기본 icon으로 사용하지 않고 필요할 때 14px system icon을 사용한다.

---

## 10. Modal / BottomSheet

### 10.1 Modal

- **[추가]** 간단한 확인/정보/작업은 center modal을 사용한다. 모바일 width는 `calc(100% - 32px)`, max-width 390px, radius-sheet, padding 20–24px, shadow-3.
- **[추가]** title 18px/700, body 14–15px, action gap 8px. destructive confirm은 취소를 먼저, 위험 action을 마지막에 둔다.

### 10.2 BottomSheet

- **[추가]** 선택 목록, 짧은 form, 완료 후 선택, 지도 위 장소 정보는 bottom sheet를 사용한다.
- **[추가]** top radius 24px, bottom radius 0, bottom padding에 safe-area를 포함한다.
- **[추가]** collapsed/half/full snap을 쓸 때 각 snap의 목적을 정의하고, drag handle target은 최소 44px 높이를 가진다.
- **[추가]** sheet 최대 높이는 `calc(100dvh - safe-area-top - 16px)`이며 header/action을 제외한 body만 scroll한다.

### 10.3 Fullscreen dialog

- **[추가]** 피드 작성/상세, 번개 작성/상세, 채팅처럼 긴 form·독립 탐색 문맥은 fullscreen dialog를 사용한다.
- **[추가]** `AppHeader + one scroll body + optional bottom action` 구조를 따른다. app body 뒤의 scroll은 잠근다.

### 10.4 Interaction/accessibility

- **[추가]** 모든 overlay는 `role="dialog"`, `aria-modal="true"`, accessible title, initial focus, focus return을 제공한다.
- **[추가]** backdrop tap close는 정보/선택 overlay에만 허용한다. 작성 중, saving 중, live run 종료 확인은 accidental close를 막는다.
- **[추가]** overlay open 동안 background scroll과 pointer interaction을 잠그고, Escape/Android back 처리 원칙을 동일하게 적용한다.
- **[추가]** z-index는 본 문서의 layer token만 사용한다. feature별 임의 29/40/70/80/90 값을 만들지 않는다.

---

## 11. Toast

- **[추가]** 단순 성공/정보 피드백에 사용하고, 중요한 error나 사용자의 선택이 필요한 상태에는 쓰지 않는다.
- **[추가]** viewport 하단 중앙, max-width `min(360px, calc(100% - 32px))`, min-height 44px, padding `12px 16px`, radius-full 또는 radius-md.
- **[추가]** 위치는 `bottom-nav/action-bar total height + 12px + safe-area-bottom`으로 계산한다.
- **[추가]** 기본 노출 2.5초, 짧은 설정 저장처럼 즉시 반복되는 피드백은 1.5–2초. 사용자가 읽어야 하는 긴 문장을 자동 소멸 toast로 표시하지 않는다.
- **[수정]** background는 `rgba(25,31,40,.92)`, text는 white, 13px/600. success/error 색으로 전체 배경을 과하게 바꾸지 않고 icon으로 tone을 보조한다.
- **[추가]** 동시에 하나만 표시하고 새 toast가 이전 toast를 교체한다. screen reader에는 polite live region을 사용한다.

---

## 12. Loading / Skeleton

- **[추가]** 첫 진입의 구조가 예측 가능한 list/card는 skeleton, 인증 검증·지도 SDK·저장처럼 진행 상태 자체가 중요한 경우 spinner/status를 사용한다.
- **[추가]** skeleton은 실제 콘텐츠와 동일한 크기를 유지하며 `bg-subtle`과 낮은 대비 shimmer를 사용한다. 과도한 orange shimmer는 쓰지 않는다.
- **[추가]** spinner size는 20px inline, 32px section, 40px page. stroke는 border-subtle, active stroke는 brand-500.
- **[추가]** loading text는 13–14px/secondary. 400ms 이내 응답에는 spinner가 번쩍이지 않도록 지연 표시를 고려하되 기존 API 동작은 바꾸지 않는다.
- **[추가]** 버튼 loading은 버튼 안에서 표시하며 page loading으로 전환하지 않는다.
- **[추가]** map loading은 map frame 크기를 보존하고 중앙 status를 표시한다.

---

## 13. Empty / Error State

### 13.1 Empty

- **[추가]** section empty는 padding 32px 20px, page empty는 최소 240px 높이와 padding 48px 20px.
- **[추가]** 48–56px muted icon, 16px/700 title, 13–14px secondary description, 필요할 때 하나의 primary/secondary action.
- **[추가]** dashed border는 사용자가 무언가 추가할 수 있는 creation empty에만 사용한다. 단순 결과 없음에는 borderless surface를 사용한다.

### 13.2 Error

- **[추가]** section error는 danger-bg notice, page error는 Empty 구조에 retry action을 추가한다.
- **[추가]** error와 empty를 같은 메시지/배열 fallback으로 시각적으로 혼동하지 않는다. 단, 리디자인 단계에서는 현재 service fallback 의미를 임의로 변경하지 않는다.
- **[추가]** inline form error는 해당 field 바로 아래, submit/general error는 action 위에 둔다.
- **[추가]** GPS, 지도 SDK, network, permission 오류는 원인과 사용자가 할 수 있는 다음 행동을 함께 보여준다.

---

## 14. Divider

- **[추가]** `--divider-color: var(--color-border-subtle)`, 기본 1px.
- **[추가]** inset divider는 leading content/avatar 뒤에서 시작하고, full divider는 app header/footer와 section 경계에만 사용한다.
- **[추가]** list row 사이에 divider를 쓰면 row gap/card shadow를 함께 쓰지 않는다.
- **[추가]** 섹션 구분은 우선 24–32px spacing으로 처리한다. divider는 정보 그룹의 관계가 이어질 때만 사용한다.
- **[수정]** Header/BottomNavigation의 `border-default`는 시각적으로 강하므로 `border-subtle`로 낮춘다.

---

## 15. SectionHeader

- **[수정]** `SectionTitle`을 `SectionHeader`로 확장한다. title 20px/800, optional leading icon 20px, optional description 14px, trailing action 13px/700.
- **[유지]** title과 content 사이는 기본 16–20px, section 간은 24–32px이다.
- **[추가]** emoji icon은 홈의 브랜드성 콘텐츠에서만 제한적으로 허용하고, 기능 icon에는 system SVG를 사용한다.
- **[추가]** trailing “전체보기”는 최소 44px hit area를 가지며 text 자체를 불필요하게 크게 만들지 않는다.
- **[추가]** 화면마다 같은 의미의 section title 크기를 임의로 17/18/21px로 만들지 않는다.

---

## 16. AppHeader

### 16.1 Anatomy

- **[수정]** content height 56px + `env(safe-area-inset-top)`을 total height로 사용한다.
- **[수정]** 좌우 gutter 16px 또는 표준 20px. leading/trailing slot은 각각 최소 44px 폭을 확보한다.
- **[수정]** title은 18px/700. 위치 기반 홈 header는 15px/700의 location label과 optional chevron을 허용한다.
- **[추가]** variant는 `root`(location/brand + actions), `detail`(back + centered title + action), `fullscreen`(close/back + title + submit) 세 가지다.
- **[추가]** background는 canvas 96% + blur 또는 surface 중 하나이며 한 흐름에서 일관되게 사용한다. bottom border는 border-subtle.

### 16.2 Behavior

- **[유지]** 일반 페이지 상단에 고정 또는 scroll container 내부 sticky로 사용한다.
- **[추가]** app 전체에서 `Header`, 마이 `PageHeader`, running/course/community header를 동일 anatomy로 수렴한다.
- **[추가]** back action은 route 흐름에서 정의된 handler를 그대로 사용하며, 무조건 동일 URL로 보내지 않는다.
- **[추가]** title 중앙 정렬은 좌우 action 폭과 무관하게 시각적으로 중앙이어야 한다.
- **[추가]** notification badge는 숫자 `99+` 상한을 사용하고 icon target을 침범하지 않는다.

---

## 17. BottomNavigation

### 17.1 Structure

- **[유지]** 홈 / 코스 / 중앙 러닝 / 커뮤니티 / 마이의 5개 구조와 pathname 기반 active mapping을 유지한다.
- **[유지]** 중앙 러닝은 일반 tab이 아니라 `/running` flow로 진입시키는 강조 action이다.
- **[수정]** navigation content height는 64px, 전체 높이는 `64px + env(safe-area-inset-bottom)`이다. 기존 고정 83px 상수는 폐기한다.
- **[수정]** 배경은 white surface, top border-subtle, `shadow-navigation`을 사용한다. 강한 색 배경·gradient·두꺼운 shadow를 navigation bar 자체에 사용하지 않는다.
- **[수정]** inner 영역은 max 430px, 좌우 gutter 8px + safe-left/right다. 작은 화면에서도 각 탭 폭을 균등하게 확보한다.
- **[수정]** tab icon은 24px SVG, icon slot은 36×28px, label은 12px/600이며 각 item의 64px 전체 높이를 target으로 사용한다.

### 17.2 State and behavior

- **[수정]** active icon/label은 brand-500/700, inactive는 ink-tertiary를 사용한다.
- **[수정]** 일반 tab의 active icon slot에는 brand-050 배경을 사용하고 scale 변화는 사용하지 않는다.
- **[수정]** 중앙 FAB가 active일 때 3px brand-050 ring과 orange label을 사용한다.
- **[추가]** live running, running ready/complete, course builder/save, fullscreen dialog에서는 숨긴다.
- **[추가]** 본문 bottom padding은 `bottom-nav total height + 24px`로 계산한다. 개별 페이지가 83/82/107px을 직접 쓰지 않는다.

---

## 18. FAB

- **[유지]** 브랜드 gradient, 원형, running icon, 다른 navigation item보다 위로 떠 있는 구조를 유지한다.
- **[수정]** 기본 52×52px, icon 24px, `shadow-fab`을 사용한다. navigation top 기준 vertical offset은 `-10px`로 제한해 과도하게 돌출되지 않게 한다.
- **[추가]** 중앙 navigation FAB 아래에는 `러닝` label을 12px/600으로 표시해 나머지 네 tab과 정보 구조를 맞춘다.
- **[추가]** FAB는 화면당 하나만 허용하며 primary bottom action과 동시에 경쟁시키지 않는다.
- **[추가]** bottom navigation 중앙 FAB 외 floating action이 필요한 경우 우하단 `20px + safe-area` 위치를 쓰되 map control과 겹치지 않는다.
- **[추가]** label 없는 FAB는 accessible name과 tooltip/title을 제공한다.

---

## 19. 지도 overlay UI

### 19.1 Map canvas

- **[추가]** 지도는 배경 layer(`z-map`)이며 화면 높이/카드 aspect를 먼저 확정한다. 지도 SDK가 mount된 뒤 display none으로 숨기지 않는다.
- **[추가]** loading/error에서도 동일 container 크기를 유지해 relayout과 layout shift를 방지한다.
- **[추가]** route planned는 info blue, recorded는 brand orange로 구분하고 legend에서 text와 함께 설명한다.

### 19.2 Controls

- **[추가]** 현재 위치/zoom/map action은 최소 48×48px, surface background, radius-full, shadow-1, icon 22px.
- **[추가]** control 간격 8px, viewport edge 16px 또는 20px, top/bottom safe-area와 app bar/sheet 높이를 포함해 위치를 계산한다.
- **[추가]** 지도 위 floating label/card는 surface 96%, blur는 최대 10px, text 대비를 유지한다.
- **[추가]** live running의 pause/stop은 일반 map control과 시각적으로 분리하고, stop은 danger 의미를 label 또는 형태와 함께 전달한다.

### 19.3 Map sheets and gestures

- **[추가]** 장소 상세, 주변 목록, 경유지 draft는 공통 BottomSheet anatomy를 사용한다.
- **[추가]** sheet가 half/full일 때 지도 interactive 영역을 명확히 남기거나 완전히 잠근다. 애매한 pointer pass-through를 허용하지 않는다.
- **[추가]** 검색 결과, sheet body, page body 중 동시에 두 개 이상이 같은 방향 scroll을 소유하지 않도록 한다.
- **[추가]** drag handle, map pan, horizontal chip scroll의 gesture 영역을 겹치지 않게 배치한다.
- **[추가]** 위치 권한 거부/정확도 낮음/경로 load 실패는 지도 위 notice와 재시도 action을 제공한다.

---

## 20. Page Layout

### 20.1 Viewport and canvas

- **[수정]** 지원 폭은 320–430px fluid mobile canvas다. 앱 내부 canvas는 `#F8F9FA`, 주요 surface는 white이며 430px 이상 외부 영역도 orange tint 없이 neutral 배경을 사용한다.
- **[수정]** 높이는 `100dvh`를 우선하고 fallback으로 `100vh`를 제공한다.
- **[추가]** 앱 shell의 scroll owner는 main 하나다. body/root와 main이 동시에 scroll하지 않게 한다.

### 20.2 Standard page

```text
AppViewport
├─ AppHeader: safe-top + 56px
├─ Main: one vertical scroll owner
│  └─ PageContent: 20px gutter, 24px section gap
└─ BottomNavigation: 64px + safe-bottom
```

- **[유지]** 일반 page gutter 20px, section gap 24px.
- **[수정]** main top/bottom inset은 header/nav total-height token으로 계산하고 `pt-20`, `pb-[107px]` 같은 literal을 사용하지 않는다.
- **[추가]** 페이지 제목이 AppHeader에 있으면 content에서 중복 h1을 만들지 않는다. root tab 화면처럼 content title이 필요한 경우 header는 위치/brand action 역할로 제한한다.

### 20.3 Fullscreen flow

- **[추가]** login/onboarding, course builder/save, running ready/live/complete, community fullscreen dialog는 BottomNavigation 없는 fullscreen shell을 사용한다.
- **[추가]** `header + scroll body + bottom action` 또는 `map + overlays` 중 하나를 선택한다. fixed header와 body padding이 서로 다른 숫자를 갖지 않는다.
- **[추가]** bottom action bar는 surface/blur + shadow-top, padding `12px 20px calc(12px + safe-bottom)`이며 본문은 action total height만큼 inset을 가진다.

### 20.4 Responsive and overflow

- **[추가]** 360px 이하에서 gutter는 16px, 2열 date/stat/action은 콘텐츠가 유지되지 않으면 1열로 전환한다.
- **[추가]** 의도적 carousel/filter만 horizontal scroll을 허용한다. page root에는 `overflow-x: clip`을 적용하고 긴 text에는 wrapping/min-width 0 규칙을 둔다.
- **[추가]** 가로 scroll은 다음 item 일부 노출, edge fade 또는 label로 스크롤 가능성을 알려준다.
- **[추가]** keyboard open 시 focused field와 submit/error가 보이도록 `dvh` 및 scroll-into-view를 검증한다.

---

## 21. Safe-area

- **[수정]** `index.html` viewport는 `width=device-width, initial-scale=1, viewport-fit=cover`를 전제로 한다.
- **[추가]** 전역 inset token:

| Token | Value |
| --- | --- |
| `--safe-top` | `env(safe-area-inset-top, 0px)` |
| `--safe-right` | `env(safe-area-inset-right, 0px)` |
| `--safe-bottom` | `env(safe-area-inset-bottom, 0px)` |
| `--safe-left` | `env(safe-area-inset-left, 0px)` |

- **[추가]** AppHeader가 top inset을, BottomNavigation 또는 bottom action이 bottom inset을 소유한다. 자식 화면이 같은 inset을 중복 padding하지 않는다.
- **[추가]** landscape에서는 좌우 fixed/map control에 safe-left/right를 포함한다.
- **[추가]** toast, FAB, map control, fixed error notice는 nav/action/sheet와 safe-bottom을 모두 고려한다.
- **[추가]** login hero 이미지와 screen lock scrim은 화면 끝까지 확장하되, interactive content만 safe-area 안에 둔다.

---

## 22. Z-index

| Token | Value | 상태 | 대상 |
| --- | --- | --- | --- |
| `--z-base` | `0` | **[추가]** | page content |
| `--z-map` | `0` | **[추가]** | map canvas |
| `--z-map-content` | `10` | **[추가]** | marker/route wrapper, 일반 map overlay |
| `--z-sticky` | `20` | **[추가]** | sticky tabs/section control |
| `--z-header` | `30` | **[추가]** | AppHeader |
| `--z-navigation` | `40` | **[추가]** | BottomNavigation, bottom action, FAB |
| `--z-popover` | `50` | **[추가]** | search result, notification panel |
| `--z-backdrop` | `100` | **[추가]** | modal/sheet backdrop |
| `--z-dialog` | `110` | **[추가]** | modal, bottom sheet, fullscreen dialog |
| `--z-toast` | `120` | **[추가]** | toast/global feedback |
| `--z-critical` | `130` | **[추가]** | screen lock, countdown, 안전 관련 blocking overlay |

- **[추가]** 같은 layer 내부에서는 DOM order로 해결하고 z-index를 1씩 증가시키지 않는다.
- **[추가]** 새로운 stacking context를 만드는 `transform`, `filter`, `opacity`, `isolation`을 app shell/overlay ancestor에 무심코 적용하지 않는다.
- **[추가]** notification panel은 popover이지만 fullscreen dialog가 열리면 닫히거나 dialog 아래에 있어야 한다.
- **[추가]** critical layer는 live running lock/countdown처럼 정말 모든 입력을 막아야 하는 경우에만 사용한다.

---

## 23. 구현 및 유지보수 규칙

### 23.1 Token architecture

- **[추가]** 모든 원시 값은 CSS custom property를 단일 원본으로 둔다.
- **[추가]** Tailwind v4에서는 semantic utility가 같은 custom property를 참조하도록 구성한다. `bg-[#FF6F0F]`, `text-[13px]`, `rounded-[16px]`, 임의 shadow를 새 코드에서 금지한다.
- **[추가]** component CSS가 필요한 지도/gesture/복잡한 state UI도 동일 token을 참조한다.
- **[추가]** 동적 이미지 URL, map SDK 좌표/선 스타일, drag transform 같은 runtime 값은 inline style을 허용한다. 정적 color/radius/spacing은 inline style로 쓰지 않는다.

### 23.2 Component hierarchy

```text
tokens
└─ primitives
   ├─ Button / IconButton / FAB
   ├─ Surface / Divider
   ├─ TextField / TextArea / SearchField / Switch
   ├─ Badge / FilterChip / ChoiceChip
   └─ Spinner / Skeleton
      └─ patterns
         ├─ AppHeader / BottomNavigation / SectionHeader
         ├─ Card / ListRow / StatGroup / AsyncState
         ├─ Modal / BottomSheet / FullscreenDialog / Toast
         └─ MapControl / MapNotice / MapSheet
            └─ domain UI
               ├─ CourseCard / CourseStats / CourseBadge
               ├─ RunStats / RunControlDock
               └─ MeetupCard / FeedCard / ChatRow
```

- **[추가]** domain component가 raw color/spacing을 소유하지 않고 primitive variant를 조합한다.
- **[추가]** `Button`, `Card`, `Badge`, `SectionTitle`은 홈 전용이 아니라 전체 화면 요구를 수용하는 public primitive/pattern으로 재정의한다.
- **[추가]** `PageHeader`, `Loading`, `Empty`, `Toggle`, feature별 private `Badge`처럼 반복되는 private 구현을 새로 만들지 않는다.
- **[추가]** 기능 controller(API, route state, GPS, timer, realtime, Zustand)는 presentational component와 분리하되, 리디자인 과정에서 동작 계약을 변경하지 않는다.

### 23.3 Icon and motion

- **[추가]** 하나의 SVG icon set을 사용하며 기본 stroke 2px, sizes 16/20/24/28px로 제한한다.
- **[추가]** emoji는 브랜드 illustration 또는 사용자 콘텐츠 fallback에만 제한하고 navigation/action icon으로 사용하지 않는다.
- **[추가]** motion duration은 120ms press, 180ms control, 220ms sheet이며 easing은 일관된 ease-out을 쓴다.
- **[추가]** `prefers-reduced-motion`에서는 shimmer, scale, sheet animation을 축소하거나 제거한다.

---

## 24. 실제 화면 적용 우선순위

1. Token, icon, Button/Input/Card/Badge/AsyncState primitive를 확정한다.
2. `AppViewport`, `AppHeader`, `BottomNavigation`, bottom action과 safe-area contract를 적용한다.
3. 홈과 단순 조회형 마이페이지로 typography/spacing/density를 검증한다.
4. `CourseListView`, `CourseDetailPage`, `CoursePreviewModal`, `BookmarksPage`의 공통 course presentation을 통합한다.
5. onboarding/profile/trip/course-save form을 동일 field/chip/switch 체계로 통합한다.
6. modal/bottom sheet/fullscreen dialog와 toast를 통합한다.
7. 커뮤니티의 overlay 전환과 realtime UI를 적용한다.
8. 마지막으로 `CourseBuilderPage`와 `LiveRunningPage`의 map/gesture/control UI를 적용한다.

각 단계에서 기존 API 호출, route state, handler, GPS, timer, realtime, Zustand 동작은 변경하지 않는다. 시각 컴포넌트의 적용 전후로 loading/empty/error/disabled/open 상태와 320/360/390/430px, standalone safe-area, keyboard, map relayout을 확인한다.

---

## 25. 완료 기준

- 모든 색상, 글꼴, spacing, radius, shadow, layer 값이 token을 참조한다.
- 동일 역할의 header, button, input, chip, toggle, card, modal, state UI가 동일 component/variant를 사용한다.
- 일반 interactive target은 최소 44×44px이며, form text와 주요 metadata는 12px 미만이 아니다.
- 모든 fixed/sticky UI의 inset이 safe-area와 하나의 shell dimension contract로 계산된다.
- BottomNavigation, bottom action, toast, FAB, map control, sheet가 서로 겹치지 않는다.
- page body와 overlay body의 scroll owner가 명확하며 horizontal overflow는 의도한 carousel/filter에만 존재한다.
- 거리, 시간, 페이스는 tabular number와 일관된 stat hierarchy로 즉시 읽힌다.
- 한 화면에 경쟁하는 primary CTA가 없고 destructive action은 명확히 분리된다.
- PWA standalone의 320–430px 폭, 짧은 화면, keyboard, GPS permission/error, offline/loading 상태에서 핵심 행동이 가려지지 않는다.
- 앱 shell과 page background는 white/neutral이며 warm orange tint가 넓은 면적으로 남아 있지 않는다.
- 브랜드 orange는 CTA, FAB, active navigation, 주요 icon과 제한적 상태 표현에만 사용한다.
- gradient는 FAB 또는 한 화면의 유일한 핵심 CTA 외에는 사용하지 않는다.
