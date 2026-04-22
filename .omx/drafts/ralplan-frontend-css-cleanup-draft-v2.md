# RALPLAN Draft v2: Frontend CSS Cleanup Planning

- Source spec: `.omx/specs/deep-interview-frontend-cleanup.md`
- Context snapshot: `.omx/context/frontend-cleanup-20260422T040750Z.md`
- Prior draft: `.omx/drafts/ralplan-frontend-css-cleanup-draft.md`
- Scope: plan-only; no frontend source edits.
- Consensus mode: RALPLAN-DR short mode.
- Architect v1 verdict: ITERATE; v2 addresses the missing full inventory and screenshot protocol specificity.

## Requirements Summary

EasyOrange frontend cleanup should begin with a **CSS-focused planning artifact**, not code changes. The user wants to reduce frontend redundancy and disorder while preserving the current UI and functionality. This v2 plan includes the requested CSS inventory now, not as a deferred follow-up, and pairs it with a visual-baseline protocol design before any future cleanup execution.

Hard constraints from deep-interview:

- No UI/visual changes.
- No behavior/functionality changes.
- No code edits in this planning phase.
- Later CSS cleanup must include key-page before/after screenshot comparison.
- No new dependencies; keep vanilla TypeScript + Vite.

## RALPLAN-DR Summary

### Principles

1. **Evidence before deletion** — classify files and selectors as referenced/possibly-unused/legacy only from observable imports, HTML links, inline style layers, build output, and screenshots.
2. **Visual invariance first** — any later CSS edit must prove unchanged UI via key-page screenshot comparison before being accepted.
3. **Small reversible phases** — sequence cleanup from inventory and baseline protocol toward higher-risk consolidation only after gates pass.
4. **Separate active styles from experiments** — distinguish production entrypoint CSS from v3/refined/polish/premium experimental layers before recommending removal or consolidation.
5. **No execution during planning** — this plan defines next work; it must not modify `easyorange-frontend` source files.

### Top Decision Drivers

1. **Regression risk**: CSS cleanup can silently alter layout, typography, spacing, responsive behavior, or cascade order.
2. **Traceability**: current styles are split between TS imports, root HTML preload/link tags, external font stylesheets, inline `<style>` blocks, and potential Vite bundle output.
3. **Cleanup ROI**: large and duplicated style surfaces should be prioritized only after active/legacy status and screenshot baselines are known.

### Viable Options

#### Option A — Inventory-first audit plus visual protocol design (recommended)

Approach: Produce a CSS file inventory, dependency/reference map, effective-style-layer map, risk classification, staged cleanup roadmap, and screenshot protocol design. No source edits.

Pros:
- Matches the user-selected plan-only boundary.
- Includes the CSS inventory required by the source spec.
- Avoids false confidence by pairing static inventory with a visual protocol before future cleanup.

Cons:
- Does not reduce code immediately.
- Some active/legacy conclusions remain provisional until a future execution lane can inspect Vite output and capture screenshots.

#### Option B — Visual baseline protocol first, inventory second

Approach: Make the primary deliverable a screenshot baseline protocol and page-state matrix before detailed CSS classification.

Pros:
- Strongest protection for “no UI change.”
- Forces viewport/data/auth assumptions before any cleanup.

Cons:
- Less directly responsive to the user’s requested CSS inventory + roadmap deliverable.
- Without inventory, it does not identify where redundancy exists.

#### Option C — Direct low-risk CSS deletion/consolidation

Approach: Delete or consolidate files with no direct filename references immediately.

Pros:
- Fast visible reduction in file count.

Cons:
- Rejected for this phase: violates plan-only boundary; filename scans can miss dynamic/manual usage; CSS cascade is too risky without screenshots.

## Brownfield Evidence

### Tooling and verification anchors

- `easyorange-frontend/package.json:6-18` defines `dev`, `build`, `lint:check`, `typecheck`, and related scripts.
- `easyorange-frontend/package.json:20-31` includes Playwright, TypeScript, ESLint, Prettier, and Vite dev dependencies.
- `easyorange-frontend/playwright.config.ts:4` sets `tests/e2e` as E2E test dir.
- `easyorange-frontend/playwright.config.ts:17-21` configures trace/screenshot/video behavior; screenshots are currently failure-only, so baseline capture needs an explicit protocol.
- `easyorange-frontend/playwright.config.ts:29-34` starts `npm run dev` at `http://localhost:5173`.

