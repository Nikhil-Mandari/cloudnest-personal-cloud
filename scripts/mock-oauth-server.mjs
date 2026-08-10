/**
 * Mock OAuth2 provider for runtime-testing CloudNest social login.
 *
 * Implements the three endpoints the auth-service calls during the
 * authorization-code flow, so the whole Google/GitHub path can be exercised
 * locally without real provider credentials:
 *
 *   GET  /authorize?client_id&redirect_uri&state&scope&response_type=code
 *        -> 302 to redirect_uri?code=<code>&state=<state>
 *   POST /token  (form-encoded client_id/client_secret/code/grant_type/redirect_uri)
 *        -> { "access_token": "...", "token_type": "bearer" }
 *   GET  /userinfo (Authorization: Bearer <token>)
 *        -> { "sub": "...", "name": "...", "email": "...", "email_verified": true }
 *   GET  /user/emails (Authorization: Bearer <token>)  [GitHub fallback]
 *        -> [{ "email": "...", "primary": true, "verified": true }]
 *
 * Usage: node scripts/mock-oauth-server.mjs [port] [email] [name]
 */
import http from 'node:http';
import { URL } from 'node:url';

const PORT = Number(process.argv[2] || 9999);
const EMAIL = process.argv[3] || `oauth.mock.${Date.now()}@example.com`;
const NAME = process.argv[4] || 'OAuth Mock User';

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const log = (msg) => console.log(`[mock-oauth] ${req.method} ${url.pathname} ${msg}`);

  // ── /authorize — bounce the browser back with a code ─────────────────────
  if (req.method === 'GET' && url.pathname === '/authorize') {
    const redirectUri = url.searchParams.get('redirect_uri');
    const state = url.searchParams.get('state');
    if (!redirectUri) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'missing redirect_uri' }));
      return;
    }
    const code = `mock-code-${Date.now()}`;
    const target = new URL(redirectUri);
    target.searchParams.set('code', code);
    target.searchParams.set('state', state || '');
    log(`redirecting to callback with code=${code}`);
    res.writeHead(302, { Location: target.toString() });
    res.end();
    return;
  }

  // ── /token — exchange the code for an access token ───────────────────────
  if (req.method === 'POST' && url.pathname === '/token') {
    let body = '';
    req.on('data', (chunk) => (body += chunk));
    req.on('end', () => {
      log(`code exchanged (body: ${body.replace(/\n/g, ' ')})`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(
        JSON.stringify({
          access_token: `mock-access-${Date.now()}`,
          token_type: 'bearer',
          scope: 'email profile',
        }),
      );
    });
    return;
  }

  // ── /userinfo — resolve the authenticated user ───────────────────────────
  if (req.method === 'GET' && url.pathname === '/userinfo') {
    const auth = req.headers.authorization || '';
    log(`userinfo (auth: ${auth.slice(0, 24)}...)`);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(
      JSON.stringify({
        sub: 'mock-provider-user-1',
        name: NAME,
        email: EMAIL,
        email_verified: true,
        picture: 'https://example.com/avatar.png',
      }),
    );
    return;
  }

  // ── /user/emails — GitHub-style email fallback ───────────────────────────
  if (req.method === 'GET' && url.pathname === '/user/emails') {
    log('user/emails');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify([{ email: EMAIL, primary: true, verified: true, visibility: 'public' }]));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'not found' }));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[mock-oauth] listening on :${PORT}`);
  console.log(`[mock-oauth] will return email=${EMAIL} name=${NAME}`);
});
