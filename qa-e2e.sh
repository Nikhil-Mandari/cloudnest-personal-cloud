#!/usr/bin/env bash
# CloudNest live E2E test suite — runs against the API Gateway (localhost:8080).
set +e
BASE="http://localhost:8080/api"
TS="$(date +%s)"
EMAIL_A="qa_a_${TS}@cloudnest.test"
EMAIL_B="qa_b_${TS}@cloudnest.test"
USER_A="qa_a_${TS}"
USER_B="qa_b_${TS}"
PASS='Qa@123456'

PASS_N=0; FAIL_N=0
ok()   { PASS_N=$((PASS_N+1)); echo "  PASS  $1"; }
bad()  { FAIL_N=$((FAIL_N+1)); echo "  FAIL  $1"; }
check() { # $1 label, $2 expected http code, $3 actual code
  if [ "$2" = "$3" ]; then ok "$1 (HTTP $3)"; else bad "$1 (expected $2, got $3)"; fi
}

# NOTE: this suite assumes dev mode (MAIL_ENABLED=false) so OTP codes are
# returned in the response as `devOtp`. With real SMTP you would read the
# code from the mailbox instead.
DEV_A="e2e-device-a-$TS"
DEV_B="e2e-device-b-$TS"

echo "== AUTH =="
# 1. Register user A -> 201 (account created PENDING_VERIFICATION, OTP emailed)
CODE=$(curl -s -o /tmp/qa_regA.json -w '%{http_code}' -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d "{\"username\":\"$USER_A\",\"email\":\"$EMAIL_A\",\"password\":\"$PASS\"}")
check "Register user A" 201 "$CODE"
OTP_A=$(sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p' /tmp/qa_regA.json)
[ -n "$OTP_A" ] && ok "Registration returns dev OTP" || bad "Registration returns dev OTP"

# 2. Duplicate registration -> 409
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d "{\"username\":\"$USER_A\",\"email\":\"$EMAIL_A\",\"password\":\"$PASS\"}")
check "Duplicate registration rejected" 409 "$CODE"

# 3. Register user B -> 201
CODE=$(curl -s -o /tmp/qa_regB.json -w '%{http_code}' -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d "{\"username\":\"$USER_B\",\"email\":\"$EMAIL_B\",\"password\":\"$PASS\"}")
check "Register user B" 201 "$CODE"
OTP_B=$(sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p' /tmp/qa_regB.json)

# 4. Login before email verification -> 403
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_A\",\"password\":\"$PASS\"}")
check "Login before email verification -> 403" 403 "$CODE"

