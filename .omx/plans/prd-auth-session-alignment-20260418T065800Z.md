# PRD: Auth Session Alignment and Contract Repair

- Date: 2026-04-18
- Mode: ralplan consensus (deliberate mode auto-enabled: auth/security scope)
- Status: Approved for planning
- Context snapshot: `.omx/context/clarify-auth-change-20260418T065238Z.md`
- Residual risk: Product intent was never clarified in deep-interview; this plan is therefore approved only for the repo-evidenced technical auth hardening scope below, not for a broader auth/product redesign.

## Requirements Summary

Repair the current authentication stack so Web, shared SDK, and mini program do not drift on token state, endpoint contracts, or unauthorized handling.

### Evidence-backed problem statement
- Web login and register persist the token only in storage, but do not update the already-initialized shared `ApiClient`, so shared-authenticated calls can remain unauthenticated until a reload (`easyorange-frontend/src/pages/home/auth.ts:253-262`, `easyorange-frontend/src/pages/home/auth.ts:320-337`, `easyorange-frontend/src/main.ts:22-30`).
- Web logout clears browser storage only; it does not revoke the backend token or clear the in-memory shared client token (`easyorange-frontend/src/components/Header.ts:235-244`, `easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/controller/AuthController.java:37-44`, `easyorange-shared/src/adapters/web-adapter.ts:124-129`).
- The legacy frontend request layer reads the token from storage on every request, while the shared web adapter keeps an in-memory token; this makes mixed local/shared callers inconsistent (`easyorange-frontend/src/api/core/request.ts:221-225`, `easyorange-shared/src/adapters/web-adapter.ts:22-24`, `easyorange-shared/src/adapters/web-adapter.ts:124-129`).
- Shared auth/user endpoint contracts do not match the backend user/auth controllers: shared logout and user-info paths use `/api/user/...`, while the backend exposes `/api/auth/...` and `/api/users/...` (`easyorange-shared/src/api/modules/auth.ts:4-10`, `easyorange-shared/src/api/modules/user.ts:4-10`, `easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/controller/AuthController.java:21-55`, `easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/controller/UserController.java:19-40`).
- The current design intent already calls for unified auth handling across web and mini program, especially 401 handling and token injection (`docs/optimize/web-mini-data-unification-guide.md:8-15`, `docs/optimize/web-mini-data-unification-guide.md:430-459`, `docs/optimize/web-mini-data-unification-guide.md:542-546`).

## Scope

### In scope
1. Web session-state synchronization across storage, shared `ApiClient`, and UI lifecycle.
2. Correct shared auth/user endpoint paths to match the backend controllers.
3. Consistent logout and 401 behavior for the web app while the legacy request layer still exists.
4. Mini program compatibility review for any shared auth contract changes.
5. Verification that the chosen auth behavior matches the repo's documented navigation/login flow (`docs/optimize/frontend-navigation-best-practices.md:38-54`, `docs/optimize/frontend-navigation-best-practices.md:484-516`).

### Out of scope / Non-goals
- No new identity provider, SSO, campus-auth redesign, or WeChat-auth redesign.
- No cookie/session architecture migration away from JWT + Redis-backed token validation (`easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/service/TokenService.java:17-117`).
- No automatic token-refresh rollout in this tranche unless verification proves forced re-login is unacceptable.
- No full frontend API migration to shared in the same change; only auth/session touchpoints needed for consistency.

## Decision Boundaries

OMX may decide without more confirmation:
- exact file/module names for the new web auth-session bridge,
- whether to use a small shared helper or a frontend-local service to synchronize token state,
- whether local web 401 handling is implemented via request-layer hook/interceptor or thin wrapper, provided behavior stays “clear auth + return user to home/login flow”.

OMX must not decide without more confirmation:
- changing login UX/copy/layout,
- changing backend token TTLs or security policy,
- expanding this work into campus/miniprogram product-flow redesign,
- shipping silent refresh as the default behavior.

## RALPLAN-DR Summary

