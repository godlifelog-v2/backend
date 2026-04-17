# GodLifeLog v2 Backend - REST API 레퍼런스

> 작성일: 2026-04-07 / 최종 수정: 2026-04-13
> 기준 브랜치: `dev`
> Base URL: `/api/v1`

WebSocket(`AdminChatController`, `QnaAdminController`) 및 테스트 컨트롤러는 제외.

---

## 도메인별 상세 문서

| 도메인 | 포함 섹션 | 참조 파일 |
|---|---|---|
| 인증/공통 | §1 Common, §2 JWT, §3 User, §4 Verify, §5 Category | @.claude/docs/api/api-auth.md |
| 루틴/리스트 | §6 Plan (v1+v2), §7 List (v1+v2) | @.claude/docs/api/api-plan.md |
| 마이페이지 | §8 MyPage | @.claude/docs/api/api-mypage.md |
| 챌린지 | §9 Challenge | @.claude/docs/api/api-challenge.md |
| 콘텐츠/유틸 | §10 FAQ, §11 Notice, §12 QNA, §13 Report, §14 Search, §15 Image, §16 Analysis | @.claude/docs/api/api-content.md |
| 관리자 | §17~23 Admin 전체 | @.claude/docs/api/api-admin.md |
| DTO 정의 | PlanDTO, UserDTO, QnaDTO 등 공통 DTO + v2 Request/Response DTO | @.claude/docs/api/api-dto.md |

---

## v2 엔드포인트 요약

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/v2/plan/detail/{planIdx}` | 루틴 상세 조회 (boolean 플래그) | ❌ |
| **POST** | **`/api/v2/plan/auth`** | **루틴 생성** (활동 미포함) | ✅ |
| **POST** | **`/api/v2/plan/auth/{sourcePlanIdx}/fork`** | **루틴 포크 생성** (공개 루틴만) | ✅ |
| **PATCH** | **`/api/v2/plan/auth/{planIdx}`** | **루틴 부분 수정** | ✅ |
| **DELETE** | **`/api/v2/plan/auth/{planIdx}`** | **루틴 삭제** | ✅ |
| **POST** | **`/api/v2/plan/auth/{planIdx}/activities`** | **활동 생성** | ✅ |
| **PATCH** | **`/api/v2/plan/auth/{planIdx}/activities/{activityIdx}`** | **활동 부분 수정** | ✅ |
| **DELETE** | **`/api/v2/plan/auth/{planIdx}/activities/{activityIdx}`** | **활동 삭제** | ✅ |
| GET | `/api/v2/list/auth/myPlans` | 내 루틴 전체 목록 | ✅ |
| GET | `/api/v2/list/auth/todayPlans` | 오늘의 루틴 목록 | ✅ |
| GET | `/api/v2/analysis/auth/todayStats` | 오늘 요약 통계 | ✅ |

> **굵은 항목**: 2026-04-13 신규 추가 (루틴/활동 단일 책임 분리 + 부분 수정 지원)

---

## 공통 인증/응답 규약

### 인증 헤더

```
Authorization: Bearer {accessToken}
```

- `/auth/` 경로 포함 → **JWT 필수**
- `/admin/` 경로 포함 → **관리자 권한 JWT 필수**
- `@RequestHeader(required=false)` 표시 → 비로그인 호출 가능, 토큰 유무에 따라 응답 분기

### 공통 응답 포맷

```json
// 패턴 A — 단일 리소스 / 목록 (data 필드에 DTO 또는 배열 포함)
{ "code": 200, "status": "success", "message": "조회 성공", "data": { /* DTO */ } }

// 패턴 B — 페이징 목록 (루트 레벨에 페이징 정보 포함, 엔드포인트마다 루트 키 이름 상이)
{ "plans": [ /* DTO[] */ ], "totalPages": 5, "currentPage": 1, "pageSize": 10 }

// 패턴 C — 단순 결과 (data 없음, message만 문자열)
{ "code": 200, "status": "success", "message": "처리 완료" }
```

> **필드 규칙**: `message`는 항상 **문자열**. 리소스 데이터(DTO, 배열 등)는 `data` 필드에 담김.
> 페이징 응답은 엔드포인트별로 루트 키 이름이 다름 (`plans`, `challenges`, `qnaList` 등).

### 페이징 파라미터(공통)

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | int | 1 | 페이지 번호 |
| `size` | int | 10 | 페이지 크기 |
| `sort` | string | - | 정렬 컬럼 |
| `order` | string | desc | 정렬 방향 (`asc`/`desc`) |

### HTTP 상태코드 의미

| 코드 | 의미 |
|---|---|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 성공(데이터 없음) |
| 400 | 잘못된 요청 / 유효성 실패 |
| 401 | 인증 실패 / 토큰 없음 |
| 403 | 권한 없음 / 작성자 아님 |
| 404 | 리소스 없음 |
| 409 | 중복 / 충돌 |
| 410 | 탈퇴 유저 |
| 412 | 선행 조건 미충족 (이메일 인증 등) |
| 422 | 처리 불가능 (필드 누락) |
| 500 | 서버 오류 |

---

## 보안 / 아키텍처 특징 요약

1. **JWT Bearer 인증**: `Authorization: Bearer {token}` 헤더 사용
2. **선택적 인증**: 일부 API는 비로그인도 호출 가능, 토큰 유무에 따라 응답 분기
3. **이메일 인증**: Redis(`EMAIL_VERIFIED` 키) 기반 1회성 인증 (유효 10분)
4. **관리자 권한**: `/admin/` 경로 또는 `@PreAuthorize`로 분리
5. **쿠키 추적**: 검색 기록, 루틴 조회수 중복 방지에 사용
6. **WebSocket**: `AdminChatController`, `QnaAdminController`는 본 문서 범위 외 (별도 STOMP 채널)
