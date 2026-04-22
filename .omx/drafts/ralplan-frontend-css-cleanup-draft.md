# RALPLAN Draft: Frontend CSS Cleanup Planning

- Source spec: `.omx/specs/deep-interview-frontend-cleanup.md`
- Context snapshot: `.omx/context/frontend-cleanup-20260422T040750Z.md`
- Scope: plan-only; no frontend source edits.
- Consensus mode: RALPLAN-DR short mode.

## Requirements Summary

EasyOrange frontend cleanup should begin with a **CSS-focused planning artifact**, not code changes. The user wants to reduce frontend redundancy and disorder while preserving the current UI and functionality. The first deliverable must identify CSS files, style-layer overlap, likely legacy/experimental variants, risks, and a low-risk-to-high-risk cleanup sequence.

Hard constraints from deep-interview:

- No UI/visual changes.
- No behavior/functionality changes.
- No code edits in this planning phase.
- Later CSS cleanup must include key-page before/after screenshot comparison.
- No new dependencies; keep vanilla TypeScript + Vite.

## RALPLAN-DR Summary

### Principles

1. **Evidence before deletion** — classify files and selectors as referenced/possibly-unused/legacy only from observable imports, HTML links, build output, and screenshots.
2. **Visual invariance first** — any later CSS edit must prove unchanged UI via key-page screenshot comparison before being accepted.
3. **Small reversible phases** — sequence cleanup from documentation-only and dead-candidate isolation toward higher-risk consolidation only after gates pass.
4. **Separate active styles from experiments** — distinguish production entrypoint CSS from v3/refined/polish/premium experimental layers before recommending removal or consolidation.
5. **No execution during planning** — this plan defines next work; it must not modify `easyorange-frontend` source files.

### Top Decision Drivers

1. **Regression risk**: CSS cleanup can silently alter layout, typography, spacing, or responsive behavior.
2. **Traceability**: current references are split between TS imports and root HTML preload/link tags.
3. **Cleanup ROI**: largest and most duplicated style surfaces should be prioritized only after active/legacy status is known.

### Viable Options

#### Option A — Inventory-first audit, then phased roadmap (recommended)

Approach: Produce a CSS file inventory, dependency/reference map, risk classification, and staged cleanup roadmap with verification gates.

Pros:
- Fully matches the user-selected plan-only boundary.
- Enables safe later execution with evidence labels.
- Low risk because it does not edit source code.

Cons:
- Does not reduce code immediately.
- Requires later execution approval and verification setup.

#### Option B — Add visual regression harness first, then audit

Approach: Plan around creating screenshot baselines/playwright visual checks before detailed CSS classification.

Pros:
- Strongest protection before any CSS edit.
- Helps quantify “no UI change.”

Cons:
- Heavier than the requested first deliverable.
- May drift into tooling/test implementation, which violates plan-only scope if done now.

#### Option C — Direct low-risk CSS deletion/consolidation

Approach: Use current reference scan to delete apparently unreferenced polish/refined CSS files immediately.

Pros:
- Fast visible reduction in file count.
- Targets likely redundant style variants.

Cons:
- Violates first-phase plan-only boundary.
- Reference scans alone may miss dynamic/manual usage and visual dependencies.
- Rejected for current ralplan; can only be considered after inventory, screenshot baselines, and explicit execution handoff.

## Brownfield Evidence

### Tooling and verification anchors

- `easyorange-frontend/package.json:6-18` defines `dev`, `build`, `lint:check`, `typecheck`, and related scripts.
- `easyorange-frontend/package.json:20-31` includes Playwright, TypeScript, ESLint, Prettier, and Vite dev dependencies.
- `easyorange-frontend/playwright.config.ts:4` sets `tests/e2e` as E2E test dir.
- `easyorange-frontend/playwright.config.ts:17-21` configures trace/screenshot/video behavior.
- `easyorange-frontend/playwright.config.ts:29-34` starts `npm run dev` at `http://localhost:5173`.

### Active entrypoint CSS references

