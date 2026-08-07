/**
 * Lightweight `clsx`-style class name combiner (zero dependencies).
 */
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}
