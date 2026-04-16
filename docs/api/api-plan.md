# API — 루틴/리스트 (§6~7)

> 관련 섹션: Plan(루틴), List(리스트 조회)

---

## 6. 루틴 (Plan)

### v1

**Base Path**: `/api/v1/plan`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/auth/write` | 루틴 작성 | Body: `PlanDTO` | ✅ JWT |
| GET | `/detail/{planIdx}` | 루틴 상세 조회 | Path: `planIdx`, Cookie: `viewed_plans` | ❌ |
| PATCH | `/auth/modify` | 루틴 수정 | Body: `PlanDTO` | ✅ JWT |
| PATCH | `/auth/delete/{planIdx}` | 루틴 삭제 | Path: `planIdx` | ✅ JWT |
| PATCH | `/auth/stopNgo` | 루틴 활성화/비활성화 | Body: `PlanRequestDTO` | ✅ JWT |
| POST | `/auth/likePlan/{planIdx}` | 루틴 추천 | Path: `planIdx` | ✅ JWT |
| GET | `/checkLike/{planIdx}` | 추천 여부 확인 | Path: `planIdx` | 선택 |
| DELETE | `/auth/unLikePlan/{planIdx}` | 추천 취소 | Path: `planIdx` | ✅ JWT |
| PATCH | `/auth/earlyComplete/{planIdx}` | 루틴 조기 완료 | Path: `planIdx` | ✅ JWT |
| PATCH | `/auth/addReview` | 루틴 후기 작성 | Body: `PlanRequestDTO` | ✅ JWT |
| PATCH | `/auth/modifyReview` | 루틴 후기 수정 | Body: `PlanRequestDTO` | ✅ JWT |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/plan/auth/write` | `{ code, message: String, status }` | 201/400/410/412/500 |
| GET `/plan/detail/{planIdx}` | `{ code, message: String, status, data: PlanDTO }` | 200/404/500 |
| PATCH `/plan/auth/modify` | `{ code, message: String, status }` | 200/400/403/404/409/410/500 |
| PATCH `/plan/auth/delete/{planIdx}` | `{ code, message: String, status }` | 200/403/404/410/500 |
| PATCH `/plan/auth/stopNgo` | `{ code, message: String, status }` | 200/400/403/404/410/500 |
| POST `/plan/auth/likePlan/{planIdx}` | `{ code, message: String, status }` | 200/404/409/410/500 |
| GET `/plan/checkLike/{planIdx}` | `{ code, message: String, status, data: Boolean }` | 200 |
| DELETE `/plan/auth/unLikePlan/{planIdx}` | `{ code, message: String, status }` | 200/404/410/500 |
| PATCH `/plan/auth/earlyComplete/{planIdx}` | `{ code, message: String, status }` | 200/403/404/409/410/412/500 |
| PATCH `/plan/auth/addReview` | `{ code, message: String, status }` | 200/403/404/409/410/412/500 |
| PATCH `/plan/auth/modifyReview` | `{ code, message: String, status }` | 200/403/404/410/412/500 |

---

### v2

**Base Path**: `/api/v2/plan`

> 인증 필요 엔드포인트: `/auth/**` 경로 — `Authorization: Bearer {accessToken}` 헤더 필수

#### 루틴 CRUD

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/detail/{planIdx}` | 루틴 상세 조회 (boolean 플래그, 읽기 전용 DTO) | Path: `planIdx`, Cookie: `viewed_plans` | ❌ |
| GET | `/auth/{planIdx}/extra` | 루틴 추가 정보 조회 (포크·날짜·카운트·완료·후기) | Path: `planIdx` | ✅ JWT |
| POST | `/auth` | 루틴 생성 (활동 미포함) | Body: `PlanCreateRequestV2` | ✅ JWT |
| PATCH | `/auth/{planIdx}` | 루틴 부분 수정 (null 필드 제외, planImp 제외) | Path: `planIdx`, Body: `PlanUpdateRequestV2` | ✅ JWT |
| DELETE | `/auth/{planIdx}` | 루틴 소프트 삭제 | Path: `planIdx` | ✅ JWT |

#### 활동 CRUD

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/auth/{planIdx}/activities` | 활동 생성 (1개 이상) | Path: `planIdx`, Body: `ActivityCreateRequestV2` | ✅ JWT |
| PATCH | `/auth/{planIdx}/activities/{activityIdx}` | 활동 부분 수정 (null 필드 제외, activityImp 제외) | Path: `planIdx`, `activityIdx`, Body: `ActivityUpdateRequestV2` | ✅ JWT |
| DELETE | `/auth/{planIdx}/activities/{activityIdx}` | 활동 소프트 삭제 | Path: `planIdx`, `activityIdx` | ✅ JWT |