# 5. Verify registration A -> activates account + auto sign-in (token pair)
CODE=$(curl -s -o /tmp/qa_verifyA.json -w '%{http_code}' -X POST "$BASE/auth/register/verify" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_A" -d "{\"email\":\"$EMAIL_A\",\"code\":\"$OTP_A\"}")
check "Verify registration A (auto sign-in)" 200 "$CODE"
TOKEN_A=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' /tmp/qa_verifyA.json)
REFRESH_A=$(sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p' /tmp/qa_verifyA.json)
AUTH_A="Authorization: Bearer $TOKEN_A"
[ -n "$TOKEN_A" ] && ok "Auto sign-in returns JWT + refresh token" || bad "Auto sign-in returns JWT + refresh token"

# 6. Login A -> password ok, device unknown -> OTP challenge
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_A" -d "{\"usernameOrEmail\":\"$EMAIL_A\",\"password\":\"$PASS\"}")
echo "$LOGIN" | grep -q '"requiresOtp":true' && ok "Login requires OTP for new device" || bad "Login requires OTP for new device"
CHALLENGE_A=$(echo "$LOGIN" | sed -n 's/.*"challengeToken":"\([^"]*\)".*/\1/p')
LOGIN_OTP_A=$(echo "$LOGIN" | sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p')

# 7. Verify login OTP (rememberDevice=true -> device becomes trusted)
CODE=$(curl -s -o /tmp/qa_loginA.json -w '%{http_code}' -X POST "$BASE/auth/login/verify?rememberDevice=true" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_A" -d "{\"challengeToken\":\"$CHALLENGE_A\",\"code\":\"$LOGIN_OTP_A\"}")
check "Complete OTP login A" 200 "$CODE"
TOKEN_A=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' /tmp/qa_loginA.json)
AUTH_A="Authorization: Bearer $TOKEN_A"

# 8. Trusted device: next login skips OTP
LOGIN2=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_A" -d "{\"usernameOrEmail\":\"$EMAIL_A\",\"password\":\"$PASS\"}")
echo "$LOGIN2" | grep -q '"requiresOtp":false' && ok "Trusted device skips OTP" || bad "Trusted device skips OTP"

# 9. Verify registration B + auto sign-in
CODE=$(curl -s -o /tmp/qa_verifyB.json -w '%{http_code}' -X POST "$BASE/auth/register/verify" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_B" -d "{\"email\":\"$EMAIL_B\",\"code\":\"$OTP_B\"}")
check "Verify registration B" 200 "$CODE"
TOKEN_B=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' /tmp/qa_verifyB.json)
AUTH_B="Authorization: Bearer $TOKEN_B"
[ -n "$TOKEN_B" ] && ok "Login B returns JWT" || bad "Login B returns JWT"

# 10. Refresh-token rotation: old refresh token is revoked after use
CODE=$(curl -s -o /tmp/qa_refresh.json -w '%{http_code}' -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH_A\"}")
check "Refresh rotates token pair" 200 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH_A\"}")
check "Reused refresh token -> 401" 401 "$CODE"

# 11. Invalid login -> 401
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_A\",\"password\":\"wrongpass\"}")
check "Invalid password -> 401" 401 "$CODE"

# 12. Protected route without token -> 401
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/files")
check "No token on /files -> 401" 401 "$CODE"

# 13. Invalid token -> 401
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/files" -H "Authorization: Bearer garbage.token.here")
check "Garbage token -> 401" 401 "$CODE"

echo "== USER =="
# 9. Get profile
CODE=$(curl -s -o /tmp/qa_me.json -w '%{http_code}' "$BASE/users/me" -H "$AUTH_A")
check "GET /users/me" 200 "$CODE"
grep -q '"username"' /tmp/qa_me.json && ok "Profile contains username" || bad "Profile contains username"

# 10. Update profile
CODE=$(curl -s -o /tmp/qa_upd.json -w '%{http_code}' -X PUT "$BASE/users/me" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"displayName\":\"QA Tester One\",\"bio\":\"integration test\"}")
check "PUT /users/me" 200 "$CODE"
grep -q 'QA Tester One' /tmp/qa_upd.json && ok "Profile updated (displayName)" || bad "Profile updated (displayName)"

echo "== FOLDERS =="
# 11. Create folder -> 201
FOLDER=$(curl -s -X POST "$BASE/folders" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"name":"QA Folder"}')
FOLDER_ID=$(echo "$FOLDER" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
echo "$FOLDER" | grep -q '"name":"QA Folder"' && ok "Create folder (id=$FOLDER_ID)" || bad "Create folder"
[ -z "$FOLDER_ID" ] && FOLDER_ID="00000000-0000-0000-0000-000000000000"

# 12. Nested folder -> 201
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/folders" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"name\":\"Nested\",\"parentFolderId\":\"$FOLDER_ID\"}")
check "Create nested folder" 201 "$CODE"

# 13. List folders
CODE=$(curl -s -o /tmp/qa_folders.json -w '%{http_code}' "$BASE/folders" -H "$AUTH_A")
check "GET /folders" 200 "$CODE"
grep -q 'QA Folder' /tmp/qa_folders.json && ok "Folders list contains folder" || bad "Folders list contains folder"

# 14. Rename folder
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/folders/$FOLDER_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"name":"QA Renamed"}')
check "Rename folder" 200 "$CODE"

# 15. Move folder (to root, null destination)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/folders/$FOLDER_ID/move" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"destinationFolderId":null}')
check "Move folder to root" 200 "$CODE"

# 16. Get folder children (root-level) via GET /folders/root
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/folders/root" -H "$AUTH_A")
check "GET /folders/root" 200 "$CODE"

echo "== FILES =="
# 17. Upload -> 201
printf 'hello cloudnest %s\n' "$TS" > /tmp/qa.txt
CODE=$(curl -s -o /tmp/qa_up.json -w '%{http_code}' -X POST "$BASE/files/upload" -F "file=@/tmp/qa.txt" -H "$AUTH_A")
check "Upload file" 201 "$CODE"
FILE_ID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' /tmp/qa_up.json | head -1)
[ -n "$FILE_ID" ] && ok "Upload returns file id ($FILE_ID)" || bad "Upload returns file id"
[ -z "$FILE_ID" ] && FILE_ID=1

# 18. List files
CODE=$(curl -s -o /tmp/qa_files.json -w '%{http_code}' "$BASE/files" -H "$AUTH_A")
check "GET /files" 200 "$CODE"
grep -q 'qa.txt' /tmp/qa_files.json && ok "Files list contains upload" || bad "Files list contains upload"

# 19. Search
CODE=$(curl -s -o /tmp/qa_search.json -w '%{http_code}' "$BASE/files/search?query=qa" -H "$AUTH_A")
check "GET /files/search" 200 "$CODE"
grep -q 'qa.txt' /tmp/qa_search.json && ok "Search finds file" || bad "Search finds file"

# 20. Download (blob)
CODE=$(curl -s -o /tmp/qa_dl.txt -w '%{http_code}' "$BASE/files/$FILE_ID/download" -H "$AUTH_A")
check "Download file" 200 "$CODE"
grep -q 'hello cloudnest' /tmp/qa_dl.txt && ok "Downloaded content matches" || bad "Downloaded content matches"

# 21. Preview (txt -> 200 inline)
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/files/$FILE_ID/preview" -H "$AUTH_A")
check "Preview file (txt)" 200 "$CODE"

# 22. Rename file
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/files/$FILE_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"originalFileName":"renamed.txt"}')
check "Rename file" 200 "$CODE"

# 23. Move file (to folder)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/files/$FILE_ID/move?folderId=$FOLDER_ID" -H "$AUTH_A")
check "Move file into folder" 200 "$CODE"

# 24. Favorite
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/files/$FILE_ID/favorite?favorite=true" -H "$AUTH_A")
check "Set favorite" 200 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/files/favorites" -H "$AUTH_A")
check "List favorites" 200 "$CODE"

echo "== SHARE + NOTIFICATIONS (Feign) =="
# 25. Share file with user B -> 201
CODE=$(curl -s -o /tmp/qa_share.json -w '%{http_code}' -X POST "$BASE/shares/file/$FILE_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"sharedWithEmail\":\"$EMAIL_B\",\"permission\":\"VIEW\"}")
check "Share file with user B" 201 "$CODE"
SHARE_ID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' /tmp/qa_share.json | head -1)
[ -n "$SHARE_ID" ] && ok "Share returns id ($SHARE_ID)" || bad "Share returns id"

# 26. A: my-shares
CODE=$(curl -s -o /tmp/qa_myshares.json -w '%{http_code}' "$BASE/shares/my-shares" -H "$AUTH_A")
check "A: my-shares" 200 "$CODE"
grep -q 'resourceName' /tmp/qa_myshares.json && ok "Share enriched with resourceName" || bad "Share enriched with resourceName"

# 27. B: shared-with-me
CODE=$(curl -s -o /tmp/qa_swm.json -w '%{http_code}' "$BASE/shares/shared-with-me" -H "$AUTH_B")
check "B: shared-with-me" 200 "$CODE"
grep -q 'renamed.txt\|qa.txt' /tmp/qa_swm.json && ok "B sees shared file" || bad "B sees shared file"

# 28. B: notifications (created by share-service via Feign)
CODE=$(curl -s -o /tmp/qa_notif.json -w '%{http_code}' "$BASE/notifications" -H "$AUTH_B")
check "B: GET /notifications" 200 "$CODE"
grep -qi 'share' /tmp/qa_notif.json && ok "B received share notification" || bad "B received share notification"

# 29. B: unread-count
CODE=$(curl -s -o /tmp/qa_unc.json -w '%{http_code}' "$BASE/notifications/unread-count" -H "$AUTH_B")
check "B: unread-count" 200 "$CODE"
grep -q '"count"' /tmp/qa_unc.json && ok "Unread count shape ok" || bad "Unread count shape ok"

# 30. B: mark all as read
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/notifications/read-all" -H "$AUTH_B")
check "B: mark all as read" 200 "$CODE"

# 31. Update share permission
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/shares/$SHARE_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"permission":"EDIT"}')
check "Update share permission" 200 "$CODE"

