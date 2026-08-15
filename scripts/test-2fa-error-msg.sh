#!/usr/bin/env bash
set -euo pipefail

B=http://localhost:8080/api
H='Content-Type: application/json'
D='X-Device-Id: phase6-test-1'

echo '=== 1) login (2FA currently disabled) ==='
curl -s -X POST "$B/auth/login" -H "$H" -H "$D" \
  -d '{"usernameOrEmail":"p6user@test.local","password":"CloudNest@123"}' > /tmp/H1.json
TOK=$(grep -oE '"token":"[^"]+' /tmp/H1.json | sed 's/"token":"//')
echo "token len: ${#TOK}"
echo "$TOK" > /tmp/tok.txt

echo
echo '=== 2) 2FA setup ==='
curl -s -X POST "$B/auth/2fa/setup" \
  -H "Authorization: Bearer $TOK" -H "$H" -H "$D" > /tmp/H2.json
SECRET=$(grep -oE '"secret":"[^"]+' /tmp/H2.json | sed 's/"secret":"//')
echo "secret: ${SECRET:0:8}..."
echo "$SECRET" > /tmp/secret.txt

echo
echo '=== 3) enable 2FA (field: code) ==='
CODE=$(node /tmp/totp.mjs "$SECRET")
curl -s -X POST "$B/auth/2fa/enable" \
  -H "Authorization: Bearer $TOK" -H "$H" -H "$D" \
  -d "{\"code\":\"$CODE\"}" | head -c 200

echo
echo
echo '=== 4) 2FA status ==='
curl -s "$B/auth/2fa/status" \
  -H "Authorization: Bearer $TOK" -H "$D" | grep -oE '"enabled":[a-z]+'

echo
echo '=== 5) login from NEW untrusted device ==='
D3='X-Device-Id: phase6-test-3'
curl -s -X POST "$B/auth/login" -H "$H" -H "$D3" \
  -d '{"usernameOrEmail":"p6user@test.local","password":"CloudNest@123"}' > /tmp/H3.json
grep -oE '"requires2fa":[a-z]+' /tmp/H3.json
CT=$(grep -oE '"challengeToken":"[^"]+' /tmp/H3.json | sed 's/"challengeToken":"//')
echo "challenge: ${CT:0:24}..."

echo
echo '=== 6) WRONG 2FA code (expect 401 + specific message, not generic password msg) ==='
curl -s -w "\nHTTP %{http_code}\n" -X POST "$B/auth/login/2fa" \
  -H "$H" -H "$D3" \
  -d "{\"challengeToken\":\"$CT\",\"code\":\"000000\"}"

echo
echo '=== 7) CORRECT TOTP (expect 200 + token) ==='
CODE2=$(node /tmp/totp.mjs "$SECRET")
curl -s -X POST "$B/auth/login/2fa" \
  -H "$H" -H "$D3" \
  -d "{\"challengeToken\":\"$CT\",\"code\":\"$CODE2\"}" > /tmp/H4.json
grep -oE '"token":"[^"]+' /tmp/H4.json | head -c 40
echo

echo
echo '=== 8) BACKUP-CODE 2FA login ==='
# regenerate backup codes first
curl -s -X POST "$B/auth/2fa/backup-codes/regenerate" \
  -H "Authorization: Bearer $TOK" -H "$H" -H "$D" > /tmp/H5.json
BC=$(grep -oE '"backupCodes":\["[^"]+' /tmp/H5.json | sed 's/"backupCodes":\["//' | tr -d '",]' | head -1)
echo "backup code: ${BC:0:6}..."
curl -s -X POST "$B/auth/login" -H "$H" -H "$D3" \
  -d '{"usernameOrEmail":"p6user@test.local","password":"CloudNest@123"}' > /tmp/H6.json
CT2=$(grep -oE '"challengeToken":"[^"]+' /tmp/H6.json | sed 's/"challengeToken":"//')
curl -s -o /dev/null -w 'backup-code 2FA login -> HTTP %{http_code}\n' \
  -X POST "$B/auth/login/2fa" \
  -H "$H" -H "$D3" \
  -d "{\"challengeToken\":\"$CT2\",\"code\":\"$BC\"}"

echo
echo '=== 9) REUSE same backup code (expect 401 single-use) ==='
curl -s -X POST "$B/auth/login" -H "$H" -H "$D3" \
  -d '{"usernameOrEmail":"p6user@test.local","password":"CloudNest@123"}' > /tmp/H7.json
CT3=$(grep -oE '"challengeToken":"[^"]+' /tmp/H7.json | sed 's/"challengeToken":"//')
curl -s -w "\nHTTP %{http_code}\n" -X POST "$B/auth/login/2fa" \
  -H "$H" -H "$D3" \
  -d "{\"challengeToken\":\"$CT3\",\"code\":\"$BC\"}"

echo
echo '=== 10) WRONG PASSWORD (expect 401 + generic msg unchanged) ==='
curl -s -w "\nHTTP %{http_code}\n" -X POST "$B/auth/login" \
  -H "$H" -H "$D3" \
  -d '{"usernameOrEmail":"p6user@test.local","password":"wrongpass123"}'

echo
echo '=== DONE ==='