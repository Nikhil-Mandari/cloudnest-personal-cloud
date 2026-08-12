import { useCallback, useState } from 'react';
import { Copy, Loader2, LocateFixed, MapPin, X } from 'lucide-react';
import { toast } from 'react-toastify';

import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { cn } from '@/utils/cn';

/** A single geolocation fix (WGS84 coordinates). */
interface Coordinates {
  latitude: number;
  longitude: number;
  /** Estimated radius of accuracy in metres (when the browser reports it). */
  accuracy?: number;
}

type LocationStatus = 'idle' | 'requesting' | 'ready' | 'error' | 'unsupported';

const formatCoordinate = (value: number, positive: string, negative: string): string =>
  `${Math.abs(value).toFixed(6)}° ${value >= 0 ? positive : negative}`;

/**
 * Browser-geolocation card (Profile page).
 *
 * Location is ONLY captured when the user explicitly clicks "Detect my
 * location" — the browser permission prompt is never bypassed and nothing is
 * read silently. The result stays in the browser (UI-only): CloudNest has no
 * server-side location infrastructure, and none is introduced here.
 */
export function LocationCard() {
  const [status, setStatus] = useState<LocationStatus>('idle');
  const [position, setPosition] = useState<Coordinates | null>(null);
  const [message, setMessage] = useState<string | undefined>();

  const detect = useCallback(() => {
    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      setStatus('unsupported');
      setMessage('Your browser does not support geolocation.');
      return;
    }

    setStatus('requesting');
    setPosition(null);
    setMessage(undefined);

    navigator.geolocation.getCurrentPosition(
      (result) => {
        setPosition({
          latitude: result.coords.latitude,
          longitude: result.coords.longitude,
          accuracy: result.coords.accuracy,
        });
        setStatus('ready');
      },
      (error) => {
        setStatus('error');
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setMessage(
              'Permission denied. Allow location access for this site in your browser to use this feature.',
            );
            break;
          case error.POSITION_UNAVAILABLE:
            setMessage('Your location is currently unavailable. Please try again in a moment.');
            break;
          case error.TIMEOUT:
            setMessage('The location request timed out. Please try again.');
            break;
          default:
            setMessage('Could not determine your location.');
        }
      },
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 0 },
    );
  }, []);

  const clear = useCallback(() => {
    setStatus('idle');
    setPosition(null);
    setMessage(undefined);
  }, []);

  const copyCoordinates = useCallback(async () => {
    if (!position) {
      return;
    }
    const text = `${position.latitude.toFixed(6)}, ${position.longitude.toFixed(6)}`;
    try {
      await navigator.clipboard.writeText(text);
      toast.success('Coordinates copied to clipboard');
    } catch {
      toast.error('Could not copy the coordinates');
    }
  }, [position]);

  const mapsUrl = position
    ? `https://www.google.com/maps?q=${position.latitude},${position.longitude}`
    : null;

  return (
    <Card>
      <CardHeader
        title="Location"
        description="Detect your current coordinates — shared only with your browser."
        action={<MapPin className="h-5 w-5 text-gray-400" />}
      />
      <CardBody>
        {status === 'idle' && (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Your browser will ask for permission before your position is read.
          </p>
        )}

        {status === 'unsupported' && (
          <p className="text-sm text-amber-600 dark:text-amber-400">{message}</p>
        )}

        {status === 'error' && (
          <div className="space-y-3">
            <p className="text-sm text-rose-600 dark:text-rose-400">{message}</p>
            <Button variant="outline" size="sm" onClick={detect} leftIcon={<LocateFixed className="h-4 w-4" />}>
              Try again
            </Button>
          </div>
        )}

        {status === 'requesting' && (
          <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
            <Loader2 className="h-4 w-4 animate-spin" />
            Waiting for your location…
          </div>
        )}

        {status === 'ready' && position && (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-2">
              <div className="rounded-lg bg-gray-50 px-3 py-2.5 dark:bg-gray-900">
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Latitude</p>
                <p className="mt-0.5 font-mono text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                  {formatCoordinate(position.latitude, 'N', 'S')}
                </p>
              </div>
              <div className="rounded-lg bg-gray-50 px-3 py-2.5 dark:bg-gray-900">
                <p className="text-[10px] tracking-wide text-gray-400 uppercase">Longitude</p>
                <p className="mt-0.5 font-mono text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                  {formatCoordinate(position.longitude, 'E', 'W')}
                </p>
              </div>
            </div>

            {typeof position.accuracy === 'number' && (
              <p className="text-xs text-gray-400 dark:text-gray-500">
                Accurate to about {Math.round(position.accuracy)} m
              </p>
            )}

            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={() => void copyCoordinates()} leftIcon={<Copy className="h-4 w-4" />}>
                Copy coordinates
              </Button>
              {mapsUrl && (
                <a
                  href={mapsUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-brand-600 hover:text-brand-700 dark:text-brand-400 inline-flex items-center gap-1.5 text-sm font-medium transition-colors hover:underline"
                >
                  <MapPin className="h-4 w-4" />
                  Open in maps
                </a>
              )}
            </div>

            <button
              type="button"
              onClick={clear}
              className={cn(
                'flex items-center gap-1.5 text-xs font-medium text-gray-400 transition-colors',
                'hover:text-gray-600 dark:hover:text-gray-200',
              )}
            >
              <X className="h-3.5 w-3.5" />
              Clear location
            </button>
          </div>
        )}

        {status === 'idle' && (
          <Button
            variant="primary"
            size="sm"
            className="mt-3"
            onClick={detect}
            leftIcon={<LocateFixed className="h-4 w-4" />}
          >
            Detect my location
          </Button>
        )}
      </CardBody>
    </Card>
  );
}
