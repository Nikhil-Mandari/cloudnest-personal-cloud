/**
 * Global application constants.
 */

export const APP_NAME = 'CloudNest';
export const APP_TAGLINE = 'Your Personal Cloud. Your Files. Anywhere.';
export const APP_VERSION = '0.1.0';

/** Base URL of the CloudNest API Gateway (all services are routed through it). */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

/** Default request timeout for the shared Axios client. */
export const REQUEST_TIMEOUT_MS = 30_000;