echo "== FILE MGMT (Phase 2) =="
# 31a. Duplicate upload with ASK -> reports duplicate, uploads nothing
CODE=$(curl -s -o /tmp/qa_dup.json -w '%{http_code}' -X POST "$BASE/files/upload?onDuplicate=ASK" -F "file=@/tmp/qa.txt" -H "$AUTH_A")
check "Duplicate upload (ASK) -> 201" 201 "$CODE"
grep -q '"duplicate":true' /tmp/qa_dup.json && grep -q '"actionTaken":"ASK"' /tmp/qa_dup.json && ok "ASK reports duplicate + actionTaken=ASK" || bad "ASK reports duplicate + actionTaken=ASK"

# 31b. KEEP_BOTH -> second copy created
CODE=$(curl -s -o /tmp/qa_kb.json -w '%{http_code}' -X POST "$BASE/files/upload?onDuplicate=KEEP_BOTH" -F "file=@/tmp/qa.txt" -H "$AUTH_A")
check "Duplicate upload (KEEP_BOTH) -> 201" 201 "$CODE"
grep -q '"actionTaken":"KEEP_BOTH"' /tmp/qa_kb.json && ok "KEEP_BOTH creates second copy" || bad "KEEP_BOTH creates second copy"

# 31c. SKIP -> uploads nothing
CODE=$(curl -s -o /tmp/qa_skip.json -w '%{http_code}' -X POST "$BASE/files/upload?onDuplicate=SKIP" -F "file=@/tmp/qa.txt" -H "$AUTH_A")
check "Duplicate upload (SKIP) -> 201" 201 "$CODE"
grep -q '"actionTaken":"SKIP"' /tmp/qa_skip.json && ok "SKIP uploads nothing" || bad "SKIP uploads nothing"

# 31d. REPLACE -> archives previous content as a version, updates the file
CODE=$(curl -s -o /tmp/qa_rep.json -w '%{http_code}' -X POST "$BASE/files/upload?onDuplicate=REPLACE" -F "file=@/tmp/qa.txt" -H "$AUTH_A")
check "Duplicate upload (REPLACE) -> 201" 201 "$CODE"
grep -q '"actionTaken":"REPLACE"' /tmp/qa_rep.json && ok "REPLACE replaces content" || bad "REPLACE replaces content"

# 31e. Version history lists the archived snapshot
CODE=$(curl -s -o /tmp/qa_vers.json -w '%{http_code}' "$BASE/files/$FILE_ID/versions" -H "$AUTH_A")
check "GET /files/{id}/versions" 200 "$CODE"
grep -q '"versionNumber"' /tmp/qa_vers.json && ok "Version list non-empty" || bad "Version list non-empty"
VERSION_ID=$(grep -o '"id":[0-9]*' /tmp/qa_vers.json | head -1 | cut -d: -f2)
[ -n "$VERSION_ID" ] && ok "Version id captured ($VERSION_ID)" || bad "Version id captured"

# 31f. Upload a new version
printf 'version two %s\n' "$TS" > /tmp/qa_v2.txt
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/files/$FILE_ID/versions" -F "file=@/tmp/qa_v2.txt" -H "$AUTH_A")
check "Upload new version" 200 "$CODE"

# 31g. Restore the first version
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/files/$FILE_ID/versions/$VERSION_ID/restore" -H "$AUTH_A")
check "Restore version" 200 "$CODE"

# 31h. Bulk ZIP download
CODE=$(curl -s -o /tmp/qa.zip -w '%{http_code}' -X POST "$BASE/files/download-zip" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"fileIds\":[$FILE_ID]}")
check "Bulk ZIP download" 200 "$CODE"
if [ -s /tmp/qa.zip ] && file /tmp/qa.zip 2>/dev/null | grep -q 'Zip'; then ok "ZIP archive valid"; else bad "ZIP archive valid"; fi

# 31i. Storage analytics overview
CODE=$(curl -s -o /tmp/qa_stats.json -w '%{http_code}' "$BASE/files/stats/overview" -H "$AUTH_A")
check "Storage analytics overview" 200 "$CODE"
grep -q '"storageUsed"' /tmp/qa_stats.json && ok "Analytics has storageUsed" || bad "Analytics has storageUsed"

# 31j. Audit logs (paged)
CODE=$(curl -s -o /tmp/qa_audit.json -w '%{http_code}' "$BASE/files/audit-logs?size=50" -H "$AUTH_A")
check "Audit logs" 200 "$CODE"
grep -q '"totalElements"' /tmp/qa_audit.json && ok "Audit logs paged shape" || bad "Audit logs paged shape"

# 31k. Virus-scan status
CODE=$(curl -s -o /tmp/qa_scan.json -w '%{http_code}' "$BASE/files/$FILE_ID/scan-status" -H "$AUTH_A")
check "Scan status" 200 "$CODE"
grep -q '"scanStatus"' /tmp/qa_scan.json && ok "Scan status present" || bad "Scan status present"

# 31l. Share with password + DOWNLOAD permission
CODE=$(curl -s -o /tmp/qa_share2.json -w '%{http_code}' -X POST "$BASE/shares/file/$FILE_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"sharedWithEmail\":\"$EMAIL_B\",\"permission\":\"DOWNLOAD\",\"password\":\"secret123\"}")
check "Share with password + DOWNLOAD" 201 "$CODE"
SHARE2_TOKEN=$(sed -n 's/.*"shareToken":"\([^"]*\)".*/\1/p' /tmp/qa_share2.json | head -1)
grep -q '"hasPassword":true' /tmp/qa_share2.json && ok "hasPassword=true returned" || bad "hasPassword=true returned"

# 31m. Verify password on the protected share (correct + wrong)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/shares/public/$SHARE2_TOKEN/verify-password" -H 'Content-Type: application/json' -d '{"password":"secret123"}')
check "Verify share password (correct) -> 200" 200 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/shares/public/$SHARE2_TOKEN/verify-password" -H 'Content-Type: application/json' -d '{"password":"wrong"}')
check "Verify share password (wrong) -> 401" 401 "$CODE"

# 31n. Share analytics (owner only)
CODE=$(curl -s -o /tmp/qa_san.json -w '%{http_code}' "$BASE/shares/$SHARE_ID/analytics" -H "$AUTH_A")
check "Share analytics (owner) -> 200" 200 "$CODE"
grep -q '"viewCount"' /tmp/qa_san.json && ok "Analytics has viewCount" || bad "Analytics has viewCount"

