/**
 * Global application constants.
 */

export const APP_NAME = 'CloudNest';
export const APP_TAGLINE = 'Your Personal Cloud. Your Files. Anywhere.';
export const APP_VERSION = '0.1.0';

/** Backend release (single version across the microservices). */
export const BACKEND_VERSION = '0.1.0';

/** Maintainer / organisation displayed on the settings page. */
export const APP_DEVELOPER = 'CloudNest Team';

/** Public repository for the CloudNest project. */
export const APP_GITHUB_URL = 'https://github.com/cloudnest/cloudnest-personal-cloud';

/** Technologies powering CloudNest (shown on the settings "About" card). */
export const APP_TECH_STACK: readonly string[] = [
  'React 19',
  'TypeScript',
  'Tailwind CSS',
  'TanStack Query',
  'Zustand',
  'Framer Motion',
  'Spring Boot 3',
  'Spring Cloud Gateway',
  'Eureka',
  'MySQL',
  'MinIO',
  'Docker',
];

/** Base URL of the CloudNest API Gateway (all services are routed through it). */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

/** Origin (scheme + host + port) of the API Gateway, without the `/api` prefix. */
export const API_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '');

/** Default request timeout for the shared Axios client. */
export const REQUEST_TIMEOUT_MS = 30_000;