### Effective style layers that must be considered

1. **TS CSS imports** — examples: `src/main.ts:7-8`, `src/pages/products/ProductsPage.ts:5-7`, `src/pages/publish/index.ts:7-9`.
2. **Root HTML links/preloads** — examples: `products.html:14-15`, `profile.html:13-14`, `publish.html:13-14`, `index-v3.html:17-22`, `index.html:17`.
3. **Inline style blocks** — `easyorange-frontend/publish.html:15-56` defines publish-page overrides with `!important`; this is a high-risk cascade constraint.
4. **External font stylesheets** — root HTML files link fonts from `fonts.loli.net`, which can affect visual baselines.
5. **Vite bundle output** — future execution should inspect built asset references before deleting candidate files.

### Active entrypoint CSS references

- Home imports `main.css` and `floating-nav.css` in `easyorange-frontend/src/main.ts:7-8`; `easyorange-frontend/index.html:17` also links `luxury-v3.css`, and `index.html:938` loads `/src/main.ts`.
- Favorites imports `main.css`, `floating-nav.css`, and `favorites.css` in `easyorange-frontend/src/pages/favorites.ts:1-3`; `favorites.html:14-15` preloads `main.css` and `favorites.css`.
- Messages imports `main.css` and `messages.css` in `easyorange-frontend/src/pages/messages.ts:5-6`; `messages.html:14-15` preloads those CSS files.
- Products imports `main.css`, `floating-nav.css`, and `products.css` in `easyorange-frontend/src/pages/products/ProductsPage.ts:5-7`; `products.html:14-15` preloads `main.css` and `products.css`.
- Profile imports `main.css` and `profile.css` in `easyorange-frontend/src/pages/profile.ts:1-2`; `profile.html:13-14` preloads those CSS files.
- Publish has both legacy and modular TS entry references to `publish.css`: `easyorange-frontend/src/pages/publish.ts:1-3` and `easyorange-frontend/src/pages/publish/index.ts:7-9`; `publish.html:13-14` preloads `main.css` and `publish.css`, and `publish.html:504` loads the modular `publish/index.ts` entry.
- Orders preloads `main.css` and `orders.css` in `orders.html:13-14` and loads `orders.ts` in `orders.html:147`, but `src/pages/orders.ts:1-8` currently does not import CSS directly.
- Editorial/v3 CSS is linked by `index-v3.html:17-22`.
- `luxury-v3.css` is linked by `index.html:17` and `luxury-v3.html:17`.

### CSS Inventory

Evidence labels: direct references are from filename scans across TS/HTML/CSS excluding `dist` and `node_modules`; “No direct filename refs found” is **not** proof of unused status.

