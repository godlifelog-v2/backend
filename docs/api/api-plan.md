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
| GET `/plan/detail/{planIdx}` | `{ code, message: PlanDTO, status }` | 200/404/500 |
| PATCH `/plan/auth/modify` | `{ code, message: String, status }` | 200/400/403/404/409/410/500 |
| PATCH `/plan/auth/delete/{planIdx}` | `{ code, message: String, status }` | 200/403/404/410/500 |
| PATCH `/plan/auth/stopNgo` | `{ code, message: String, status }` | 200/400/403/404/410/500 |
| POST `/plan/auth/likePlan/{planIdx}` | `{ code, message: String, status }` | 200/404/409/410/500 |
| GET `/plan/checkLike/{planIdx}` | `{ code, message: Boolean, status }` | 200 |
| DELETE `/plan/auth/unLikePlan/{planIdx}` | `{ code, message: String, status }` | 200/404/410/500 |
| PATCH `/plan/auth/earlyComplete/{planIdx}` | `{ code, message: String, status }` | 200/403/404/409/410/412/500 |
| PATCH `/plan/auth/addReview` | `{ code, message: String, status }` | 200/403/404/409/410/412/500 |
| PATCH `/plan/auth/modifyReview` | `{ code, message: String, status }` | 200/403/404/410/412/500 |

---

### v2

**Base Path**: `/api/v2/plan`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/detail/{planIdx}` | 루틴 상세 조회 (boolean 플래그, 읽기 전용 DTO) | Path: `planIdx`, Cookie: `viewed_plans` | ❌ |

**변경 내용 (v1 → v2)**
- 응답 DTO: `PlanDTO`(읽기/쓰기 공용) → `PlanDetailDTO`(조회 전용) 분리
- `isShared`, `isActive`, `isCompleted`, `isWriter` 타입: `int(0/1)` → **`boolean`**
- 불필요 필드 제거: `userIdx`, `targetIdx`, `jobIdx`, `lastExp`, `isDeleted`, `deleteActivityIdx`, `planSubMod`

**응답 (`/detail/{planIdx}`) 예시**
```json
{
  "code": 200,
  "message": {
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
  },
  "status": "success"
}
```

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/plan/detail/{planIdx}` | `{ code, message: PlanDetailDTO, status }` | 200/404/500 |

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
| GET `/list/auth/myPlans` | `{ code, message: List<MyPlanDTO>, status }` | 200/204/500 |
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
| GET | `/auth/todayStats` | 오늘 요약 통계 | - | ✅ JWT |

**변경 내용 (v1 → v2)**
- `/auth/myPlans` 응답 DTO: `MyPlanDTO` → `MyPlanV2DTO`
  - `myPlanInfos` → `planInfos`, `myActivities` → `activities`, `targetInfos` → `targetCateDTO`, `fireInfos` → `fireInfo`, `jobDefaultInfos` → `jobCateDTO`, `jobAddedInfos` → `jobEtcCateDTO`
  - `isShared`, `isActive`: `int(0/1)` → **`boolean`**
  - `repeatDays`: `"mon,tue"` (String) → `["mon","tue"]` (Array)
- `/auth/todayPlans`, `/auth/todayStats`: v2 신규 추가

**`/auth/myPlans` 응답 예시**
```json
{
  "code": 200,
  "message": [
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
  ],
  "status": "success"
}
```

**`/auth/todayPlans` 응답 예시**
```json
{ "code": 200, "message": [ /* MyPlanV2DTO[] — 오늘 요일 해당 & IS_ACTIVE=1 루틴만 */ ], "status": "success" }
```

**`/auth/todayStats` 응답 예시**
```json
{
  "code": 200,
  "message": {
    "totalToday": 3,
    "completedToday": 1,
    "combo": 13
  },
  "status": "success"
}
```

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/list/auth/myPlans` | `{ code, message: List<MyPlanV2DTO>, status }` | 200/204/500 |
| GET `/list/auth/todayPlans` | `{ code, message: List<MyPlanV2DTO>, status }` | 200/204/500 |
| GET `/list/auth/todayStats` | `{ code, message: TodayStatsDTO, status }` | 200/500 |