# 31o. Public share view (no auth) exposes hasPassword
CODE=$(curl -s -o /tmp/qa_pshare.json -w '%{http_code}' "$BASE/shares/public/$SHARE2_TOKEN")
check "Public share view (no auth) -> 200" 200 "$CODE"
grep -q '"hasPassword":true' /tmp/qa_pshare.json && ok "Public view exposes hasPassword" || bad "Public view exposes hasPassword"

# 31p. Public download without password -> 401; with password -> 200 + content
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/public/$SHARE2_TOKEN/download")
check "Download protected share w/o password -> 401" 401 "$CODE"
CODE=$(curl -s -o /tmp/qa_shared_dl.txt -w '%{http_code}' "$BASE/shares/public/$SHARE2_TOKEN/download?password=secret123")
check "Download protected share w/ password -> 200" 200 "$CODE"
grep -q 'hello cloudnest' /tmp/qa_shared_dl.txt && ok "Shared download content matches" || bad "Shared download content matches"

# 31q. Internal validate endpoint rejects authenticated callers (token oracle guard)
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/internal/validate?token=$SHARE2_TOKEN" -H "$AUTH_B")
check "Internal validate rejects authenticated callers -> 403" 403 "$CODE"

echo "== SHARE LINKS (Phase 3) =="
# 31r. Public preview streams the file WITHOUT counting as a download
CODE=$(curl -s -o /tmp/qa_preview.txt -w '%{http_code}' "$BASE/shares/public/$SHARE2_TOKEN/preview?password=secret123")
check "Public preview (w/ password) -> 200" 200 "$CODE"
grep -q 'hello cloudnest' /tmp/qa_preview.txt && ok "Preview content matches" || bad "Preview content matches"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/public/$SHARE2_TOKEN/preview")
check "Public preview w/o password -> 401" 401 "$CODE"

# 31s. VIEW-permission links: preview ok, download blocked (403)
CODE=$(curl -s -o /tmp/qa_view_preview.txt -w '%{http_code}' "$BASE/shares/public/$SHARE_ID/preview")
check "VIEW link preview -> 200" 200 "$CODE"
grep -q 'hello cloudnest' /tmp/qa_view_preview.txt && ok "VIEW preview content matches" || bad "VIEW preview content matches"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/public/$SHARE_ID/download")
check "VIEW link download -> 403" 403 "$CODE"

# 31t. Already-expired link -> 410 on public view
CODE=$(curl -s -o /tmp/qa_exp_share.json -w '%{http_code}' -X POST "$BASE/shares/file/$FILE_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"sharedWithEmail\":\"$EMAIL_B\",\"permission\":\"DOWNLOAD\",\"expiryDate\":\"2020-01-01T00:00:00\"}")
check "Create already-expired share -> 201" 201 "$CODE"
EXP_SHARE_ID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' /tmp/qa_exp_share.json | head -1)
EXP_TOKEN=$(sed -n 's/.*"shareToken":"\([^"]*\)".*/\1/p' /tmp/qa_exp_share.json | head -1)
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/public/$EXP_TOKEN")
check "Public view of expired link -> 410" 410 "$CODE"

# 31u. Owner clears expiry + password via update -> link works again
CODE=$(curl -s -o /tmp/qa_cleared.json -w '%{http_code}' -X PUT "$BASE/shares/$EXP_SHARE_ID" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"permission":"DOWNLOAD","clearExpiry":true,"clearPassword":true}')
check "Update share (clearExpiry + clearPassword) -> 200" 200 "$CODE"
grep -q '"hasPassword":false' /tmp/qa_cleared.json && ok "Password cleared on update" || bad "Password cleared on update"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/public/$EXP_TOKEN")
check "Previously-expired link works after clearExpiry -> 200" 200 "$CODE"

# 31v. Revoking a share makes the link 404
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/shares/$EXP_SHARE_ID" -H "$AUTH_A")
check "Revoke share -> 200" 200 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/shares/public/$EXP_TOKEN")
check "Revoked link -> 404" 404 "$CODE"

echo "== TRASH =="
# 32. Soft delete file -> trash
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/files/$FILE_ID" -H "$AUTH_A")
check "Soft-delete file" 200 "$CODE"
CODE=$(curl -s -o /tmp/qa_trash.json -w '%{http_code}' "$BASE/files/trash" -H "$AUTH_A")
check "GET /files/trash" 200 "$CODE"
grep -q 'renamed.txt' /tmp/qa_trash.json && ok "File appears in trash" || bad "File appears in trash"

# 33. Restore file
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/files/$FILE_ID/restore" -H "$AUTH_A")
check "Restore file" 200 "$CODE"

# 34. Soft-delete folder -> folder trash
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/folders/$FOLDER_ID" -H "$AUTH_A")
check "Soft-delete folder" 200 "$CODE"
CODE=$(curl -s -o /tmp/qa_ftrash.json -w '%{http_code}' "$BASE/folders/trash" -H "$AUTH_A")
check "GET /folders/trash" 200 "$CODE"
grep -q 'QA Renamed' /tmp/qa_ftrash.json && ok "Folder appears in trash" || bad "Folder appears in trash"

# 34a. Empty-trash flow: soft-delete again, empty file trash, verify gone
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/files/$FILE_ID" -H "$AUTH_A")
check "Soft-delete file (for empty-trash test)" 200 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/files/trash" -H "$AUTH_A")
check "Empty file trash -> 200" 200 "$CODE"
CODE=$(curl -s -o /tmp/qa_etrash.json -w '%{http_code}' "$BASE/files/trash" -H "$AUTH_A")
check "File trash list after empty-trash" 200 "$CODE"
if ! grep -q 'renamed.txt' /tmp/qa_etrash.json; then ok "Trash no longer contains the file"; else bad "Trash no longer contains the file"; fi
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/folders/trash" -H "$AUTH_A")
check "Empty folder trash -> 200" 200 "$CODE"

echo "== SECURITY =="
# 35. Change password (revokes refresh tokens, keeps access token valid)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/auth/change-password" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"currentPassword\":\"$PASS\",\"newPassword\":\"Qa@654321\"}")
check "Change password" 200 "$CODE"

# 36. Login with NEW password -> 200 (device already trusted -> no OTP)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_A" -d "{\"usernameOrEmail\":\"$EMAIL_A\",\"password\":\"Qa@654321\"}")
check "Login with new password (trusted device)" 200 "$CODE"

