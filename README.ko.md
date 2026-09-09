# AWS Cost Calculator

**덜 추측하고, 서비스를 설명하면 결정론적인 AWS 월간 비용을 계산합니다.**

실행 중인 서비스: **[aws-costpilot.trade](https://aws-costpilot.trade)**

`catchmeif404`

**한국어** · [English](README.md)

## 왜 만들었나

AWS Pricing Calculator는 인스턴스 타입과 노드 수처럼 이미 아키텍처를 알고 있어야 사용할 수
있습니다. AWS Cost Calculator는 트래픽, 단계, 서비스 형태만으로 월간 비용을 추정합니다.
금액은 LLM이 추측하지 않고 실제 가격 엔진이 계산합니다.

## 주요 기능

- **서비스 기반 추정** — 트래픽, 단계, 서비스 종류로 추천 아키텍처와 월간 비용 계산
- **수동 빌더** — ECS, RDS, Redis, ALB, S3, Lambda, DynamoDB, CloudFront 등을 직접 조립
- **결정론적 비용 엔진** — 모든 금액은 AWS Price List 데이터와 `CostEngine`에서 계산
- **AI 비용 최적화** — Graviton, RDS, Redis, NAT Gateway, CloudFront 최적화 제안
- **쉬운 설명** — 계산 결과가 왜 그렇게 나왔는지 자연어로 설명
- **계산 기록** — 저장 당시 가격 기준으로 계산 결과를 다시 확인
- **카드 내보내기** — 계산 결과를 PDF 카드로 저장
- **언어 전환** — 한국어와 영어 UI를 상단 탭에서 전환

## 기술 스택

| 영역 | 기술 |
|---|---|
| 프론트엔드 | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| 백엔드 | Spring Boot, Java 21 |
| 데이터 | PostgreSQL 16 |
| 인증 | Google + Kakao OAuth2, 로컬 이메일/비밀번호, JWT |
| AI provider | 로컬 Codex CLI, Claude API, Gemini API |
| 가격 데이터 | AWS Price List Bulk API snapshot + DB |
| 배포 | Cloudflare Workers 프론트엔드 + Railway 백엔드 |

## 로컬 실행

```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

백엔드 테스트:

```bash
cd backend && ./mvnw test
cd backend && ./mvnw test -Dtest=PricingServiceTest
```

AWS 가격 snapshot 갱신:

```bash
backend/scripts/fetch_pricing.sh [region]
```

## 핵심 설계

AI는 가격을 계산하지 않습니다. 모든 비용 계산은 `CostEngine`이 담당하고, AI는 계산된 결과를
설명하거나 최적화 방안을 제안합니다. AI provider가 실패하거나 설정되지 않아도 규칙 기반
fallback이 동작합니다.

## 알려진 제한사항

- 저장된 계산을 공유하는 URL은 아직 없으며 PDF 내보내기만 제공합니다.
- 과거 계정 생성 전 데이터의 `Project.user`는 비어 있을 수 있습니다.
- 로컬 이메일 회원가입에는 실제 이메일 인증이 연결되어 있지 않습니다.
- 일부 AI 최적화 절감액은 모델 추정치일 수 있으며, 총액은 서버에서 다시 계산합니다.

## 문서

- [`api.md`](api.md)
- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md)
- [`docs/PROMOTION_STRATEGY.md`](docs/PROMOTION_STRATEGY.md)

## 라이선스

MIT

<div align="center">

Built by `catchmeif404` - building things nobody asked for.

</div>
