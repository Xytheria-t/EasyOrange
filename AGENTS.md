# EasyOrange 校园二手交易平台

**Generated:** 2026-04-01
**Stack:** Java Spring Boot 3 (backend) + TypeScript/Vite (frontend)
**Architecture:** Multi-module Maven monorepo

## STRUCTURE

```
EasyOrange/
├── easyorange-backend/          # Spring Boot multi-module
│   ├── easyorange-application/  # Bootstrap + integration tests
│   ├── easyorange-common/       # Shared annotations, Result<T>, enums
│   ├── easyorange-framework/    # Security, Redis, MyBatis-Plus configs
│   ├── easyorange-user/         # Auth, registration, profiles
│   ├── easyorange-product/      # Product CRUD, categories, search
│   ├── easyorange-order/        # Order lifecycle
│   ├── easyorange-payment/      # Payment integration (Alipay/WeChat)
│   └── easyorange-message/      # WebSocket messaging, notifications
├── easyorange-frontend/         # Vanilla TS SPA (no framework)
└── .idea/                       # IntelliJ workspace
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Auth/JWT | `backend/easyorange-framework/` + `backend/easyorange-user/` | SecurityConfig + UserServiceImpl |
| Product CRUD | `backend/easyorange-product/` | Standard controller/service/mapper pattern |
| Orders | `backend/easyorange-order/` | Order lifecycle management |
| Payments | `backend/easyorange-payment/` | Alipay/WeChat Pay integration |
| Messaging | `backend/easyorange-message/` | WebSocket + system notifications |
| Frontend API layer | `frontend/src/api/` | Fetch-based, proxy to :8080 |
| Frontend pages | `frontend/src/pages/` | Vanilla TS page logic |

## CONVENTIONS

### Backend (Java)
- Package: `com.cartethyia.easyorange.{module}`
- Pattern: Controller → Service → ServiceImpl → Mapper → Entity
- DTOs: `dto/request/` (input), `dto/response/` (output), `dto/vo/` (view objects)
- Return type: `Result<T>` from easyorange-common
- Annotations: `@Log`, `@RateLimiter`, `@RepeatSubmit` from common
- Password: BCrypt encoding
- Auth: JWT Bearer token, stateless session

### Frontend (TypeScript)
- Zero framework — vanilla TS only
- Path aliases: `@`, `@api`, `@utils`, `@components`, `@pages`
- API proxy: `/api` → `http://localhost:8080`
- No state management library (raw TS)
- CSS: BEM naming, CSS variables

## ANTI-PATTERNS (THIS PROJECT)
- NEVER log passwords (excluded via `@Log` annotation)
- NEVER return password hashes in API responses
- No `as any` or `@ts-ignore` in frontend
- No framework imports in frontend (vanilla TS only)
- CSRF disabled (stateless API) — do not re-enable

## COMMANDS

```bash
# Backend (from easyorange-backend/)
mvn clean install                              # Full build
mvn clean install -pl easyorange-{module}      # Single module

# Frontend (from easyorange-frontend/)
npm run dev                                    # Dev server (:5173)
npm run build                                  # Production build
npm run lint                                   # ESLint + auto-fix
npm run typecheck                              # TypeScript type check
```

## NOTES
- Frontend dev server proxies `/api` to backend `:8080`
- Each backend module is independently buildable via `-pl` flag
- `target/` directories contain compiled classes — ignore for source analysis
- IntelliJ project files in `.idea/` — not source code
