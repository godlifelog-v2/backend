# API — 관리자 (§17~23)

> Base URL: `/api/v1`
> 모든 관리자 API: Admin 권한 JWT 필수

---

## 17. 관리자 - 사용자 관리

**Base Path**: `/api/v1/admin/users`

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/` | 유저 목록(페이징) | Query: `page,size` |
| PATCH | `/ban/{userIdx}` | 유저 정지/복구 | Path: `userIdx` |
| GET | `/authority/list` | 권한 목록 조회 | - |
| GET | `/authority/adminList` | 관리자 명단 | - |
| GET | `/authority/{authorityIdx}` | 권한별 유저 조회 | Path: `authorityIdx` |
| PATCH | `/authority/updateAuth/{userIdx}` | 유저 권한 변경 | Path: `userIdx`, Body: `{authorityIdx: int}` |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/users/` | `{ code, status, message: String, data: { users: List<AdminUserDTO>, page, size, count, total, totalPages } }` | 200/204/500 |
| PATCH `/admin/users/ban/{userIdx}` | `{ code, status, message: String }` | 200/404/500 |
| GET `/admin/users/authority/list` | `{ code, status, message: String, data: { authorityList: List<AuthorityCateDTO> } }` | 200/404/500 |
| GET `/admin/users/authority/adminList` | `{ code, status, message: String, data: { userList: List<AdminListDTO> } }` | 200/404/500 |
| GET `/admin/users/authority/{authorityIdx}` | `{ code, status, message: String, data: { userList: List<AdminUserDTO> } }` | 200/404/500 |
| PATCH `/admin/users/authority/updateAuth/{userIdx}` | `{ code, status, message: String }` | 200/404/500 |

---

## 18. 관리자 - 챌린지 관리

**Base Path**: `/api/v1/admin/challenges`

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/latest` | 챌린지 목록(필터) | Query: `challState,challCategoryIdx,visibilityType,challengeType,onlyEnded,page,size` |
| GET | `/detail/{challIdx}` | 챌린지 상세 | Path: `challIdx` |
| POST | `/create` | 챌린지 생성 | Body: `ChallengeDTO` |
| PATCH | `/modify` | 챌린지 수정 | Body: `ChallengeDTO` |
| PATCH | `/delete` | 챌린지 삭제 | Body: `ChallengeDTO` |
| POST | `/visibility/{challIdx}` | 공개/비공개 변경 | Path + Query: `visibilityType` |
| POST | `/type/{challIdx}` | 챌린지 타입 변경 | Path + Query: `challengeType` |
| PUT | `/earlyFinish/{challIdx}` | 챌린지 조기 종료 | Path: `challIdx` |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/challenges/latest` | `{ code, status, message: String, data: { challenges: List<ChallengeDTO>, totalPages, currentPage, pageSize } }` | 200 |
| GET `/admin/challenges/detail/{challIdx}` | `{ code, status, message: String, data: ChallengeDTO }` | 200 |
| POST `/admin/challenges/create` | `{ code, status, message: String }` | 201/400/500 |
| PATCH `/admin/challenges/modify` | `{ code, status, message: String }` | 200/400/404/500 |
| PATCH `/admin/challenges/delete` | `{ code, status, message: String }` | 200/400/404/500 |
| POST `/admin/challenges/visibility/{challIdx}` | `{ code, status, message: String }` | 200/404/500 |
| POST `/admin/challenges/type/{challIdx}` | `{ code, status, message: String }` | 200/404/500 |
| PUT `/admin/challenges/earlyFinish/{challIdx}` | `{ code, status, message: String }` | 200/400 |

---

## 19. 관리자 - 콘텐츠 관리

**Base Path**: `/api/v1/admin/compContent`

모두 `{ code, status, message: String }` 형태의 단순 응답.

### 목표 카테고리
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/targetCategory` | `TargetCateDTO` | 201/409/500 |
| PATCH | `/targetCategory/{targetCateIdx}` | `TargetCateDTO` | 200/404/409/500 |
| DELETE | `/targetCategory/{idx}` | - | 200/404/500 |

### 직업 카테고리
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/jobCategory` | `JobCateDTO` | 200/400/500 |
| PATCH | `/jobCategory/{jobIdx}` | `JobCateDTO` | 200/404/500 |
| DELETE | `/jobCategory/{jobIdx}` | - | 200/404/500 |

