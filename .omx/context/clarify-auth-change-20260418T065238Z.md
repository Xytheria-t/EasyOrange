# Context Snapshot: clarify-auth-change

- Task statement: Clarify "the auth change" before any planning or implementation.
- Desired outcome: A requirements-ready understanding of what auth behavior should change, why, and where scope stops.
- Stated solution: None yet beyond "clarify the auth change".
- Probable intent hypothesis: The user wants to avoid implementing the wrong authentication-related change in a brownfield repo with multiple auth flows.

## Known facts / evidence
- This is a brownfield repo with auth logic in both the web frontend and the miniprogram.
- `easyorange-frontend/src/pages/home/auth.ts` owns the web login/register modal, stores `token` and `user`, auto-logins after registration, and dispatches `user-login` after success.
- `easyorange-frontend/src/main.ts` initializes `WebAdapter` with a `handleUnauthorized()` callback that clears `token` and `user` and redirects to home with a `redirect` query.
- `easyorange-shared/src/adapters/web-adapter.ts` attaches a bearer token and triggers the unauthorized callback on HTTP 401.
- `easyorange-shared/src/api/modules/auth.ts` and related APIs indicate multiple auth paths: standard login, logout, campus login, miniprogram login, and campus callback flow.
- `easyorange-miniprogram/miniprogram/pages/login/index.ts` handles WeChat login and token storage for the mini-program.

## Constraints
- Deep-interview mode only: clarify requirements; do not implement.
- Must reduce ambiguity before handing off to planning/execution.
- Need explicit non-goals and decision boundaries before crystallizing.

## Unknowns / open questions
- Which auth flow is changing: web login/register, unauthorized/session handling, campus auth, miniprogram auth, or shared token plumbing?
- Is the desired change behavioral, UX, security, API-contract, or architecture related?
- What problem or user pain is motivating the auth change?
- What should remain explicitly out of scope?
- What can OMX decide without further confirmation?

## Decision-boundary unknowns
- Whether OMX may change only frontend behavior or also shared/miniprogram code.
- Whether backend/API contract changes are allowed or must be treated as fixed.
- Whether auth UX copy/layout changes are allowed or only logic changes.

## Likely codebase touchpoints
- `easyorange-frontend/src/pages/home/auth.ts`
- `easyorange-frontend/src/main.ts`
- `easyorange-shared/src/adapters/web-adapter.ts`
- `easyorange-shared/src/api/modules/auth.ts`
- `easyorange-miniprogram/miniprogram/pages/login/index.ts`
