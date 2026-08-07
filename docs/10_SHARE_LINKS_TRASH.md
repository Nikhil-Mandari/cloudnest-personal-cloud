# Phase 3 — Public Share Links, My Shares Management & Trash

**Status:** Complete · **Scope:** frontend-first, small backend delta
**Validated:** share-service compiles · frontend `tsc` / production build / ESLint all clean · `qa-e2e.sh` extended with 9 new Phase 3 scenarios

---

## 1. What was built

### 1.1 Public share-link browse page (`/s/:token`)

A standalone, **unauthenticated** page for anyone opening a CloudNest share link.

- **Link states:** loading skeleton → valid share → password gate → expired (`410`) → removed/not-found (`404`) → generic error with retry.
- **Password prompt:** protected links show a lock gate; password is verified against `POST /shares/public/{token}/verify-password` before the content is revealed. Wrong passwords render inline (never a session-timeout redirect).
- **View:** image files preview in-browser via a new **public preview endpoint** that streams the bytes *without* counting as a download.
- **Download:** streams through `GET /shares/public/{token}/download` with the verified password sent via the `X-Share-Password` header; if the owner changed the password, a `401` sends the visitor back to the gate.
- **Permission-aware:** `VIEW` links hide the Download action (preview still allowed); folders show a "opens inside CloudNest" note instead of download.
- Copy-link, sign-in / "Go to My Files" escape hatches, and a "Powered by CloudNest" footer.

### 1.2 My Shares management page (`/my-shares`)

Owner view of every share link the user created, with **analytics** and **link settings**.

- **Table:** resource, permission, link status (password lock + expiry), views, downloads, last access, created date; search + sort (date / name / views / downloads).
- **Link settings dialog:**
  - Copyable link URL.
  - **Analytics cards** (views, downloads, last access) from the owner-only analytics endpoint.
  - **Permission** (Can view / Download only / Can edit).
  - **Expiry** presets — including **"Never expires"**, which now truly *clears* the expiry via a new `clearExpiry` flag.
  - **Password** — set a new one or remove protection.
  - **Revoke** (with confirmation) — the link 404s immediately afterwards.
- The dialog form resets on every open so it always reflects the current share record.

### 1.3 Trash page enhancements

- **Restore all** button added to the toolbar (restores every item with one summary toast instead of one toast per item).
- Batch operations (restore all, restore selection, delete selection) now use **quiet mutations** + a single summary toast — restoring 50 items no longer queues 50 toasts.
- Existing restore-selected, permanent-delete and empty-trash flows preserved.

## 2. Files changed

### Backend — share-service
| File | Change |
|---|---|
| `controller/ShareController.java` | New `GET /public/{token}/preview` endpoint (streams without counting a download). |
| `service/ShareService.java` | New `previewPublicShare(token, password)` contract. |
| `service/impl/ShareServiceImpl.java` | `previewPublicShare` impl; `downloadPublicShare` now enforces `requireDownloadPermission` (VIEW → 403); extracted shared `streamShareContent` helper; `updateShare` honours `clearExpiry`. |
| `dto/UpdatePermissionRequest.java` | New `clearExpiry` flag (mirrors `clearPassword`). |

### Frontend
| Area | Change |
|---|---|
| `pages/share/PublicSharePage.tsx` | New standalone public page (state machine + auth-free layout). |
| `components/share-public/SharePasswordGate.tsx` | New password gate form. |
| `components/share-public/PublicShareCard.tsx` | New content card: preview / download / copy, permission-aware. |
| `pages/share/MySharesPage.tsx` | New management page. |
| `components/shares/MySharesTable.tsx`, `MySharesRow.tsx` | New owner table with analytics columns. |
| `components/shares/ShareSettingsDialog.tsx` | New link-settings dialog (analytics + permission/expiry/password/revoke). |
| `services/share.service.ts` | `getPublicShare`, `downloadPublicShare`, `previewPublicShare` — all `silent` + `skipAuthRedirect` so public 401s (wrong share password) never trigger the force-logout interceptor. |
| `hooks/useShare.ts` | `usePublicShareQuery` (no retry, no refetch-on-focus — every fetch records a view server-side), `useRevokeShareMutation`. |
| `hooks/useTrash.ts` | Quiet batch mutations. |
| `pages/trash/TrashPage.tsx`, `components/trash/TrashToolbar.tsx` | Restore-all action + batch summary toasts. |
| `utils/file.ts` | `buildShareUrl` now returns the frontend `/s/:token` URL; `isShareableImageName`. |
| `utils/format.ts` | `toLocalDateTimeIso` — strips the `Z` suffix that Jackson's `LocalDateTime` parser rejects (fixes a latent ShareDialog expiry bug too). |
| `types/share.types.ts`, `utils/share.ts` | `MySharesSortKey`, `clearExpiry`, my-shares filter/sort helpers. |
| `constants/routes.ts`, `apiEndpoints.ts`, `routes/pages.ts`, `routes/index.tsx`, `Sidebar.tsx`, `types/index.ts`, `ShareEmptyState.tsx`, `ShareDialog.tsx` | Wiring + small fixes. |

## 3. New / changed APIs

| Method & path | Auth | Description |
|---|---|---|
| `GET /api/shares/public/{token}/preview` | None (public) | Streams the shared file for preview; **does not** increment `downloadCount`. Password via `X-Share-Password` / `password`. |
| `GET /api/shares/public/{token}/download` | None (public) | Now returns **403** for `VIEW`-permission links. |
| `PUT /api/shares/{id}` | JWT + owner | Accepts `clearExpiry` to remove the expiry date. |

## 4. Security improvements

- **Preview ≠ download:** image previews stream through a dedicated endpoint so owner analytics stay honest.
- **VIEW links are truly view-only:** anonymous downloads of `VIEW` shares are rejected with 403 — coherent with "Can view" vs "Download only".
- **No authz bypass on public flows:** all public calls set `skipAuthRedirect` + `silent`, so a wrong share password renders as an inline error instead of hijacking the session into a logout/redirect loop.
- **Expired links are hard-blocked (410)** before any content or password check, and revoked links 404.
- **Internal validation endpoint** remains token-oracle-guarded (authenticated callers → 403).

## 5. Validation

- `share-service` — `mvn -q -DskipTests compile` ✅ (the `contextLoads` test failure is the same pre-existing MySQL/migration dependency as earlier phases).
- Frontend — `tsc -b` ✅, `npm run build` ✅, `eslint --max-warnings=0` ✅.
- `qa-e2e.sh` — `bash -n` ✅; new scenarios: public preview (with/without password), VIEW-link preview-ok/download-403, expired link 410, `clearExpiry`+`clearPassword` revival, revoke → 404, and the empty-trash flow.

## 6. Future enhancements

- Public **folder share pages** (list + bulk ZIP through the link).
- Per-share access **timeline** (who/when viewed & downloaded) on the analytics panel.
- Expiry **notifications** to owners before links lapse.
