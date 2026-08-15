# CloudNest Frontend

The CloudNest Personal Cloud **React + TypeScript** single-page application.

A modern, fully implemented cloud-storage UI: authentication (incl. OTP and
password reset), a dashboard, a full file explorer, folders, sharing with
public share links, trash, profile/settings, notifications, storage
analytics, audit logs, a security page (MFA/passkeys), an admin dashboard
guard, global search and an about page.

---

## Tech Stack

| Concern        | Technology                                                          |
|----------------|---------------------------------------------------------------------|
| Framework      | React 19 + TypeScript 5.9, Vite 6, `@vitejs/plugin-react`           |
| Styling        | Tailwind CSS 4 (`@tailwindcss/vite`)                                |
| Routing        | React Router 7 (`createBrowserRouter` + lazy pages)                 |
| Server state   | TanStack React Query 5                                              |
| Client state   | Zustand 5 (persisted via `zustand/middleware`)                      |
| Forms          | React Hook Form 7                                                   |
| HTTP           | Axios (interceptors: auth header, refresh-on-401, 401 logout)       |
| Motion         | Framer Motion 12                                                    |
| Icons          | lucide-react                                                        |
| Notifications  | react-toastify                                                      |
| PDF preview    | pdfjs-dist                                                          |
| QR codes       | qrcode.react (MFA setup)                                            |
| Lint / Format  | ESLint 10, Prettier 3 (+ Tailwind plugin), typescript-eslint        |

The `@` alias maps to `src/` (see `vite.config.ts`).

---

## Scripts

```bash
npm run dev            # Vite dev server on http://localhost:5173
npm run typecheck      # tsc -b (project references)
npm run build          # tsc -b && vite build
npm run preview        # serve the production build (vite preview)
npm run lint           # ESLint
npm run format         # Prettier write
npm run format:check   # Prettier check
```

---

## Project Structure

```text
src/
├── api/            # axios instance + interceptors, React Query client
├── assets/         # static assets
├── components/
│   ├── common/     # shared UI (Brand, Card, Button, Modal, EmptyState, ...)
│   ├── files/      # explorer UI (FileGrid/Table/Card/Row, toolbars, modals)
│   ├── folders/    # folder grids / toolbars
│   ├── layout/     # Sidebar, Navbar, GlobalSearch, UserMenu, NotificationBell
│   ├── notifications/
│   ├── security/   # MfaPanels (TwoFactorPanel, PasskeysPanel)
│   ├── share-public/  # public share-link UI (PublicShareCard, SharePasswordGate)
│   ├── shares/     # share dialogs/badges
│   ├── trash/
│   └── ui/         # low-level primitives
├── constants/      # app config, routes, API endpoints, storage keys, file rules
├── context/        # AppProviders (error boundary + React Query + router + toasts)
├── hooks/          # 26 feature hooks (data fetching, mutations, ui helpers)
├── layouts/        # AuthLayout, DashboardLayout
├── pages/          # feature pages (see below)
├── routes/         # router config, lazy page registry, route guards
├── services/       # per-domain API service modules
├── store/          # zustand stores
├── styles/         # global CSS
├── types/          # domain type modules + barrel
└── utils/          # cn, format, file, download, error, theme/device helpers
```

---

## Feature Modules (implemented)

| Area                  | Pages / Routes                                        | Notes |
|-----------------------|-------------------------------------------------------|-------|
| Authentication        | `/login`, `/register`                                 | JWT, device remember, passkey sign-in |
| OTP verification      | `/verify-otp`                                         | Redirects to `/login` without a pending challenge |
| Forgot / reset        | `/forgot-password`                                    | Email reset-code flow |
| Dashboard             | `/dashboard`                                          | Storage overview, recent files/folders, stats |
| File explorer         | `/files`                                              | Grid/table/card views, search, sort/filter, favorites |
| File operations       | -                                                     | Upload (dropzone/progress), download, preview (incl. PDF), rename, move, delete, duplicate, versions, zip |
| Folders               | `/folders`                                            | Folder management |
| Sharing               | `/shared`                                             | Share dialogs, expiry/permission badges |
| My Shares             | `/my-shares`                                          | Manage shares (route wired, guest-safe redirect) |
| Public share links    | `/s/:token` (public, no auth)                         | Password gate, download, preview |
| Trash                 | `/trash`                                              | Restore / permanent delete |
| Profile               | `/profile`                                            | User info, avatar, change password |
| Settings              | `/settings`                                           | Preferences / devices |
| Notifications         | `/notifications`                                      | Unread badge in the layout bell |
| Analytics             | `/analytics`                                          | Storage analytics (hand-rolled charts) |
| Audit logs            | `/audit-logs`                                         | File audit trail |
| Security              | `/security`                                           | 2FA/MFA, passkeys, sessions, trusted devices |
| Admin                 | `/admin` (`AdminRoute` guard restored, **route not wired**) | See *Known gaps* below |
| About                 | `/about`                                              | App info + gateway health |