# 37. Active sessions
CODE=$(curl -s -o /tmp/qa_sessions.json -w '%{http_code}' "$BASE/auth/sessions" -H "$AUTH_A")
check "GET /auth/sessions" 200 "$CODE"
grep -q '"current":true' /tmp/qa_sessions.json && ok "Current session flagged" || bad "Current session flagged"

# 38. Security overview
CODE=$(curl -s -o /tmp/qa_secov.json -w '%{http_code}' "$BASE/auth/security-overview" -H "$AUTH_A")
check "GET /auth/security-overview" 200 "$CODE"
grep -q '"securityScore"' /tmp/qa_secov.json && ok "Security score present" || bad "Security score present"

# 39. Login history + security logs
CODE=$(curl -s -o /tmp/qa_hist.json -w '%{http_code}' "$BASE/auth/login-history" -H "$AUTH_A")
check "GET /auth/login-history" 200 "$CODE"
grep -q '"content"' /tmp/qa_hist.json && ok "Login history shape ok" || bad "Login history shape ok"
CODE=$(curl -s -o /tmp/qa_seclogs.json -w '%{http_code}' "$BASE/auth/security-logs" -H "$AUTH_A")
check "GET /auth/security-logs" 200 "$CODE"

# 40. Ownership: user B cannot read user A's file (404/403)
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/files/$FILE_ID" -H "$AUTH_B")
if [ "$CODE" = "403" ] || [ "$CODE" = "404" ]; then
  ok "B denied on A's file (HTTP $CODE)"
else
  bad "B denied on A's file (expected 403/404, got $CODE)"
fi

echo "== ACCOUNT LOCK =="
# 41. Register + verify a throwaway user C for the brute-force test
EMAIL_C="qa_c_${TS}@cloudnest.test"
USER_C="qa_c_${TS}"
CODE=$(curl -s -o /tmp/qa_regC.json -w '%{http_code}' -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d "{\"username\":\"$USER_C\",\"email\":\"$EMAIL_C\",\"password\":\"$PASS\"}")
check "Register user C" 201 "$CODE"
OTP_C=$(sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p' /tmp/qa_regC.json)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/register/verify" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL_C\",\"code\":\"$OTP_C\"}")
check "Verify user C" 200 "$CODE"

# 42. Wrong password attempts 1-4 -> 401
for i in 1 2 3 4; do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_C\",\"password\":\"wrongpass\"}")
  check "Wrong password attempt $i -> 401" 401 "$CODE"
done

# 43. 5th failure locks the account -> 423
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_C\",\"password\":\"wrongpass\"}")
check "5th failure locks account -> 423" 423 "$CODE"

# 44. Even the correct password is blocked while locked -> 423
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_C\",\"password\":\"$PASS\"}")
check "Correct password blocked while locked -> 423" 423 "$CODE"

echo "== ADMIN (Phase 4) =="
# 45. Sign in as the bootstrap admin (admin@cloudnest.test / Admin@123456).
#     New device -> OTP challenge -> verify with rememberDevice to trust it.
ADMIN_DEV="e2e-admin-device-$TS"
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -H "X-Device-Id: $ADMIN_DEV" -d '{"usernameOrEmail":"admin@cloudnest.test","password":"Admin@123456"}')
echo "$LOGIN" | grep -q '"requiresOtp":true' && ok "Admin login requires OTP" || bad "Admin login requires OTP"
ADMIN_CHALLENGE=$(echo "$LOGIN" | sed -n 's/.*"challengeToken":"\([^"]*\)".*/\1/p')
ADMIN_OTP=$(echo "$LOGIN" | sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p')
CODE=$(curl -s -o /tmp/qa_admin.json -w '%{http_code}' -X POST "$BASE/auth/login/verify?rememberDevice=true" -H 'Content-Type: application/json' -H "X-Device-Id: $ADMIN_DEV" -d "{\"challengeToken\":\"$ADMIN_CHALLENGE\",\"code\":\"$ADMIN_OTP\"}")
check "Admin OTP verify -> 200" 200 "$CODE"
TOKEN_ADMIN=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' /tmp/qa_admin.json)
AUTH_ADMIN="Authorization: Bearer $TOKEN_ADMIN"
[ -n "$TOKEN_ADMIN" ] && ok "Admin JWT captured" || bad "Admin JWT captured"

# 46. Admin profile synced to user-service by the bootstrap runner
CODE=$(curl -s -o /tmp/qa_admin_me.json -w '%{http_code}' "$BASE/users/me" -H "$AUTH_ADMIN")
check "Admin GET /users/me -> 200 (profile provisioned)" 200 "$CODE"
grep -q '"role":"ROLE_ADMIN"' /tmp/qa_admin_me.json && ok "Admin profile has ROLE_ADMIN" || bad "Admin profile has ROLE_ADMIN"

# 47. Admin users summary + list
CODE=$(curl -s -o /tmp/qa_admin_users_summary.json -w '%{http_code}' "$BASE/users/admin/summary" -H "$AUTH_ADMIN")
check "Admin user summary -> 200" 200 "$CODE"
grep -q '"totalUsers"' /tmp/qa_admin_users_summary.json && ok "User summary has totalUsers" || bad "User summary has totalUsers"
CODE=$(curl -s -o /tmp/qa_admin_users.json -w '%{http_code}' "$BASE/users/admin?page=0&size=20" -H "$AUTH_ADMIN")
check "Admin user list -> 200" 200 "$CODE"
grep -q '"content"' /tmp/qa_admin_users.json && ok "User list paged shape" || bad "User list paged shape"

# 48. Admin storage overview + audit + MinIO status
CODE=$(curl -s -o /tmp/qa_admin_storage.json -w '%{http_code}' "$BASE/files/admin/storage-overview" -H "$AUTH_ADMIN")
check "Admin storage overview -> 200" 200 "$CODE"
grep -q '"totalBytes"' /tmp/qa_admin_storage.json && ok "Admin storage has totalBytes" || bad "Admin storage has totalBytes"
CODE=$(curl -s -o /tmp/qa_admin_audit.json -w '%{http_code}' "$BASE/files/admin/audit-logs?size=50" -H "$AUTH_ADMIN")
check "Admin audit logs -> 200" 200 "$CODE"
grep -q '"ownerId"' /tmp/qa_admin_audit.json && ok "Admin audit entries carry ownerId" || bad "Admin audit entries carry ownerId"
CODE=$(curl -s -o /tmp/qa_admin_minio.json -w '%{http_code}' "$BASE/files/admin/minio-status" -H "$AUTH_ADMIN")
check "Admin MinIO status -> 200" 200 "$CODE"
grep -q '"reachable"' /tmp/qa_admin_minio.json && ok "MinIO status has reachable" || bad "MinIO status has reachable"

# 49. Admin security views + system health
CODE=$(curl -s -o /tmp/qa_admin_sec.json -w '%{http_code}' "$BASE/auth/admin/security-overview" -H "$AUTH_ADMIN")
check "Admin security overview -> 200" 200 "$CODE"
grep -q '"totalAccounts"' /tmp/qa_admin_sec.json && ok "Security overview has totalAccounts" || bad "Security overview has totalAccounts"
CODE=$(curl -s -o /tmp/qa_admin_lh.json -w '%{http_code}' "$BASE/auth/admin/login-history?size=20" -H "$AUTH_ADMIN")
check "Admin login history -> 200" 200 "$CODE"
CODE=$(curl -s -o /tmp/qa_admin_sl.json -w '%{http_code}' "$BASE/auth/admin/security-logs?size=20" -H "$AUTH_ADMIN")
check "Admin security logs -> 200" 200 "$CODE"
CODE=$(curl -s -o /tmp/qa_admin_health.json -w '%{http_code}' "$BASE/admin/system/health" -H "$AUTH_ADMIN")
check "Admin system health -> 200" 200 "$CODE"
grep -q '"healthyCount"' /tmp/qa_admin_health.json && ok "System health has healthyCount" || bad "System health has healthyCount"

# 50. Role guard: user A (ROLE_USER) is denied every admin view -> 403
for ENDPOINT in "users/admin/summary" "files/admin/storage-overview" "files/admin/minio-status" "auth/admin/security-overview" "admin/system/health"; do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/$ENDPOINT" -H "$AUTH_A")
  check "User A denied on $ENDPOINT -> 403" 403 "$CODE"
done

# 51. Header-spoofing: user A fakes X-User-Role: ROLE_ADMIN -> still 403
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/users/admin/summary" -H "$AUTH_A" -H 'X-User-Role: ROLE_ADMIN')
check "Spoofed X-User-Role ignored -> 403" 403 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/admin/system/health" -H "$AUTH_A" -H 'X-User-Role: ROLE_ADMIN')
check "Spoofed role on system health ignored -> 403" 403 "$CODE"

# 52. User A cannot provision profiles (POST /api/users is admin-only)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/users" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"username\":\"sneaky\",\"email\":\"sneaky@cloudnest.test\"}")
check "User A cannot provision profiles -> 403" 403 "$CODE"

# 53. Admin disables user B -> B login blocked; re-enable -> login proceeds
USER_B_ID=$(grep -o '"id":[0-9]*' /tmp/qa_admin_users.json | head -1 | cut -d: -f2)
[ -n "$USER_B_ID" ] || USER_B_ID=2
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/auth/admin/users/$USER_B_ID/enabled?enabled=false" -H "$AUTH_ADMIN")
check "Admin disables user B -> 200" 200 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_B\",\"password\":\"$PASS\"}")
check "Disabled user B login blocked -> 403" 403 "$CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/auth/admin/users/$USER_B_ID/enabled?enabled=true" -H "$AUTH_ADMIN")
check "Admin re-enables user B -> 200" 200 "$CODE"

