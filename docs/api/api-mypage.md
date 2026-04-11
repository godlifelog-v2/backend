# API — 마이페이지 (§8)

> Base URL: `/api/v1`
> Base Path: `/api/v1/myPage/auth` (전체 JWT 필수)

---

## 8. 마이페이지 (MyPage)

### 8.1 계정 정보

| Method | Path | 설명 | Body |
|---|---|---|---|
| GET | `/myAccount` | 내 계정 정보 조회 | - |
| PATCH | `/myAccount/modify/personal` | 개인정보 수정 | `ModifyPersonalRequestDTO` |
| PATCH | `/myAccount/modify/nickName` | 닉네임 수정 | `ModifyNicknameRequestDTO` |
| PATCH | `/myAccount/modify/email` | 이메일 수정 (인증 필수) | `ModifyEmailRequestDTO` |
| PATCH | `/myAccount/modify/job-target` | 직업/목표 수정 | `ModifyJobTargetRequestDTO` |

### 8.2 보안

| Method | Path | 설명 | Body |
|---|---|---|---|
| PATCH | `/security/change/password` | 비밀번호 변경 | `GetUserPwRequestDTO` |
| PATCH | `/accountDeletion` | 회원 탈퇴 신청 | `GetUserPwRequestDTO` |
| PATCH | `/accountDeletion/cancel` | 회원 탈퇴 취소 | `GetUserPwRequestDTO` |

### 8.3 내 루틴

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/list/myPlan` | 내 루틴 페이징 조회 | Query: `page,size,status,target,job,sort,order,search` |
| PATCH | `/delete/plans` | 루틴 일괄 삭제 | Body: `List<Integer>` |
| PATCH | `/switch/isShared` | 공개/비공개 일괄 전환 | Body: `List<Integer>`, Query: `mode` |

### 8.4 챌린지/좋아요

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/list/myChall` | 참여 중인 챌린지 | - |
| GET | `/list/myLike` | 좋아요한 루틴 | Query: `page,size,target,job,order,search` |
| DELETE | `/delete/likes` | 좋아요 일괄 취소 | Query: `planIndexes` (List) |

---

## 응답 상세

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/myPage/auth/myAccount` | `{ code, message: MyPageUserInfosResponseDTO, status }` | 200/404 |
| PATCH `/myPage/auth/myAccount/modify/personal` | `{ code, message: String, status }` | 200/400/404/500 |
| PATCH `/myPage/auth/myAccount/modify/nickName` | `{ code, message: String, status }` | 200/400/404/500 |
| PATCH `/myPage/auth/myAccount/modify/email` | `{ code, message: String, status }` | 200/400/404/412/500 |
| PATCH `/myPage/auth/myAccount/modify/job-target` | `{ code, message: String, status }` | 200/400/404/500 |
| PATCH `/myPage/auth/security/change/password` | `{ code, message: String, status }` | 200/400/403/404/409/422/500 |
| PATCH `/myPage/auth/accountDeletion` | `{ code, message: String, status }` | 200/403/404/500 |
| PATCH `/myPage/auth/accountDeletion/cancel` | `{ code, message: String, status }` | 200/403/404/500 |
| GET `/myPage/auth/list/myPlan` | `{ plans: List<PlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| PATCH `/myPage/auth/delete/plans` | `{ code, message: String, status }` | 200/400/404/500 |
| PATCH `/myPage/auth/switch/isShared` | `{ code, message: String, status }` | 200/400/404/500 |
| GET `/myPage/auth/list/myChall` | `{ code, message: List<MyChallengeDTO>, status }` | 200 |
| GET `/myPage/auth/list/myLike` | `{ plans: List<PlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| DELETE `/myPage/auth/delete/likes` | `{ code, message: String, status }` | 200/400/404/500 |