Theme (light / dark / system) is persisted and applied pre-hydration;
navigation includes the sidebar, navbar with global search and the
notification bell.

---

## Routing & Guards

The router is built with `createBrowserRouter` (`src/routes/index.tsx`) with
lazy page components registered in `src/routes/pages.ts`:

- **Public** - `/s/:token` renders the public share page without any auth.
- **Guest-only** (`GuestRoute`) - `/login`, `/register`, `/verify-otp`,
  `/forgot-password`; signed-in users are redirected to `/dashboard`.
- **Protected** (`ProtectedRoute` + `DashboardLayout`) - every app page;
  guests are redirected to `/login` (with the original location preserved).
- **Admin guard** (`AdminRoute`, restored standalone) - reserved for the
  admin dashboard, not yet wired into the router.
- **Catch-all** - unknown paths render the 404 `NotFoundPage`.

`/` redirects to `/dashboard`.

---

## State & Data

**Zustand stores** (`src/store/`):

- `authStore` - access/refresh tokens, user, device id, persistence,
  login/logout hydration.
- `themeStore` - `light` / `dark` / `system` theme with `applyTheme`.
- `uiStore` - sidebar collapse + mobile drawer state.
- `filesStore` / `foldersStore` - explorer selection, view mode, sorting,
  filters, folder tree/breadcrumb state.
- `explorerStore` - navigation stack + breadcrumb coordination.

**API layer** (`src/api/`):

- `axios.ts` - single axios instance (`API_BASE_URL`, default
  `http://localhost:8080/api`, overridable via `VITE_API_BASE_URL`) with a
  request auth-token interceptor, a refresh-on-401 interceptor, and 401
  logout handling.
- `queryClient.ts` - shared React Query client.

**Services** (`src/services/`): `auth`, `user`, `folder`, `file`, `share`,
`notification`, `admin`, `system`. Feature data is consumed through
type-safe hooks in `src/hooks/` (e.g. `useFiles`, `useFolders`, `useShare`,
`useNotifications`, `useSecurity`, `useAdmin`, `useAuditLogs`,
`useStorageAnalytics`).

---

## Validation Status

| Check                | Result |
|----------------------|--------|
| `npm run typecheck`  | **0 errors** (passes clean) |
| `npm run build`      | **PASS** (tsc + vite, exit 0) |
| Browser smoke test   | All public + guest + protected routes resolve correctly; guests are redirected to `/login`; `/s/:token` renders without auth; only unknown paths show the 404 page; zero console errors |
| `git diff --check`   | PASS |

**Backend-dependent flows** (login/logout against a live server, file
CRUD, upload, share download, notification counts, MFA provisioning) are
implemented end-to-end but require the backend stack (see the repo root
`docker-compose.yml`) to be running; API failures are handled gracefully by
error states and toasts.

---

## Recovery / Alignment Status

The frontend was reconstructed against the recovery reference
`recovery/cloudnest-full-work` (`dd74437`) through a sequence of
feature-scoped passes and is now **aligned with intentional differences**:

- `184 / 188` recovery source files are **byte-for-byte identical**.
- `package.json`, `package-lock.json`, `apiEndpoints.ts`, `routes.ts` and
  `app.ts` are byte-identical to recovery.
- `types/index.ts` - same exports (a superset: also exports
  `RefreshTokenRequest`), with a NOTE documenting the reconstruction.
- `routes/index.tsx` / `routes/pages.ts` - same routes, hand-ordered; the
  `/admin` route is intentionally deferred (guard restored standalone).
- `services/index.ts` - recovery's services barrel is not present (no
  consumers on this branch; the individual service modules are).
- `components/common/SearchBar.tsx` - intentionally retained; unused
  (the app uses `components/files/SearchBar.tsx` and
  `layout/GlobalSearch.tsx`).

### Alignment status

- **Unexpected recovery differences: 0.** The `/my-shares` route is wired
  (guest access redirects to `/login`, runtime-verified) and
  `Highlight.tsx` is byte-for-byte aligned with recovery.
- `184 / 188` recovery source files are byte-for-byte identical; the 3
  remaining content-differing files (`routes/index.tsx`,
  `routes/pages.ts`, `types/index.ts`) are intentional, and the `/admin`
  route stays intentionally deferred (guard restored standalone).

**Backend-dependent testing** (login, file CRUD, sharing, notifications,
MFA, gateway health) remains pending until the backend stack is running.

**Not claimed:** byte-for-byte equality with recovery, since the
intentional differences above remain.