### Principles
1. One auth truth per runtime: storage and in-memory clients must converge immediately after login/logout/401.
2. Contract correctness before feature breadth: endpoint mismatches must be repaired before more shared-auth rollout.
3. Keep the first tranche reversible and low-blast-radius.
4. Preserve the documented login redirect flow instead of inventing a second login surface.
5. Prefer explicit failure handling over hidden retries in the initial auth-hardening pass.

### Decision Drivers
1. Prevent stale or missing bearer tokens during mixed local/shared API usage.
2. Align shared SDK contracts with the backend that exists today.
3. Reduce auth regressions while the web app is still mid-migration to `easyorange-shared`.

### Viable Options

#### Option A — Incremental session bridge + contract repair **(Chosen)**
- **Approach:** Add a web auth-session coordinator, repair shared auth/user endpoints, and make login/logout/401 paths all call the same coordinator while keeping refresh deferred.
- **Pros:** Smallest reversible slice; directly fixes the observed token drift; compatible with existing docs; safer while web still mixes local and shared APIs.
- **Cons:** Users still re-login on expiry; leaves refresh endpoint unused for now; requires touching both compat and shared layers.

#### Option B — Full silent refresh rollout now
- **Approach:** In addition to Option A, wire `/api/auth/refresh` into adapter-level retry/refresh behavior before redirecting on 401.
- **Pros:** Better UX for expired access tokens; leverages existing backend refresh endpoint (`easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/controller/AuthController.java:47-54`).
- **Cons:** Higher complexity around concurrent 401s, replay safety, stale token races, and mixed local/shared callers; harder to verify without an automated test harness.

#### Invalidated alternative — Session/cookie architecture switch
- Rejected because the backend is already built around JWT + Redis token keys and explicit logout/revocation (`easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/service/TokenService.java:20-117`); changing auth architecture is much broader than the evidenced problem.

## Architect Review

### Steelman antithesis
If token expiry is frequent in production, Option A may feel like a half-fix: users will still be kicked back to login even though the backend already exposes `/api/auth/refresh`. In that case, shipping only session alignment could pay down technical debt without materially improving user-perceived auth reliability.

### Real tradeoff tension
- **Simplicity vs continuity:** redirect-on-401 is easier to reason about; silent refresh gives better UX but introduces race conditions and retry complexity.
- **Incremental repair vs migration completeness:** fixing auth bridges the current mixed-stack reality; a full migration to shared would be cleaner long-term but is too broad for a safe auth-only tranche.
- **Central coordinator vs implicit storage reads:** a coordinator adds a new abstraction, but it removes drift between storage-backed and memory-backed auth state.

### Synthesis
Adopt Option A now, but design the coordinator boundary so `refresh-if-eligible` can be inserted later behind a single auth-session API instead of scattering refresh logic across pages and adapters.

## Critic Verdict

**APPROVE with improvements applied**

Required improvements that are incorporated below:
1. Add an explicit rollout gate for legacy `request.ts` 401 behavior so web auth remains consistent before full shared migration.
2. Make logout correctness a first-class acceptance criterion: browser storage, in-memory client token, and backend revocation must all be addressed together.
3. Call out mini program regression checks because shared endpoint repairs can break its build/runtime.

## ADR

- **Decision:** Approve Option A: incremental auth-session bridge + shared contract repair, with silent refresh deferred.
- **Drivers:** token drift exists today; shared contracts do not match backend routes; current repo docs explicitly target unified auth behavior.
- **Alternatives considered:** full silent refresh now; auth architecture rewrite.
- **Why chosen:** It fixes the evidenced defects with lower coordination cost and lower regression risk than a full refresh rollout.
- **Consequences:** Users may still re-login on token expiry; a follow-up decision may still be needed if expiry friction is high.
- **Follow-ups:** Reassess silent refresh only after session sync and contract correctness are verified on web + mini program.

## Implementation Plan

### Step 1 — Introduce a single web auth-session coordinator
**Files:**
- `easyorange-frontend/src/main.ts`
- `easyorange-frontend/src/pages/home/auth.ts`
- `easyorange-frontend/src/components/Header.ts`
- likely new `easyorange-frontend/src/app/authSession.ts`