| CSS file | Lines | !important | Direct references | Family | Risk | Recommended later action |
| --- | ---: | ---: | --- | --- | --- | --- |
| src/styles/chrome-polish.css | 498 | 0 | No direct filename refs found by TS/HTML/CSS scan | polish-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/editorial-footer-v3.css | 78 | 0 | index-v3.html:21 | v3/editorial-active-or-demo | Medium | keep; baseline before any edit |
| src/styles/editorial-hero-v3.css | 355 | 0 | index-v3.html:19 | v3/editorial-active-or-demo | Medium | keep; baseline before any edit |
| src/styles/editorial-nav-v3.css | 408 | 0 | index-v3.html:18 | v3/editorial-active-or-demo | Medium | keep; baseline before any edit |
| src/styles/editorial-product-v3.css | 719 | 0 | index-v3.html:20 | v3/editorial-active-or-demo | Medium | keep; baseline before any edit |
| src/styles/editorial-search-v3.css | 573 | 0 | index-v3.html:22 | v3/editorial-active-or-demo | Medium | keep; baseline before any edit |
| src/styles/editorial-v3.css | 347 | 4 | index-v3.html:17 | v3/editorial-active-or-demo | High | keep; baseline before any edit |
| src/styles/favorites-refined.css | 550 | 0 | No direct filename refs found by TS/HTML/CSS scan | refined-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/favorites.css | 658 | 0 | favorites.html:15, src/pages/favorites.ts:3 | page-active | Medium | keep; baseline before any edit |
| src/styles/floating-nav.css | 762 | 1 | src/main.ts:8, src/pages/favorites.ts:2, src/pages/publish.ts:2, src/pages/products/ProductsPage.ts:6, src/pages/publish/index.ts:8 | shared-active | High | keep; baseline before any edit |
| src/styles/home-polish.css | 590 | 0 | No direct filename refs found by TS/HTML/CSS scan | polish-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/home-reference.css | 1005 | 0 | No direct filename refs found by TS/HTML/CSS scan | unknown | Medium-Unknown | investigate; no deletion without proof |
| src/styles/home-refined.css | 562 | 0 | No direct filename refs found by TS/HTML/CSS scan | refined-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/luxury-v3.css | 2297 | 0 | index.html:17, luxury-v3.html:17 | v3/editorial-active-or-demo | High | keep; baseline before any edit |
| src/styles/main.css | 3616 | 1 | favorites.html:14, messages.html:14, orders.html:13, products.html:14, profile.html:13, publish.html:13, src/main.ts:7, src/pages/favorites.ts:1, src/pages/messages.ts:5, src/pages/profile.ts:1, src/pages/publish.ts:1, src/styles/profile-premium.css:1532, src/pages/products/ProductsPage.ts:5, src/pages/publish/index.ts:7 | shared-active | High | keep; baseline before any edit |
| src/styles/market-theme.css | 137 | 3 | No direct filename refs found by TS/HTML/CSS scan | shared/utility-candidate | Medium-Unknown | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/messages-refined.css | 898 | 0 | No direct filename refs found by TS/HTML/CSS scan | refined-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/messages.css | 239 | 0 | messages.html:15, src/pages/messages.ts:6 | page-active | Medium | keep; baseline before any edit |
| src/styles/modal-polish.css | 1032 | 0 | No direct filename refs found by TS/HTML/CSS scan | polish-variant | Medium-Unknown | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/motion-polish.css | 97 | 0 | No direct filename refs found by TS/HTML/CSS scan | polish-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/orders.css | 1111 | 0 | orders.html:14 | page-active | High | keep; baseline before any edit |
| src/styles/product-card-polish.css | 404 | 0 | No direct filename refs found by TS/HTML/CSS scan | polish-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/product-card-refined.css | 245 | 0 | No direct filename refs found by TS/HTML/CSS scan | refined-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/products.css | 2194 | 1 | products.html:15, src/pages/products/ProductsPage.ts:7 | page-active | High | keep; baseline before any edit |
| src/styles/profile-premium.css | 2241 | 42 | No direct filename refs found by TS/HTML/CSS scan | premium-variant | Medium-Unknown | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/profile-refined.css | 683 | 7 | No direct filename refs found by TS/HTML/CSS scan | refined-variant | Medium-Unknown | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/profile.css | 1801 | 0 | profile.html:14, src/pages/profile.ts:2 | page-active | High | keep; baseline before any edit |
| src/styles/publish-refined.css | 547 | 10 | No direct filename refs found by TS/HTML/CSS scan | refined-variant | Medium-Unknown | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/publish.css | 1829 | 62 | publish.html:14, src/pages/publish.ts:3, src/pages/publish/index.ts:9 | page-active | High | keep; baseline before any edit |
| src/styles/skeleton.css | 535 | 1 | No direct filename refs found by TS/HTML/CSS scan | shared/utility-candidate | Medium-Unknown | investigate; candidate isolate only after bundle/runtime proof |
| src/styles/state-polish.css | 368 | 0 | No direct filename refs found by TS/HTML/CSS scan | polish-variant | Unknown/Low after confirmation | investigate; candidate isolate only after bundle/runtime proof |


### Duplicate-selector signals

Read-only selector scan found exact selector duplication across multiple files. These are cleanup signals, not safe-delete proof. High-overlap examples include:

- `.section-header` across 9 CSS files.
- `.btn-primary` and `.btn-secondary` across 8 CSS files.
- `.nav-item`, `.nav-item.active`, `.nav-item:hover`, and `.section-title` across 7 CSS files.
- `.modal-content`, `.modal-header`, `.toast`, `.card-title`, `.form-input`, and related common selectors across 5+ files.

