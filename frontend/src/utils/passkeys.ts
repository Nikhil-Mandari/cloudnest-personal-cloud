/**
 * WebAuthn (passkey) helpers shared across the auth flows.
 */

/**
 * Whether the browser supports WebAuthn — i.e. can register and verify
 * passkeys (Face ID, Touch ID, Windows Hello, security keys).
 */
export function isPasskeySupported(): boolean {
  return typeof window !== 'undefined' && 'PublicKeyCredential' in window;
}