- Home entry imports `main.css` and `floating-nav.css` in `easyorange-frontend/src/main.ts:7-8`; `easyorange-frontend/index.html:17` also links `luxury-v3.css`, and `index.html:938` loads `/src/main.ts`.
- Favorites imports `main.css`, `floating-nav.css`, and `favorites.css` in `easyorange-frontend/src/pages/favorites.ts:1-3`; `favorites.html:14-15` preloads `main.css` and `favorites.css`.
- Messages imports `main.css` and `messages.css` in `easyorange-frontend/src/pages/messages.ts:5-6`; `messages.html:14-15` preloads those CSS files.
- Products imports `main.css`, `floating-nav.css`, and `products.css` in `easyorange-frontend/src/pages/products/ProductsPage.ts:5-7`; `products.html:14-15` preloads `main.css` and `products.css`.
- Profile imports `main.css` and `profile.css` in `easyorange-frontend/src/pages/profile.ts:1-2`; `profile.html:13-14` preloads those CSS files.
- Publish has both legacy and modular TS entry references to `publish.css`: `easyorange-frontend/src/pages/publish.ts:1-3` and `easyorange-frontend/src/pages/publish/index.ts:7-9`; `publish.html:13-14` preloads `main.css` and `publish.css`, and `publish.html:504` loads the modular `publish/index.ts` entry.
- Orders preloads `main.css` and `orders.css` in `orders.html:13-14` and loads `orders.ts` in `orders.html:147`, but `src/pages/orders.ts:1-8` currently does not import CSS directly.
- Editorial/v3 CSS is linked by `index-v3.html:17-22`.
- `luxury-v3.css` is linked by `index.html:17` and `luxury-v3.html:17`.

### Size and duplication signals from read-only scan

- 31 CSS files exist under `easyorange-frontend/src/styles`.
- Largest CSS files by approximate line count:
  - `src/styles/main.css` ~3616 lines.
  - `src/styles/luxury-v3.css` ~2297 lines.
  - `src/styles/profile-premium.css` ~2241 lines.
  - `src/styles/products.css` ~2194 lines.
  - `src/styles/publish.css` ~1829 lines.
  - `src/styles/profile.css` ~1801 lines.
  - `src/styles/orders.css` ~1111 lines.
  - `src/styles/modal-polish.css` ~1032 lines.
- Exact selector duplicates were detected across multiple files; examples include `.section-header`, `.btn-primary`, `.btn-secondary`, `.nav-item`, `.section-title`, `.card-title`, `.modal-content`, `.toast`, and `:root`. These are evidence of possible overlap, not proof of safe deletion.
- Several CSS files had no direct filename references in the TS/HTML grep scan: `chrome-polish.css`, `favorites-refined.css`, `home-polish.css`, `home-reference.css`, `home-refined.css`, `market-theme.css`, `messages-refined.css`, `modal-polish.css`, `motion-polish.css`, `product-card-polish.css`, `product-card-refined.css`, `profile-premium.css`, `profile-refined.css`, `publish-refined.css`, `skeleton.css`, `state-polish.css`. Treat as “unconfirmed direct references,” not unused, until build/runtime checks confirm.

## Proposed PRD

### Problem

The frontend has many large and variant CSS files with overlapping selectors and unclear active/legacy boundaries. This makes safe cleanup difficult because deleting or consolidating CSS may silently alter visible UI.

### Goal

Create a precise, evidence-backed CSS cleanup plan that can be used for a later implementation phase without changing current UI or functionality.

### Non-goals

- No source edits in this planning phase.
- No CSS deletion or consolidation in this planning phase.
- No visual/function behavior changes.
- No dependency or framework changes.
- No backend/API changes.

### Users / stakeholders

- Future frontend maintainer who needs to reduce CSS redundancy safely.
- QA/verifier who needs concrete commands and screenshot pages to validate no regressions.
- Execution agent/team that may later implement cleanup with clear stop conditions.

### Deliverables

1. **CSS inventory table** with columns:
   - File path
   - Lines/rough size
   - Direct references with file:line
   - Entrypoint/page association
   - Variant family (`active`, `v3`, `refined`, `polish`, `premium`, `shared`, `unknown`)
   - Duplicate-selector signals
   - Risk level
   - Evidence confidence (`evidence`, `inference`, `unknown`)
   - Recommended action for later phase (`keep`, `baseline first`, `candidate isolate`, `candidate merge`, `investigate`)
2. **Style-family map** grouping files into:
   - Global/shared: `main.css`, `floating-nav.css`, `market-theme.css`, state/skeleton/modal/product-card variants.
   - Page active: `favorites.css`, `messages.css`, `orders.css`, `products.css`, `profile.css`, `publish.css`.
   - Home/v3/editorial/luxury: `luxury-v3.css`, `editorial-*`, `home-*`.
   - Experimental/refined/polish/premium: `*-refined.css`, `*-polish.css`, `profile-premium.css`.
