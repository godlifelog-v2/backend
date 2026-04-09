# GodLifeLog v2 Backend - REST API 레퍼런스

> 작성일: 2026-04-07
> 기준 브랜치: `dev`
> Base URL: `/api/v1`

본 문서는 현재 구현된 모든 REST API 엔드포인트를 도메인별로 정리한 것입니다.
WebSocket 컨트롤러(`AdminChatController`, `QnaAdminController`) 및 테스트 컨트롤러(`TestController`, `RedisController`)는 제외되었습니다.

---

## 📑 목차

### 공개 / 인증
1. [공통 (Common)](#1-공통-common)
2. [JWT 토큰 관리](#2-jwt-토큰-관리)
3. [사용자 (User)](#3-사용자-user)
4. [인증/검증 (Verify)](#4-인증검증-verify)
5. [카테고리 (Category)](#5-카테고리-category)

### 사용자 도메인
6. [루틴 (Plan)](#6-루틴-plan)
7. [리스트 조회 (List)](#7-리스트-조회-list)
8. [마이페이지 (MyPage)](#8-마이페이지-mypage)
9. [챌린지 (Challenge)](#9-챌린지-challenge)
10. [FAQ](#10-faq)
11. [공지사항 (Notice)](#11-공지사항-notice)
12. [QNA 1:1 문의](#12-qna-11-문의)
13. [신고 (Report)](#13-신고-report)
14. [검색 기록 (Search)](#14-검색-기록-search)
15. [이미지 업로드 (Image)](#15-이미지-업로드-image)
16. [분석 (Analysis)](#16-분석-analysis)

### 관리자(Admin) 도메인
17. [관리자 - 사용자 관리](#17-관리자---사용자-관리)
18. [관리자 - 챌린지 관리](#18-관리자---챌린지-관리)
19. [관리자 - 콘텐츠 관리](#19-관리자---콘텐츠-관리)
20. [관리자 - 시스템 관리](#20-관리자---시스템-관리)
21. [관리자 - 루틴 관리](#21-관리자---루틴-관리)
22. [관리자 - 신고 관리](#22-관리자---신고-관리)
23. [관리자 - 서비스 센터](#23-관리자---서비스-센터)

### 부록
- [공통 인증/응답 규약](#-공통-인증응답-규약)
- [📦 응답 데이터 상세](#-응답-데이터-상세)
- [📐 핵심 DTO 필드 정의](#-핵심-dto-필드-정의)

---

## 📌 공통 인증/응답 규약

### 인증 헤더
JWT 인증이 필요한 엔드포인트는 다음 헤더를 포함해야 합니다.

```
Authorization: Bearer {accessToken}
```

- 경로에 `/auth/`가 포함된 엔드포인트는 **JWT 필수**입니다.
- 경로에 `/admin/`이 포함된 엔드포인트는 **관리자 권한 JWT 필수**입니다.
- `@RequestHeader(required=false)`로 표시된 경우 비로그인 사용자도 호출 가능하며, 토큰 유무에 따라 응답이 달라집니다.

### 공통 응답 포맷
대부분의 API는 다음과 같은 `Map<String, Object>` 형태로 응답합니다.

```json
{
  "status": 200,
  "message": "성공 메시지",
  "data": { ... }
}
```

오류 발생 시 HTTP 상태 코드와 함께 `message` 필드에 사유가 포함됩니다.

### 페이징 파라미터(공통)
| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | int | 1 | 페이지 번호 |
| `size` | int | 10 | 페이지 크기 |
| `sort` | string | - | 정렬 컬럼 |
| `order` | string | desc | 정렬 방향 (`asc`/`desc`) |

---

## 1. 공통 (Common)

### HealthCheckController

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/v1/hc` | 헬스 체크 | ❌ |
| GET | `/api/v1/env` | 환경 변수 조회 | ❌ |

**응답 예시 (`/hc`)**
```json
{ "status": "UP" }
```

### HomeController

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/` | 홈 페이지(HTML) 렌더링 | ❌ |

---

## 2. JWT 토큰 관리

### ReissueController

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/v1/reissue` | Refresh 토큰으로 Access 토큰 재발급 | ❌ (Refresh Cookie 필요) |

**요청**
- Header: `Cookie: refresh={refreshToken}`

**응답**
- Header: `Authorization: Bearer {newAccessToken}`
- Body: 토큰 재발급 결과 메시지

---

## 3. 사용자 (User)

**Base Path**: `/api/v1/user`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/join` | 회원가입 | Body: `UserDTO` | ❌ |
| GET | `/checkId/{userId}` | 아이디 중복 확인 | Path: `userId` | ❌ |
| GET | `/find/userId` | 아이디 찾기 (마스킹) | Query: `GetNameNEmail` | ❌ |
| GET | `/find/userId/noMask` | 아이디 찾기 (마스킹 해제) | Query: `GetNameNEmail` | ❌ (이메일 인증 필수) |
| PATCH | `/find/userPw/{userEmail}` | 비밀번호 초기화 | Path: `userEmail`, Body: `GetUserPwRequestDTO` | ❌ (이메일 인증 필수) |

**`UserDTO` 주요 필드**: `userId`, `userPw`, `userName`, `userEmail`, `userNick`, `jobIdx`, `targetIdxList`

**응답 (공통)**
```json
{ "status": 200, "message": "회원가입 성공", "data": { "userIdx": 1 } }
```

---

## 4. 인증/검증 (Verify)

**Base Path**: `/api/v1/verify`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/auth/routine` | 루틴 활동 인증 | Body: `VerifyRequestDTO` | ✅ JWT |
| POST | `/emails/send/verification-requests` | 이메일 인증코드 전송(수정용) | Body: `ModifyEmailRequestDTO` | ❌ |
| POST | `/emails/send/just/verification-requests` | 이메일 인증코드 전송(단순) | Body: `GetEmailRequestDTO` | ❌ |
| POST | `/emails/verifications` | 이메일 인증코드 검증(수정용) | Body: `ModifyEmailRequestDTO`, Query: `code` | ❌ |
| POST | `/emails/just/verifications` | 이메일 인증코드 검증(단순) | Body: `GetEmailRequestDTO`, Query: `code` | ❌ |

---

## 5. 카테고리 (Category)

**Base Path**: `/api/v1/categories`

| Method | Path | 설명 | 응답 타입 | 인증 |
|---|---|---|---|---|
| GET | `/topMenu` | 탑 메뉴 카테고리 | `List<TopMenu>` | ❌ |
| GET | `/job` | 직업 카테고리 | `List<JobCateDTO>` | ❌ |
| GET | `/target` | 관심사(목표) 카테고리 | `List<TargetCateDTO>` | ❌ |
| GET | `/challenge` | 챌린지 카테고리 | `List<ChallengeCateDTO>` | ❌ |
| GET | `/icon` | 아이콘(사용자) | `List<IconDTO>` | ❌ |
| GET | `/qna` | QNA 카테고리 | `List<QnaParent>` | ❌ |
| GET | `/faq` | FAQ 카테고리 | `List<FaqCateDTO>` | ❌ |
| GET | `/shortcut` | 숏컷 카테고리 (Deprecated) | `List<ShortCutCateDTO>` | ❌ |
| GET | `/admin/authority` | 권한 카테고리 | `List<AuthorityCateDTO>` | ✅ Admin |
| GET | `/admin/icon` | 아이콘(관리자) | `List<IconDTO>` | ✅ Admin |
| GET | `/admin/fireInfos` | 불꽃 등급 정보 | `List<FireDTO>` | ✅ Admin |
| GET | `/admin/userLevelInfos` | 사용자 레벨 정보 | `List<UserLevelDTO>` | ✅ Admin |

---

## 6. 루틴 (Plan)

**Base Path**: `/api/v1/plan`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/auth/write` | 루틴 작성 | Body: `PlanDTO` | ✅ JWT |
| GET | `/detail/{planIdx}` | 루틴 상세 조회 | Path: `planIdx`, Cookie: `viewedPlans` | ❌ |
| PATCH | `/auth/modify` | 루틴 수정 | Body: `PlanDTO` | ✅ JWT |
| PATCH | `/auth/delete/{planIdx}` | 루틴 삭제 | Path: `planIdx` | ✅ JWT |
| PATCH | `/auth/stopNgo` | 루틴 활성화/비활성화 | Body: `PlanRequestDTO` | ✅ JWT |
| POST | `/auth/likePlan/{planIdx}` | 루틴 추천 | Path: `planIdx` | ✅ JWT |
| GET | `/checkLike/{planIdx}` | 추천 여부 확인 | Path: `planIdx` | 선택 |
| DELETE | `/auth/unLikePlan/{planIdx}` | 추천 취소 | Path: `planIdx` | ✅ JWT |
| PATCH | `/auth/earlyComplete/{planIdx}` | 루틴 조기 완료 | Path: `planIdx` | ✅ JWT |
| PATCH | `/auth/addReview` | 루틴 후기 작성 | Body: `PlanRequestDTO` | ✅ JWT |
| PATCH | `/auth/modifyReview` | 루틴 후기 수정 | Body: `PlanRequestDTO` | ✅ JWT |

**응답 (`/detail/{planIdx}`)**
```json
{
  "status": 200,
  "message": "조회 성공",
  "data": {
    "planIdx": 1,
    "planTitle": "...",
    "planSubTitle": "...",
    "userNick": "...",
    "likeCount": 12,
    "viewCount": 100,
    "activities": [ ... ]
  }
}
```

---

## 7. 리스트 조회 (List)

**Base Path**: `/api/v1/list`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/auth/myPlans` | 내 루틴 목록 | - | ✅ JWT |
| GET | `/plan/{mode}` | 루틴 목록(필터) | Path: `mode`, Query: `page,size,status,target,job,sort,order,search` | ❌ |
| GET | `/auth/qna` | 내 1:1 문의 목록 | Query: `page,size,status,sort,order,search` | ✅ JWT |

`mode`: `all`, `popular`, `latest` 등

---

## 8. 마이페이지 (MyPage)

**Base Path**: `/api/v1/myPage/auth` (전체 JWT 필수)

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

## 9. 챌린지 (Challenge)

**Base Path**: `/api/v1/challenges`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/latest` | 최신 챌린지(필터) | Query: `challState,challCategoryIdx,visibilityType,challengeType,onlyEnded,onlyJoined,page,size` | 선택 |
| GET | `/{challIdx}` | 챌린지 상세 | Path: `challIdx` | ❌ |
| GET | `/verify-records/{challIdx}` | 인증 기록 조회 | Path: `challIdx` | ✅ JWT |
| POST | `/auth/join/{challIdx}` | 챌린지 참여 | Path: `challIdx`, Body: `ChallengeJoinRequest` | ✅ JWT |
| POST | `/auth/verify/{challIdx}` | 챌린지 인증 | Path: `challIdx`, Body: `ChallengeVerifyDTO` | ✅ JWT |
| GET | `/search` | 챌린지 검색 | Query: `challTitle,challCategoryIdx,page,size,sort` | ❌ |

---

## 10. FAQ

**Base Path**: `/api/v1/faq`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/` | FAQ 전체 목록 | - | ❌ |
| GET | `/category/{faqCategory}` | 카테고리별 FAQ | Path: `faqCategory` | ❌ |
| GET | `/{faqIdx}` | FAQ 상세 | Path: `faqIdx` | ❌ |
| GET | `/search` | FAQ 검색 | Query: `SearchQueryDTO` | ❌ |
| POST | `/admin/write` | FAQ 작성 | Body: `FaQDTO` | ✅ Admin |
| PATCH | `/admin/{faqIdx}` | FAQ 수정 | Path: `faqIdx`, Body: `FaQDTO` | ✅ Admin |
| DELETE | `/admin/{faqIdx}` | FAQ 삭제 | Path: `faqIdx` | ✅ Admin |

---

## 11. 공지사항 (Notice)

**Base Path**: `/api/v1/notice`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/` | 공지 목록 | Query: `page,size` | ❌ |
| GET | `/{noticeIdx}` | 공지 상세 | Path: `noticeIdx` | ❌ |
| GET | `/popup` | 팝업 공지 조회 | - | ❌ |
| PATCH | `/admin/popup` | 팝업 공지 설정 | Body: `NoticeDTO` | ✅ Admin |
| POST | `/admin/create` | 공지 작성 | Body: `NoticeDTO` | ✅ Admin |
| PATCH | `/admin/{noticeIdx}` | 공지 수정 | Path: `noticeIdx`, Body: `NoticeDTO` | ✅ Admin |
| DELETE | `/admin/{noticeIdx}` | 공지 삭제 | Path: `noticeIdx` | ✅ Admin |

---

## 12. QNA 1:1 문의

**Base Path**: `/api/v1/qna/auth` (전체 JWT 필수)

| Method | Path | 설명 | 파라미터 | 권한 |
|---|---|---|---|---|
| POST | `/create` | 1:1 문의 작성 | Body: `QnaDTO` | User |
| GET | `/get/just/content/{qnaIdx}` | 문의 본문 조회 | Path: `qnaIdx` | User |
| GET | `/{qnaIdx}` | 문의 상세 조회 | Path: `qnaIdx` | User |
| PATCH | `/modify` | 문의 수정 | Body: `QnaDTO` | User |
| DELETE | `/delete/{qnaIdx}` | 문의 삭제 | Path: `qnaIdx` | User |
| PATCH | `/complete/{qnaIdx}` | 문의 완료 처리 | Path: `qnaIdx` | User |
| POST | `/comment/reply` | 답변 등록 | Body: `QnaReplyDTO` | Admin |
| PATCH | `/modify/reply` | 답변 수정 | Body: `QnaReplyDTO` | Admin |
| DELETE | `/delete/reply/{qnaIdx}` | 답변 삭제 | Path: `qnaIdx`, Query: `qnaReplyIdx` | Admin |

---

## 13. 신고 (Report)

**Base Path**: `/api/v1/report`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/auth/plan/{planIdx}` | 루틴 신고 | Path: `planIdx`, Body: `PlanReportDTO` | ✅ JWT |
| PATCH | `/auth/plan/cancel/{planIdx}` | 루틴 신고 취소 | Path: `planIdx` | ✅ JWT |
| POST | `/auth/user/{reportedIdx}` | 유저 신고 | Path: `reportedIdx`, Body: `UserReportDTO` | ✅ JWT |
| PATCH | `/auth/user/cancel/{reportedIdx}` | 유저 신고 취소 | Path: `reportedIdx` | ✅ JWT |

---

## 14. 검색 기록 (Search)

**Base Path**: `/api/v1/search`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/log` | 검색 기록 조회/저장 | Query: `keyword` (optional) | 선택 |
| PATCH | `/log/{logIdx}` | 검색 기록 삭제 | Path: `logIdx` | 선택 |

> 비로그인 시 쿠키 기반, 로그인 시 사용자 단위로 저장됩니다.

---

## 15. 이미지 업로드 (Image)

**Base Path**: `/api/v1/upload/auth`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/image-upload/{category}` | 이미지 업로드 (multipart) | Path: `category`, Form: `image` (`MultipartFile`) | ✅ JWT |

**응답**
```json
{ "status": 200, "url": "https://.../uploaded.png" }
```

---

## 16. 분석 (Analysis)

**Base Path**: `/api/v1/analysis`

> 현재 정의된 엔드포인트 없음 (레이아웃 컨트롤러).

---

## 17. 관리자 - 사용자 관리

**Base Path**: `/api/v1/admin/users` (전체 Admin 권한 필수)

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/` | 유저 목록(페이징) | Query: `page,size` |
| PATCH | `/ban/{userIdx}` | 유저 정지/복구 | Path: `userIdx` |
| GET | `/authority/list` | 권한 목록 조회 | - |
| GET | `/authority/adminList` | 관리자 명단 | - |
| GET | `/authority/{authorityIdx}` | 권한별 유저 조회 | Path: `authorityIdx` |
| PATCH | `/authority/updateAuth/{userIdx}` | 유저 권한 변경 | Path: `userIdx`, Body: `{authorityIdx: int}` |

---

## 18. 관리자 - 챌린지 관리

**Base Path**: `/api/v1/admin/challenges` (Admin)

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

---

## 19. 관리자 - 콘텐츠 관리

**Base Path**: `/api/v1/admin/compContent` (Admin)

### 목표 카테고리
| Method | Path | Body |
|---|---|---|
| POST | `/targetCategory` | `TargetCateDTO` |
| PATCH | `/targetCategory/{targetCateIdx}` | `TargetCateDTO` |
| DELETE | `/targetCategory/{idx}` | - |

### 직업 카테고리
| Method | Path | Body |
|---|---|---|
| POST | `/jobCategory` | `JobCateDTO` |
| PATCH | `/jobCategory/{jobIdx}` | `JobCateDTO` |
| DELETE | `/jobCategory/{jobIdx}` | - |

### 불꽃 등급
| Method | Path | Body |
|---|---|---|
| POST | `/fire` | `FireDTO` |
| PATCH | `/fire/{lvIdx}` | `FireDTO` |
| DELETE | `/fire/{lvIdx}` | - |

### 챌린지 카테고리
| Method | Path | Body |
|---|---|---|
| POST | `/challCate` | `ChallengeCateDTO` |
| PATCH | `/challCate/{challCategoryIdx}` | `ChallengeCateDTO` |
| DELETE | `/challCate/{challCategoryIdx}` | - |

---

## 20. 관리자 - 시스템 관리

**Base Path**: `/api/v1/admin/compSystem` (Admin)

### FAQ 카테고리
| Method | Path | Body |
|---|---|---|
| POST | `/faqCategory` | `FaqCateDTO` |
| PATCH | `/faqCategory/{faqCategoryIdx}` | `FaqCateDTO` |
| DELETE | `/faqCategory/{faqCategoryIdx}` | - |

### QNA 카테고리
| Method | Path | Body |
|---|---|---|
| POST | `/qnaCategory` | `QnaCateDTO` |
| PATCH | `/qnaCategory/{qnaCategoryIdx}` | `QnaCateDTO` |
| DELETE | `/qnaCategory/{qnaCategoryIdx}` | - |

### TopMenu
| Method | Path | Body |
|---|---|---|
| POST | `/topMenu` | `TopCateDTO` |
| PATCH | `/topMenu/{topIdx}` | `TopCateDTO` |
| DELETE | `/topMenu/{topIdx}` | - |

### 아이콘
| Method | Path | Body |
|---|---|---|
| POST | `/icon` | `IconDTO` |
| PATCH | `/icon/{iconKey}` | `IconDTO` |
| DELETE | `/icon/{iconKey}` | - |

---

## 21. 관리자 - 루틴 관리

**Base Path**: `/api/v1/admin/plans` (Admin)

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/` | 관리자 루틴 목록 | Query: `page,size` |
| GET | `/{targetIdx}` | 카테고리별 루틴 | Path: `targetIdx`, Query: `page,size` |
| GET | `/all` | 전체 루틴 조회 | Query: `page,size` |

---

## 22. 관리자 - 신고 관리

**Base Path**: `/api/v1/admin/report` (Admin)

| Method | Path | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/userReport` | 유저 신고 목록 | Query: `page,size,status?` |
| POST | `/userReportState` | 유저 신고 상태 변경 | Query: `userReportIdx,isApproved` |
| GET | `/planReport` | 루틴 신고 목록 | Query: `page,size,status?` |
| PATCH | `/plans/{planIdx}/reports/{planReportIdx}/status` | 루틴 신고 상태 변경 | Path + Body: `PlanReportDTO` |

---

## 23. 관리자 - 서비스 센터

**Base Path**: `/api/v1/service/admin` (Admin)

| Method | Path | 설명 |
|---|---|---|
| GET | `/get/status` | 관리자 온라인 상태 조회 |
| PATCH | `/switch/status` | 관리자 온라인 상태 전환 |
| POST | `/autoMatch/wakeUp` | QNA 자동 매칭 강제 활성화 |

---

## 🔐 보안 / 아키텍처 특징 요약

1. **JWT Bearer 인증**: `Authorization: Bearer {token}` 헤더 사용
2. **선택적 인증**: 일부 API는 비로그인도 호출 가능, 토큰 유무에 따라 응답 분기
3. **이메일 인증**: Redis(`EMAIL_VERIFIED` 키) 기반 1회성 인증
4. **관리자 권한**: `/admin/` 경로 또는 `@PreAuthorize`로 분리
5. **쿠키 추적**: 검색 기록, 루틴 조회수 중복 방지에 사용
6. **WebSocket**: `AdminChatController`, `QnaAdminController`는 본 문서 범위 외 (별도 STOMP 채널)

---

## 📦 응답 데이터 상세

본 절은 각 엔드포인트가 실제 컨트롤러에서 반환하는 응답 본문(키/타입/HTTP 상태 코드)을 정리합니다. 별도 명시가 없으면 응답은 `Map<String, Object>` 또는 `ResponseEntity<...>` 형태입니다.

### 표준 응답 패턴
```json
// 패턴 A — 단일 리소스
{ "status": 200, "message": "...", "data": { /* DTO */ } }

// 패턴 B — 페이징 목록
{ "status": 200, "message": "...", "data": [ /* DTO[] */ ],
  "totalPages": 5, "currentPage": 1, "pageSize": 10 }

// 패턴 C — 단순 결과
{ "status": 200, "message": "..." }
```

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

### 1. 공통 (Common) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/api/v1/hc` | `{ serverName, serverAddress, serverPort, env: String }` | 200 |
| GET `/api/v1/env` | `String` (env 값) | 200 |
| GET `/` | HTML 페이지 | 200 |

### 2. JWT — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/api/v1/reissue` | Header: `Authorization`, Cookie: `refresh` 갱신 / Body: 메시지 | 200, 401 |

### 3. 사용자 (User) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/user/join` | `{ message: "회원가입 완료" }` 또는 필드 에러 맵 | 200, 400 |
| GET `/user/checkId/{userId}` | `Boolean` | 200 |
| GET `/user/find/userId` | `{ status, message, data: maskedUserId }` | 200, 404 |
| GET `/user/find/userId/noMask` | `{ status, message, data: userId }` | 200, 404, 412 |
| PATCH `/user/find/userPw/{userEmail}` | `{ status, message }` | 200/400/404/412/422/500 |

### 4. 인증/검증 (Verify) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/verify/auth/routine` | `{ status, message }` | 200/400/403/404/409/410/412/500 |
| POST `/verify/emails/send/verification-requests` | (No content) | 200 |
| POST `/verify/emails/send/just/verification-requests` | (No content) | 200 |
| POST `/verify/emails/verifications` | `{ verified: Boolean }` | 200, 400 |
| POST `/verify/emails/just/verifications` | `{ verified: Boolean }` | 200, 400 |

### 5. 카테고리 (Category) — 응답

모두 200 응답이며, 본문은 해당 DTO 배열입니다.

| 엔드포인트 | 응답 타입 |
|---|---|
| GET `/categories/topMenu` | `List<TopMenu>` |
| GET `/categories/job` | `List<JobCateDTO>` |
| GET `/categories/target` | `List<TargetCateDTO>` |
| GET `/categories/challenge` | `List<ChallengeCateDTO>` |
| GET `/categories/icon` | `List<IconDTO>` |
| GET `/categories/qna` | `List<QnaParent>` |
| GET `/categories/faq` | `List<FaqCateDTO>` |
| GET `/categories/shortcut` | `List<ShortCutCateDTO>` (Deprecated) |
| GET `/categories/admin/authority` | `List<AuthorityCateDTO>` |
| GET `/categories/admin/icon` | `List<IconDTO>` |
| GET `/categories/admin/fireInfos` | `List<FireDTO>` |
| GET `/categories/admin/userLevelInfos` | `List<UserLevelDTO>` |

### 6. 루틴 (Plan) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/plan/auth/write` | `{ status, message }` | 201/400/410/412/500 |
| GET `/plan/detail/{planIdx}` | `{ status, message, data: PlanDTO }` | 200/404/500 |
| PATCH `/plan/auth/modify` | `{ status, message }` | 200/400/403/404/409/410/500 |
| PATCH `/plan/auth/delete/{planIdx}` | `{ status, message }` | 200/403/404/410/500 |
| PATCH `/plan/auth/stopNgo` | `{ status, message }` | 200/400/403/404/410/500 |
| POST `/plan/auth/likePlan/{planIdx}` | `{ status, message }` | 200/404/409/410/500 |
| GET `/plan/checkLike/{planIdx}` | `{ status, message, data: Boolean }` | 200 |
| DELETE `/plan/auth/unLikePlan/{planIdx}` | `{ status, message }` | 200/404/410/500 |
| PATCH `/plan/auth/earlyComplete/{planIdx}` | `{ status, message }` | 200/403/404/409/410/412/500 |
| PATCH `/plan/auth/addReview` | `{ status, message }` | 200/403/404/409/410/412/500 |
| PATCH `/plan/auth/modifyReview` | `{ status, message }` | 200/403/404/410/412/500 |

### 7. 리스트 조회 (List) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/list/auth/myPlans` | `{ status, message, data: List<MyPlanDTO> }` | 200/204/500 |
| GET `/list/plan/{mode}` | `{ plans: List<PlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| GET `/list/auth/qna` | `{ qnaList: List<QnaListDTO>, totalPages, currentPage, pageSize }` | 200 |

### 8. 마이페이지 (MyPage) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/myPage/auth/myAccount` | `{ status, message, data: MyPageUserInfosResponseDTO }` | 200/404 |
| PATCH `/myPage/auth/myAccount/modify/personal` | `{ status, message }` | 200/400/404/500 |
| PATCH `/myPage/auth/myAccount/modify/nickName` | `{ status, message }` | 200/400/404/500 |
| PATCH `/myPage/auth/myAccount/modify/email` | `{ status, message }` | 200/400/404/412/500 |
| PATCH `/myPage/auth/myAccount/modify/job-target` | `{ status, message }` | 200/400/404/500 |
| PATCH `/myPage/auth/security/change/password` | `{ status, message }` | 200/400/403/404/409/422/500 |
| PATCH `/myPage/auth/accountDeletion` | `{ status, message }` | 200/403/404/500 |
| PATCH `/myPage/auth/accountDeletion/cancel` | `{ status, message }` | 200/403/404/500 |
| GET `/myPage/auth/list/myPlan` | `{ plans: List<PlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| PATCH `/myPage/auth/delete/plans` | `{ status, message }` | 200/400/404/500 |
| PATCH `/myPage/auth/switch/isShared` | `{ status, message }` | 200/400/404/500 |
| GET `/myPage/auth/list/myChall` | `{ status, message, data: List<MyChallengeDTO> }` | 200 |
| GET `/myPage/auth/list/myLike` | `{ plans: List<PlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| DELETE `/myPage/auth/delete/likes` | `{ status, message }` | 200/400/404/500 |

### 9. 챌린지 (Challenge) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/challenges/latest` | `{ status, message, challenges: List<ChallengeDTO>, totalPages, currentPage, pageSize }` | 200/401 |
| GET `/challenges/{challIdx}` | `{ success: Boolean, challenge: ChallengeDTO }` | 200/404 |
| GET `/challenges/verify-records/{challIdx}` | `{ success: Boolean, records: List<VerifyRecordDTO> }` | 200/401 |
| POST `/challenges/auth/join/{challIdx}` | `ChallengeDTO` | 200/400/403/500 |
| POST `/challenges/auth/verify/{challIdx}` | `{ status, message, timestamp: LocalDateTime }` | 200/400/500 |
| GET `/challenges/search` | `List<ChallengeDTO>` | 200 |

### 10. FAQ — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/faq/` | `{ status, message, data: List<FaqListDTO> }` | 200/404 |
| GET `/faq/category/{faqCategory}` | `{ status, message, data: List<FaqListDTO> }` | 200/404 |
| GET `/faq/{faqIdx}` | `{ status, message, data: FaQDTO }` | 200/404 |
| GET `/faq/search` | `List<FaQDTO>` | 200 |
| POST `/faq/admin/write` | `String` (성공/실패 메시지) | 200/500 |
| PATCH `/faq/admin/{faqIdx}` | `String` | 200/500 |
| DELETE `/faq/admin/{faqIdx}` | `String` | 200/500 |

### 11. 공지사항 (Notice) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/notice/` | `{ status, message, data: List<NoticeDTO>, totalPages, currentPage, pageSize }` | 200/500 |
| GET `/notice/{noticeIdx}` | `{ status, message, data: NoticeDTO }` | 200/404/500 |
| GET `/notice/popup` | `{ status, message, notice: List<NoticeDTO> }` | 200/204 |
| PATCH `/notice/admin/popup` | `{ status, message }` | 200/400/404/500 |
| POST `/notice/admin/create` | `{ status, message }` | 201/403/409/500 |
| PATCH `/notice/admin/{noticeIdx}` | `{ status, message }` | 200/400/403/404/500 |
| DELETE `/notice/admin/{noticeIdx}` | `{ status, message }` | 200/404/500 |

### 12. QNA — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/qna/auth/create` | (No Content) | 201/400 |
| GET `/qna/auth/get/just/content/{qnaIdx}` | `{ status, message, data: String }` | 200 |
| POST `/qna/auth/comment/reply` | (No Content) | 201/400 |
| PATCH `/qna/auth/modify` | `{ status, message }` | 200/400 |
| PATCH `/qna/auth/modify/reply` | `{ status, message }` | 200/400 |
| DELETE `/qna/auth/delete/{qnaIdx}` | `{ status, message }` | 200 |
| DELETE `/qna/auth/delete/reply/{qnaIdx}` | `{ status, message }` | 200 |
| GET `/qna/auth/{qnaIdx}` | `{ status, message, data: QnaDetailDTO }` | 200/400 |
| PATCH `/qna/auth/complete/{qnaIdx}` | `{ status, message }` | 200/400 |

### 13. 신고 (Report) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/report/auth/plan/{planIdx}` | `{ status, message }` | 200/404/409/422/500 |
| PATCH `/report/auth/plan/cancel/{planIdx}` | `{ status, message }` | 200/404/500 |
| POST `/report/auth/user/{reportedIdx}` | `{ status, message }` | 200/400/404/409/410/422/500 |
| PATCH `/report/auth/user/cancel/{reportedIdx}` | `{ status, message }` | 200/404/500 |

### 14. 검색 (Search) — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/search/log` | `{ status, message, data: List<SearchLogsResponseDTO> }` | 200/204/500 |
| PATCH `/search/log/{logIdx}` | `{ status, message }` | 200/400/412/422/500 |

### 15. 이미지 업로드 — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/upload/auth/image-upload/{category}` | `{ url: String }` | 200/400/500 |

### 16. 분석 (Analysis) — 응답

> 정의된 엔드포인트 없음.

### 17. 관리자 - 사용자 — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/users/` | `{ status, message, data: { users: List<AdminUserDTO>, page, size, count, total, totalPages } }` | 200/204/500 |
| PATCH `/admin/users/ban/{userIdx}` | `{ status, message }` | 200/404/500 |
| GET `/admin/users/authority/list` | `{ status, message, authorityList: List<AuthorityCateDTO> }` | 200/404/500 |
| GET `/admin/users/authority/adminList` | `{ status, message, userList: List<AdminListDTO> }` | 200/404/500 |
| GET `/admin/users/authority/{authorityIdx}` | `{ status, message, userList: List<AdminUserDTO> }` | 200/404/500 |
| PATCH `/admin/users/authority/updateAuth/{userIdx}` | `{ status, message }` | 200/404/500 |

### 18. 관리자 - 챌린지 — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/challenges/latest` | `{ status, message, challenges: List<ChallengeDTO>, totalPages, currentPage, pageSize }` | 200 |
| GET `/admin/challenges/detail/{challIdx}` | `ChallengeDTO` | 200 |
| POST `/admin/challenges/create` | `{ status, message }` | 201/400/500 |
| PATCH `/admin/challenges/modify` | `{ status, message }` | 200/400/404/500 |
| PATCH `/admin/challenges/delete` | `{ status, message }` | 200/400/404/500 |
| POST `/admin/challenges/visibility/{challIdx}` | `{ status, message }` | 200/404/500 |
| POST `/admin/challenges/type/{challIdx}` | `{ status, message }` | 200/404/500 |
| PUT `/admin/challenges/earlyFinish/{challIdx}` | `String` | 200/400 |

### 19. 관리자 - 콘텐츠 — 응답

모두 `{ status, message }` 형태의 단순 응답입니다.

| 엔드포인트 | 상태 |
|---|---|
| POST `/admin/compContent/targetCategory` | 201/409/500 |
| PATCH `/admin/compContent/targetCategory/{targetCateIdx}` | 200/404/409/500 |
| DELETE `/admin/compContent/targetCategory/{idx}` | 200/404/500 |
| POST `/admin/compContent/jobCategory` | 200/400/500 |
| PATCH `/admin/compContent/jobCategory/{jobIdx}` | 200/404/500 |
| DELETE `/admin/compContent/jobCategory/{jobIdx}` | 200/404/500 |
| POST `/admin/compContent/fire` | 201/409/500 |
| PATCH `/admin/compContent/fire/{lvIdx}` | 200/404/500 |
| DELETE `/admin/compContent/fire/{lvIdx}` | 200/404/500 |
| POST `/admin/compContent/challCate` | 201/409/500 |
| PATCH `/admin/compContent/challCate/{challCategoryIdx}` | 200/404/500 |
| DELETE `/admin/compContent/challCate/{challCategoryIdx}` | 200/404/500 |

### 20. 관리자 - 시스템 — 응답

기본 형태는 `{ status, message }`이며, 일부 삭제 API는 충돌 시 데이터 목록을 함께 반환합니다.

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/admin/compSystem/faqCategory` | `{ status, message }` | 201/409/500 |
| PATCH `/admin/compSystem/faqCategory/{faqCategoryIdx}` | `{ status, message }` | 200/404/500 |
| DELETE `/admin/compSystem/faqCategory/{faqCategoryIdx}` | `{ status, message, faqList?: List<FaQDTO> }` | 200/400/500 |
| POST `/admin/compSystem/qnaCategory` | `{ status, message }` | 201/409/500 |
| PATCH `/admin/compSystem/qnaCategory/{qnaCategoryIdx}` | `{ status, message }` | 200/404/500 |
| DELETE `/admin/compSystem/qnaCategory/{qnaCategoryIdx}` | `{ status, message, data?: List<QnaDTO> }` | 200/409/500 |
| POST `/admin/compSystem/topMenu` | `{ status, message }` | 201/409/500 |
| PATCH `/admin/compSystem/topMenu/{topIdx}` | `{ status, message }` | 200/404/409/500 |
| DELETE `/admin/compSystem/topMenu/{topIdx}` | `{ status, message }` | 200/404/409/500 |
| POST `/admin/compSystem/icon` | `{ status, message }` | 201/409/500 |
| PATCH `/admin/compSystem/icon/{iconKey}` | `{ status, message }` | 200/404/500 |
| DELETE `/admin/compSystem/icon/{iconKey}` | `{ status, message }` | 200/404/500 |

### 21. 관리자 - 루틴 — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/plans/` | `{ status, message, plans: List<CustomAdminPlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| GET `/admin/plans/{targetIdx}` | `{ status, message, plans: List<CustomAdminPlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |
| GET `/admin/plans/all` | `{ status, message, plans: List<CustomAdminPlanListDTO>, totalPages, currentPage, pageSize }` | 200/204 |

### 22. 관리자 - 신고 — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/admin/report/userReport` | `{ total, reports: List<UserReportDTO>, page, size }` | 200 |
| POST `/admin/report/userReportState` | `{ status, message }` | 200/400/500 |
| GET `/admin/report/planReport` | `{ page, size, total, list: List<PlanReportDTO> }` | 200 |
| PATCH `/admin/report/plans/{planIdx}/reports/{planReportIdx}/status` | `String` | 200/403/500 |

### 23. 관리자 - 서비스 센터 — 응답

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/service/admin/get/status` | `{ status, message, data: String }` | 200 |
| PATCH `/service/admin/switch/status` | `{ status, message, data: String }` | 200 |
| POST `/service/admin/autoMatch/wakeUp` | `{ status, message }` | 200 |

---

## 📐 핵심 DTO 필드 정의

### PlanDTO (루틴)
| 필드 | 타입 | 비고 |
|---|---|---|
| planIdx | int | PK |
| userIdx | int | 작성자 |
| planTitle | String | NotBlank |
| forkIdx / forkTitle | Integer / String | 포크 원본 |
| endTo | int | Min 7 |
| repeatDays | List\<String\> | 반복 요일 |
| targetIdx / jobIdx | int | Min 1 |
| planImp | int | 1~10 |
| planSubDate / planSubMod / planSubStart / planSubEnd | LocalDateTime | |
| viewCount / verifyCount / likeCount / forkCount | int | |
| certExp / lastExp | int | |
| isShared / isActive / isCompleted / isDeleted | int | |
| review | String | 후기 |
| activities | List\<ActivityDTO\> | Min 1 |
| deleteActivityIdx | List\<Integer\> | 수정 시 삭제 대상 |
| jobCateDTO / targetCateDTO / jobEtcCateDTO | DTO | 조인 |
| fireInfo | FireDTO | 등급 정보 |
| forked / fireState | boolean | |
| isWriter | int | |

### UserDTO (유저)
| 필드 | 타입 | 비고 |
|---|---|---|
| userIdx | int | PK |
| userName | String | 3~15, NotBlank |
| userId | String | 4~15, `[a-zA-Z0-9]*` |
| userPw | String | 8~20, 특수문자 허용 |
| userNick | String | 2~15 |
| nickTag | String | `#...` |
| userEmail | String | Email |
| jobIdx / targetIdx | int | Min 1 |
| userPhone | String | 010-... |
| userGender | int | Min 1 |
| userJoin | LocalDateTime | |
| maxFireIdx / authorityIdx | int | |
| combo | int | 연속 달성 |
| userExp | double | |
| userLv | int | |
| roleStatus | boolean | 관리자 온라인 여부 |
| reportCount | int | |
| isBanned | int | 0 정상 / 1 정지 |

### ChallengeDTO (챌린지)
| 필드 | 타입 | 비고 |
|---|---|---|
| challIdx | Long | PK |
| challTitle / challDescription | String | |
| challCategoryIdx | int | |
| minParticipationTime / totalClearTime / maxVerifyTime | int | 분 단위 |
| maxParticipants / currentParticipants | int | |
| challStartTime / challEndTime / challCreatedAt | LocalDateTime | |
| challState | String | |
| userJoin | int | |
| duration | Integer | |
| participants | List\<ChallengeJoinDTO\> | |
| isJoined | boolean | |
| visibilityType | String | PUBLIC / PRIVATE |
| challengeType | String | NORMAL / SPECIAL |

### ChallengeJoinRequest
| 필드 | 타입 |
|---|---|
| userIdx | int |
| duration | int |
| StartTime / EndTime | LocalDateTime |
| activityTime | int |

### ChallengeVerifyDTO
| 필드 | 타입 |
|---|---|
| challIdx | Long |
| userIdx | Long |
| startTime / endTime | LocalDateTime |
| activity | String |

### QnaDTO
| 필드 | 타입 | 비고 |
|---|---|---|
| qnaIdx | int | |
| qUserIdx / aUserIdx | int | 질문자 / 답변자 |
| title / content | String | NotBlank |
| createdAt / modifiedAt / respondingDate | LocalDateTime | |
| category | int | Min 1 |
| qCount / aCount | int | |
| qnaStatus | String | |

### NoticeDTO
| 필드 | 타입 | 비고 |
|---|---|---|
| noticeIdx | int | |
| noticeTitle / noticeSub | String | NotBlank |
| userIdx | Integer | |
| noticeDate / noticeModify | LocalDateTime | |
| isPopup | String | Y/N (default N) |
| popupStartDate / popupEndDate | LocalDateTime | |
| writeName | String | |

### FaQDTO
| 필드 | 타입 |
|---|---|
| faqIdx | int |
| faqTitle / faqAnswer | String |
| faqCategory | Integer |
| faqCategoryName | String |

### PlanReportDTO
| 필드 | 타입 | 비고 |
|---|---|---|
| reporterIdx | Integer | |
| planIdx | Integer | |
| reportReason | String | |
| status | int | 0 처리중 / 1 완료 |
| reportDate | LocalDateTime | |
| planReportIdx | int | |
| isShared | Integer | 0 비공개 / 1 공개 |

### ModifyPersonalRequestDTO
| 필드 | 타입 | 비고 |
|---|---|---|
| userIdx | int | |
| userName | String | 3~15 |
| userPhone | String | 010-... |
| userGender | int | Min 1 |

### ModifyEmailRequestDTO
| 필드 | 타입 |
|---|---|
| userIdx | int |
| userEmail | String (Email, Unique) |

### GetUserPwRequestDTO
| 필드 | 타입 |
|---|---|
| userIdx | int |
| userPw | String (4~15) |
| originalPw | String |
| userPwConfirm | String |

### TargetCateDTO / JobCateDTO / ChallengeCateDTO
| 필드 | 타입 |
|---|---|
| idx (또는 challCateIdx) | int |
| name (또는 challName) | String |
| iconKey / icon / color | String |

### FireDTO
| 필드 | 타입 |
|---|---|
| lvIdx | Long |
| minExp / maxExp | int |
| fireName / fireColor / fireEffect | String |

### IconDTO
| 필드 | 타입 |
|---|---|
| iconKey / icon / color | String |
| visible | int |
| originalIconKey | String |

### FaqCateDTO
| 필드 | 타입 |
|---|---|
| faqCategoryIdx | int |
| faqCategoryName | String |

### QnaCateDTO
| 필드 | 타입 |
|---|---|
| categoryIdx | int |
| parentIdx | Integer |
| categoryName | String |
| categoryLevel | int |
| createDate / updateDate | LocalDateTime |
| deprecated | int |

### TopMenu / TopCateDTO
| 필드 | 타입 |
|---|---|
| topIdx | int |
| topName | String |
| iconKey / icon / color | String |
| ord | int |

