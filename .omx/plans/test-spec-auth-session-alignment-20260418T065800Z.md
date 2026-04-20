# Test Spec: Auth Session Alignment and Contract Repair

- PRD: `.omx/plans/prd-auth-session-alignment-20260418T065800Z.md`
- Date: 2026-04-18
- Scope: web session sync, shared auth/user contract repair, web 401/logout consistency, mini program compatibility smoke

## Test Matrix

| Area | Scenario | Expected result | Evidence |
| --- | --- | --- | --- |
| Web login | Login from homepage modal, then perform a shared-client authenticated call without reload | Request includes bearer token and succeeds without manual reload | Browser network trace + manual note |
| Web register | Register, auto-login, then redirect to profile completion | User lands on profile completion flow with valid authenticated state | Browser trace + UI capture |
| Web logout | Logout from header while token exists | Storage cleared, shared client token cleared, backend logout called, next protected action redirects to login flow | Network trace + backend `action=logout` log |
| Local-request 401 | Force a 401 through `easyorange-frontend/src/api/core/request.ts` | Session clears once and user returns to home/login flow without redirect loop | Browser trace + QA note |
| Shared-adapter 401 | Force a 401 through `easyorange-shared/src/adapters/web-adapter.ts` | Same outcome as local-request 401 | Browser trace + QA note |
| Route contract | Shared auth/user module routes | Match `/api/auth/...` and `/api/users/...` controllers present in backend | File diff / code review |
| Mini program startup | Launch app with persisted token | Client initializes with token and remains logged in | DevTools/manual note |
| Mini program login | WeChat login path | Token persists and app navigates to home | DevTools/manual note |

## Verification Commands

1. `cd easyorange-shared && npm run typecheck && npm run build`
2. `cd easyorange-frontend && npm run typecheck && npm run build`
3. `cd easyorange-miniprogram && npm run build`

## Manual Smoke Checklist

- [ ] Protected web route redirects to `/?redirect=...` when logged out.
- [ ] Web login returns to redirect target.
- [ ] Web login enables an immediate authenticated shared request.
- [ ] Web logout revokes session locally and server-side.
- [ ] Injected 401 from both request stacks converges on the same logout behavior.
- [ ] First registration still routes to profile completion.
- [ ] Mini program login still succeeds.

## Known Test Gaps

- No dedicated automated frontend unit-test runner is currently declared in `easyorange-frontend/package.json`.
- No dedicated shared-package unit-test runner is currently declared in `easyorange-shared/package.json`.
- Silent-refresh behavior is intentionally not covered because it is deferred by the approved plan.