3. **Phased cleanup roadmap** from lowest to highest risk.
4. **Verification matrix** including build/type/lint/e2e and key-page screenshots.
5. **Stop/rollback conditions** for later execution.

### Acceptance criteria

- AC1: Every CSS file in `easyorange-frontend/src/styles/*.css` appears in the inventory.
- AC2: Every direct TS/HTML CSS reference includes file:line evidence.
- AC3: Files with no direct references are labeled as “unconfirmed direct references,” not “unused,” unless additional evidence proves unused.
- AC4: The plan ranks CSS cleanup candidates by risk and separates evidence from inference.
- AC5: The roadmap starts with no-op/documentation and verification setup before any deletion/merge phase.
- AC6: Later implementation steps require `npm run build`, `npm run typecheck`, `npm run lint:check`, and key-page screenshot comparison.
- AC7: The screenshot baseline list includes at least home, products, publish, profile, and any page affected by shared CSS.
- AC8: Stop conditions include unexplained screenshot diffs, failed build/typecheck/lint, uncertain active usage, or selector changes touching shared/global classes.

## Phased Roadmap for Later Cleanup Execution

### Phase 0 — Baseline and inventory only

- Produce inventory and style-family map.
- Capture baseline screenshots for home, products, publish, profile; add favorites/messages/orders if shared CSS is touched.
- Run `npm run typecheck`, `npm run lint:check`, and `npm run build` as baseline if execution is approved.
- No CSS edits.

### Phase 1 — Mark active vs candidate files

- Confirm active CSS via TS imports, root HTML links/preloads, Vite bundle output, and runtime screenshots.
- Label unreferenced/refined/polish/premium files as candidates only after checking build output and any manual HTML/demo entrypoint.
- No deletion yet unless separately approved.

### Phase 2 — Low-risk isolation

- Move candidate-only cleanup into a reversible branch/commit plan.
- Prefer deleting or archiving files only after baseline screenshots and build output prove they are not loaded.
- Validate after each small group.

### Phase 3 — Shared token and selector consolidation

- Consolidate duplicated variables/selectors only when exact visual equivalence is protected by screenshots.
- Start with duplicated tokens or identical declarations, not broad selectors.
- Avoid changing cascade order for `main.css`, `publish.css`, `products.css`, `profile.css`, and `luxury-v3.css` until high-confidence baselines exist.

### Phase 4 — Page CSS simplification

- Tackle page styles one page at a time: publish/products/profile before lower-risk smaller pages only if user agrees with risk/ROI.
- Use page-specific screenshot before/after and functional smoke for the page.

## Verification Matrix

| Phase | Required checks | Evidence expected | Stop condition |
| --- | --- | --- | --- |
| Inventory | Read-only reference scan; line counts; selector overlap scan | Inventory table with evidence/inference labels | Any file cannot be classified even as unknown |
| Baseline | `npm run typecheck`; `npm run lint:check`; `npm run build`; baseline screenshots | Command logs + screenshots | Baseline fails before cleanup |
| Candidate isolation | Build/type/lint + screenshot pages affected by removed/isolated files | Zero command failures; no unexplained visual diffs | Any unexplained visual diff or dynamic usage uncertainty |
| Shared consolidation | Same checks + focused screenshots for all pages using `main.css`/`floating-nav.css` | Visual parity for all shared-style pages | Cascade/order changes create diff |
| Page simplification | Same checks + page-specific functional smoke | Visual parity + page behavior unaffected | Functional flow or screenshot diff regression |

## Recommended key screenshots

- Home: `index.html` / `/` because it loads `luxury-v3.css` and `src/main.ts`.
- Products: `products.html` because it combines `main.css`, `floating-nav.css`, and `products.css`.
- Publish: `publish.html` because it combines `main.css`, `floating-nav.css`, `publish.css`, and modular publish TS.
- Profile: `profile.html` because it combines `main.css` and `profile.css`.
- Favorites/messages/orders if their page or shared CSS is touched.
- `index-v3.html` and `luxury-v3.html` when evaluating editorial/luxury CSS families.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| CSS filename grep misses dynamic/manual usage | Accidental deletion of apparently unused files | Use Vite build output, root HTML checks, and runtime screenshots before deletion |
| Shared selectors appear duplicated but depend on cascade order | Visual regressions | Treat duplicate selectors as candidates only; compare declarations and screenshots before merge |
| `main.css` and `luxury-v3.css` both affect home | High visual risk | Baseline home first; avoid shared/global edits until later phases |
| Publish has both legacy `publish.ts` and modular `publish/index.ts` references | Confusing active entry ownership | Plan must map actual HTML script entry before touching imports or styles |
| Existing Playwright screenshots are failure-only | Need before/after visual evidence | Later execution can add temporary screenshot capture or manual screenshot protocol without new dependencies |
| Existing lint script checks only TS | CSS quality issues may not be linted | Use CSS inventory/static scans and screenshots rather than pretending ESLint covers CSS |