# 54. Admin cannot demote themselves
ADMIN_ID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' /tmp/qa_admin_me.json | head -1)
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/auth/admin/users/$ADMIN_ID/role?role=ROLE_USER" -H "$AUTH_ADMIN")
check "Admin cannot change own role -> 400" 400 "$CODE"

# 55. Admin promotes user B to admin, verifies, demotes back
CODE=$(curl -s -o /tmp/qa_role.json -w '%{http_code}' -X PATCH "$BASE/auth/admin/users/$USER_B_ID/role?role=ROLE_ADMIN" -H "$AUTH_ADMIN")
check "Admin promotes user B -> 200" 200 "$CODE"
grep -q '"role":"ROLE_ADMIN"' /tmp/qa_role.json && ok "Promotion reflected in response" || bad "Promotion reflected in response"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/auth/admin/users/$USER_B_ID/role?role=ROLE_USER" -H "$AUTH_ADMIN")
check "Admin demotes user B back -> 200" 200 "$CODE"

echo "== SECURITY NOTIFICATIONS (Phase 5) =="
# 56. A's notification list carries the in-app security events
CODE=$(curl -s -o /tmp/qa_n5a.json -w '%{http_code}' "$BASE/notifications" -H "$AUTH_A")
check "A: GET /notifications" 200 "$CODE"
grep -q '"type":"PASSWORD_CHANGED"' /tmp/qa_n5a.json && ok "A has password-changed notification" || bad "A has password-changed notification"
grep -q '"type":"LOGIN_ALERT"' /tmp/qa_n5a.json && ok "A has new sign-in notification" || bad "A has new sign-in notification"

# 57. Unread-count still served (bell badge)
CODE=$(curl -s -o /tmp/qa_n5uc.json -w '%{http_code}' "$BASE/notifications/unread-count" -H "$AUTH_A")
check "A: unread-count" 200 "$CODE"
grep -q '"count"' /tmp/qa_n5uc.json && ok "Unread count shape ok" || bad "Unread count shape ok"

