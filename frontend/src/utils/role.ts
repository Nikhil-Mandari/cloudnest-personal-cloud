/**
 * Role helpers.
 *
 * The backend stores roles with the Spring Security `ROLE_` prefix
 * (`ROLE_USER` / `ROLE_ADMIN`), while the frontend type is `'USER' | 'ADMIN'`.
 * These helpers accept both so a raw API response can never break the check.
 */

/** True when a role string denotes an administrator. */
export function isAdminRole(role: string | null | undefined): boolean {
  if (!role) {
    return false;
  }
  return role.toUpperCase().replace(/^ROLE_/, '') === 'ADMIN';
}

/** True when a role string denotes a regular user. */
export function isUserRole(role: string | null | undefined): boolean {
  if (!role) {
    return true;
  }
  return role.toUpperCase().replace(/^ROLE_/, '') === 'USER';
}

/** Normalises a backend role to the frontend `'USER' | 'ADMIN'` shape. */
export function normalizeRole(role: string | null | undefined): 'USER' | 'ADMIN' {
  return isAdminRole(role) ? 'ADMIN' : 'USER';
}