## ADR

### Decision

Adopt **Option A: Inventory-first audit, then phased roadmap** as the planning approach. The current ralplan output should produce PRD and test-spec artifacts for CSS cleanup planning, not implementation.

### Drivers

- The user explicitly chose plan-only for the first phase.
- CSS cleanup is visually risky and requires screenshot evidence.
- Current repo has many CSS variants and overlapping selectors, so deletion/consolidation without inventory is unsafe.

### Alternatives considered

- **Visual harness first**: useful later, but too implementation-heavy for the first plan-only artifact.
- **Direct deletion/consolidation**: rejected because it violates the user boundary and could alter UI.

### Why chosen

Inventory-first produces immediate clarity and a safe execution contract while respecting no-code-change constraints.

### Consequences

- No source cleanup happens until a later execution handoff.
- The next execution mode can start with concrete files, phases, and verification gates.
- Some cleanup opportunities remain inferred until execution gathers build/runtime evidence.

### Follow-ups

- Generate a full CSS inventory artifact.
- Decide whether later execution should run via `$ralph` for sequential cleanup or `$team` for parallel inventory/verification/cleanup lanes.
- Add or script screenshot baseline capture only after explicit execution approval.

## Available Agent Types Roster

Relevant available roles for follow-up:

- `explore`: fast repo lookup and file/symbol mapping.
- `planner`: plan sequencing and risk flags.
- `architect`: system/design review and tradeoff analysis.
- `critic`: plan/design challenge.
- `executor`: implementation/refactoring.
- `test-engineer`: test strategy and coverage.
- `verifier`: completion evidence and validation.
- `code-reviewer`: comprehensive code review.
- `designer` or `vision`: visual review/screenshot interpretation if needed.

## Follow-up Staffing Guidance

### `$ralph` sequential path

Recommended when the user wants maximum safety and one persistent owner.

- `explore` / low reasoning: produce the complete CSS inventory and active-reference map.
- `test-engineer` / medium reasoning: define baseline screenshot protocol and command matrix.
- `executor` / high reasoning: implement one low-risk cleanup phase at a time after approval.
- `verifier` / high reasoning: compare screenshots, build/type/lint logs, and stop-condition evidence.

Launch hint:

```bash
$ralph .omx/plans/prd-frontend-css-cleanup.md .omx/plans/test-spec-frontend-css-cleanup.md
```

### `$team` parallel path

Recommended if the user wants faster but coordinated planning/execution after approval.

Suggested lanes:

1. Inventory lane (`explore`): CSS references, line counts, selector duplicates, Vite output.
2. Verification lane (`test-engineer`): screenshot baseline protocol and Playwright/manual capture path.
3. Risk lane (`architect`/`critic`): cascade/order and shared-selector risk review.
4. Implementation lane (`executor`) only after inventory + verification gates are accepted.
5. Verification lane (`verifier`) before team shutdown.

Launch hints:

```bash
$team .omx/plans/prd-frontend-css-cleanup.md .omx/plans/test-spec-frontend-css-cleanup.md
# or
omx team .omx/plans/prd-frontend-css-cleanup.md .omx/plans/test-spec-frontend-css-cleanup.md
```

Team verification path:

- Team must prove no source edits happen during inventory-only phase.
- Before any cleanup implementation, team must produce baseline build/type/lint logs and screenshots.
- After implementation, team must provide before/after screenshot evidence and command outputs.
- A final `$ralph` or `verifier` pass should check that all acceptance criteria and stop conditions were respected.

## Draft Changelog

- Created initial RALPLAN-DR short-mode plan from deep-interview spec.
- Added evidence-backed CSS references, risk categories, phased roadmap, verification matrix, ADR, and staffing guidance.
