# DESIGN.md — MiREMS Platform UI/UX Design Specification

> **대상 독자:** AI Agent (Hermes) 및 Frontend 개발자
> **목적:** MiREMS Platform의 모든 UI/UX 구현에 대한 단일 진실 공급원(Single Source of Truth)
> **준수 기준:** NIST VVSG 2.0 Vol. I §4, WCAG 2.1 Level AA, ARIA 1.2
> **연관 파일:** `README.md`, `PLAN.md`, `docs/auth/ROLE_MATRIX.md`, `docs/vvsg/VVSG2_MAPPING.md`

---

## 목차

1. [Design Philosophy](#1-design-philosophy)
2. [Design Tokens](#2-design-tokens)
3. [Typography System](#3-typography-system)
4. [Spacing & Grid System](#4-spacing--grid-system)
5. [Component Library — ui-core](#5-component-library--ui-core)
6. [Shell Layout & Navigation](#6-shell-layout--navigation)
7. [Theming System](#7-theming-system)
8. [Accessibility Standards](#8-accessibility-standards)
9. [Role-Based UI Rendering](#9-role-based-ui-rendering)
10. [BPMN Process-Aware UI Patterns](#10-bpmn-process-aware-ui-patterns)
11. [Page Specifications](#11-page-specifications)
12. [Voting Kiosk UI (VVSG Special)](#12-voting-kiosk-ui-vvsg-special)
13. [Extension Pack UI Integration](#13-extension-pack-ui-integration)
14. [Responsive Design](#14-responsive-design)
15. [Internationalization (i18n)](#15-internationalization-i18n)
16. [Motion & Animation](#16-motion--animation)
17. [Icon System](#17-icon-system)
18. [Error & Loading States](#18-error--loading-states)
19. [Agent Implementation Checklist](#19-agent-implementation-checklist)

---

## 1. Design Philosophy

### 1.1 핵심 원칙

MiREMS는 선거라는 인류의 가장 중요한 민주주의 절차를 지원하는 플랫폼이다. UI는 다음 세 가지 원칙에서 한 치도 벗어나지 않는다.

**Trustworthy Authority (신뢰할 수 있는 권위)**
모든 화면은 공식 공공기관 시스템의 중량감을 전달해야 한다. 경박하거나 화려한 디자인 요소를 배제하고, 제도적 신뢰를 시각적으로 구현한다. 색상·서체·레이아웃은 "이 시스템은 믿을 수 있다"는 메시지를 무언 중에 전달해야 한다.

**Radical Clarity (급진적 명료성)**
모든 상태(State), 전환(Transition), 역할(Role), 제약(Constraint)은 사용자가 명시적으로 이해할 수 있는 방식으로 표현되어야 한다. 숨겨진 기능, 모호한 레이블, 추측을 요구하는 인터페이스는 허용되지 않는다. BPMN 프로세스의 현재 단계와 다음 행동은 항상 화면 위에 존재해야 한다.

**Accessible by Default (기본값으로 접근 가능)**
WCAG 2.1 AA는 타협의 여지없이 모든 컴포넌트에 적용된다. 투표 Kiosk UI는 그보다 엄격한 VVSG 2.0 §4 기준(고대비, 대형 터치 타겟, 스크린리더 완전 지원, 키보드 전용 내비게이션)을 준수한다.

### 1.2 Aesthetic Direction — "Civic Precision"

**테마 키워드:** Institutional · Deliberate · Transparent · Monumental · Precise

선거 IT 시스템의 UI는 소비자 SaaS 대시보드나 스타트업 스타일의 UI를 절대 참조하지 않는다. 대신 다음을 참조한다:

- 미국 연방 정부 문서의 정보 계층구조 (US Government Printing Office)
- 스위스 국제 타이포그래피 스타일 (Swiss International Typographic Style)
- 법원 판결문·관보의 텍스트 밀도와 엄격한 그리드

**색채 전략:** 짙은 해군청(Navy)을 지배 색으로, 호박색(Amber)을 경보·강조 액센트로, 녹회색(Sage Green)을 성공 상태로 사용한다. 흰색/회색 배경 위에 색을 절제하며, 컬러는 기능적 의미를 전달할 때만 사용한다.

**타이포그래피 전략:** 헤딩에는 `IBM Plex Serif`(공공기관의 권위와 가독성), 본문·UI 레이블에는 `IBM Plex Sans`(뛰어난 가독성과 기술적 명료성), 코드·해시값에는 `IBM Plex Mono`를 사용한다. IBM Plex 패밀리는 전 세계 공공기관 시스템에서 검증된 서체로, 한글(`Noto Sans KR`)과의 조합도 자연스럽다.

---

## 2. Design Tokens

`packages/ui-core/src/tokens/` 디렉토리에 위치한다. 모든 값은 CSS Custom Properties로 선언되며, JavaScript/TypeScript 상수로도 export된다.

### 2.1 Color Tokens

```css
/* packages/ui-core/src/tokens/colors.css */
:root {
  /* ─── Brand Core ─────────────────────────────────────────── */
  --color-navy-950: #050d1a;
  --color-navy-900: #0a1628;
  --color-navy-800: #102040;
  --color-navy-700: #1a3058;
  --color-navy-600: #244478;
  --color-navy-500: #2e5899;
  --color-navy-400: #4a78c4;
  --color-navy-300: #7aa0d8;
  --color-navy-200: #b0c8ec;
  --color-navy-100: #d8e6f7;
  --color-navy-50:  #eef4fb;

  /* ─── Accent: Amber (경보·강조·경고) ─────────────────────── */
  --color-amber-900: #451a00;
  --color-amber-700: #b54708;
  --color-amber-500: #f79009;
  --color-amber-400: #fbb040;
  --color-amber-200: #fde8c0;
  --color-amber-100: #fef5e4;
  --color-amber-50:  #fffbf0;

  /* ─── Success: Sage Green (완료·인증·승인) ───────────────── */
  --color-sage-900: #0d3320;
  --color-sage-700: #1d6843;
  --color-sage-500: #3aaa6e;
  --color-sage-400: #5fc08a;
  --color-sage-200: #b8e4cc;
  --color-sage-100: #daf0e5;
  --color-sage-50:  #f0faf5;

  /* ─── Danger: Crimson (오류·거부·위험) ──────────────────── */
  --color-crimson-900: #3b0a0a;
  --color-crimson-700: #991414;
  --color-crimson-500: #d92020;
  --color-crimson-400: #e65555;
  --color-crimson-200: #f8c4c4;
  --color-crimson-100: #fce8e8;
  --color-crimson-50:  #fff5f5;

  /* ─── Neutral ────────────────────────────────────────────── */
  --color-neutral-950: #080c10;
  --color-neutral-900: #111620;
  --color-neutral-800: #1f2837;
  --color-neutral-700: #2f3c50;
  --color-neutral-600: #445168;
  --color-neutral-500: #5f7080;
  --color-neutral-400: #8096a8;
  --color-neutral-300: #a8bcc8;
  --color-neutral-200: #ccd8e0;
  --color-neutral-100: #e4ecf0;
  --color-neutral-50:  #f4f7f9;
  --color-neutral-0:   #ffffff;

  /* ─── Semantic (Light Theme Default) ────────────────────── */
  --color-bg-canvas:       var(--color-neutral-50);
  --color-bg-surface:      var(--color-neutral-0);
  --color-bg-surface-alt:  var(--color-neutral-50);
  --color-bg-overlay:      rgba(5, 13, 26, 0.48);
  --color-bg-inset:        var(--color-neutral-100);

  --color-border-default:  var(--color-neutral-200);
  --color-border-strong:   var(--color-neutral-300);
  --color-border-focus:    var(--color-navy-500);
  --color-border-error:    var(--color-crimson-500);

  --color-text-primary:    var(--color-neutral-900);
  --color-text-secondary:  var(--color-neutral-600);
  --color-text-disabled:   var(--color-neutral-400);
  --color-text-inverse:    var(--color-neutral-0);
  --color-text-link:       var(--color-navy-600);
  --color-text-link-hover: var(--color-navy-800);

  --color-interactive-primary:       var(--color-navy-700);
  --color-interactive-primary-hover: var(--color-navy-800);
  --color-interactive-primary-press: var(--color-navy-900);

  --color-status-draft:      var(--color-neutral-500);
  --color-status-published:  var(--color-navy-500);
  --color-status-active:     var(--color-sage-500);
  --color-status-closed:     var(--color-amber-500);
  --color-status-certified:  var(--color-sage-700);
  --color-status-pending:    var(--color-amber-400);
  --color-status-approved:   var(--color-sage-500);
  --color-status-rejected:   var(--color-crimson-500);

  /* ─── Process Node Colors (BPMN) ────────────────────────── */
  --color-process-active:    var(--color-navy-500);
  --color-process-waiting:   var(--color-amber-400);
  --color-process-completed: var(--color-sage-500);
  --color-process-error:     var(--color-crimson-500);
  --color-process-skipped:   var(--color-neutral-300);
}
```

### 2.2 Elevation & Shadow Tokens

```css
:root {
  --shadow-xs:  0 1px 2px rgba(8, 12, 16, 0.06);
  --shadow-sm:  0 1px 3px rgba(8, 12, 16, 0.10), 0 1px 2px rgba(8, 12, 16, 0.06);
  --shadow-md:  0 4px 6px rgba(8, 12, 16, 0.07), 0 2px 4px rgba(8, 12, 16, 0.06);
  --shadow-lg:  0 10px 15px rgba(8, 12, 16, 0.10), 0 4px 6px rgba(8, 12, 16, 0.05);
  --shadow-xl:  0 20px 25px rgba(8, 12, 16, 0.12), 0 10px 10px rgba(8, 12, 16, 0.04);
  --shadow-2xl: 0 25px 50px rgba(8, 12, 16, 0.25);
  --shadow-focus-ring: 0 0 0 3px rgba(46, 88, 153, 0.40);
  --shadow-focus-ring-error: 0 0 0 3px rgba(217, 32, 32, 0.30);
}
```

### 2.3 Border Radius Tokens

```css
:root {
  --radius-none: 0;
  --radius-xs:   2px;
  --radius-sm:   4px;
  --radius-md:   6px;
  --radius-lg:   8px;
  --radius-xl:   12px;
  --radius-2xl:  16px;
  --radius-full: 9999px;
}
```

### 2.4 Z-Index Scale

```css
:root {
  --z-below:   -1;
  --z-base:    0;
  --z-raised:  10;
  --z-dropdown: 100;
  --z-sticky:  200;
  --z-overlay: 300;
  --z-modal:   400;
  --z-toast:   500;
  --z-tooltip: 600;
}
```

---

## 3. Typography System

### 3.1 Font Stack

```css
/* packages/ui-core/src/tokens/typography.css */
@import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Serif:wght@400;600;700&family=IBM+Plex+Sans:wght@300;400;500;600;700&family=IBM+Plex+Mono:wght@400;500&family=Noto+Sans+KR:wght@400;500;700&display=swap');

:root {
  --font-serif:  'IBM Plex Serif', 'Noto Serif KR', Georgia, serif;
  --font-sans:   'IBM Plex Sans', 'Noto Sans KR', -apple-system, sans-serif;
  --font-mono:   'IBM Plex Mono', 'Fira Code', 'Consolas', monospace;
}
```

### 3.2 Type Scale

```css
:root {
  /* Size */
  --text-xs:   0.75rem;   /* 12px */
  --text-sm:   0.875rem;  /* 14px */
  --text-base: 1rem;      /* 16px */
  --text-md:   1.125rem;  /* 18px */
  --text-lg:   1.25rem;   /* 20px */
  --text-xl:   1.5rem;    /* 24px */
  --text-2xl:  1.875rem;  /* 30px */
  --text-3xl:  2.25rem;   /* 36px */
  --text-4xl:  3rem;      /* 48px */

  /* Weight */
  --font-light:    300;
  --font-regular:  400;
  --font-medium:   500;
  --font-semibold: 600;
  --font-bold:     700;

  /* Line Height */
  --leading-tight:  1.25;
  --leading-snug:   1.375;
  --leading-normal: 1.5;
  --leading-relaxed:1.625;
  --leading-loose:  2;

  /* Letter Spacing */
  --tracking-tight:  -0.025em;
  --tracking-normal:  0;
  --tracking-wide:    0.025em;
  --tracking-widest:  0.1em;
}
```

### 3.3 Semantic Text Roles

| Role | Font Family | Size | Weight | Line Height | Use Case |
|---|---|---|---|---|---|
| `page-title` | Serif | 2xl (30px) | Bold | Tight | 페이지 제목 (H1) |
| `section-heading` | Serif | xl (24px) | Semibold | Snug | 섹션 제목 (H2) |
| `subsection-heading` | Sans | lg (20px) | Semibold | Snug | 서브섹션 (H3) |
| `card-title` | Sans | md (18px) | Semibold | Normal | 카드 제목 |
| `body` | Sans | base (16px) | Regular | Relaxed | 일반 본문 |
| `body-sm` | Sans | sm (14px) | Regular | Normal | 보조 텍스트 |
| `label` | Sans | sm (14px) | Medium | Normal | 폼 레이블 |
| `caption` | Sans | xs (12px) | Regular | Normal | 캡션, 메타데이터 |
| `code` | Mono | sm (14px) | Regular | Normal | 해시값, ID, 코드 |
| `kiosk-heading` | Serif | 3xl (36px) | Bold | Tight | 투표 Kiosk H1 |
| `kiosk-body` | Sans | xl (24px) | Medium | Relaxed | 투표 Kiosk 본문 |

> **Agent Rule:** 투표 Kiosk UI(`/vote/$sessionId`)에서는 일반 type scale을 사용하지 않는다. 반드시 `kiosk-*` 역할의 크기를 사용하여 모든 텍스트가 최소 24px 이상이 되도록 한다. (VVSG 2.0 §4.1.3)

---

## 4. Spacing & Grid System

### 4.1 Spacing Scale

4px 기반 8-point 그리드를 사용한다.

```css
:root {
  --space-0:    0;
  --space-0-5:  2px;
  --space-1:    4px;
  --space-1-5:  6px;
  --space-2:    8px;
  --space-2-5:  10px;
  --space-3:    12px;
  --space-4:    16px;
  --space-5:    20px;
  --space-6:    24px;
  --space-8:    32px;
  --space-10:   40px;
  --space-12:   48px;
  --space-14:   56px;
  --space-16:   64px;
  --space-20:   80px;
  --space-24:   96px;
  --space-32:   128px;
}
```

### 4.2 Layout Grid

```
Desktop (≥1280px):  12-column, 24px gutter, 48px side margin
Tablet (768–1279px): 8-column, 20px gutter, 32px side margin
Mobile (<768px):     4-column, 16px gutter, 16px side margin
```

### 4.3 Breakpoints

```ts
// packages/ui-core/src/tokens/breakpoints.ts
export const breakpoints = {
  mobile:  '(max-width: 767px)',
  tablet:  '(min-width: 768px) and (max-width: 1279px)',
  desktop: '(min-width: 1280px)',
  wide:    '(min-width: 1600px)',
} as const;

// CSS Custom Properties for use in media queries
// --bp-mobile:  767px
// --bp-tablet:  1279px
// --bp-desktop: 1280px
```

### 4.4 Content Width Constraints

```css
:root {
  --width-content-xs:  360px;   /* 좁은 폼, 확인 다이얼로그 */
  --width-content-sm:  560px;   /* 일반 폼, 소형 패널 */
  --width-content-md:  720px;   /* 기사형 콘텐츠, 중형 폼 */
  --width-content-lg:  960px;   /* 대형 폼, 상세 페이지 */
  --width-content-xl: 1200px;   /* 대시보드, 테이블 */
  --width-content-max:1440px;   /* 최대 컨텐츠 너비 */
}
```

---

## 5. Component Library — ui-core

`packages/ui-core/src/components/` 에 위치한다. 모든 컴포넌트는 다음 원칙을 따른다:

- **Headless-First:** 스타일은 CSS Modules로, 로직은 분리된 hook으로
- **ARIA 완전 지원:** 모든 interactive 컴포넌트는 적절한 ARIA role, state, property를 가진다
- **ref Forwarding:** 모든 컴포넌트는 `React.forwardRef`를 사용한다
- **Compound Components:** 복잡한 컴포넌트(`Tabs`, `Modal`)는 Compound Component 패턴을 사용한다

### 5.1 Button

```typescript
// 파일: packages/ui-core/src/components/Button/Button.tsx
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'danger-ghost';
type ButtonSize = 'sm' | 'md' | 'lg' | 'kiosk';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;  // default: 'primary'
  size?: ButtonSize;        // default: 'md'
  loading?: boolean;        // 로딩 상태 (Spinner 표시, disabled 처리)
  icon?: React.ReactNode;   // 아이콘 (텍스트 앞)
  iconRight?: React.ReactNode; // 아이콘 (텍스트 뒤)
  fullWidth?: boolean;      // 100% 너비
}
```

**Size 스펙:**

| Size | Height | Padding H | Font Size | Min Width | Touch Target |
|---|---|---|---|---|---|
| `sm` | 32px | 12px | 14px | 64px | 44×44px (padding 추가) |
| `md` | 40px | 16px | 14px | 80px | — |
| `lg` | 48px | 20px | 16px | 96px | — |
| `kiosk` | 64px | 32px | 20px | 160px | — (자체가 충분히 큼) |

**Variant 스타일:**

| Variant | Background | Text | Border | Hover BG | Focus Ring |
|---|---|---|---|---|---|
| `primary` | `--color-navy-700` | white | none | `--color-navy-800` | navy focus ring |
| `secondary` | white | `--color-navy-700` | `--color-navy-700` 1px | `--color-navy-50` | navy focus ring |
| `ghost` | transparent | `--color-navy-600` | none | `--color-navy-50` | navy focus ring |
| `danger` | `--color-crimson-500` | white | none | `--color-crimson-700` | crimson focus ring |
| `danger-ghost` | transparent | `--color-crimson-500` | none | `--color-crimson-50` | crimson focus ring |

**상태 규칙:**
- `disabled`: `opacity: 0.4`, `cursor: not-allowed`, `pointer-events: none`
- `loading`: `disabled` 상태 + 내부 텍스트 위치에 `Spinner` 표시 (텍스트는 opacity 0으로 유지해 너비 변화 방지)
- Focus: `outline: none` + `box-shadow: var(--shadow-focus-ring)` (outline 대신 shadow 사용)

> **Agent Rule:** Kiosk 모드에서는 반드시 `size="kiosk"`를 사용한다. 절대 `sm` 버튼을 투표 세션 UI에 사용하지 않는다.

### 5.2 Input

```typescript
// 파일: packages/ui-core/src/components/Input/Input.tsx
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;           // 항상 필수 (스크린리더)
  error?: string;          // 에러 메시지
  hint?: string;           // 힌트 텍스트
  prefix?: React.ReactNode; // 왼쪽 아이콘/텍스트
  suffix?: React.ReactNode; // 오른쪽 아이콘/텍스트
  size?: 'sm' | 'md' | 'lg';
  required?: boolean;
  // label이 없어 보이게 하려면 hideLabel prop 사용 (aria-label은 유지됨)
  hideLabel?: boolean;
}
```

**레이아웃 구조:**
```
[label (required marker)]
[prefix | input | suffix]  ← border 1px solid --color-border-default
[error message | hint text]
```

**상태 스타일:**
- Default: `border: 1px solid --color-border-default`
- Hover: `border-color: --color-border-strong`
- Focus: `border-color: --color-border-focus` + `box-shadow: var(--shadow-focus-ring)`
- Error: `border-color: --color-border-error` + `box-shadow: var(--shadow-focus-ring-error)`
- Disabled: `background: --color-bg-inset`, `cursor: not-allowed`

**ARIA 연결:**
```tsx
<label htmlFor={inputId}>{label}</label>
<input
  id={inputId}
  aria-describedby={error ? errorId : hint ? hintId : undefined}
  aria-invalid={error ? 'true' : undefined}
  aria-required={required ? 'true' : undefined}
/>
{error && <p id={errorId} role="alert">{error}</p>}
```

### 5.3 Select

Input과 동일한 레이아웃 구조를 사용하되, native `<select>`를 커스텀 드롭다운으로 교체한다.

```typescript
interface SelectProps {
  label: string;
  options: Array<{ value: string; label: string; disabled?: boolean }>;
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  error?: string;
  searchable?: boolean; // 검색 기능 활성화
  size?: 'sm' | 'md' | 'lg';
  required?: boolean;
  disabled?: boolean;
}
```

**드롭다운 패널:**
- `z-index: var(--z-dropdown)`
- `max-height: 240px`, `overflow-y: auto`
- 키보드: `↑↓`로 이동, `Enter`로 선택, `Escape`로 닫기
- `aria-haspopup="listbox"`, `aria-expanded`, `role="listbox"`, `role="option"`

### 5.4 DatePicker

```typescript
interface DatePickerProps {
  label: string;
  value?: Date | null;
  onChange?: (date: Date | null) => void;
  minDate?: Date;
  maxDate?: Date;
  disabledDates?: Date[];
  error?: string;
  locale?: 'en' | 'ko';  // i18n 연동
  required?: boolean;
}
```

- native `<input type="date">`를 기반으로 하되, 커스텀 캘린더 팝오버 제공
- 캘린더: Month/Year 내비게이션 + 날짜 그리드 (`role="grid"`)
- 키보드: Arrow keys로 날짜 이동, `Enter` 선택, `Escape` 닫기
- 오늘 날짜 강조 (outline), 선택 날짜 강조 (filled navy)

### 5.5 Table

선거 시스템에서 가장 중요한 컴포넌트. 대용량 데이터(수천 개 선거인 목록)를 처리해야 한다.

```typescript
interface Column<T> {
  key: string;
  header: string;
  width?: string;        // CSS width 값
  sortable?: boolean;
  render?: (row: T, index: number) => React.ReactNode;
  align?: 'left' | 'center' | 'right';
  fixed?: 'left' | 'right';  // 수평 스크롤 시 고정 열
}

interface TableProps<T extends { id: string }> {
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  empty?: React.ReactNode;        // 빈 상태 컴포넌트
  pagination?: PaginationState;
  onPageChange?: (page: number) => void;
  onSort?: (key: string, dir: 'asc' | 'desc') => void;
  onRowClick?: (row: T) => void;
  selectable?: boolean;
  selectedIds?: string[];
  onSelectionChange?: (ids: string[]) => void;
  stickyHeader?: boolean;
  caption: string;               // 접근성 필수: table caption
}
```

**테이블 스타일:**
- 헤더: `background: --color-navy-900`, `color: white`, `font: label medium`
- 정렬 아이콘: 헤더 오른쪽, 정렬 활성 시 amber 색상
- 행: 기본 배경 white, hover 시 `--color-navy-50`
- 짝수 행: `--color-bg-surface-alt`로 미묘한 구분 (zebra striping 선택 가능)
- 경계선: horizontal divider (`border-bottom: 1px solid --color-border-default`)
- 선택된 행: `background: --color-navy-100`
- `role="table"`, `scope="col"` on `<th>`, `aria-sort` 속성 관리

**페이지네이션:**
```
[← 이전] [1] [2] [3] ... [n] [다음 →]    페이지당 행 수: [25 ▾]    총 1,234건
```
- 현재 페이지: navy filled
- `aria-label="페이지 N으로 이동"`, `aria-current="page"`

### 5.6 Modal

```typescript
interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
  closeOnOverlay?: boolean;  // default: false (선거 시스템의 실수 방지)
  closeOnEscape?: boolean;   // default: true
  description?: string;      // aria-describedby 연결
  children: React.ReactNode;
  footer?: React.ReactNode;
  hideCloseButton?: boolean; // 확인/취소가 필수인 경우
}
```

**동작:**
- 열릴 때: `document.body` 스크롤 lock, 포커스 trap (첫 번째 focusable 요소로 이동)
- 닫힐 때: 열기 전 포커스 요소로 복원
- `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, `aria-describedby`
- 오버레이: `background: --color-bg-overlay`, `backdrop-filter: blur(4px)`

**Size 스펙:**

| Size | Max Width | Use Case |
|---|---|---|
| `sm` | 400px | 확인 다이얼로그, 간단한 알림 |
| `md` | 560px | 일반 폼 모달 |
| `lg` | 720px | 복잡한 폼, 프리뷰 |
| `xl` | 960px | 프로세스 상세, 투표 결과 |
| `full` | 100% - 48px | 대규모 데이터 표시 |

### 5.7 Badge

```typescript
type BadgeVariant =
  | 'draft' | 'published' | 'active' | 'closed' | 'certified'  // ElectionStatus
  | 'pending' | 'approved' | 'withdrawn' | 'disqualified'       // CandidateStatus
  | 'opened' | 'cast' | 'spoiled' | 'expired'                  // SessionStatus
  | 'info' | 'success' | 'warning' | 'error'                   // 일반 상태
  | 'neutral';

interface BadgeProps {
  variant: BadgeVariant;
  label: string;
  dot?: boolean;  // 앞에 색상 점 표시
  size?: 'sm' | 'md';
}
```

**상태별 색상 매핑:**

| Variant | Background | Text | Border |
|---|---|---|---|
| `draft` | `--color-neutral-100` | `--color-neutral-600` | `--color-neutral-200` |
| `published` | `--color-navy-100` | `--color-navy-700` | `--color-navy-200` |
| `active` | `--color-sage-100` | `--color-sage-700` | `--color-sage-200` |
| `closed` | `--color-amber-100` | `--color-amber-700` | `--color-amber-200` |
| `certified` | `--color-sage-50` | `--color-sage-900` | `--color-sage-500` |
| `pending` | `--color-amber-100` | `--color-amber-700` | `--color-amber-200` |
| `approved` | `--color-sage-100` | `--color-sage-700` | `--color-sage-200` |
| `disqualified` | `--color-crimson-100` | `--color-crimson-700` | `--color-crimson-200` |
| `error` | `--color-crimson-100` | `--color-crimson-700` | `--color-crimson-200` |

모든 Badge는 `border: 1px solid` 스타일로 통일. `border-radius: var(--radius-full)`.

### 5.8 Alert

```typescript
type AlertVariant = 'info' | 'success' | 'warning' | 'error';

interface AlertProps {
  variant: AlertVariant;
  title?: string;
  children: React.ReactNode;
  dismissible?: boolean;
  onDismiss?: () => void;
  actions?: React.ReactNode;
  icon?: React.ReactNode;  // 기본 아이콘 대체
}
```

**레이아웃:**
```
[Icon] [Title (optional)]
       [Content]
       [Actions (optional)]        [✕ Dismiss]
```
- `role="alert"` (에러), `role="status"` (info, success)
- 아이콘: 각 variant별 고정 아이콘 (ℹ, ✓, ⚠, ✕)
- border-left: 4px solid (variant 색상)

### 5.9 Spinner

```typescript
interface SpinnerProps {
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  label?: string;  // aria-label (기본: '로딩 중')
  color?: 'navy' | 'white' | 'neutral';
}
```

| Size | Diameter | Border Width |
|---|---|---|
| `xs` | 12px | 1.5px |
| `sm` | 16px | 2px |
| `md` | 24px | 2.5px |
| `lg` | 32px | 3px |
| `xl` | 48px | 4px |

- `role="status"`, `aria-label` 필수
- SVG 기반 회전 애니메이션 (CSS `animation: spin 1s linear infinite`)
- 인라인 사용 시: `aria-hidden="true"` + 외부 sr-only 텍스트

### 5.10 Card

```typescript
interface CardProps {
  children: React.ReactNode;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  variant?: 'flat' | 'elevated' | 'outlined' | 'inset';
  interactive?: boolean;  // hover 효과, cursor pointer
  href?: string;          // 카드 전체가 링크인 경우
  as?: React.ElementType; // 기본값 'div'
  className?: string;
}
```

**Variant 스타일:**

| Variant | Background | Border | Shadow |
|---|---|---|---|
| `flat` | `--color-bg-surface` | none | none |
| `elevated` | `--color-bg-surface` | none | `--shadow-md` |
| `outlined` | `--color-bg-surface` | 1px `--color-border-default` | none |
| `inset` | `--color-bg-inset` | none | inset `--shadow-xs` |

### 5.11 Tabs

```typescript
// Compound Component 패턴
// <Tabs defaultValue="overview">
//   <Tabs.List>
//     <Tabs.Tab value="overview">개요</Tabs.Tab>
//     <Tabs.Tab value="contests">후보</Tabs.Tab>
//   </Tabs.List>
//   <Tabs.Panel value="overview">...</Tabs.Panel>
//   <Tabs.Panel value="contests">...</Tabs.Panel>
// </Tabs>

interface TabsProps {
  defaultValue?: string;
  value?: string;           // controlled
  onChange?: (value: string) => void;
  orientation?: 'horizontal' | 'vertical';
  variant?: 'line' | 'pill' | 'block';
}
```

**라인 variant (기본):** 탭 바 아래 `border-bottom: 2px solid --color-border-default`, 선택된 탭은 `border-bottom: 2px solid --color-navy-700`.

**ARIA:** `role="tablist"`, `role="tab"`, `role="tabpanel"`, `aria-selected`, `aria-controls`. 키보드: `←→`로 탭 이동.

### 5.12 ProgressSteps (Wizard 전용)

선거 생성 Wizard, BPMN 프로세스 진행 표시에 사용.

```typescript
interface Step {
  id: string;
  label: string;
  status: 'completed' | 'current' | 'upcoming' | 'error';
  optional?: boolean;
}

interface ProgressStepsProps {
  steps: Step[];
  orientation?: 'horizontal' | 'vertical';
}
```

**수평 레이아웃:**
```
[✓ 1. 기본정보] — [● 2. 후보등록] — [○ 3. 투표소] — [○ 4. 검토] — [○ 5. 제출]
```
- 완료: `--color-sage-500` 체크 아이콘
- 현재: `--color-navy-700` 채운 원 + 펄스 애니메이션
- 오류: `--color-crimson-500` X 아이콘
- 미완료: `--color-neutral-300` 빈 원
- 연결선: 완료된 경우 sage, 미완료 경우 neutral

**수직 레이아웃 (BPMN 태스크 목록):** 각 단계가 세로로 나열, 연결선은 왼쪽 수직선.

---

## 6. Shell Layout & Navigation

### 6.1 Shell 구조

```
┌─────────────────────────────────────────────────────────────────┐
│ TopBar (64px)                                                   │
│ [MiREMS Logo] [Election Scope Selector ▾]    [User Menu ▾] [🔔]│
├────────────┬────────────────────────────────────────────────────┤
│ Sidebar    │ Main Content Area                                  │
│ (256px)    │                                                    │
│            │  ┌──────────────────────────────────────────────┐  │
│ [Nav Items]│  │ Page Header                                  │  │
│            │  │ [Breadcrumb]                                 │  │
│            │  │ [Page Title]         [Action Buttons]        │  │
│            │  └──────────────────────────────────────────────┘  │
│            │                                                    │
│            │  [Page Content]                                    │
│            │                                                    │
└────────────┴────────────────────────────────────────────────────┘
```

### 6.2 TopBar

```
height: 64px
background: --color-navy-900
border-bottom: 1px solid rgba(255,255,255,0.08)
position: sticky; top: 0; z-index: var(--z-sticky)
```

**콘텐츠 (좌 → 우):**
1. **MiREMS 로고** (좌측): 텍스트 로고 `MiREMS` (IBM Plex Serif Bold, white), 하단 `Miru Election Management Solution` (IBM Plex Sans 11px, navy-200)
2. **선거 범위 선택기** (로고 우측): 현재 선택된 선거/관할 표시. `[📋 제22대 국회의원 선거 ▾]` 형태. 드롭다운으로 접근 가능한 선거 목록 표시. `SYSTEM_ADMIN`에게만 표시.
3. **확장 Pack 배지** (우측 그룹): 활성화된 Extension Pack 표시. `[KR]`, `[US]` 작은 배지.
4. **알림 벨** (우측): 미완료 BPMN 태스크 수 배지 표시. 클릭 시 알림 드롭다운.
5. **사용자 메뉴** (우측 끝): 아바타 + 이름. 드롭다운: 내 프로필, 환경설정, 로그아웃.

### 6.3 Sidebar (Desktop)

```
width: 256px (expanded) / 64px (collapsed)
background: --color-neutral-900
border-right: 1px solid rgba(255,255,255,0.06)
position: sticky; top: 64px; height: calc(100vh - 64px)
overflow-y: auto
```

**확장/축소:** 사이드바 하단 `[◀]` 버튼으로 토글. 축소 시 아이콘만 표시 + 호버 시 tooltip.

**네비게이션 아이템 구조:**

```
[Icon] 레이블               (배지/카운트)
```

- 기본: `color: --color-neutral-400`
- 호버: `background: rgba(255,255,255,0.06)`, `color: white`
- 활성: `background: rgba(46,88,153,0.30)`, `color: --color-navy-200`, left border 3px navy-400

**네비게이션 항목 (역할별 표시 제어):**

| 아이콘 | 레이블 | 경로 | 최소 역할 |
|---|---|---|---|
| 🏠 | 대시보드 | `/` | 모든 인증 사용자 |
| 📋 | 선거 관리 | `/elections` | 모든 인증 사용자 |
| 👤 | 후보자 관리 | `/elections/$id/candidates` | `ELECTION_OFFICER+` |
| 🗳 | 투표소 관리 | `/elections/$id/ballots` | `ELECTION_OFFICER+` |
| 👥 | 선거인 명부 | `/voters` | `ELECTION_OFFICER+` |
| 📊 | 개표 결과 | `/elections/$id/results` | 모든 인증 사용자 |
| 📁 | 감사 로그 | `/audit` | `AUDITOR`, `SYSTEM_ADMIN` |
| ⚙️ | 시스템 관리 | `/admin` | `SYSTEM_ADMIN` |

**섹션 구분선:** 관리 항목과 일반 항목 사이 `border-top: 1px solid rgba(255,255,255,0.08)` + 소형 레이블 (`SYSTEM`, `AUDIT` 등).

### 6.4 Bottom Navigation (Mobile)

```
height: 60px
background: --color-neutral-900
border-top: 1px solid rgba(255,255,255,0.08)
position: fixed; bottom: 0; left: 0; right: 0;
safe-area-inset-bottom: env(safe-area-inset-bottom)
```

핵심 5개 항목만 표시 (역할에 따라 조정). 더 많은 항목은 "더보기" 버튼.

### 6.5 Page Header

```typescript
interface PageHeaderProps {
  title: string;
  breadcrumb?: BreadcrumbItem[];
  description?: string;
  actions?: React.ReactNode;
  tabs?: React.ReactNode;        // 탭이 있는 페이지
  badge?: React.ReactNode;       // 선거 상태 배지 등
  backHref?: string;             // 뒤로가기 링크
}
```

**레이아웃:**
```
[← 뒤로가기]  [선거 관리] / [제22대 국회의원선거] / [후보자]    ← Breadcrumb
제22대 국회의원선거 후보자 관리  [ACTIVE ●]                     ← Title + Badge
후보자를 등록하고 자격을 검토합니다.                              ← Description
                                     [+ 후보자 등록] [내보내기]  ← Actions
─────────────────────────────────────────────────────────────────
[개요] [후보자 목록] [자격 심사] [통계]                          ← Optional Tabs
```

---

## 7. Theming System

### 7.1 테마 종류

| 테마 | 파일 | 활성화 조건 |
|---|---|---|
| Light (기본) | `theme-light.css` | `[data-theme="light"]` |
| Dark | `theme-dark.css` | `[data-theme="dark"]` |
| High Contrast | `theme-high-contrast.css` | `[data-theme="high-contrast"]` |
| Kiosk | `theme-kiosk.css` | `[data-theme="kiosk"]` (투표 세션 전용) |

### 7.2 Dark Theme Token Overrides

```css
[data-theme="dark"] {
  --color-bg-canvas:      var(--color-neutral-950);
  --color-bg-surface:     var(--color-neutral-900);
  --color-bg-surface-alt: var(--color-neutral-800);
  --color-bg-inset:       var(--color-neutral-800);

  --color-border-default: var(--color-neutral-700);
  --color-border-strong:  var(--color-neutral-600);

  --color-text-primary:   var(--color-neutral-50);
  --color-text-secondary: var(--color-neutral-300);
  --color-text-disabled:  var(--color-neutral-600);
  --color-text-link:      var(--color-navy-300);
  --color-text-link-hover:var(--color-navy-200);
}
```

### 7.3 High Contrast Theme (투표 Kiosk 접근성)

```css
[data-theme="high-contrast"] {
  --color-bg-canvas:     #000000;
  --color-bg-surface:    #000000;
  --color-text-primary:  #ffffff;
  --color-text-secondary:#ffffff;
  --color-border-default:#ffffff;
  --color-border-focus:  #ffff00;
  --color-interactive-primary:       #ffffff;
  --color-interactive-primary-hover: #ffff00;
  --shadow-focus-ring: 0 0 0 4px #ffff00;
  /* 모든 텍스트 최소 contrast ratio: 7:1 (WCAG AAA) */
}
```

### 7.4 Kiosk Theme

```css
[data-theme="kiosk"] {
  /* High Contrast 기반으로 확장 */
  --text-base: 1.5rem;     /* 24px (모든 기본 텍스트) */
  --text-lg:   2rem;       /* 32px */
  --text-xl:   2.5rem;     /* 40px */
  /* 모든 터치 타겟 최소 48×48px */
  --min-touch-target: 48px;
  /* 포커스 링 강화 */
  --shadow-focus-ring: 0 0 0 5px #ffff00;
}
```

### 7.5 테마 관리

```typescript
// packages/ui-core/src/hooks/useTheme.ts
type Theme = 'light' | 'dark' | 'high-contrast' | 'kiosk';

interface ThemeContext {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

// 중요: VVSG 요구사항에 따라 localStorage 사용 금지.
// 테마 선택은 현재 세션 메모리에만 저장된다.
// 투표 Kiosk 페이지 진입 시 강제로 'kiosk' 테마로 전환.
// Kiosk 페이지 이탈 시 이전 테마로 복원.
```

---

## 8. Accessibility Standards

### 8.1 WCAG 2.1 AA 필수 준수 항목

**1. 색상 대비 (WCAG 1.4.3)**
- 일반 텍스트: 최소 4.5:1
- 대형 텍스트 (18px+ bold, 24px+): 최소 3:1
- UI 컴포넌트 경계, 아이콘: 최소 3:1
- 검증 도구: `axe-core` 자동 검사 (모든 PR에 포함)

**2. 키보드 접근성 (WCAG 2.1.1)**
- 모든 interactive 요소는 Tab으로 포커스 가능
- 모든 기능은 마우스 없이 키보드만으로 수행 가능
- 포커스 가시성: `box-shadow: var(--shadow-focus-ring)` (절대 `outline: none`만 적용하지 않음)
- 논리적 포커스 순서 (DOM 순서 = 시각적 순서)

**3. 포커스 트랩 (WCAG 2.4.3)**
- Modal, Dropdown, DatePicker 열림 시 포커스 컨테이너 내부로 제한
- `focus-trap-react` 라이브러리 또는 직접 구현

**4. ARIA 레이블 (WCAG 1.1.1, 4.1.2)**
- 아이콘 버튼: `aria-label` 필수
- 이미지: `alt` 텍스트 필수 (장식용: `alt=""`, `aria-hidden="true"`)
- 폼 입력: `<label>` 연결 또는 `aria-label`/`aria-labelledby` 필수
- 상태 변경: `role="alert"` 또는 `aria-live="polite"`

**5. 에러 식별 (WCAG 3.3.1, 3.3.3)**
- 에러: 색상만으로 구분하지 않음 (아이콘 + 텍스트 병용)
- 에러 메시지: 구체적이고 수정 방법 안내

### 8.2 VVSG 2.0 §4 투표 시스템 접근성 추가 요건

| 요건 | 구현 방법 |
|---|---|
| 4.1.1 - 시각적 접근성 | 고대비 테마 제공, 텍스트 크기 조정 가능 |
| 4.1.2 - 텍스트 크기 | 최소 18pt(24px), 브라우저 텍스트 크기 조정 지원 |
| 4.1.3 - 터치 타겟 | 최소 6.35mm (≈48px) × 6.35mm |
| 4.1.4 - 색맹 접근성 | 색상+패턴+텍스트 병용, 색 단독 정보 전달 금지 |
| 4.2.1 - 청각 접근성 | 소리 경고는 시각적 동등 표시 동반 |
| 4.3.1 - 인지 접근성 | 명확한 언어, 단계별 안내, 확인 단계 |

### 8.3 컴포넌트별 axe-core 검사 강제 사항

```typescript
// vitest.setup.ts
import { configureAxe } from 'jest-axe';

const axe = configureAxe({
  rules: {
    'color-contrast': { enabled: true },
    'label': { enabled: true },
    'button-name': { enabled: true },
    'link-name': { enabled: true },
    'image-alt': { enabled: true },
    'aria-required-attr': { enabled: true },
  }
});

// 모든 컴포넌트 테스트에서 expect(await axe(container)).toHaveNoViolations()
```

---

## 9. Role-Based UI Rendering

### 9.1 역할 계층 및 표시 제어

```typescript
// packages/ui-core/src/auth/roles.ts
export const ROLES = [
  'SYSTEM_ADMIN',
  'ELECTION_ADMIN',
  'ELECTION_OFFICER',
  'TABULATION_OFFICER',
  'AUDITOR',
  'OBSERVER',
  'VOTER',
] as const;

type Role = typeof ROLES[number];

// 역할 계층: 상위 역할은 하위 역할의 권한 포함
const ROLE_HIERARCHY: Record<Role, Role[]> = {
  'SYSTEM_ADMIN':       ['ELECTION_ADMIN', 'ELECTION_OFFICER', 'TABULATION_OFFICER', 'AUDITOR', 'OBSERVER'],
  'ELECTION_ADMIN':     ['ELECTION_OFFICER', 'OBSERVER'],
  'ELECTION_OFFICER':   ['OBSERVER'],
  'TABULATION_OFFICER': ['OBSERVER'],
  'AUDITOR':            ['OBSERVER'],
  'OBSERVER':           [],
  'VOTER':              [],
};
```

### 9.2 RoleGuard 컴포넌트

```typescript
// packages/ui-core/src/auth/RoleGuard.tsx
interface RoleGuardProps {
  roles: Role | Role[];
  fallback?: React.ReactNode;  // 권한 없을 때 표시 (기본: null)
  children: React.ReactNode;
}

// 사용 예:
// <RoleGuard roles={['ELECTION_ADMIN', 'SYSTEM_ADMIN']}>
//   <Button>선거 발행</Button>
// </RoleGuard>
```

> **Agent Rule:** RoleGuard는 UI 숨김 처리만 한다. 실제 권한 검사는 백엔드가 담당한다. RoleGuard로 숨긴 요소라도 API 호출은 백엔드에서 403을 반환한다.

### 9.3 기능별 역할 매트릭스

| 기능 | SYSTEM_ADMIN | ELECTION_ADMIN | ELECTION_OFFICER | TABULATION_OFFICER | AUDITOR | OBSERVER | VOTER |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 선거 생성/수정 | ✓ | ✓ | — | — | — | — | — |
| 선거 발행 (BPMN) | ✓ | ✓ | — | — | — | — | — |
| 선거 마감 | ✓ | ✓ | — | — | — | — | — |
| 후보자 등록 | ✓ | ✓ | ✓ | — | — | — | — |
| 후보자 심사 (BPMN) | ✓ | ✓ | ✓ | — | — | — | — |
| 선거인 명부 열람 | ✓ | ✓ | ✓ | — | — | — | — |
| 선거인 적격성 수정 | ✓ | ✓ | ✓ | — | — | — | — |
| 투표 세션 개설 | ✓ | ✓ | ✓ | — | — | — | — |
| 투표 행사 | — | — | — | — | — | — | ✓ |
| 개표 개시 (BPMN) | ✓ | — | — | ✓ | — | — | — |
| 개표 결과 열람 (인증 전) | ✓ | ✓ | — | ✓ | ✓ | — | — |
| 개표 결과 열람 (인증 후) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 선거 인증 (BPMN) | ✓ | ✓ | — | — | — | — | — |
| 감사 로그 열람 | ✓ | — | — | — | ✓ | — | — |
| 프로세스 관리 | ✓ | — | — | — | — | — | — |
| 시스템 관리 | ✓ | — | — | — | — | — | — |

---

## 10. BPMN Process-Aware UI Patterns

MiREMS는 Hybrid Architecture를 사용한다. BPMN 프로세스가 실행 중인 경우, UI는 프로세스 상태를 명시적으로 시각화하고 사용자가 취해야 할 행동을 명확하게 안내해야 한다.

### 10.1 ProcessStatusPanel 컴포넌트

BPMN 프로세스가 활성화된 모든 페이지에 표시되는 패널.

```typescript
interface ProcessNode {
  id: string;
  name: string;
  type: 'start' | 'task' | 'gateway' | 'end';
  status: 'completed' | 'active' | 'waiting' | 'error' | 'upcoming';
  assignee?: string;      // User Task인 경우 담당자
  dueAt?: Date;           // Timer Boundary Event
  completedAt?: Date;
}

interface ProcessStatusPanelProps {
  processId: string;
  processName: string;
  instanceId: string;
  status: 'running' | 'completed' | 'error' | 'suspended';
  nodes: ProcessNode[];
  currentNode: ProcessNode;
  onSignal?: (signalName: string, payload?: object) => void;  // User Task 완료
}
```

**시각화:**
```
┌─────────────────────────────────────────────────────────┐
│ 🔄 선거 발행 프로세스 · 실행 중                 [상세 ▾] │
├─────────────────────────────────────────────────────────┤
│ [✓ 시작] ──── [✓ 구성 검증] ──── [● 관리자 검토]        │
│                               ──── [○ 발행 완료] ─── [○ 끝]│
├─────────────────────────────────────────────────────────┤
│ ⏳ 현재 단계: 관리자 검토 (ELECTION_ADMIN 필요)          │
│ 담당자: admin@election.go.kr                            │
│ 기한: 2026-05-20 18:00 (3일 2시간 남음)                │
│                          [승인 ✓] [반려 ✗] [위임 →]    │
└─────────────────────────────────────────────────────────┘
```

### 10.2 프로세스별 UI 통합 규칙

**ElectionPublicationProcess (선거 발행):**
- 선거 상세 페이지의 상단에 `ProcessStatusPanel` 표시
- "발행 요청" 버튼 클릭 시 확인 Modal → API 호출 → Panel 즉시 업데이트
- `ELECTION_ADMIN` 역할 사용자에게 승인/반려 버튼 표시
- SSE 또는 30초 폴링으로 상태 업데이트

**CandidateRegistrationProcess (후보자 등록):**
- 후보자 행에 인라인 프로세스 상태 배지 표시
- `ELECTION_OFFICER` 대시보드에 "대기 중 심사" 카운트 배지
- 심사 페이지에서 `ProcessStatusPanel` + 제출 문서 뷰어 병렬 표시
- 72시간 카운트다운 타이머 표시

**BallotTabulationProcess (개표):**
- 실시간 개표 진행률 표시 (ProgressBar + 숫자)
- 각 선거구별 집계 상태 표시

**VoteCorrectionProcess (투표 수정):**
- 이중 승인 단계를 ProgressSteps로 표시
- 현재 승인자 / 2차 승인자 정보 표시

**ResultCertificationProcess (결과 인증):**
- 선거 결과 페이지에 인증 프로세스 배너 표시

### 10.3 Task Notification (알림 시스템)

```typescript
interface TaskNotification {
  id: string;
  processName: string;
  taskName: string;
  instanceId: string;
  dueAt?: Date;
  urgency: 'low' | 'normal' | 'high' | 'overdue';
  actionUrl: string;
}
```

- TopBar 벨 아이콘: 미처리 태스크 수 배지 (빨간 점)
- 드롭다운: 최근 5개 태스크 목록, "모두 보기" 링크
- overdue 상태: 경고색 배경 + 진동 애니메이션

---

## 11. Page Specifications

### 11.1 Public Landing Page (`/`)

**목적:** 비인증 사용자를 위한 플랫폼 소개 및 로그인 진입점.

**레이아웃:**
```
┌──────────────────────────────────────────────────────────────┐
│ [MiREMS 로고]                              [로그인] [한국어] │
├──────────────────────────────────────────────────────────────┤
│                    HERO SECTION                              │
│  미루(Miru) 선거 관리 솔루션                                │
│  신뢰할 수 있고, 감사 가능한, 확장 가능한 선거 플랫폼       │
│                                [시스템 로그인 →]            │
│                                                              │
│  [NIST VVSG 2.0] [보안 감사 완료] [WCAG 2.1 AA]            │
├──────────────────────────────────────────────────────────────┤
│ FEATURE CARDS                                                │
│ [투명한 감사] [불변 투표 기록] [다국가 지원] [접근성]       │
└──────────────────────────────────────────────────────────────┘
```

**디자인 세부:**
- Hero: `background: --color-navy-900`, 미묘한 기하학적 패턴 SVG 배경
- 타이포그래피: 플랫폼명은 `font-serif, 4xl, bold`, 슬로건은 `font-sans, xl, light`
- CTA 버튼: `variant="secondary"` (white border on dark background)
- 인증 배지: 회색 테두리 pill 형태의 뱃지 3개 (클릭 불가, 신뢰 마커)

### 11.2 Authentication Pages

**OIDC 리다이렉트 흐름이므로 별도 로그인 폼 없음.** 대신:

**로그인 시작 (`/login`):**
```
┌──────────────────────────────────────┐
│           MiREMS                     │
│  Keycloak으로 안전하게 로그인합니다  │
│  [Keycloak으로 로그인 →]            │
│                                      │
│  이 시스템은 공식 선거 관리 시스템  │
│  으로, 권한 있는 사용자만 접근       │
│  가능합니다.                         │
└──────────────────────────────────────┘
```

**로그아웃 확인 (`/logout`):**
- 세션 종료 확인 메시지
- "안전하게 로그아웃 되었습니다" 확인 후 Landing으로 리다이렉트

### 11.3 Dashboard (`/`)

**목적:** 로그인 후 진입 페이지. 역할별 맞춤 요약 정보.

**레이아웃 (SYSTEM_ADMIN / ELECTION_ADMIN):**

```
Dashboard — 안녕하세요, 홍길동님                  2026년 5월 15일 금요일

[METRIC CARDS ROW]
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ 진행 중  │ │ 대기 중  │ │ 등록 후보│ │ 선거인수 │
│ 선거 수  │ │ 태스크   │ │ 자 검토  │ │          │
│    3     │ │    7     │ │    12    │ │ 45,230   │
└──────────┘ └──────────┘ └──────────┘ └──────────┘

[두 컬럼 레이아웃]
┌──────────────────────────────┐ ┌────────────────────────────┐
│ 내 대기 중인 태스크           │ │ 최근 활동 (감사 로그)      │
│ ─────────────────────────── │ │ ─────────────────────────  │
│ [● 선거 발행 승인 필요]      │ │ [이벤트 목록...]           │
│ [● 후보자 등록 심사 (3건)]   │ │                            │
│ [● 개표 결과 검토]           │ │                            │
└──────────────────────────────┘ └────────────────────────────┘

[진행 중인 선거 목록]
선거명 | 유형 | 상태 | 투표 현황 | 선거일 | 액션
──────────────────────────────────────────────────
제22대... | 국회의원 | [ACTIVE ●] | 67% | 2026-04-10 | [상세 →]
```

**Metric Cards:**
- 각 카드: `variant="elevated"`, 큰 숫자 (`font-serif, 4xl, bold, --color-navy-800`), 레이블 (`font-sans, sm, secondary`)
- 중요 변화 시 amber 강조

**TABULATION_OFFICER 대시보드:**
- 진행 중인 개표 현황 + 진행률 바 중심 구성

**AUDITOR 대시보드:**
- 최근 감사 이벤트 목록 + 이상 탐지 알림 중심

**OBSERVER 대시보드:**
- 공개 선거 결과 요약 (read-only)

### 11.4 Election List Page (`/elections`)

```
선거 관리                                        [+ 새 선거 만들기]

[필터 바]
상태: [전체 ▾] [DRAFT] [PUBLISHED] [ACTIVE] [CLOSED] [CERTIFIED]
유형: [전체 ▾]    날짜: [2026.01.01] ~ [2026.12.31]    [검색 🔍]

[선거 테이블]
선거명                 | 유형        | 상태      | 선거일     | 생성일     | 액션
─────────────────────────────────────────────────────────────────────────────────
제22대 국회의원선거    | 비례대표    | [ACTIVE ●]| 2026-04-10 | 2026-01-05 | [상세]
제8회 지방선거        | 지방자치    | [DRAFT ○] | 2026-06-01 | 2026-02-10 | [상세]
...
                                                    ← 1  2  3  →    총 24건
```

**+ 새 선거 만들기 버튼:** `ELECTION_ADMIN` 이상에게만 표시 (`RoleGuard`)

**상태 필터:** 다중 선택 가능한 토글 버튼 그룹. 선택된 상태는 filled navy 배경.

**테이블 정렬:** 선거일, 생성일 컬럼 정렬 가능.

**빈 상태:**
```
┌───────────────────────────────────┐
│   📋                              │
│   아직 등록된 선거가 없습니다.    │
│   [+ 첫 번째 선거 만들기]        │
└───────────────────────────────────┘
```

### 11.5 Election Detail Page (`/elections/$id`)

**탭 구조 (역할별 탭 표시 조정):**

```
[개요] [후보자] [투표소] [개표 결과] [프로세스] [감사 내역]
       OFFICER+  OFFICER+  모두(인증후) ADMIN+     AUDITOR+
```

**개요 탭:**

```
┌──────────────────────────────────────────────────────────────┐
│ 프로세스 상태 패널 (진행 중인 경우)                          │
│ [ProcessStatusPanel]                                         │
└──────────────────────────────────────────────────────────────┘

[선거 정보 그리드 — 두 컬럼]
기본 정보                        일정 정보
─────────────────────────        ─────────────────────────
선거명: 제22대 국회의원선거      선거일: 2026년 4월 10일
유형: 국회의원선거               후보 등록: 2026.02.01 ~ 02.28
관할: 대한민국                   사전투표: 2026.04.05 ~ 04.06
상태: [ACTIVE ●]                선거인수: 44,197,692명
Extension: [KR]

후보자 현황                      투표 현황
─────────────────────────        ─────────────────────────
등록: 954명                      투표율: 67.0%
승인: 890명                      ████████████░░░░░░ 67%
대기: 64명                       투표자: 29,612,453명
```

**액션 버튼 (상태 및 역할별 활성화):**
- `DRAFT` + `ELECTION_ADMIN`: [발행 요청 →] [삭제]
- `PUBLISHED` + `ELECTION_ADMIN`: [활성화] [발행 취소]
- `ACTIVE` + `ELECTION_ADMIN`: [마감]
- `CLOSED` + `TABULATION_OFFICER`: [개표 시작]
- `CLOSED` + `ELECTION_ADMIN`: [인증 요청]

**선거 상태 전환 확인 Modal:**
```
선거를 발행하시겠습니까?
이 선거는 검토 프로세스를 시작하고 두 번째 관리자의 승인이 필요합니다.
발행 후에는 일부 정보를 수정할 수 없습니다.

[취소]  [발행 요청 →]
```

### 11.6 Election Creation Wizard (`/elections/new`)

**5단계 Wizard:**

```
[✓ 1. 기본정보] — [● 2. 선거구/관할] — [○ 3. 후보등록] — [○ 4. 투표소] — [○ 5. 검토]
```

**Step 1: 기본 정보**
```
선거명 *
[______________________________]

선거 유형 *                     관할 국가 *
[국회의원선거 ▾]                [대한민국 ▾]

선거일 *                        Extension Pack
[2026-__-__  📅]               [ext-kr (공직선거법)] (자동)

설명
[________________________________]
[                                ]

                              [다음 단계 →]
```

**Step 2: 선거구/관할**
- 관할 계층 선택 (ext-kr: 시/도 → 시/군/구 → 선거구)
- 선택된 관할 요약 표시

**Step 3: 후보 등록 설정**
- Contest(선거구) 추가 ([+ Contest 추가] 버튼)
- 각 Contest: 이름, 유형(CANDIDATE_CHOICE/RANKED_CHOICE/PROPORTIONAL), 의석 수
- Contest 당 Contest Type에 따라 추가 필드 표시

**Step 4: 투표소(Ballot Style) 설정**
- 투표소(BallotStyle) 추가/연결
- 언어, 접근성 기능 설정

**Step 5: 검토 및 제출**
```
입력하신 정보를 확인하세요.

기본 정보           [수정]
─────────────────────────────
선거명: 제22대 국회의원선거
유형: 국회의원선거
선거일: 2026-04-10
...

선거구 (3)          [수정]
─────────────────────────────
• 지역구 (CANDIDATE_CHOICE, 253석)
• 비례대표 (PROPORTIONAL, 47석)
• 교육감 (CANDIDATE_CHOICE, 1석)

                [← 이전] [선거 생성]
```

**폼 유효성 검사:**
- 실시간 검사 (입력 후 blur 시)
- 다음 단계 이동 시 현재 단계 전체 검사
- 서버 검사 에러는 Alert로 표시

> **Agent Rule:** Wizard 상태는 URL query param이나 localStorage가 아닌 React state로만 관리한다. (VVSG 요구 + 브라우저 저장 금지)

### 11.7 Candidate Management (`/elections/$id/candidates`)

**탭 구조:**
```
[후보자 목록] [심사 대기] [승인된 후보자] [통계]
```

**후보자 목록 탭:**

```
[+ 후보자 등록] (ELECTION_OFFICER+)        [Excel 내보내기]

[필터] 선거구: [전체 ▾] 상태: [전체 ▾] [검색]

이름          | 선거구        | 소속 정당   | 상태         | 등록일     | 액션
─────────────────────────────────────────────────────────────────────────────
홍길동        | 강남갑        | 민주당     | [APPROVED ✓] | 2026.02.01 | [상세]
김철수        | 종로          | 국민의힘   | [PENDING ⏳] | 2026.02.03 | [심사]
이영희        | 비례대표      | 정의당     | [PENDING ⏳] | 2026.02.04 | [심사]
박민준        | 강남을        | -          | [REJECTED ✗] | 2026.02.02 | [상세]
```

**심사 페이지 (`/elections/$id/candidates/$candidateId/review`):**

```
┌──────────────────────────────────────────────────────────────┐
│ 프로세스: 후보자 등록 심사 [● 진행 중]                      │
│ [✓ 적격성 검증] ── [● 서류 심사] ── [○ 결과 통보]           │
│ 기한까지: 47시간 23분                                        │
└──────────────────────────────────────────────────────────────┘

[두 컬럼 레이아웃]
┌──────────────────────────┐  ┌──────────────────────────────┐
│ 후보자 정보              │  │ 제출 서류                    │
│ ─────────────────────── │  │ ─────────────────────────    │
│ 이름: 김철수             │  │ [서류 뷰어]                  │
│ 선거구: 종로             │  │ • 주민등록등본 ✓             │
│ 정당: 국민의힘           │  │ • 기탁금 납입 ✓             │
│ 피선거권 검증:           │  │ • 출마 등록서 ✓             │
│ [DMN 결과: ✓ 적격]      │  │ • 재정신고서 ⚠ 검토 필요   │
│                          │  │                              │
│ 심사 의견                │  │ [PDF 뷰어 컴포넌트]         │
│ [____________________]   │  │                              │
│ [                    ]   │  │                              │
│                          │  │                              │
│ [반려 ✗]  [승인 ✓]      │  │                              │
└──────────────────────────┘  └──────────────────────────────┘
```

### 11.8 Voter Roll Pages

**선거인 명부 (`/voters`):**

```
선거인 명부                                     [+ 선거인 등록]

[검색] 선거: [전체 ▾] 등록상태: [전체 ▾]

[경고: 개인정보 마스킹 적용됨 — 전체 ID는 표시되지 않습니다]

ID (마스킹)     | 등록 상태  | 적격 선거                    | 등록일
─────────────────────────────────────────────────────────────────
****7890        | [등록]     | 제22대 국회의원선거, 지방선거 | 2026-01-15
****2341        | [등록]     | 제22대 국회의원선거           | 2026-01-20
****5623        | [미등록]   | -                            | -
```

**선거인 적격성 확인 (`/voters/eligibility`):**

```
선거인 적격성 확인

선거인 ID *            선거 선택 *
[___________________]  [제22대 국회의원선거 ▾]

                       [적격성 확인]

결과:
┌────────────────────────────────────┐
│ ✓ 선거권 있음                     │
│                                    │
│ 항목         결과    사유          │
│ 연령 (만18+) ✓      만 26세       │
│ 등록 상태   ✓      등록 완료      │
│ 거주 확인   ✓      서울시 거주    │
│ 선거 유형   ✓      국회의원선거   │
└────────────────────────────────────┘
```

### 11.9 Ballot & BallotStyle Pages

**투표지 관리 (`/elections/$id/ballots`):**

```
투표지 관리                              [+ 새 투표지 버전] [미리보기]

현재 버전: v3  [ACTIVE]

[투표지 스타일 목록]
스타일 코드 | 선거구     | 언어   | 접근성 기능                    | 액션
──────────────────────────────────────────────────────────────────────────
STY-001     | 강남갑      | 한국어 | [고대비] [확대]               | [수정] [미리보기]
STY-002     | 강남갑      | English| [High Contrast] [Large Print] | [수정] [미리보기]
STY-003     | 강남을      | 한국어 | [고대비]                      | [수정] [미리보기]
```

**투표지 미리보기 (`/elections/$id/ballots/$ballotId/preview`):**

투표 Kiosk UI와 동일한 렌더링 컴포넌트를 사용하되, 실제 투표 불가. "PREVIEW" 워터마크 표시.

### 11.10 Results Dashboard (`/elections/$id/results`)

**결과 개요:**

```
제22대 국회의원선거 결과

인증 상태: [CERTIFIED ✓]  인증일: 2026-04-12
TabulationReport Hash: [abc123...def456]  (SHA-256 서명 검증됨)

                                          [PDF 공식 결과 다운로드]

[탭: 지역구 결과 | 비례대표 결과 | 선거구별 지도 | 투표율 분석]

지역구 결과 — 강남갑
────────────────────────────────────────────────────
후보자      정당          득표수    득표율   당선여부
홍길동      민주당        45,230    52.3%   [당선 ★]
김철수      국민의힘      38,100    44.1%   -
이영희      정의당         3,100     3.6%   -

[Bar Chart: 후보자별 득표율]
홍길동  ████████████████████████████████████░░░░░░░░ 52.3%
김철수  ████████████████████████████████░░░░░░░░░░░░ 44.1%
이영희  ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  3.6%
```

**미인증 결과 (TABULATION_OFFICER, ELECTION_ADMIN만 열람):**
```
┌────────────────────────────────────────────────┐
│ ⚠️ 이 결과는 아직 공식 인증되지 않았습니다.   │
│    관계자 외 열람이 제한됩니다.                │
└────────────────────────────────────────────────┘
```

### 11.11 Audit Log Viewer (`/audit`)

```
감사 로그                                          [내보내기 CSV]

[필터 바]
집계 유형: [전체 ▾] [ELECTION] [BALLOT] [VOTER] [SECURITY]
날짜 범위: [____] ~ [____]
행위자: [____]
이벤트 유형: [____]
                                                    [검색]

[타임라인 뷰 / 테이블 뷰 토글]

[테이블 뷰]
시각                | 이벤트 유형              | 집계 유형   | 집계 ID    | 행위자      | IP
─────────────────────────────────────────────────────────────────────────────────────────────
2026-05-15 09:23:41 | ELECTION_PUBLISHED        | ELECTION    | uuid-1234  | admin@...   | 203.x.x.x
2026-05-15 09:20:15 | CANDIDATE_APPROVED        | CANDIDATE   | uuid-5678  | officer@...  | 203.x.x.x
2026-05-15 09:15:03 | SECURITY_VIOLATION        | SECURITY    | -          | unknown      | 192.x.x.x [⚠]

[행 클릭 → 상세 드로어]
이벤트 상세
──────────────────────────────────────────
이벤트 ID:   [코드 형식]
이벤트 유형: ELECTION_PUBLISHED
발생 시각:   2026-05-15 09:23:41 UTC+9
집계 ID:     [코드 형식]
행위자 ID:   [코드 형식]
Source IP:   203.x.x.x

페이로드:
{
  "electionId": "...",
  "electionName": "...",
  "publishedBy": "..."
}
```

**SECURITY_VIOLATION 이벤트:** 행 배경 `--color-crimson-50`, 텍스트 `--color-crimson-700`.

### 11.12 Admin Dashboard (`/admin`)

```
시스템 관리

[시스템 상태 카드 행]
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ PostgreSQL       │ │ Kafka            │ │ Kogito           │
│ [● 정상]        │ │ [● 정상]        │ │ [● 정상]        │
│ 연결 풀: 12/20  │ │ 오프셋 지연: 0  │ │ 활성 인스턴스: 3│
└──────────────────┘ └──────────────────┘ └──────────────────┘

[두 컬럼]
┌──────────────────────────────────┐ ┌────────────────────────────────┐
│ 활성 BPMN 프로세스 인스턴스      │ │ Extension Pack 상태            │
│ ─────────────────────────────── │ │ ──────────────────────────────  │
│ 선거 발행 프로세스   [2] [관리]  │ │ [KR] ext-kr v1.0.0   [활성 ✓] │
│ 후보자 등록 프로세스 [5] [관리]  │ │ [US] ext-us          [비활성]  │
│ 개표 프로세스        [1] [관리]  │ │                                │
│                 [모두 보기 →]   │ │ [Extension Pack 설정 →]        │
└──────────────────────────────────┘ └────────────────────────────────┘

[프로세스 시그널 패널]
인스턴스 ID: [_______________]  시그널: [_______________]  페이로드: [{...}]
                                                            [시그널 전송]
```

---

## 12. Voting Kiosk UI (VVSG Special)

> **이 섹션은 최우선 순위다.** VVSG 2.0 §4 준수는 선거 시스템의 법적 요건이다. Kiosk UI의 모든 구현은 이 섹션의 규칙을 최우선으로 따른다.

### 12.1 Kiosk 모드 진입 및 설정

```typescript
// routes/vote/$sessionId.tsx
// 이 라우트 진입 시 자동으로 Kiosk 모드 적용

export const Route = createRoute({
  component: VotingSessionPage,
  beforeLoad: async ({ context }) => {
    // 1. Kiosk 테마로 강제 전환
    context.theme.setTheme('kiosk');

    // 2. 뒤로가기 방지 (의도치 않은 이탈 방지)
    window.history.pushState(null, '', window.location.href);

    // 3. 세션 유효성 검증
    // 4. 자동 로그아웃 타이머 설정 (10분 무활동)
  },
  onLeave: ({ context }) => {
    // 이전 테마 복원
    context.theme.restorePreviousTheme();
  }
});
```

### 12.2 Kiosk 레이아웃

```
┌──────────────────────────────────────────────────────────────┐
│ 투표 시스템                              세션: #A-2041       │
│ MiREMS | Miru Election Management Solution                  │
│ [고대비 ◑] [글자 크기 A-  A+]                               │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ← 이전 선거구               1 / 3 선거구              다음 →│
│                                                              │
│  [Contest 영역]                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│                     [투표 완료] (모든 선거구 선택 후 활성화) │
│                     [이 투표지 무효 처리]                    │
└──────────────────────────────────────────────────────────────┘
```

**헤더 고정:** 항상 상단에 표시. 사이드바 없음. 최대한 단순한 레이아웃.

### 12.3 Contest 렌더링 — CANDIDATE_CHOICE

```
제22대 국회의원선거 — 강남갑 선거구

한 명을 선택하세요.

┌──────────────────────────────────────────┐
│ ○  홍길동                                │  ← 최소 높이 80px
│    민주당                                │  ← 충분한 패딩
└──────────────────────────────────────────┘
┌──────────────────────────────────────────┐
│ ○  김철수                                │
│    국민의힘                              │
└──────────────────────────────────────────┘
┌──────────────────────────────────────────┐
│ ○  이영희                                │
│    정의당                                │
└──────────────────────────────────────────┘
┌──────────────────────────────────────────┐
│ ○  기권 (해당 선거구 선택 안 함)         │
└──────────────────────────────────────────┘
```

**선택 후:**
```
┌──────────────────────────────────────────┐
│ ●  홍길동           ← 선택됨 ✓          │  ← navy 배경, white 텍스트
│    민주당                                │
└──────────────────────────────────────────┘
```

**스타일 규칙:**
- 후보자 카드: `border-radius: var(--radius-lg)`, `border: 3px solid --color-border-default`
- 선택 시: `border-color: --color-navy-700`, `background: --color-navy-700`, `color: white`
- 고대비 모드 선택 시: `border-color: #ffff00`, `background: #000`, `color: #ffff00`
- 최소 터치 타겟: 카드 전체 클릭 영역 (전체 카드가 하나의 버튼)
- `role="radio"`, `aria-checked`, `name` 그룹핑

### 12.4 Contest 렌더링 — RANKED_CHOICE (ext-us)

```
후보자를 선호 순서대로 드래그하거나 번호를 클릭하세요.

┌───┬──────────────────────────────────────────┐
│ 1 │ ✋ 홍길동 (민주당)                        │ ← 드래그 핸들
├───┴──────────────────────────────────────────┤
│ 2 │ ✋ 김철수 (국민의힘)                      │
├───┴──────────────────────────────────────────┤
│ 순위 없음 │ 이영희 (정의당)                   │ ← 아직 순위 미배정
└───────────────────────────────────────────────┘
```

키보드 대안: 각 후보자 옆에 `[↑]` `[↓]` 버튼 제공.

### 12.5 검토 단계 (Review Step)

```
투표 내용을 확인하세요.

────────────────────────────────────────────────────────
선거구                    선택
────────────────────────────────────────────────────────
강남갑 지역구 선거         홍길동 (민주당)
비례대표 선거             민주당
────────────────────────────────────────────────────────

[수정하기 ←]                    [투표 확정 →]
```

**확정 버튼:** 크고 명확하게 표시. 확정 전 마지막 확인 Modal:

```
┌─────────────────────────────────────────────────────┐
│ 투표를 제출하시겠습니까?                             │
│                                                     │
│ 제출 후에는 취소할 수 없습니다.                     │
│                                                     │
│ 선택하신 내용:                                      │
│ • 강남갑: 홍길동 (민주당)                           │
│ • 비례대표: 민주당                                  │
│                                                     │
│ [아니오, 돌아가기]     [예, 제출합니다]             │
└─────────────────────────────────────────────────────┘
```

### 12.6 투표 완료 (Receipt)

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│              ✓                                       │
│                                                      │
│         투표가 완료되었습니다.                       │
│                                                      │
│    투표 영수증 번호                                  │
│    [abc123def456...789]  ← IBM Plex Mono             │
│                                                      │
│    이 번호로 투표 집계 여부를 확인할 수 있습니다.   │
│    영수증 번호를 기록해 두세요.                      │
│                                                      │
│                   [완료]                             │
│                                                      │
└──────────────────────────────────────────────────────┘
```

**완료 후:** 세션 종료, 초기 화면으로 복귀. 타이머 30초 후 자동 복귀.

### 12.7 접근성 특화 기능

**글자 크기 조정 (최소 → 최대: 1단계 씩):**
```typescript
const fontScales = [1.0, 1.25, 1.5, 1.75, 2.0]; // rem 배수
// document.documentElement.style.setProperty('--kiosk-font-scale', scale);
// 모든 Kiosk 텍스트: calc(var(--text-base) * var(--kiosk-font-scale))
```

**무활동 타이머:**
- 5분 경과 시: "투표를 계속하시겠습니까?" Modal (30초 카운트다운)
- 10분 경과 시: 세션 만료 처리 후 초기 화면 복귀

**스크린리더 지원:**
- 후보자 선택 시: `aria-live="polite"` 영역에 "홍길동 민주당 선택됨" 발화
- 페이지 이동 시: 선거구명 + 진행상황 발화
- 확정 Modal: 포커스 트랩 + 선택 내용 낭독

---

## 13. Extension Pack UI Integration

### 13.1 통합 원칙

Extension Pack UI는 Core Shell이 동적으로 로드한다. Core는 Extension UI를 직접 import하지 않는다. 플러그인 등록 패턴을 사용한다.

```typescript
// packages/ui-core/src/extension/ExtensionPlugin.ts
interface ExtensionPlugin {
  packId: string;      // 'kr', 'us', etc.
  displayName: string;

  // 라우트 세그먼트 주입
  routes?: RouteSegment[];

  // 기존 페이지에 컴포넌트 주입 (슬롯 시스템)
  slots?: {
    [slotId: string]: React.ComponentType<SlotProps>;
  };

  // i18n 네임스페이스
  i18nNamespace?: string;

  // Kiosk 투표 컴포넌트 오버라이드 (투표지 렌더링)
  ballotRenderer?: React.ComponentType<BallotRendererProps>;
}
```

### 13.2 슬롯 시스템

Core UI는 Extension이 컴포넌트를 주입할 수 있는 슬롯을 정의한다.

```typescript
// Core에서 정의한 슬롯
const SLOTS = {
  // 후보자 등록 폼 하단 추가 필드
  'candidate-form:extra-fields': 'candidate-form:extra-fields',

  // 선거 생성 wizard Step 2 관할 선택 컴포넌트
  'election-wizard:jurisdiction': 'election-wizard:jurisdiction',

  // 투표지 렌더링 (Kiosk)
  'ballot:contest-renderer': 'ballot:contest-renderer',

  // 대시보드 extension 정보 카드
  'dashboard:extension-card': 'dashboard:extension-card',

  // 결과 페이지 extension 집계 탭
  'results:extra-tab': 'results:extra-tab',
} as const;

// 슬롯 컴포넌트
interface SlotProps {
  electionId?: string;
  extensionData?: Record<string, unknown>;
}
```

### 13.3 ext-kr UI 특화 컴포넌트

**KrJurisdictionSelector** (`election-wizard:jurisdiction` 슬롯):
```
[시/도 선택 ▾] → [시/군/구 선택 ▾] → [읍/면/동 선택 ▾]

선택된 관할: 서울특별시 강남구
선거구 코드: 서울 강남갑
```

**KrProportionalBallot** (`ballot:contest-renderer` 슬롯):
```
비례대표 — 정당 선택

┌─────────────────────────────────────────┐
│ ○  민주당                               │
│    비례 후보: 1번 김○○, 2번 박○○...   │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│ ○  국민의힘                             │
│    비례 후보: 1번 이○○, 2번 최○○...   │
└─────────────────────────────────────────┘
```

**KrResultsTab** (`results:extra-tab` 슬롯):
- D'Hondt 방법에 의한 비례 의석 배분 계산 테이블 표시
- 정당 득표율 → 의석 수 변환 시각화

**KrElectionCalendar** (Extension 전용 라우트):
- 공직선거법 기반 선거 일정 달력 위젯
- 후보 등록 기간, 사전투표일, 선거일 강조

### 13.4 ext-us UI 특화 컴포넌트

**UsJurisdictionSelector** (`election-wizard:jurisdiction` 슬롯):
```
[State ▾] → [County ▾] → [Precinct ▾]
FIPS Code: 06 037 1234
```

**UsRcvBallot** (`ballot:contest-renderer` 슬롯):
Ranked Choice Voting 투표지 렌더링 (§12.4 참조)

**UsAbsenteeTracker** (Extension 전용 라우트):
UOCAVA 해외/군인 부재자 투표 추적 인터페이스

---

## 14. Responsive Design

### 14.1 레이아웃 변환 규칙

**Desktop (≥1280px):** 풀 사이드바 + 메인 콘텐츠

**Tablet (768–1279px):**
- 사이드바 → 아이콘 전용 축소 (64px) 또는 오버레이
- 테이블: 중요 컬럼만 표시, 나머지 드로어로

**Mobile (<768px):**
- 사이드바 → 하단 내비게이션 바
- 테이블 → 카드 뷰 (각 행이 카드로 변환)
- Wizard → 단계별 전체 화면
- PageHeader action 버튼 → FAB(Floating Action Button) 또는 상단 오른쪽 메뉴

### 14.2 테이블 반응형 전략

```typescript
interface ResponsiveColumn<T> {
  key: string;
  header: string;
  // 어떤 뷰포트에서 표시할지
  visibleFrom?: 'mobile' | 'tablet' | 'desktop';
  // 숨겨진 컬럼은 행 클릭 시 드로어에서 표시
}
```

**Mobile 카드 뷰 예시 (선거 목록):**
```
┌──────────────────────────────┐
│ 제22대 국회의원선거          │
│ 국회의원선거  [ACTIVE ●]     │
│ 선거일: 2026-04-10           │
│                   [상세 →]  │
└──────────────────────────────┘
```

### 14.3 Kiosk UI 반응형

Kiosk UI는 반응형이 아니다. 고정 크기(최소 1024×768) 화면을 대상으로 한다. 태블릿 세로 모드에서는 별도 레이아웃 제공.

---

## 15. Internationalization (i18n)

### 15.1 i18n 설정

```typescript
// packages/i18n/src/config.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

i18n.use(initReactI18next).init({
  lng: 'ko',           // 기본 언어 (한국어)
  fallbackLng: 'en',
  ns: ['common', 'election', 'candidate', 'ballot', 'voter', 'audit', 'kiosk'],
  defaultNS: 'common',

  // Extension Pack 네임스페이스 동적 로드
  // ext-kr: 'ext-kr', ext-us: 'ext-us'
});
```

### 15.2 번역 키 구조

```json
// packages/i18n/locales/ko.json
{
  "common": {
    "actions": {
      "save": "저장",
      "cancel": "취소",
      "confirm": "확인",
      "delete": "삭제",
      "edit": "수정",
      "view": "보기",
      "export": "내보내기",
      "search": "검색"
    },
    "status": {
      "loading": "로딩 중...",
      "error": "오류가 발생했습니다",
      "empty": "데이터가 없습니다"
    }
  },
  "election": {
    "status": {
      "DRAFT": "초안",
      "PUBLISHED": "발행됨",
      "ACTIVE": "진행 중",
      "CLOSED": "마감됨",
      "CERTIFIED": "인증됨"
    },
    "type": {
      "CANDIDATE_CHOICE": "단순 다수제",
      "PROPORTIONAL": "비례대표",
      "RANKED_CHOICE": "순위 선택"
    }
  },
  "kiosk": {
    "selectCandidate": "후보자를 선택하세요",
    "reviewTitle": "투표 내용을 확인하세요",
    "confirmSubmit": "투표를 제출하시겠습니까?",
    "submittedTitle": "투표가 완료되었습니다",
    "receiptLabel": "투표 영수증 번호"
  }
}
```

### 15.3 언어 전환

- 헤더 우측: 언어 토글 `[한국어 | English]`
- 선택 시 즉시 전환 (페이지 새로고침 없음)
- 현재 선택: 메모리에 저장 (localStorage 금지)
- Kiosk 모드: 투표 시작 전 언어 선택 화면 제공

### 15.4 숫자/날짜 형식

```typescript
// packages/i18n/src/formatters.ts
const formatters = {
  ko: {
    date: 'YYYY년 M월 D일',
    dateTime: 'YYYY년 M월 D일 HH:mm',
    number: (n: number) => n.toLocaleString('ko-KR'),
    percent: (n: number) => `${n.toFixed(1)}%`,
  },
  en: {
    date: 'MMMM D, YYYY',
    dateTime: 'MMMM D, YYYY HH:mm',
    number: (n: number) => n.toLocaleString('en-US'),
    percent: (n: number) => `${n.toFixed(1)}%`,
  },
};
```

---

## 16. Motion & Animation

### 16.1 기본 원칙

선거 시스템은 화려한 애니메이션보다 **명확한 피드백**을 우선한다. 모든 애니메이션은 기능적 목적이 있어야 한다.

```css
:root {
  --duration-fast:   100ms;
  --duration-normal: 200ms;
  --duration-slow:   300ms;
  --duration-slower: 500ms;

  --ease-standard:   cubic-bezier(0.4, 0, 0.2, 1);
  --ease-enter:      cubic-bezier(0, 0, 0.2, 1);
  --ease-exit:       cubic-bezier(0.4, 0, 1, 1);
}
```

### 16.2 컴포넌트별 애니메이션

| 컴포넌트 | 애니메이션 | Duration | Easing |
|---|---|---|---|
| Button hover | `transform: translateY(-1px)` + shadow 강화 | 100ms | standard |
| Button press | `transform: translateY(0)` + shadow 약화 | 50ms | standard |
| Modal 열기 | `scale(0.95) → scale(1)` + `opacity: 0→1` | 200ms | enter |
| Modal 닫기 | `scale(1) → scale(0.95)` + `opacity: 1→0` | 150ms | exit |
| Dropdown 열기 | `translateY(-8px) → translateY(0)` + `opacity: 0→1` | 150ms | enter |
| Alert 진입 | `translateX(-8px) → translateX(0)` + `opacity: 0→1` | 200ms | enter |
| Page 전환 | `opacity: 0→1` | 150ms | enter |
| Process node 활성화 | pulse ring 애니메이션 | 1.5s infinite | — |

### 16.3 감소된 모션 지원 (WCAG 2.3.3)

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 16.4 페이지 전환

TanStack Router의 `pendingComponent`를 활용하되, 최소한의 스켈레톤 UI 표시:

```typescript
// 페이지 전환 스켈레톤
function PageSkeleton() {
  return (
    <div aria-busy="true" aria-label="페이지 로딩 중">
      <div className={styles.pageHeaderSkeleton} />
      <div className={styles.contentSkeleton} />
    </div>
  );
}
```

---

## 17. Icon System

### 17.1 아이콘 라이브러리

`lucide-react@0.383.0` (이미 README에 명시된 버전)을 사용한다.

```typescript
// 사용 패턴
import { Check, AlertTriangle, ChevronRight } from 'lucide-react';

// 크기: 16px(sm), 20px(md, 기본), 24px(lg)
<Check size={20} strokeWidth={2} aria-hidden="true" />

// 아이콘 전용 버튼은 반드시 aria-label 제공
<button aria-label="삭제">
  <Trash2 size={20} aria-hidden="true" />
</button>
```

### 17.2 기능별 아이콘 매핑

| 기능 | 아이콘 | Lucide Name |
|---|---|---|
| 선거 | 📋 | `ClipboardList` |
| 후보자 | 👤 | `User` |
| 투표소/투표지 | 🗳 | `Vote` |
| 선거인 | 👥 | `Users` |
| 감사 | 📁 | `FolderOpen` |
| 시스템 관리 | ⚙️ | `Settings` |
| 프로세스 | 🔄 | `GitBranch` |
| 결과 | 📊 | `BarChart2` |
| 승인 | ✓ | `CheckCircle` |
| 반려 | ✗ | `XCircle` |
| 경고 | ⚠️ | `AlertTriangle` |
| 잠금 | 🔒 | `Lock` |
| 해시/서명 | # | `Hash` |
| 다운로드 | ↓ | `Download` |
| 발행 | 📤 | `Send` |
| 인증 | 🏆 | `Award` |

### 17.3 상태 아이콘 컴포넌트

```typescript
// packages/ui-core/src/components/StatusIcon/StatusIcon.tsx
interface StatusIconProps {
  status: ElectionStatus | CandidateStatus | SessionStatus;
  size?: number;
}

// 상태에 따라 적절한 아이콘 + 색상 반환
// 색상은 Badge와 동일한 semantic 색상 사용
```

---

## 18. Error & Loading States

### 18.1 로딩 상태 패턴

**전체 페이지 로딩:** TanStack Router `pendingComponent` 스켈레톤
**데이터 로딩:** 테이블/카드 내 스켈레톤 UI
**버튼 액션:** `loading` prop (Spinner 인라인)
**즉각 피드백:** `optimistic update` (TanStack Query)

**스켈레톤 컴포넌트:**
```typescript
// packages/ui-core/src/components/Skeleton/Skeleton.tsx
interface SkeletonProps {
  variant?: 'text' | 'rect' | 'circle';
  width?: string | number;
  height?: string | number;
  lines?: number;  // text variant에서 여러 줄
}

// CSS: pulse 애니메이션으로 배경색 변화
// background: linear-gradient(90deg, --color-neutral-100, --color-neutral-200, --color-neutral-100)
// background-size: 200% 100%; animation: shimmer 1.5s infinite
```

### 18.2 에러 상태 패턴

**API 에러 처리 계층:**

```
API 에러 (4xx, 5xx)
├── 401 Unauthorized → 로그인 페이지로 리다이렉트
├── 403 Forbidden → 인라인 Alert (권한 없음)
├── 404 Not Found → 404 페이지 또는 인라인 메시지
├── 409 Conflict → 인라인 Alert (예: 중복 투표)
├── 422 Validation → 폼 필드별 에러 메시지
└── 5xx Server Error → Error Boundary (재시도 옵션)
```

**전역 Error Boundary (루트 레벨):**
```
┌──────────────────────────────────────────────────────┐
│                    오류가 발생했습니다                │
│                                                      │
│  시스템에 예상치 못한 오류가 발생했습니다.           │
│  잠시 후 다시 시도해 주세요.                        │
│                                                      │
│  오류 코드: [ERR-2026-0515-001]                     │
│                                                      │
│  [홈으로 이동]  [다시 시도]  [오류 신고]            │
└──────────────────────────────────────────────────────┘
```

**인라인 에러 (컴포넌트 레벨):**
```
<Alert variant="error" title="데이터를 불러올 수 없습니다">
  네트워크 연결을 확인하고 다시 시도해 주세요.
  <Button variant="ghost" size="sm">다시 시도</Button>
</Alert>
```

### 18.3 404 페이지

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│      404                                             │
│      페이지를 찾을 수 없습니다                       │
│                                                      │
│  요청하신 페이지가 존재하지 않거나                  │
│  접근 권한이 없습니다.                              │
│                                                      │
│  [← 이전 페이지]  [대시보드로 이동]                 │
└──────────────────────────────────────────────────────┘
```

### 18.4 폼 에러 패턴

```typescript
// TanStack Query + Zod 스키마 연동

// 서버 검증 에러를 폼 필드에 매핑
function mapServerErrors(
  errors: ProblemDetail,
  setFieldError: (field: string, error: string) => void
) {
  errors.violations?.forEach(v => {
    setFieldError(v.field, v.message);
  });
}

// RFC 7807 ProblemDetail 타입
interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  violations?: Array<{ field: string; message: string }>;
}
```

---

## 19. Agent Implementation Checklist

AI Agent(Hermes)가 각 PLAN.md Goal을 구현할 때 반드시 확인해야 하는 항목.

### 19.1 모든 컴포넌트 구현 시

```markdown
- [ ] CSS Custom Property 토큰만 사용 (하드코딩 색상/크기 금지)
- [ ] `aria-*` 속성 완전 구현 (role, label, describedby, live 등)
- [ ] `ref` forwarding 구현 (`React.forwardRef`)
- [ ] `disabled`, `loading`, `error` 상태 처리
- [ ] 키보드 내비게이션 지원 (Tab, Enter, Space, Escape, Arrow keys)
- [ ] `@media (prefers-reduced-motion: reduce)` 처리
- [ ] `data-theme="kiosk"` 환경에서 kiosk 토큰 사용 확인
- [ ] axe-core 위반 0건 확인
- [ ] 컴포넌트 단위 테스트 (Vitest + RTL) 작성
```

### 19.2 페이지 구현 시 (P5 Goals)

```markdown
- [ ] 이 DESIGN.md의 해당 페이지 섹션 완전 검토
- [ ] PageHeader 컴포넌트 사용 (title, breadcrumb, actions 포함)
- [ ] RoleGuard로 역할별 UI 요소 제어
- [ ] 로딩 상태 처리 (Skeleton or Spinner)
- [ ] 에러 상태 처리 (Alert + 재시도)
- [ ] 빈 상태(Empty State) 처리
- [ ] 반응형 레이아웃 검증 (mobile, tablet, desktop)
- [ ] i18n 키 사용 (하드코딩 문자열 금지)
- [ ] MSW를 이용한 API 모킹 테스트 포함
- [ ] route protection (`ProtectedRoute`) 적용
```

### 19.3 BPMN 프로세스 연동 페이지 시

```markdown
- [ ] ProcessStatusPanel 컴포넌트 통합
- [ ] 프로세스 상태 폴링 또는 SSE 연동 (30초 간격)
- [ ] 역할별 프로세스 액션 버튼 표시 (RoleGuard)
- [ ] Task Notification 시스템과 연동
- [ ] 프로세스 에러 상태 처리 (Alert + 지원 연락처)
```

### 19.4 Kiosk UI 구현 시 (GOAL P5-052)

```markdown
- [ ] data-theme="kiosk" 자동 적용 확인
- [ ] 모든 텍스트 최소 24px (IBM Plex Sans)
- [ ] 모든 터치 타겟 최소 48×48px
- [ ] localStorage 사용 없음 (세션 상태는 메모리)
- [ ] 무활동 타이머 구현 (5분 경고, 10분 만료)
- [ ] 고대비 모드 전환 버튼 존재
- [ ] 글자 크기 조정 버튼 존재 (5단계)
- [ ] axe-core 위반 0건 (WCAG 2.1 AA + VVSG §4)
- [ ] 화면 낭독기 테스트 (VoiceOver / NVDA)
- [ ] 투표 확정 전 이중 확인 Modal 존재
- [ ] 뒤로가기 방지 구현
- [ ] 투표 완료 후 영수증 해시 표시
```

### 19.5 Extension Pack UI 구현 시 (P6, P7 Goals)

```markdown
- [ ] ExtensionPlugin 인터페이스 구현
- [ ] Core UI 직접 import 금지 (슬롯 시스템 사용)
- [ ] 해당 Extension 비활성화 시 UI 완전히 제거됨 확인
- [ ] Core UI와 동일한 Design Token 사용
- [ ] 확장 팩 고유 네임스페이스 i18n 키 사용
- [ ] Extension 슬롯 타입 안전성 확인 (TypeScript)
```

---

## Appendix A: File Structure for ui-core

```
packages/ui-core/
├── src/
│   ├── tokens/
│   │   ├── colors.css
│   │   ├── typography.css
│   │   ├── spacing.css
│   │   ├── shadows.css
│   │   ├── radii.css
│   │   ├── z-index.css
│   │   ├── animation.css
│   │   └── index.css          ← 모든 토큰 통합
│   ├── themes/
│   │   ├── theme-light.css
│   │   ├── theme-dark.css
│   │   ├── theme-high-contrast.css
│   │   └── theme-kiosk.css
│   ├── components/
│   │   ├── Button/
│   │   │   ├── Button.tsx
│   │   │   ├── Button.module.css
│   │   │   └── Button.test.tsx
│   │   ├── Input/
│   │   ├── Select/
│   │   ├── DatePicker/
│   │   ├── Table/
│   │   │   ├── Table.tsx
│   │   │   ├── TableRow.tsx
│   │   │   ├── Pagination.tsx
│   │   │   └── ...
│   │   ├── Modal/
│   │   ├── Badge/
│   │   ├── Alert/
│   │   ├── Spinner/
│   │   ├── Card/
│   │   ├── Tabs/
│   │   ├── ProgressSteps/
│   │   ├── Skeleton/
│   │   ├── StatusIcon/
│   │   └── ProcessStatusPanel/   ← BPMN 전용
│   ├── auth/
│   │   ├── roles.ts
│   │   └── RoleGuard.tsx
│   ├── extension/
│   │   ├── ExtensionPlugin.ts
│   │   └── SlotRenderer.tsx
│   ├── hooks/
│   │   ├── useTheme.ts
│   │   ├── useTaskNotifications.ts
│   │   └── useProcessStatus.ts
│   └── index.ts               ← Public API export
├── package.json
└── tsconfig.json
```

---

## Appendix B: CSS Module Naming Convention

```css
/* 컴포넌트.module.css */

/* 루트 클래스: 컴포넌트 이름 */
.button { }

/* 상태: is- 접두사 */
.isLoading { }
.isDisabled { }
.isError { }

/* Variant: variant- 접두사 */
.variantPrimary { }
.variantSecondary { }

/* Size: size- 접두사 */
.sizeSm { }
.sizeMd { }
.sizeLg { }
.sizeKiosk { }

/* 내부 요소: camelCase */
.innerWrapper { }
.iconLeft { }
.labelText { }
```

---

## Appendix C: Key Technical Constraints

| 제약 | 이유 | 구현 방법 |
|---|---|---|
| localStorage 사용 금지 | VVSG 2.0 보안 요건 | React state, session memory |
| sessionStorage 사용 금지 | VVSG 2.0 보안 요건 | React state |
| 토큰 메모리 저장 | VVSG 2.0 §4 | `oidc-client-ts` in-memory mode |
| 테마 선택 메모리 저장 | VVSG 보안 일관성 | React context |
| 하드코딩 문자열 금지 | i18n 요건 | i18next 키 |
| 하드코딩 색상/크기 금지 | 테마 시스템 | CSS Custom Property |
| outline: none 단독 금지 | 접근성 (WCAG 2.4.7) | box-shadow focus ring 병용 |
| 색상만으로 정보 전달 금지 | 접근성 (WCAG 1.4.1) | 아이콘 + 텍스트 병용 |
| Core의 Extension import 금지 | 아키텍처 격리 | 슬롯 + 플러그인 시스템 |
| Kogito 직접 호출 금지 | 아키텍처 격리 | MiremsProcessService 인터페이스 |

---

*MiREMS Platform DESIGN.md — Version 1.0.0*
*최종 수정: 2026-05-15*
*연관 PLAN.md Goals: P5-045 ~ P5-058, P6-064, P7-073~078*
*VVSG 2.0 Compliance: Vol. I §4 (Accessibility), §12 (Audit)*
*준수 기준: WCAG 2.1 Level AA, ARIA 1.2, ISO 639-1*
