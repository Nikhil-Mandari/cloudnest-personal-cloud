/**
 * API endpoint map, aligned with the CloudNest API contract (docs/05_API_CONTRACT.md).
 * Paths are relative to the gateway base URL (API_BASE_URL).
 */
export const API_ENDPOINTS = {
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    me: '/auth/me',
  },
  users: {
    profile: '/users/profile',
    changePassword: '/users/change-password',
  },
  folders: {
    list: '/folders',
    create: '/folders',
    rename: (id: string) => `/folders/${id}`,
    remove: (id: string) => `/folders/${id}`,
  },
  files: {
    list: '/files',
    favorites: '/files/favorites',
    upload: '/files/upload',
    search: '/files/search',
    detail: (id: number) => `/files/${id}`,
    download: (id: number) => `/files/${id}/download`,
    preview: (id: number) => `/files/${id}/preview`,
    rename: (id: number) => `/files/${id}`,
    move: (id: number) => `/files/${id}/move`,
    favorite: (id: number) => `/files/${id}/favorite`,
    remove: (id: number) => `/files/${id}`,
    restore: (id: number) => `/files/${id}/restore`,
  },
  share: {
    myShares: '/shares/my-shares',
    sharedWithMe: '/shares/shared-with-me',
    create: (fileId: number) => `/shares/file/${fileId}`,
    remove: (id: number) => `/shares/${id}`,
    public: (token: string) => `/shares/public/${token}`,
  },
  notifications: {
    list: '/notifications',
    markAsRead: (id: string) => `/notifications/${id}`,
  },
} as const;
