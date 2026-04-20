# easyorange-frontend

**Type:** TypeScript/Vite Customer SPA

## OVERVIEW

Customer-facing storefront SPA with zero framework dependencies — vanilla TypeScript only.

## STRUCTURE

```
easyorange-frontend/
├── src/
│   ├── api/        # Fetch-based API layer (~19 modules)
│   ├── assets/     # Static assets
│   ├── components/ # Reusable components (Header, ProductCard, etc.)
│   ├── constants/  # App constants
│   ├── pages/      # Page logic (home, products, profile, publish, favorites)
│   ├── styles/     # CSS (BEM naming, CSS variables)
│   ├── types/      # TypeScript type definitions
│   ├── utils/      # Helper functions
│   └── main.ts     # Entry point
├── index.html      # Home page
├── products.html   # Product listing
├── profile.html    # User profile
├── publish.html    # Publish product
├── favorites.html  # Favorites page
└── vite.config.ts  # Build config
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| API calls | `src/api/` | Fetch-based, proxy to :8080 |
| Pages | `src/pages/` | Vanilla TS page logic |
| Components | `src/components/` | Reusable UI pieces |
| Styles | `src/styles/` | BEM + CSS variables |
| Types | `src/types/` | Shared TypeScript types |

## CONVENTIONS

- Path aliases: `@`, `@api`, `@utils`, `@components`, `@pages`, `@types`, `@constants`, `@assets`
- Proxy: `/api` → `http://localhost:8080`
- CSS: BEM naming, CSS variables for theming
- Zero framework — vanilla TS only
- Files should not exceed 300 lines

## ANTI-PATTERNS

- No state management library (raw TS)
- No framework imports (vanilla TS only)
- No `as any` or `@ts-ignore`
- No `!important` in CSS
- Minimal deps (only terser in production)

## COMMANDS

```bash
cd easyorange-frontend
npm run dev         # Dev server (:5173)
npm run build       # Production build
npm run lint        # ESLint + auto-fix
npm run typecheck   # TypeScript type check
```
