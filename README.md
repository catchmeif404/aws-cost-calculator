<div align="center">

# AWS Cost Calculator

**Guess less. Describe your service, get a deterministic AWS monthly cost estimate.**

Live demo: **[aws-costpilot.trade](https://aws-costpilot.trade)**

`catchmeif404`

</div>

---

## Why I built this

AWS's own Pricing Calculator makes you already know your architecture -
instance types, node counts, every dimension - before it tells you anything.
Most people asking "what would this cost me?" don't have that yet; they have
a rough idea of traffic, a stage (MVP vs. production), and a service shape.

AWS Cost Calculator takes that instead. Describe a service in plain terms or
assemble AWS resources by hand, and get a monthly cost estimate (USD + KRW)
that a real pricing engine computed - never a number an LLM guessed.

## What it does

- **Service-based estimate** - describe traffic, stage, and service type;
  get a recommended AWS architecture and its monthly cost.
- **Manual builder** - drag together ECS, RDS, Redis, ALB, S3, Lambda,
  DynamoDB, CloudFront, and more, and price the exact stack yourself.
- **Deterministic cost engine** - every dollar figure comes from
  `CostEngine`, one method per resource type, driven by real AWS Price List
  data. An AI provider is only ever asked to explain a price already
  computed, never to compute one.
- **AI cost optimization (credit-gated)** - on-demand suggestions
  (Graviton/RDS/Redis downsizing, NAT Gateway and CloudFront tips), with a
  rule-based advisor as the always-available fallback.
- **Plain-language explanation** - an optional AI-generated summary of why
  the estimate looks the way it does.
- **Calculation history** - saved calculations are re-viewable exactly as
  they were priced, not recomputed against today's pricing.
- **Shareable card export** - export a calculation as a PDF card.

## Stack

| | |
|---|---|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| Backend | Spring Boot, Java 21 |
| Data | PostgreSQL 16 |
| Auth | Google + Kakao OAuth2, local email/password, stateless JWT |
| AI providers | `codex` CLI (local dev), Claude API, Gemini API - pluggable per feature |
| Pricing source | AWS Price List Bulk API snapshots, DB-editable at runtime |

## Architecture

```text
User input (service description or manual resource picks)
    |
    v
Backend -- resolve region pricing --------> PricingService
    |                                          |
    |                                    DB (aws_price_dimensions)
    |                                    falls back to classpath JSON snapshot
    v
CostEngine -- one method per ResourceType --> deterministic USD/KRW estimate
    |
    +-- optional: AiExplainer   -- explains the number, never computes it
    +-- optional: CostOptimizationAdvisor -- credit-gated savings suggestions
    +-- optional: ArchitectureRecommendationAdvisor -- AI-shaped architecture,
                                                        still priced by CostEngine

Saved calculation --> Calculation + CalculationResourceCost (priced-at-save-time snapshot)
```

Every AI-backed feature (`ai`, `recommendation`, `optimization`) follows the
same pattern: try the configured provider bean by name, fall back to a
rule-based implementation on any failure or misconfiguration. Callers never
see an AI error, only a lower-quality result.

## Running it locally

```bash
docker compose up -d              # PostgreSQL 16 (db: aws_calculator)

cd backend && ./mvnw spring-boot:run   # API on :8080
cd frontend && npm install && npm run dev   # frontend on :3000
```

To refresh AWS pricing snapshots:

```bash
backend/scripts/fetch_pricing.sh [region]   # requires curl, jq
```

then restart the backend.

Running the backend test suite:

```bash
cd backend && ./mvnw test
cd backend && ./mvnw test -Dtest=PricingServiceTest   # single class
```

No frontend test framework is configured; for UI changes run
`npm run lint` and `npm run build`.

For real OAuth login, AI providers, or a non-local database, set the
relevant environment variables (`DB_*`, `JWT_SECRET`, `GOOGLE_CLIENT_*`,
`KAKAO_CLIENT_*`, `APP_ADMIN_EMAILS`, `AI_EXPLAINER_PROVIDER` /
`AI_RECOMMENDATION_PROVIDER` / `AI_OPTIMIZATION_PROVIDER` set to `claude-api`
or `gemini-api`, `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`). The app boots with
non-blank placeholder credentials otherwise; real login just fails lazily
when someone actually tries it.

## Known issues

- No link-sharing for a saved calculation - export is PDF-only.
- `Project.user` is nullable; rows created before accounts existed have no
  owner and won't show up in anyone's calculation history.
- Local email/password signup has no SMTP/SES integration, so
  `emailVerified` is only ever set by an OAuth provider, never by a real
  verification email.
- AI optimization suggestions may include model-estimated savings figures;
  only the current/optimized totals are re-summed server-side, not the
  individual line items.

## Roadmap

- Shareable calculation URLs with OG image previews
- Budget-in / architecture-out reverse recommendation (pick a budget, get a
  preset architecture that fits it)
- Phase 2: connect a real AWS account (read-only, cross-account IAM role) and
  apply the same optimization rules to actual spend

## Docs

- [`api.md`](api.md) - full API reference
- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) - product/business context
- [`docs/PROMOTION_STRATEGY.md`](docs/PROMOTION_STRATEGY.md) - promotion strategy

## License

MIT

---

<div align="center">

Built by `catchmeif404` - building things nobody asked for.

</div>
