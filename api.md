# API 문서

## POST /api/architecture-recommendations

서비스 정보를 기반으로 AWS 구성과 예상 월 비용을 추천합니다. 비용 계산은 백엔드 가격 엔진이 수행하며, 가격 모델이 아직 없는 항목은 `additionalRecommendations`로 분리해 반환합니다.

### Request

```json
{
  "serviceName": "My Awesome API",
  "monthlyUsers": 10000,
  "requestsPerUser": 100,
  "busyTrafficLevel": "two_to_three_times",
  "serviceStage": "mvp",
  "serviceType": "web_api",
  "serviceDescription": "사용자 도메인이 필요하고 로그인 기능이 있습니다. REST API 중심의 서비스이며, 초기에는 관계형 데이터베이스를 사용합니다.",
  "region": "ap-northeast-2"
}
```

## 소셜 홍보 자동화 API

X와 Threads 홍보 게시물 큐, 댓글 초안 승인 큐를 관리합니다. 모든 요청은 `ADMIN` 권한 JWT가 필요합니다. 관리자 계정은 `APP_ADMIN_EMAILS=admin@example.com,owner@example.com`처럼 이메일 목록으로 지정합니다. X 자동 게시는 `SOCIAL_PUBLISHER_PROVIDER=x`, `X_CLIENT_ID`, `X_CLIENT_SECRET`, `X_REDIRECT_URI` 설정 후 사용할 수 있습니다.

### GET /api/social/accounts

관리자가 연결한 소셜 계정 목록을 조회합니다.

### POST /api/social/accounts/x/connect

X OAuth2 PKCE 연결 URL을 생성합니다. 응답의 `authorizationUrl`로 이동하면 X 승인 화면이 열립니다.

### GET /api/social/posts

내 게시물 큐 최근 50개를 조회합니다.

### POST /api/social/posts

게시물 초안 또는 예약 게시물을 저장합니다. `scheduledAt`이 없으면 초안, 있으면 예약 상태가 됩니다.

```json
{
  "platform": "X",
  "content": "AWS 비용을 먼저 추정해보세요.",
  "targetUrl": "https://example.com",
  "scheduledAt": "2026-09-03T01:00:00Z"
}
```

### POST /api/social/posts/{postId}/publish

게시물을 즉시 게시합니다. 현재 퍼블리셔가 비활성화되어 있으면 503을 반환합니다.

### POST /api/social/posts/{postId}/cancel

게시 전 게시물을 취소합니다.

### GET /api/social/comment-drafts

내 댓글 초안 최근 50개를 조회합니다.

### POST /api/social/comment-drafts

댓글 초안을 저장합니다. 댓글은 초안 저장 후 승인 단계를 거쳐 게시합니다.

```json
{
  "platform": "THREADS",
  "sourceUrl": "https://www.threads.net/@example/post/123",
  "replyContent": "서비스 구조에 따라 월 비용부터 추정해보는 것도 좋습니다."
}
```

### POST /api/social/comment-drafts/{draftId}/approve

댓글 초안을 승인합니다.

### POST /api/social/comment-drafts/{draftId}/reject

댓글 초안을 반려합니다.

### POST /api/social/comment-drafts/{draftId}/publish

승인된 댓글을 게시합니다. 현재 퍼블리셔가 비활성화되어 있으면 503을 반환합니다.

## 가격 카탈로그 API

계산 엔진은 `aws_price_dimensions` 테이블의 region별 가격 dimension을 우선 사용합니다. 앱 시작 시 기존 `classpath:/pricing/*-pricing.json` 스냅샷을 seed source로 사용해 누락된 가격 항목을 DB에 적재합니다.

### GET /api/pricing/dimensions

관리자 전용 가격 dimension 조회 API입니다. `region` 쿼리로 특정 리전만 조회할 수 있습니다.

```http
GET /api/pricing/dimensions?region=ap-northeast-2
```

### 주요 값

- `serviceType`: `web_api`, `mobile_app`, `static_site`, `batch_worker`, `internal_admin`
- `busyTrafficLevel`: `similar`, `two_to_three_times`, `five_plus_times`, `unknown`
- `serviceStage`: `toy`, `mvp`, `production`, `critical`

### Response

```json
{
  "tierName": "스타트업",
  "description": "초기 서비스 오픈 수준",
  "estimatedMonthlyRequests": 1000000,
  "analysisMode": "AI 추천",
  "recommendationReason": "월 1000000건 정도의 요청...",
  "provider": "codex-cli",
  "aiGenerated": true,
  "totalMonthlyCostUsd": 123.45,
  "totalMonthlyCostKrw": 166658,
  "resources": [
    {
      "type": "ECS",
      "monthlyCost": 24.1
    }
  ],
  "additionalRecommendations": [
    "사용자 도메인이 필요하므로 Route 53 Hosted Zone과 DNS 레코드 구성을 권장합니다."
  ],
  "note": "비용은 가격 엔진으로 계산하고..."
}
```
