# ☁️ CloudNest — Frontend

> React 19 · TypeScript · Vite 8 · Tailwind CSS v4 — the frontend foundation for
> CloudNest Personal Cloud.

## 🚀 Quick start

```bash
npm install
npm run dev
```

The dev server runs at **http://localhost:5173**.

| Script                 | Description                              |
| ---------------------- | ---------------------------------------- |
| `npm run dev`          | Start the Vite dev server                |
| `npm run build`        | Type-check (`tsc -b`) + production build |
| `npm run preview`      | Preview the production build locally     |
| `npm run lint`         | ESLint (flat config)                     |
| `npm run format`       | Format everything with Prettier          |
| `npm run format:check` | Verify Prettier formatting               |
| `npm run typecheck`    | TypeScript project-reference type-check  |

## 🔧 Environment variables

Copy `.env.example` to `.env` and adjust if the gateway is not at the default
location:

| Variable            | Default                     | Description                           |
| ------------------- | --------------------------- | ------------------------------------- |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Base URL of the CloudNest API Gateway |

## 📁 Project structure

```text
src/
├── api/          # Axios client + interceptors, QueryClient
├── assets/       # Static assets (logo, etc.)
├── components/
│   ├── common/   # Brand, Breadcrumb, EmptyState, ErrorBoundary, ErrorState,
│   │             # Loader, PageHeader, PagePlaceholder, SearchBar
│   ├── layout/   # Sidebar, Navbar, Footer, ThemeToggle, UserMenu, NotificationBell
│   └── ui/       # Button, Input, PasswordInput, Card, Modal, Spinner, ConfirmationDialog
├── constants/    # App config, routes, API endpoints, storage keys, validation rules
├── context/      # AppProviders — provider composition root
├── hooks/        # useAuth, useDebounce, useMediaQuery, useClickOutside
├── layouts/      # AuthLayout, DashboardLayout
├── pages/        # auth/, dashboard/, files/, folders/, profile/, settings/,
│                 # notifications/, share/, trash/, NotFoundPage
├── routes/       # createBrowserRouter config + ProtectedRoute/GuestRoute guards
├── services/     # Typed API service modules (auth, user, folder, file, share, notification)
├── store/        # Zustand stores: auth (persisted), theme (persisted), ui
├── styles/       # Tailwind v4 entry + design tokens
├── types/        # Domain & API types matching the backend contract
└── utils/        # cn, format, storage, error, validation helpers
```

## 🏗️ Architecture notes

- **Routing** — React Router v7 (data router, `createBrowserRouter` +
  `RouterProvider`). `ProtectedRoute` gates the dashboard shell and redirects to
  `/login`; `GuestRoute` bounces signed-in users off auth pages. All paths live
  in `constants/routes.ts`.
- **Data fetching** — TanStack Query v5 with a shared `QueryClient`
  (`staleTime: 60s`, no refetch on window focus). Typed service modules in
  `services/` are the single place that talks to the API; UI is not wired to
  them yet by design.
- **Axios** — One client against the gateway. The request interceptor attaches
  `Authorization: Bearer <jwt>`; the response interceptor handles 401 (logout +
  redirect), 403, 500 and network errors with toasts. Auth endpoints opt out of
  the 401 redirect via `skipAuthRedirect`.
- **State** — Zustand v5 with `persist` (localStorage). The auth store holds the
  JWT + user; the theme store drives class-based dark mode; the ui store holds
  sidebar state.
- **Styling** — Tailwind CSS v4 (CSS-first, `@tailwindcss/vite`). The design
  tokens live in `tailwind.config.js`, wired in via `@config` in
  `src/styles/index.css`; class-based dark mode is enabled with
  `@custom-variant dark` and applied pre-hydration from `index.html`.
- **Code quality** — Strict TypeScript (`strict`, `verbatimModuleSyntax`,
  `noUnusedLocals`), no `any`, ESLint flat config with React Hooks + Refresh
  rules, Prettier with Tailwind class sorting, absolute imports via the `@`
  alias.

## 🔐 Storage keys

Persisted Zustand stores use these localStorage keys (see `constants/storage.ts`):

| Key               | Store                   |
| ----------------- | ----------------------- |
| `cloudnest-auth`  | JWT token + user        |
| `cloudnest-theme` | Light/dark theme        |
| `cloudnest-ui`    | Sidebar collapsed state |

> Keep `cloudnest-theme` in sync with the inline pre-hydration script in
> `index.html`.

## 🧭 Next steps

1. Wire the auth pages to `authService` (login → `setAuth` + redirect; register
   → success toast + redirect to login).
2. Implement module pages on top of the existing UI kit and services.
3. Add route-based code splitting (`React.lazy`) once page bundles grow.
4. Add an error page for route-level errors and refine the 401 flow.