## Proposed PRD

### Problem

The frontend has many large and variant CSS files with overlapping selectors and unclear active/legacy boundaries. This makes safe cleanup difficult because deleting or consolidating CSS may silently alter visible UI, especially when TS imports, HTML links, inline `!important` rules, and external font stylesheets all contribute to effective styling.

### Goal

Create a precise, evidence-backed CSS cleanup plan that can be used for a later implementation phase without changing current UI or functionality.

### Non-goals

- No source edits in this planning phase.
- No CSS deletion or consolidation in this planning phase.
- No visual/function behavior changes.
- No dependency or framework changes.
- No backend/API changes.

### Deliverables

1. Completed CSS inventory table above.
2. Style-family map grouping active, shared, v3/editorial/luxury, refined/polish/premium, and utility-candidate files.
3. Visual baseline protocol design.
4. Phased cleanup roadmap.
5. Verification matrix and stop/rollback conditions.

### Acceptance criteria

- AC1: Every CSS file in `easyorange-frontend/src/styles/*.css` appears in the inventory.
- AC2: Every direct TS/HTML CSS reference found by scan includes file:line evidence.
- AC3: Files with no direct references are labeled as “No direct filename refs found,” not “unused.”
- AC4: The plan includes effective style layers: TS imports, HTML links/preloads, inline styles, external fonts, and future Vite output checks.
- AC5: The plan ranks cleanup candidates by risk and separates evidence from inference.
- AC6: The roadmap starts with no-op inventory and baseline protocol before any deletion/merge phase.
- AC7: Later implementation steps require `npm run build`, `npm run typecheck`, `npm run lint:check`, and key-page screenshot comparison.
- AC8: Screenshot protocol defines pages, viewport, browser, data/auth assumptions, and diff stop conditions.
- AC9: Stop conditions include unexplained screenshot diffs, failed build/typecheck/lint, uncertain active usage, inline style conflicts, or shared-selector/cascade-order changes.

## Visual Baseline Protocol Design

This protocol is part of planning only; it does not add tests or modify source now.

### Required baseline pages

- Home: `/` / `index.html` because it combines `luxury-v3.css`, `main.css`, and `floating-nav.css`.
- Products: `/products.html` because it combines `main.css`, `floating-nav.css`, and `products.css`.
- Publish: `/publish.html` because it combines `main.css`, `floating-nav.css`, `publish.css`, modular publish TS, and inline `!important` overrides.
- Profile: `/profile.html` because it combines `main.css` and `profile.css`.
- Favorites/messages/orders when shared CSS (`main.css`, `floating-nav.css`) or page-specific CSS is touched.
- `index-v3.html` and `luxury-v3.html` when evaluating editorial/luxury families.

### Baseline assumptions

- Browser: Chromium, matching current Playwright project (`playwright.config.ts:23-27`).
- Base URL: `http://localhost:5173`, matching current config (`playwright.config.ts:17-18`, `:29-34`).
- Viewports: at least desktop 1280x720; add mobile/tablet before touching responsive selectors/media queries.
- Data state: use stable local/mock state where available; document if backend data is variable.
- Auth state: capture logged-out baseline unless a page requires login; if login-dependent, capture explicit seeded/authenticated state.
- Fonts/network: external font loading can affect screenshots; record whether fonts loaded successfully.

### Visual acceptance rule

Any later cleanup step fails if screenshots show unexplained layout, spacing, typography, color, visibility, or interaction-state differences. A diff can pass only if it is explicitly intentional and approved outside this plan.

## Phased Roadmap for Later Cleanup Execution

### Phase 0a — Baseline protocol finalization

- Confirm target pages, viewports, auth/data state, and screenshot method.
- No CSS edits.

### Phase 0b — Inventory validation

- Re-run reference scan and selector overlap scan.
- Inspect Vite build output for CSS assets before treating any file as inactive.
- No deletion; update inventory confidence only.

### Phase 1 — Candidate isolation

- Start with files with no direct filename references and variant-family names, but only after Vite/runtime evidence confirms they are not loaded.
- Keep changes in small reversible batches.
- Validate with build/type/lint and screenshots for affected pages.

### Phase 2 — Shared token and exact duplicate consolidation