#### 정렬 우선순위 일괄 수정

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| PATCH | `/auth/bulk-imp` | 루틴 정렬 우선순위 일괄 수정 | Body: `BulkPlanImpUpdateRequest` | ✅ JWT |
| PATCH | `/auth/{planIdx}/activities/bulk-imp` | 활동 정렬 우선순위 일괄 수정 | Path: `planIdx`, Body: `BulkActivityImpUpdateRequest` | ✅ JWT |

**v1 → v2 변경 내용**
- 단일 책임 원칙: `PlanDTO`(루틴+활동 혼합) → `PlanCreateRequestV2`(루틴 전용), `ActivityCreateRequestV2`(활동 전용) 분리
- 부분 수정 지원: 수정 DTO의 모든 필드가 nullable → 변경하려는 필드만 전송 가능
- RESTful 엔드포인트: 루틴과 활동 각각 독립 CRUD 엔드포인트
- 응답 DTO: `PlanDTO`(읽기/쓰기 공용) → `PlanDetailDTO`(조회 전용) 분리
- `isShared`, `isActive`, `isCompleted`, `isWriter` 타입: `int(0/1)` → **`boolean`**
- 불필요 필드 제거: `userIdx`, `targetIdx`, `jobIdx`, `lastExp`, `isDeleted`, `deleteActivityIdx`, `planSubMod`
- 신규 필드 추가: 루틴 `description`(간략 설명), `color`(헥사코드 색상)
- 활동 필드 변경: `description` 삭제 → `event`(알림 활성화), `duration`(예상 소요 시간 분) 추가
- 활동 응답 DTO: `ActivityDTO` → `ActivityV2DTO`

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/plan/detail/{planIdx}` | `{ code, message: String, status, data: PlanDetailDTO }` | 200/404/500 |
| GET `/plan/auth/{planIdx}/extra` | `{ code, message: String, status, data: PlanExtraInfoDTO }` | 200/404/500 |
| POST `/plan/auth` | `{ code, message: String, status }` | 201/410/412/500 |
| PATCH `/plan/auth/{planIdx}` | `{ code, message: String, status }` | 200/403/404/409/410/500 |
| DELETE `/plan/auth/{planIdx}` | `{ code, message: String, status }` | 200/403/404/410/500 |
| POST `/plan/auth/{planIdx}/activities` | `{ code, message: String, status }` | 201/403/404/410/500 |
| PATCH `/plan/auth/{planIdx}/activities/{activityIdx}` | `{ code, message: String, status }` | 200/403/404/410/500 |
| DELETE `/plan/auth/{planIdx}/activities/{activityIdx}` | `{ code, message: String, status }` | 200/403/404/410/500 |
| PATCH `/plan/auth/bulk-imp` | `{ code, message: String, status }` | 200/400/403/410/500 |
| PATCH `/plan/auth/{planIdx}/activities/bulk-imp` | `{ code, message: String, status }` | 200/400/403/404/410/500 |

**루틴 추가 정보 응답 예시 (`GET /auth/{planIdx}/extra`)**
```json
{
  "code": 200,
  "status": "success",
  "message": "루틴 추가 정보 조회 성공",
  "data": {
    "planIdx": 123,
    "forkIdx": 45,
    "forkTitle": "원본 루틴 제목",
    "planSubDate": "2026-01-01 10:00:00",
    "planSubMod": "2026-04-16 02:12:48",
    "planSubStart": "2026-01-05 00:00:00",
    "viewCount": 0,
    "forkCount": 2,
    "likeCount": 5,
    "isCompleted": false,
    "review": null
  }
}
```

**루틴 IMP 일괄 수정 요청 예시 (`PATCH /auth/bulk-imp`)**
```json
{
  "planImps": [
    { "planIdx": 10, "imp": 5 },
    { "planIdx": 11, "imp": 3 },
    { "planIdx": 12, "imp": 1 }
  ]
}
```

**활동 IMP 일괄 수정 요청 예시 (`PATCH /auth/{planIdx}/activities/bulk-imp`)**
```json
{
  "activityImps": [
    { "activityIdx": 1, "imp": 10 },
    { "activityIdx": 2, "imp": 7 },
    { "activityIdx": 3, "imp": 4 }
  ]
}
```

**루틴 생성 요청 예시 (`POST /auth`)**
```json
{
  "planTitle": "아침 루틴",
  "endTo": 30,
  "repeatDays": ["mon", "tue", "wed", "thu", "fri"],
  "targetIdx": 1,
  "jobIdx": 1,
  "planImp": 5,
  "isShared": 0,
  "isActive": 1,
  "description": "매일 아침을 활기차게 시작하는 루틴",
  "color": "#FF5733FF"
}
```

**루틴 부분 수정 요청 예시 (`PATCH /auth/{planIdx}`) — 제목만 변경**
```json
{
  "planTitle": "수정된 아침 루틴"
}
```

**활동 생성 요청 예시 (`POST /auth/{planIdx}/activities`)**
```json
{
  "activities": [
    {
      "activityName": "조깅 30분",
      "setTime": "07:00",
      "activityImp": 3,
      "event": true,
      "duration": 30
    },
    {
      "activityName": "스트레칭",
      "activityImp": 1,
      "event": false,
      "duration": 10
    }
  ]
}
```

**활동 부분 수정 요청 예시 (`PATCH /auth/{planIdx}/activities/{activityIdx}`) — 알림 시간만 변경**
```json
{
  "setTime": "08:00"
}
```

**응답 (`/detail/{planIdx}`) 예시**
```json
{
  "code": 200,
  "status": "success",
  "message": "루틴 상세 조회 성공",
  "data": {
    "planIdx": 1,
    "planTitle": "루틴 제목",
    "endTo": 30,
    "repeatDays": ["mon", "tue", "wed", "thu", "fri"],
    "planImp": 3,
    "certExp": 195,
    "verifyCount": 6,
    "viewCount": 247,
    "likeCount": 4,
    "forkCount": 1,
    "isShared": true,
    "isActive": true,
    "isCompleted": false,
    "isWriter": true,
    "fireState": true,
    "forked": false,
    "planSubDate": "2025-09-01 10:00:00",
    "planSubStart": "2025-09-05 05:00:00",
    "planSubEnd": null,
    "forkIdx": null,
    "forkTitle": null,
    "review": null,
    "activities": [ ... ],
    "jobCateDTO": { "idx": 2, "name": "직업명", "color": "#6A5ACD", "icon": "Book", "iconKey": "book" },
    "jobEtcCateDTO": null,
    "targetCateDTO": { "idx": 1, "name": "카테고리명", "color": "#FF9500", "icon": "Sunrise", "iconKey": "sunrise" },
    "fireInfo": { "lvIdx": 3, "fireName": "불꽃 3단계", "fireColor": "#FF6666", "fireEffect": null }
  }
}
```

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/plan/detail/{planIdx}` | `{ code, message: String, status, data: PlanDetailDTO }` | 200/404/500 |