# 58. Unknown-device detection: B signs in from a brand-new device
DEV_B3="e2e-device-b3-$TS"
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_B3" -d "{\"usernameOrEmail\":\"$EMAIL_B\",\"password\":\"$PASS\"}")
echo "$LOGIN" | grep -q '"requiresOtp":true' && ok "B login from new device requires OTP" || bad "B login from new device requires OTP"
B3_CHALLENGE=$(echo "$LOGIN" | sed -n 's/.*"challengeToken":"\([^"]*\)".*/\1/p')
B3_OTP=$(echo "$LOGIN" | sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p')
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login/verify" -H 'Content-Type: application/json' -H "X-Device-Id: $DEV_B3" -d "{\"challengeToken\":\"$B3_CHALLENGE\",\"code\":\"$B3_OTP\"}")
check "B OTP verify on new device" 200 "$CODE"

# 59. B now has UNKNOWN_DEVICE_LOGIN notifications: one from registration
#     activation (first sign-in) + one from the brand-new device above
CODE=$(curl -s -o /tmp/qa_n5b.json -w '%{http_code}' "$BASE/notifications" -H "$AUTH_B")
check "B: GET /notifications" 200 "$CODE"
B_UD_COUNT=$(grep -o '"type":"UNKNOWN_DEVICE_LOGIN"' /tmp/qa_n5b.json | wc -l)
[ "$B_UD_COUNT" -ge 2 ] && ok "B has >=2 unknown-device notifications (got $B_UD_COUNT)" || bad "B has >=2 unknown-device notifications (got $B_UD_COUNT)"

# 60. Locked-account notification: user D registered, verified, then locked
EMAIL_D="qa_d_${TS}@cloudnest.test"
USER_D="qa_d_${TS}"
CODE=$(curl -s -o /tmp/qa_regD.json -w '%{http_code}' -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d "{\"username\":\"$USER_D\",\"email\":\"$EMAIL_D\",\"password\":\"$PASS\"}")
check "Register user D" 201 "$CODE"
OTP_D=$(sed -n 's/.*"devOtp":"\([^"]*\)".*/\1/p' /tmp/qa_regD.json)
CODE=$(curl -s -o /tmp/qa_verifyD.json -w '%{http_code}' -X POST "$BASE/auth/register/verify" -H 'Content-Type: application/json' -H "X-Device-Id: e2e-device-d-$TS" -d "{\"email\":\"$EMAIL_D\",\"code\":\"$OTP_D\"}")
check "Verify user D (auto sign-in)" 200 "$CODE"
TOKEN_D=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' /tmp/qa_verifyD.json)
AUTH_D="Authorization: Bearer $TOKEN_D"
[ -n "$TOKEN_D" ] && ok "D JWT captured" || bad "D JWT captured"
for i in 1 2 3 4 5; do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"usernameOrEmail\":\"$EMAIL_D\",\"password\":\"wrongpass\"}")
done
check "D locked after 5 failures -> 423" 423 "$CODE"
CODE=$(curl -s -o /tmp/qa_n5d.json -w '%{http_code}' "$BASE/notifications" -H "$AUTH_D")
check "D: GET /notifications (while locked)" 200 "$CODE"
grep -q '"type":"ACCOUNT_LOCKED"' /tmp/qa_n5d.json && ok "D has account-locked notification" || bad "D has account-locked notification"

# 61. Clear-read flow: mark all read, then DELETE read-all, list empty
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/notifications/read-all" -H "$AUTH_A")
check "A: mark all as read" 200 "$CODE"
CODE=$(curl -s -o /tmp/qa_n5clear.json -w '%{http_code}' -X DELETE "$BASE/notifications/read-all" -H "$AUTH_A")
check "A: clear read notifications -> 200" 200 "$CODE"
grep -q '"success":true' /tmp/qa_n5clear.json && ok "Clear-read response ok" || bad "Clear-read response ok"
CODE=$(curl -s -o /tmp/qa_n5after.json -w '%{http_code}' "$BASE/notifications" -H "$AUTH_A")
check "A: notifications after clear" 200 "$CODE"
if ! grep -q '"id":' /tmp/qa_n5after.json; then ok "A: notification list empty after clear"; else bad "A: notification list empty after clear"; fi

echo
echo "== TWO-FACTOR AUTHENTICATION (Phase 6) =="

# Minimal RFC 6238 TOTP generator (python3) — same algorithm the authenticator apps use.
totp() { # $1 base32 secret -> prints the current 6-digit code
  python3 - "$1" <<'PYEOF'
import sys, base64, hashlib, hmac, struct, time
secret = sys.argv[1]
key = base64.b32decode(secret + "=" * ((8 - len(secret) % 8) % 8))
counter = int(time.time()) // 30
digest = hmac.new(key, struct.pack(">Q", counter), hashlib.sha1).digest()
o = digest[-1] & 0x0F
code = (struct.unpack(">I", digest[o:o+4])[0] & 0x7FFFFFFF) % 1000000
print("%06d" % code)
PYEOF
}

# 62. Fresh account has 2FA off
CODE=$(curl -s -o /tmp/qa_2fa_status0.json -w '%{http_code}' "$BASE/auth/2fa/status" -H "$AUTH_A")
check "A: 2FA status (default off)" 200 "$CODE"
grep -q '"enabled":false' /tmp/qa_2fa_status0.json && ok "A: 2FA disabled by default" || bad "A: 2FA disabled by default"

# 63. Setup returns a TOTP secret + otpauth URI (the QR payload)
CODE=$(curl -s -o /tmp/qa_2fa_setup.json -w '%{http_code}' -X POST "$BASE/auth/2fa/setup" -H "$AUTH_A")
check "A: 2FA setup" 200 "$CODE"
SECRET_A=$(sed -n 's/.*"secret":"\([^"]*\)".*/\1/p' /tmp/qa_2fa_setup.json)
[ -n "$SECRET_A" ] && ok "A: TOTP secret captured" || bad "A: TOTP secret captured"
grep -q 'otpauth://totp/' /tmp/qa_2fa_setup.json && ok "A: otpauth URI returned" || bad "A: otpauth URI returned"

# 64. Enabling with a wrong code must NOT succeed
CODE=$(curl -s -o /tmp/qa_2fa_enable_bad.json -w '%{http_code}' -X POST "$BASE/auth/2fa/enable" -H "$AUTH_A" -H 'Content-Type: application/json' -d '{"code":"000000"}')
[ "$CODE" != "200" ] && ok "A: enable rejected with a bad code (HTTP $CODE)" || bad "A: enable rejected with a bad code (HTTP $CODE)"

