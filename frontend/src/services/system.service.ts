import { API_ORIGIN } from '@/constants/app';

/**
 * Result of a gateway health probe.
 *
 * The gateway exposes Spring Boot Actuator at `{origin}/actuator/health` (it
 * is CORS-enabled and public). When the Eureka client is healthy the payload
 * includes the list of registered microservices, which is the closest
 * cross-service liveness signal the frontend can read without hitting every
 * service directly.
 */
export interface SystemHealth {
  /** Overall gateway status (`UP` / `DOWN` / `unknown`). */
  status: 'UP' | 'DOWN' | 'unknown';
  /** Eureka-registered microservices (e.g. `file-service`). */
  services: string[];
  /** ISO timestamp of the probe. */
  checkedAt: string;
}

const PROBE_TIMEOUT_MS = 4_000;

/**
 * Tolerantly pulls the list of discovered services out of the actuator JSON,
 * which can shape services as an array of strings or as objects with a
 * `serviceId` field across Spring Cloud versions.
 */
function extractServices(payload: unknown): string[] {
  try {
    const discovery = (payload as {
      components?: {
        discoveryComposite?: {
          details?: {
            discoveryClient?: {
              details?: { services?: unknown };
            };
          };
        };
      };
    }).components?.discoveryComposite?.details?.discoveryClient?.details?.services;

    if (Array.isArray(discovery)) {
      return discovery
        .map((entry) =>
          typeof entry === 'string'
            ? entry
            : (entry as { serviceId?: string }).serviceId ?? '',
        )
        .filter(Boolean)
        .sort();
    }
  } catch {
    // Fall through to `unknown`.
  }
  return [];
}

/** Probes the gateway actuator health endpoint with a short timeout. */
export async function fetchGatewayHealth(): Promise<SystemHealth> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), PROBE_TIMEOUT_MS);
  try {
    const response = await fetch(`${API_ORIGIN}/actuator/health`, {
      signal: controller.signal,
      cache: 'no-store',
    });
    if (!response.ok) {
      return { status: 'DOWN', services: [], checkedAt: new Date().toISOString() };
    }
    const payload = (await response.json()) as { status?: string };
    return {
      status: payload.status === 'UP' ? 'UP' : 'DOWN',
      services: extractServices(payload),
      checkedAt: new Date().toISOString(),
    };
  } catch {
    return { status: 'unknown', services: [], checkedAt: new Date().toISOString() };
  } finally {
    window.clearTimeout(timer);
  }
}