---

## 7. 리스트 조회 (List)

### v1

**Base Path**: `/api/v1/list`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/auth/myPlans` | 내 루틴 목록 | - | ✅ JWT |
| GET | `/plan/{mode}` | 루틴 목록(필터) | Path: `mode`, Query: `page,size,status,target,job,sort,order,search` | ❌ |
| GET | `/auth/qna` | 내 1:1 문의 목록 | Query: `page,size,status,sort,order,search` | ✅ JWT |

`mode`: `all`, `popular`, `latest` 등

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/list/auth/myPlans` | `{ code, message: String, status, data: List<MyPlanDTO> }` | 200/204/500 |
| GET `/list/plan/{mode}` | `{ plans: List<PlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| GET `/list/auth/qna` | `{ qnaList: List<QnaListDTO>, totalPages, currentPage, pageSize }` | 200 |

---

### v2

**Base Path**: `/api/v2/list`

> 인증: `Authorization: Bearer {accessToken}` 헤더 사용

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/auth/myPlans` | 내 루틴 전체 목록 | - | ✅ JWT |
| GET | `/auth/todayPlans` | 오늘의 루틴 목록 | - | ✅ JWT |

**변경 내용 (v1 → v2)**
- `/auth/myPlans` 응답 DTO: `MyPlanDTO` → `MyPlanV2DTO`
  - `myPlanInfos` → `planInfos`, `myActivities` → `activities`, `targetInfos` → `targetCateDTO`, `fireInfos` → `fireInfo`, `jobDefaultInfos` → `jobCateDTO`, `jobAddedInfos` → `jobEtcCateDTO`
  - `isShared`, `isActive`: `int(0/1)` → **`boolean`**
  - `repeatDays`: `"mon,tue"` (String) → `["mon","tue"]` (Array)