if command -v python3 >/dev/null 2>&1; then
  # 65. Enable 2FA with a live TOTP code -> backup codes issued once
  TOTP_A=$(totp "$SECRET_A")
  [ -n "$TOTP_A" ] && ok "A: TOTP code generated" || bad "A: TOTP code generated"
  CODE=$(curl -s -o /tmp/qa_2fa_enable.json -w '%{http_code}' -X POST "$BASE/auth/2fa/enable" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"code\":\"$TOTP_A\"}")
  check "A: enable 2FA with a valid code" 200 "$CODE"
  grep -q '"backupCodes":' /tmp/qa_2fa_enable.json && ok "A: backup codes returned once" || bad "A: backup codes returned once"

  # 66. Status now reflects enabled + 10 unused backup codes
  CODE=$(curl -s -o /tmp/qa_2fa_status1.json -w '%{http_code}' "$BASE/auth/2fa/status" -H "$AUTH_A")
  check "A: 2FA status (on)" 200 "$CODE"
  grep -q '"enabled":true' /tmp/qa_2fa_status1.json && ok "A: status shows enabled" || bad "A: status shows enabled"
  grep -q '"backupCodesRemaining":10' /tmp/qa_2fa_status1.json && ok "A: 10 backup codes remaining" || bad "A: 10 backup codes remaining"

  # 67. Next sign-in now requires the 2FA step (before any OTP)
  LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -H "X-Device-Id: e2e-device-2fa-$TS" -d "{\"usernameOrEmail\":\"$EMAIL_A\",\"password\":\"$PASS\"}")
  echo "$LOGIN" | grep -q '"requires2fa":true' && ok "A: login now requires 2FA" || bad "A: login now requires 2FA"
  CHAL_2FA=$(echo "$LOGIN" | sed -n 's/.*"challengeToken":"\([^"]*\)".*/\1/p')
  [ -n "$CHAL_2FA" ] && ok "A: 2FA challenge token captured" || bad "A: 2FA challenge token captured"

  # 68. Complete the sign-in with a fresh TOTP code
  TOTP_A2=$(totp "$SECRET_A")
  CODE=$(curl -s -o /tmp/qa_2fa_login.json -w '%{http_code}' -X POST "$BASE/auth/login/2fa" -H 'Content-Type: application/json' -H "X-Device-Id: e2e-device-2fa-$TS" -d "{\"challengeToken\":\"$CHAL_2FA\",\"code\":\"$TOTP_A2\"}")
  check "A: 2FA login completes" 200 "$CODE"
  grep -q '"token":' /tmp/qa_2fa_login.json && ok "A: token pair returned after 2FA" || bad "A: token pair returned after 2FA"

  # 69. A wrong 2FA code is rejected (and never yields a token)
  CODE=$(curl -s -o /tmp/qa_2fa_login_bad.json -w '%{http_code}' -X POST "$BASE/auth/login/2fa" -H 'Content-Type: application/json' -d "{\"challengeToken\":\"$CHAL_2FA\",\"code\":\"000000\"}")
  [ "$CODE" != "200" ] && ok "A: wrong 2FA code rejected (HTTP $CODE)" || bad "A: wrong 2FA code rejected (HTTP $CODE)"
  ! grep -q '"token":' /tmp/qa_2fa_login_bad.json && ok "A: no token on failed 2FA" || bad "A: no token on failed 2FA"

  # 70. In-app notification for the enable event
  CODE=$(curl -s -o /tmp/qa_2fa_notif.json -w '%{http_code}' "$BASE/notifications" -H "$AUTH_A")
  check "A: notifications after enabling 2FA" 200 "$CODE"
  grep -q '"type":"TWO_FACTOR_ENABLED"' /tmp/qa_2fa_notif.json && ok "A: 2FA-enabled notification" || bad "A: 2FA-enabled notification"

  # 71. Security log carries the 2FA events
  CODE=$(curl -s -o /tmp/qa_2fa_logs.json -w '%{http_code}' "$BASE/auth/security-logs" -H "$AUTH_A")
  check "A: security logs" 200 "$CODE"
  grep -q '"action":"2FA_ENABLED"' /tmp/qa_2fa_logs.json && ok "A: security log has 2FA_ENABLED" || bad "A: security log has 2FA_ENABLED"

  # 72. Disable 2FA (password proof) -> status back off + security log entry
  CODE=$(curl -s -o /tmp/qa_2fa_disable.json -w '%{http_code}' -X POST "$BASE/auth/2fa/disable" -H "$AUTH_A" -H 'Content-Type: application/json' -d "{\"verification\":\"$PASS\"}")
  check "A: disable 2FA with password" 200 "$CODE"
  CODE=$(curl -s -o /tmp/qa_2fa_status2.json -w '%{http_code}' "$BASE/auth/2fa/status" -H "$AUTH_A")
  grep -q '"enabled":false' /tmp/qa_2fa_status2.json && ok "A: 2FA disabled again" || bad "A: 2FA disabled again"
  CODE=$(curl -s -o /tmp/qa_2fa_logs2.json -w '%{http_code}' "$BASE/auth/security-logs" -H "$AUTH_A")
  grep -q '"action":"2FA_DISABLED"' /tmp/qa_2fa_logs2.json && ok "A: security log has 2FA_DISABLED" || bad "A: security log has 2FA_DISABLED"
else
  echo "  SKIP  TOTP enable / 2FA-login tests (python3 not available for TOTP generation)"
fi

echo
echo "== PASSKEYS (Phase 6) =="

# 73. Empty passkey list on a fresh account
CODE=$(curl -s -o /tmp/qa_pk_list0.json -w '%{http_code}' "$BASE/auth/passkeys" -H "$AUTH_A")
check "A: passkey list" 200 "$CODE"
grep -q '"data":\[' /tmp/qa_pk_list0.json && ok "A: passkey list is empty" || bad "A: passkey list is empty"

# 74. Registration ceremony start returns discoverable-credential options
CODE=$(curl -s -o /tmp/qa_pk_regstart.json -w '%{http_code}' -X POST "$BASE/auth/passkeys/register/start" -H "$AUTH_A")
check "A: passkey register/start" 200 "$CODE"
grep -q '"optionsJson":' /tmp/qa_pk_regstart.json && ok "A: creation options returned" || bad "A: creation options returned"
grep -q 'residentKey' /tmp/qa_pk_regstart.json && ok "A: options request a resident (discoverable) key" || bad "A: options request a resident (discoverable) key"

# 75. Passkey sign-in ceremony start (public endpoint, discovery-less)
CODE=$(curl -s -o /tmp/qa_pk_authstart.json -w '%{http_code}' -X POST "$BASE/auth/passkeys/authenticate/start" -H 'Content-Type: application/json' -d '{}')
check "A: passkey authenticate/start (public)" 200 "$CODE"
grep -q '"credentialsGetJson":' /tmp/qa_pk_authstart.json && ok "A: assertion options returned" || bad "A: assertion options returned"

# NOTE: register/finish and authenticate/finish need a real WebAuthn
# authenticator (browser / device) and are exercised through the UI.

echo
echo "=========================================="
echo "RESULT: $PASS_N passed, $FAIL_N failed"
echo "=========================================="