### 불꽃 등급
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/fire` | `FireDTO` | 201/409/500 |
| PATCH | `/fire/{lvIdx}` | `FireDTO` | 200/404/500 |
| DELETE | `/fire/{lvIdx}` | - | 200/404/500 |

### 챌린지 카테고리
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/challCate` | `ChallengeCateDTO` | 201/409/500 |
| PATCH | `/challCate/{challCategoryIdx}` | `ChallengeCateDTO` | 200/404/500 |
| DELETE | `/challCate/{challCategoryIdx}` | - | 200/404/500 |

---

## 20. 관리자 - 시스템 관리

**Base Path**: `/api/v1/admin/compSystem`

기본 형태는 `{ code, status, message: String }`. 일부 삭제 API는 충돌 시 data 필드에 목록도 반환.

### FAQ 카테고리
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/faqCategory` | `FaqCateDTO` | 201/409/500 |
| PATCH | `/faqCategory/{faqCategoryIdx}` | `FaqCateDTO` | 200/404/500 |
| DELETE | `/faqCategory/{faqCategoryIdx}` | - | 200/400/500 (`faqList?` 포함 가능) |

### QNA 카테고리
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/qnaCategory` | `QnaCateDTO` | 201/409/500 |
| PATCH | `/qnaCategory/{qnaCategoryIdx}` | `QnaCateDTO` | 200/404/500 |
| DELETE | `/qnaCategory/{qnaCategoryIdx}` | - | 200/409/500 (`data?` 포함 가능) |

### TopMenu
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/topMenu` | `TopCateDTO` | 201/409/500 |
| PATCH | `/topMenu/{topIdx}` | `TopCateDTO` | 200/404/409/500 |
| DELETE | `/topMenu/{topIdx}` | - | 200/404/409/500 |

### 아이콘
| Method | Path | Body | 상태 |
|---|---|---|---|
| POST | `/icon` | `IconDTO` | 201/409/500 |
| PATCH | `/icon/{iconKey}` | `IconDTO` | 200/404/500 |
| DELETE | `/icon/{iconKey}` | - | 200/404/500 |

---

## 21. 관리자 - 루틴 관리

**Base Path**: `/api/v1/admin/plans`

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/` | 관리자 루틴 목록 | Query: `page,size` |
| GET | `/{targetIdx}` | 카테고리별 루틴 | Path: `targetIdx`, Query: `page,size` |
| GET | `/all` | 전체 루틴 조회 | Query: `page,size` |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/plans/` | `{ code, status, message: String, data: { plans: List<CustomAdminPlanListDTO>, totalPages, currentPage, pageSize } }` | 200/204 |
| GET `/admin/plans/{targetIdx}` | `{ code, status, message: String, data: { plans: List<CustomAdminPlanListDTO>, totalPages, currentPage, pageSize } }` | 200/204 |
| GET `/admin/plans/all` | `{ code, status, message: String, data: { plans: List<CustomAdminPlanListDTO>, totalPages, currentPage, pageSize } }` | 200/204 |

---

## 22. 관리자 - 신고 관리

**Base Path**: `/api/v1/admin/report`

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/userReport` | 유저 신고 목록 | Query: `page,size,status?` |
| POST | `/userReportState` | 유저 신고 상태 변경 | Query: `userReportIdx,isApproved` |
| GET | `/planReport` | 루틴 신고 목록 | Query: `page,size,status?` |
| PATCH | `/plans/{planIdx}/reports/{planReportIdx}/status` | 루틴 신고 상태 변경 | Path + Body: `PlanReportDTO` |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/report/userReport` | `{ code, status, message: String, data: { total, reports: List<UserReportDTO>, page, size } }` | 200 |
| POST `/admin/report/userReportState` | `{ code, status, message: String }` | 200/400/500 |
| GET `/admin/report/planReport` | `{ code, status, message: String, data: { page, size, total, list: List<PlanReportDTO> } }` | 200 |
| PATCH `/admin/report/plans/{planIdx}/reports/{planReportIdx}/status` | `{ code, status, message: String }` | 200/403/500 |

---

## 23. 관리자 - 서비스 센터

**Base Path**: `/api/v1/service/admin`

| Method | Path | 설명 |
|---|---|---|
| GET | `/get/status` | 관리자 온라인 상태 조회 |
| PATCH | `/switch/status` | 관리자 온라인 상태 전환 |
| POST | `/autoMatch/wakeUp` | QNA 자동 매칭 강제 활성화 |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/service/admin/get/status` | `{ code, status, message: String, data: String }` | 200 |
| PATCH `/service/admin/switch/status` | `{ code, status, message: String, data: String }` | 200 |
| POST `/service/admin/autoMatch/wakeUp` | `{ code, status, message: String }` | 200 |
