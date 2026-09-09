# catchmeif404.md

Project guidance for any AI coding agent working in this repository.

## Project overview

An AWS monthly cost calculator: Spring Boot backend + Next.js frontend. Users describe a service (traffic, stage, type) or manually assemble AWS resources, and get a deterministic monthly cost estimate (USD + KRW) in a chosen region, an on-demand AI-driven cost optimization recommendation (credit-gated), and an optional AI-generated plain-language explanation.

## Commands

```bash
docker compose up -d              # start local PostgreSQL 16 (db: aws_calculator)

cd backend && ./mvnw spring-boot:run   # run API on :8080
cd backend && ./mvnw test              # run all backend tests
cd backend && ./mvnw test -Dtest=PricingServiceTest   # run a single test class

cd frontend && npm install
cd frontend && npm run dev        # Next.js dev server on :3000
cd frontend && npm run build      # production build
cd frontend && npm run lint       # ESLint
```

No frontend test framework is configured. For UI changes, run `npm run lint` and `npm run build`.

To refresh AWS pricing snapshots: `backend/scripts/fetch_pricing.sh [region]` (requires `curl`, `jq`; hits AWS's public Price List Bulk API, no credentials needed), then restart the backend.

## Repository structure

This repository is split into a Spring Boot backend and a Next.js frontend. Backend code lives in
`backend/src/main/java/com/awscalculator/backend`, organized by feature packages such as
`project`, `resource`, `calculation`, `recommendation`, `pricing`, `share`, and `ai`. Backend
configuration and regional pricing JSON files are in `backend/src/main/resources`; tests are under
`backend/src/test`.

Frontend code lives in `frontend/src`. App Router files are in `frontend/src/app`, calculator
components are in `frontend/src/components/calculator`, and API helpers are in `frontend/src/lib`.
Product notes are in `docs/`.

## Architecture

### Backend: `backend/src/main/java/com/awscalculator/backend`, by feature package

- **`calculation`** — `CostEngine` is the only place dollar math happens, one method per `ResourceType` (ECS, RDS, REDIS, ALB, S3, NAT_GATEWAY, CLOUDFRONT, LAMBDA, DYNAMODB, EC2, EBS, DATA_TRANSFER, VPC_ENDPOINT, TRANSIT_GATEWAY, VPC_PEERING). Deterministic and region-aware; an LLM is never asked to compute a price, only to explain one already computed here.
- **`pricing`** — Two-layer pricing source. `PricingCatalog` loads static per-region JSON snapshots from `classpath:/pricing/<region>-pricing.json` (fetched by `scripts/fetch_pricing.sh`) at class-init. `PricingService.forRegion()` reads from the `aws_price_dimensions` DB table first and falls back to the classpath snapshot if the DB has no rows for that region; `seedFromSnapshotsIfMissing()` (run at startup via `PricingCatalogInitializer`) backfills the DB from the snapshot so pricing can later be edited in the DB via `PricingAdminController` (admin-only) without redeploying. Supported regions and the default region live in `PricingCatalog.SUPPORTED_REGIONS` / `DEFAULT_REGION`.
- **`project` / `resource`** — A `Project` (region, traffic profile) owns a list of `Resource`s (type + a JSON `configuration` map read via `ConfigValues`). `resource/catalog` exposes the resource types/fields/options the frontend's manual builder renders forms from. `Project.user` is nullable (rows created before this column existed have no owner) and set once at creation time; `GET /api/projects` lists the current user's past calculations (`calculation.Calculation`, one row per `/calculate` call, plus its per-resource `CalculationResourceCost` breakdown) as they were priced at save time — not recomputed against current pricing — via `CalculationService.listForUser()`. `GET /api/projects/{id}/calculate` re-fetches one saved calculation's full report (404s if it has no saved calculation or belongs to a different user) for 마이페이지's 계산 기록 detail view. `Calculation.diagramSnapshot`/`recommendationMetadata` are opaque jsonb pass-throughs (same treatment as `Resource.configuration`) — sent by the frontend at calculate time when calculating from the drag builder / an applied AI recommendation respectively, stored and echoed back verbatim, never interpreted by the backend.
- **`recommendation`** — Given service inputs (users, traffic, stage, type, free-text description), produces a recommended AWS architecture + cost. May call an AI provider for the architecture shape, but resulting costs always go through `CostEngine`.
- **`optimization`** — `OptimizationService` (called from `/api/projects/{id}/optimize`, credit-gated — see `CalculationController` vs this controller) tries whichever `CostOptimizationAdvisor` bean's `name()` matches `ai.optimization.provider`, same provider-select/fallback pattern as `recommendation`/`ai` below, falling back to `RuleBasedCostOptimizationAdvisor` (Graviton/RDS/Redis downsizing + a couple of static NAT Gateway/CloudFront tips, all priced through `CostEngine`) on failure or misconfiguration. Unlike the other AI features, an AI-backed advisor here may estimate its own savings figures — there's no single "recompute this the deterministic way" the model can defer to for an open-ended optimization suggestion — but the response's current/optimized totals are still summed server-side from the advisor's suggestions, never returned verbatim from the model. `ai.ExplanationService` calls `RuleBasedCostOptimizationAdvisor` directly (bypassing the AI provider and the credit charge) to fold a couple of free tips into its explanation text.
- **`ai`** — `ExplanationService` recomputes cost/optimization figures, tries whichever `AiExplainer` bean's `name()` matches `ai.explainer.provider` (`codex-cli` shells out to a locally installed `codex` CLI — local dev only, there's no `codex` binary in the deployed image; `claude-api`/`gemini-api` call the real Anthropic/Google APIs via `ClaudeApiClient`/`ANTHROPIC_API_KEY` and `GeminiApiClient`/`GEMINI_API_KEY`), and falls back to `RuleBasedAiExplainer` on any failure or misconfiguration — callers never see an AI error, only a lower-quality explanation. `recommendation.RecommendationService` and `optimization.OptimizationService` mirror the same provider-select/fallback pattern via `ArchitectureRecommendationAdvisor`/`CostOptimizationAdvisor`. Add a new implementation + it's picked up automatically; no other code changes.
- **`auth`** — Google + Kakao OAuth2 login (Kakao is not a Spring `CommonOAuth2Provider`, so it's registered manually in `application.properties`). Login issues a JWT (`JwtAuthenticationFilter`, `app.jwt.secret`); a `User` account can have multiple linked OAuth logins. Local email/password signup (`LocalAuthService`) only checks for a duplicate email — there's no SMTP/SES integration, so real email verification was never possible in a deployed environment; `User.emailVerified` is set only when an OAuth provider itself asserts the email is verified (used by `UserService` to safely link a first-time social login to an existing local account by email match). `SecurityConfig`: OAuth2 login uses a short-lived session for the handshake only, everything else is stateless JWT. Route rules matter by order (first match wins) — `/api/pricing/**` and `/api/admin/**` are `ROLE_ADMIN`-gated via `app.admin.emails`; a handful of routes require auth; the rest of `/api/**` is public.
- **`credit`** — Per-user credit ledger (`CreditTransaction`); signup grants a configurable bonus (`app.credit.signup-bonus`), usage-gated features spend credits.
- **`payment`** — Order/payment-gateway scaffolding (`Order`, `OrderStatus`, `PgProvider`).
- **`social`** — Now just `SiteVisit`/`SiteVisitController`/`SiteVisitService`: a public, no-auth hostname-keyed visit counter pinged by the frontend on every page load (`POST /api/site-visits/ping`). The X/Threads posting automation that used to live here (OAuth2 PKCE account connection, a manual publish queue, and a fully autonomous AI content + comment-reply pipeline) was removed in favor of posting via a separate local tool instead; nothing in this backend posts to social platforms anymore.

Full endpoint reference: `api.md`. Product/business context: `docs/PRODUCT_SPEC.md`, `docs/PROMOTION_STRATEGY.md`.

### Frontend: `frontend/src`

- Next.js App Router (`src/app`) on Next 16 / React 19 / Tailwind 4, TypeScript.
- `src/components/calculator` — the multi-step calculator wizard (`Calculator.tsx` orchestrates `StepProject` → `StepArchitecture` → `StepResult`), the AI/manual architecture builder (`VisualArchitectureBuilder.tsx`, `RecommendPanel.tsx`), and card export (`ShareCard.tsx`, `SharePdfModal.tsx`, `exportCard.ts` — uses `html2canvas-pro` + `jspdf` to export a shareable PDF; there is no link-sharing feature).
- `src/lib/api.ts` — typed fetch wrapper for the backend API.
- `frontend/CLAUDE.md` / `frontend/AGENTS.md` document the Next.js `next dev`-generated agent rules file; re-read it (from `node_modules/next/dist/docs/`) before assuming Next.js API/conventions match training data — this repo pins a newer, possibly-breaking Next.js version.

## Coding conventions

- Backend: Java 21, constructor injection (Lombok `@RequiredArgsConstructor`), classes grouped by feature package (`XController`, `XService`, `XRepository`, DTOs under `dto/`).
- Frontend: PascalCase components, camelCase functions/vars, component-local types unless shared.
- Use Java 21 for backend changes. Keep Spring classes in feature packages, with conventional
  names such as `ProjectController`, `ProjectService`, `ProjectRepository`, and DTOs under `dto`.
- jjwt uses the `gson` module (`jjwt-gson`), not `jjwt-jackson` — Spring Boot 4.1 ships Jackson 3 (`tools.jackson.*`), and pulling `jjwt-jackson` reintroduces a classic Jackson 2 conflict this project already hit once (see `pom.xml` and `PricingCatalog`).

## Security & config

Override via env vars for non-local environments: `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`, `SERVER_PORT`, `FRONTEND_BASE_URL`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`, `KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET`, `APP_ADMIN_EMAILS`, `AI_EXPLAINER_*`/`AI_RECOMMENDATION_*`/`AI_OPTIMIZATION_*` (set `AI_EXPLAINER_PROVIDER`/`AI_RECOMMENDATION_PROVIDER`/`AI_OPTIMIZATION_PROVIDER` to `claude-api` or `gemini-api` to use a real hosted API instead of the local-only `codex` CLI), `ANTHROPIC_API_KEY`/`ANTHROPIC_MODEL`, `GEMINI_API_KEY`/`GEMINI_MODEL`. `application.properties` ships non-blank placeholder OAuth client credentials so the app boots without real secrets; real login only fails lazily when a user actually attempts it. Never commit secrets, generated build output, or local DB volumes.

## Commits & PRs

Short, imperative, outcome-focused commit messages. PRs should call out database, environment variable, pricing data, or API contract changes, and include screenshots for visible UI changes.

Backend tests use Spring Boot and JUnit through Maven. Name test classes after the subject under
test, for example `CalculationServiceTests`, and place them in the matching package under
`backend/src/test/java`. Add focused service or controller tests when changing cost calculations,
persistence, validation, or API responses.