- `/auth/todayPlans`: v2 신규 추가

**`/auth/myPlans` 응답 예시**
```json
{
  "code": 200,
  "status": "success",
  "message": "내 루틴 목록 조회 성공",
  "data": [
    {
      "planInfos": {
        "planIdx": 4,
        "planTitle": "루틴 제목",
        "endTo": 90,
        "planSubEnd": null,
        "isShared": true,
        "isActive": true,
        "planImp": 5,
        "certExp": 225,
        "repeatDays": ["mon","tue","wed","thu","fri"],
        "fireState": true,
        "color": null
      },
      "activities": [ ... ],
      "jobCateDTO": { "idx": 2, "name": "직업명", "color": "#6A5ACD", "icon": "Book", "iconKey": "book" },
      "jobEtcCateDTO": null,
      "targetCateDTO": { "idx": 5, "name": "카테고리명", "color": "#008080", "icon": "BookOpen", "iconKey": "book-open" },
      "fireInfo": { "lvIdx": 4, "fireName": "불꽃 4단계", "fireColor": "#FF3333", "fireEffect": null }
    }
  ]
}
```

**`/auth/todayPlans` 응답 예시**
```json
{ "code": 200, "status": "success", "message": "오늘의 루틴 조회 성공", "data": [ /* MyPlanV2DTO[] — 오늘 요일 해당 & IS_ACTIVE=1 루틴만 */ ] }
```

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/list/auth/myPlans` | `{ code, message: String, status, data: List<MyPlanV2DTO> }` | 200/204/500 |
| GET `/list/auth/todayPlans` | `{ code, message: String, status, data: List<MyPlanV2DTO> }` | 200/204/500 |

---

## Analysis (v2)

**Base Path**: `/api/v2/analysis`

> 인증: `Authorization: Bearer {accessToken}` 헤더 사용

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/auth/todayStats` | 오늘 요약 통계 | - | ✅ JWT |

> `completedToday`: 활동이 1개 이상이고 모든 활동이 오늘 인증된 루틴만 집계 (활동 0개 루틴 제외)

**`/auth/todayStats` 응답 예시**
```json
{
  "code": 200,
  "status": "success",
  "message": "통계 조회 성공",
  "data": {
    "totalToday": 3,
    "completedToday": 1,
    "combo": 13
  }
}
```

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/analysis/auth/todayStats` | `{ code, message: String, status, data: TodayStatsDTO }` | 200/500 |
