/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of the CloudNest API Gateway, e.g. http://localhost:8080/api */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