**Plan:**
- Create one coordinator responsible for `setSession(token, user)`, `clearSession(reason)`, and `handleUnauthorized()`.
- Initialize the shared `ApiClient` through this coordinator so the same module can update or clear the in-memory token after login/logout/401.
- Route login/register success in `AuthManager` through the coordinator instead of writing storage directly.
- Route header logout through the coordinator so UI updates happen after token + backend cleanup are complete.

### Step 2 — Repair shared auth/user endpoint contracts to match the backend
**Files:**
- `easyorange-shared/src/api/modules/auth.ts`
- `easyorange-shared/src/api/modules/user.ts`
- `easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/controller/AuthController.java`
- `easyorange-backend/easyorange-user/src/main/java/com/cartethyia/easyorange/user/controller/UserController.java`

**Plan:**
- Align shared logout to `/api/auth/logout`.
- Align shared user-info/update paths to `/api/users/info`.
- Audit any remaining `/api/user/...` auth/user references and either repair or explicitly deprecate them.
- Treat mini program-only endpoints as a separate compatibility audit item if the backend route is absent.

### Step 3 — Make web 401 behavior consistent across both request stacks
**Files:**
- `easyorange-frontend/src/api/core/request.ts`
- `easyorange-shared/src/adapters/web-adapter.ts`
- `easyorange-frontend/src/app/navigation.ts`
- `docs/optimize/web-mini-data-unification-guide.md`

**Plan:**
- Ensure the local frontend request layer funnels 401s into the same coordinator used by `WebAdapter`.
- Preserve the documented behavior of clearing auth and returning to the home/login flow instead of inventing a separate login page.
- Guard against redirect loops by making the unauthorized handler idempotent per failure burst.

### Step 4 — Preserve redirect and first-login flows after session centralization
**Files:**
- `easyorange-frontend/src/pages/home/auth.ts`
- `easyorange-frontend/src/app/navigation.ts`
- `docs/optimize/frontend-navigation-best-practices.md`

**Plan:**
- Confirm the `redirect` query flow still lands users on the intended protected page after login.
- Preserve the first-registration path to profile completion (`easyorange-frontend/src/pages/home/auth.ts:320-337`).
- Keep “homepage modal login” as the only web login surface.

### Step 5 — Validate shared-consumer compatibility and ship behind evidence
**Files:**
- `easyorange-miniprogram/miniprogram/app.ts`
- `easyorange-miniprogram/miniprogram/pages/login/index.ts`
- `easyorange-shared/src/api/index.ts`
- package build/typecheck scripts in `easyorange-frontend/package.json`, `easyorange-shared/package.json`, `easyorange-miniprogram/package.json`

**Plan:**
- Verify mini program startup/login still compiles and uses the corrected shared auth paths where applicable.
- Run package-level typecheck/build verification before merge.
- Record any unresolved backend/shared contract gaps as a follow-up, not as silent TODOs.

## Acceptance Criteria

1. After web login or auto-login-after-register, authenticated shared-client calls can succeed without a full page reload.
2. Web logout clears browser storage, clears the in-memory shared client token, and calls the backend logout endpoint when a token exists.
3. A 401 from either the local web request layer or `WebAdapter` clears session state exactly once and returns the user to the documented home/login flow.
4. Shared auth/user endpoints used by the web app match the backend controller routes now present in the repo.
5. The existing redirect flow (`/?redirect=...`) and first-login-to-profile flow still work after the refactor.
6. `easyorange-frontend` typecheck/build, `easyorange-shared` typecheck/build, and `easyorange-miniprogram` typecheck all pass.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Mixed request stacks still diverge after partial cleanup | High | Make the coordinator the only place allowed to mutate auth state; route both request stacks into it. |
| Shared endpoint repair breaks mini program auth | High | Audit mini program shared calls before merge; verify `easyorange-miniprogram` typecheck and login smoke path. |
| Logout revokes storage but not server token (or vice versa) | High | Make logout a multi-surface acceptance criterion and manual smoke test case. |
| Silent-refresh remains absent and users re-login too often | Medium | Track as explicit deferred option; only escalate after measuring expiry friction. |
| Unauthorized redirect loops | Medium | Make unauthorized handling idempotent and preserve current home-page modal login behavior. |