- Consolidate duplicated tokens or identical declarations only after screenshots exist.
- Avoid broad shared selectors first; cascade order is high risk.

### Phase 3 — Page CSS simplification

- Tackle one page family at a time: publish/products/profile are high ROI but high risk.
- Use page-specific screenshots and smoke checks.

### Phase 4 — Legacy/v3/editorial decision

- Decide whether `index-v3.html`, `luxury-v3.html`, and `editorial-*` are active product surfaces, demos, or removable experiments.
- Requires explicit approval before removal or consolidation.

## Verification Matrix

| Phase | Required checks | Evidence expected | Stop condition |
| --- | --- | --- | --- |
| Plan/inventory | Read-only reference scan; line counts; selector overlap scan | Inventory with evidence/inference labels | Any CSS file omitted from inventory |
| Baseline | `npm run typecheck`; `npm run lint:check`; `npm run build`; baseline screenshots | Command logs + screenshots | Baseline fails before cleanup |
| Candidate isolation | Build/type/lint + screenshot pages affected by removed/isolated files | Zero command failures; no unexplained visual diffs | Any unexplained visual diff or dynamic usage uncertainty |
| Shared consolidation | Same checks + screenshots for all pages using `main.css`/`floating-nav.css` | Visual parity for all shared-style pages | Cascade/order changes create diff |
| Page simplification | Same checks + page-specific functional smoke | Visual parity + page behavior unaffected | Functional flow or screenshot diff regression |

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| CSS filename grep misses dynamic/manual usage | Accidental deletion of apparently unused files | Use Vite build output, root HTML checks, and runtime screenshots before deletion |
| Inline `!important` rules in `publish.html:15-56` affect cascade | Publish page visual regressions | Treat publish as high risk; baseline focus/filled form states |
| Shared selectors appear duplicated but depend on cascade order | Visual regressions | Treat duplicate selectors as candidates only; compare declarations and screenshots before merge |
| Home combines `luxury-v3.css` with `main.css`/`floating-nav.css` | High visual risk | Baseline home before any luxury/main/nav edits |
| Existing screenshots are failure-only | Need before/after visual evidence | Later execution should add temporary/manual screenshot capture protocol using existing Playwright/browser tooling |
| Existing lint script checks only TS | CSS quality issues may not be linted | Use CSS inventory/static scans and screenshots rather than pretending ESLint covers CSS |

## ADR

### Decision

Adopt **Option A: Inventory-first audit plus visual protocol design** as the planning approach. The current ralplan output should produce PRD and test-spec artifacts for CSS cleanup planning, not implementation.

### Drivers

- The user explicitly chose plan-only for the first phase.
- CSS cleanup is visually risky and requires screenshot evidence.
- Current repo has many CSS variants and overlapping selectors, so deletion/consolidation without inventory and baselines is unsafe.

### Alternatives considered

- **Visual baseline first**: strong safety but incomplete for the requested CSS inventory/roadmap deliverable.
- **Direct deletion/consolidation**: rejected because it violates the user boundary and could alter UI.

### Why chosen

Inventory plus baseline protocol produces immediate clarity and a safe execution contract while respecting no-code-change constraints.

### Consequences

- No source cleanup happens until a later execution handoff.
- Some file statuses remain provisional until execution inspects build output and screenshots.
- The next execution mode can start with concrete files, phases, and verification gates.

### Follow-ups

- If approved for execution, run baseline commands and capture screenshots before source edits.
- Decide whether later execution should run via `$ralph` for sequential safety or `$team` for parallel inventory/verification lanes.

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

- `explore` / low reasoning: validate the CSS inventory, references, and Vite output.
- `test-engineer` / medium reasoning: define and run screenshot baseline protocol.
- `executor` / high reasoning: implement one cleanup phase at a time after approval.
- `verifier` / high reasoning: compare screenshots, build/type/lint logs, and stop-condition evidence.

Launch hint after final PRD + test-spec approval:

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

Launch hints after final PRD + test-spec approval:

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

## Draft v2 Changelog

- Added complete CSS inventory table for all `src/styles/*.css` files.
- Added effective style layers, including inline `publish.html` `!important` styles and external fonts.
- Added Phase 0a visual baseline protocol design before any cleanup.
- Tightened stop conditions around visual diffs, uncertain usage, and cascade-order changes.
