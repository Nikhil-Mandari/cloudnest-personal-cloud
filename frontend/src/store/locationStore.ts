import { create } from 'zustand';

export type LocationStatus =
  | 'idle'
  | 'requesting'
  | 'ready'
  | 'denied'
  | 'unavailable'
  | 'timeout'
  | 'unsupported';

interface LocationState {
  status: LocationStatus;
  latitude: number | null;
  longitude: number | null;
  /** Estimated radius of accuracy in metres (when the browser reports it). */
  accuracy?: number;
  /** Best-effort human-readable area name from reverse geocoding. */
  areaName?: string;
  error?: string;
  /** Epoch ms of the last successful fix. */
  updatedAt?: number;
  /** True once an attempt has been made this session — never nags the user. */
  asked: boolean;

  capture: () => void;
  clear: () => void;
}

/**
 * Free reverse-geocoding endpoint (BigDataCloud client API). No API key is
 * required for low-volume client-side use, so nothing secret ever reaches the
 * browser. Best-effort only: on any failure the UI falls back to coordinates.
 */
const REVERSE_GEOCODE_URL = 'https://api.bigdatacloud.net/data/reverse-geocode-client';

async function reverseGeocode(latitude: number, longitude: number): Promise<string | undefined> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 8_000);
  try {
    const url = new URL(REVERSE_GEOCODE_URL);
    url.searchParams.set('latitude', String(latitude));
    url.searchParams.set('longitude', String(longitude));
    url.searchParams.set('localityLanguage', 'en');
    const response = await fetch(url, { signal: controller.signal });
    if (!response.ok) {
      return undefined;
    }
    const data = (await response.json()) as {
      city?: string;
      locality?: string;
      principalSubdivision?: string;
      countryName?: string;
    };
    const parts = [data.city ?? data.locality, data.principalSubdivision, data.countryName].filter(
      Boolean,
    );
    return parts.join(', ') || undefined;
  } catch {
    return undefined;
  } finally {
    window.clearTimeout(timer);
  }
}

/**
 * Browser-geolocation store.
 *
 * Location is captured automatically once per session after a successful
 * sign-in (see DashboardLayout). The browser permission prompt is never
 * bypassed, coordinates are never faked, and a denied permission is surfaced
 * without re-asking. The result stays client-side — CloudNest has no
 * server-side location infrastructure, and none is introduced here.
 */
export const useLocationStore = create<LocationState>((set) => ({
  status: 'idle',
  latitude: null,
  longitude: null,
  accuracy: undefined,
  areaName: undefined,
  error: undefined,
  updatedAt: undefined,
  asked: false,

  capture: () => {
    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      set({
        status: 'unsupported',
        error: 'Your browser does not support geolocation.',
        asked: true,
      });
      return;
    }

    set({ status: 'requesting', error: undefined, asked: true });

    navigator.geolocation.getCurrentPosition(
      async (result) => {
        const latitude = result.coords.latitude;
        const longitude = result.coords.longitude;
        set({
          status: 'ready',
          latitude,
          longitude,
          accuracy: result.coords.accuracy,
          updatedAt: Date.now(),
        });
        // Reverse geocoding is best-effort and never blocks the coordinate fix.
        const areaName = await reverseGeocode(latitude, longitude);
        set({ areaName });
      },
      (error) => {
        switch (error.code) {
          case error.PERMISSION_DENIED:
            set({
              status: 'denied',
              error: 'Location permission was denied. You can allow access in your browser settings.',
            });
            break;
          case error.POSITION_UNAVAILABLE:
            set({ status: 'unavailable', error: 'Your location is currently unavailable.' });
            break;
          case error.TIMEOUT:
            set({ status: 'timeout', error: 'The location request timed out. Please try again.' });
            break;
          default:
            set({ status: 'unavailable', error: 'Could not determine your location.' });
        }
      },
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 0 },
    );
  },

  clear: () =>
    set({
      status: 'idle',
      latitude: null,
      longitude: null,
      accuracy: undefined,
      areaName: undefined,
      error: undefined,
      updatedAt: undefined,
      asked: false,
    }),
}));