## Pre-mortem (Deliberate Mode)

1. **Failure:** Login appears successful, but the first shared API call still gets 401.  
   **Cause:** storage updated but coordinator forgot to update `ApiClient`/adapter token.  
   **Prevention:** acceptance criterion #1 + manual smoke immediately after login.
2. **Failure:** Logout clears the UI but the old token still works on the next request.  
   **Cause:** header path clears storage only; backend revoke or adapter clear is missing.  
   **Prevention:** centralize logout and explicitly verify revoke + memory clear + storage clear.
3. **Failure:** Shared route repairs fix web but break mini program login or build.  
   **Cause:** shared package consumers rely on old endpoint assumptions.  
   **Prevention:** include mini program compatibility review and package-level verification before merge.

## Expanded Test Plan

### Unit / module-level
- Coordinator tests or equivalent targeted checks for `setSession`, `clearSession`, and idempotent unauthorized handling.
- Shared auth/user contract checks for route strings and caller coverage.

### Integration
- Web login -> immediate authenticated shared call without reload.
- Web logout -> backend logout + subsequent protected call forced through unauthenticated path.
- Injected 401 in local request layer and shared adapter both converge on the same logout flow.

### E2E / manual smoke
- Unauthenticated visit to a protected web route redirects to `/?redirect=...`, then login returns to target page.
- First registration still redirects to profile completion.
- Mini program login still stores token and continues to app home.

### Observability
- Temporary console/network tracing for login/logout/401 transitions during QA.
- Backend log confirmation for `action=logout` during web logout verification.

## Verification Steps

1. `cd easyorange-shared && npm run typecheck && npm run build`
2. `cd easyorange-frontend && npm run typecheck && npm run build`
3. `cd easyorange-miniprogram && npm run build`
4. Manual web smoke: login, immediate protected shared call, logout, forced 401, redirect preservation.
5. Manual mini program smoke: login and auth-expiry handling.

## Available-Agent-Types Roster

- `executor` — implementation/refactor work
- `architect` — design/tradeoff review
- `critic` — plan quality and risk review
- `debugger` — auth failure diagnosis if smoke tests fail
- `test-engineer` — verification matrix and regression coverage
- `verifier` — completion evidence and release-readiness check
- `writer` — doc/update pass for auth behavior notes

## Follow-up Staffing Guidance

### If executed via `$ralph`
- Primary lane: `executor` (high reasoning) for coordinator + contract repair.
- Verification lane: `test-engineer` (medium) then `verifier` (high) before closeout.
- Use `architect` only as an escalation lane if the refresh-vs-redirect tradeoff reopens.

### If executed via `$team`
- Lane 1: `executor` (high) — web auth-session coordinator + local 401 alignment.
- Lane 2: `executor` (high) — shared auth/user route repair + consumer audit.
- Lane 3: `test-engineer` (medium) — cross-surface verification matrix and smoke scripts.
- Lane 4: `verifier` (high) — final evidence pass and residual-risk check.

## Launch Hints

- Sequential execution: `$ralph .omx/plans/prd-auth-session-alignment-20260418T065800Z.md`
- Coordinated execution: `$team .omx/plans/prd-auth-session-alignment-20260418T065800Z.md`
- OMX CLI equivalent: `omx team .omx/plans/prd-auth-session-alignment-20260418T065800Z.md`

## Team Verification Path

1. Team proves contract fixes, coordinator wiring, and cross-stack 401 consistency are implemented.
2. Team produces build/typecheck evidence for shared, frontend, and mini program.
3. Ralph/verifier follow-up confirms manual auth smoke flows, logout revocation behavior, and any deferred refresh risks before declaring complete.

## Applied review improvements
- Added explicit legacy `request.ts` 401 convergence requirement.
- Elevated logout correctness to a first-class acceptance criterion.
- Added mini program compatibility checks and deliberate-mode pre-mortem/test plan.
