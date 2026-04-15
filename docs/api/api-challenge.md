# API — 챌린지 (§9)

> Base URL: `/api/v1`
> Base Path: `/api/v1/challenges`

---

## 9. 챌린지 (Challenge)

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/latest` | 최신 챌린지(필터) | Query: `challState,challCategoryIdx,visibilityType,challengeType,onlyEnded,onlyJoined,page,size` | 선택 |
| GET | `/{challIdx}` | 챌린지 상세 | Path: `challIdx` | ❌ |
| GET | `/verify-records/{challIdx}` | 인증 기록 조회 | Path: `challIdx` | ✅ JWT |
| POST | `/auth/join/{challIdx}` | 챌린지 참여 | Path: `challIdx`, Body: `ChallengeJoinRequest` | ✅ JWT |
| POST | `/auth/verify/{challIdx}` | 챌린지 인증 | Path: `challIdx`, Body: `ChallengeVerifyDTO` | ✅ JWT |
| GET | `/search` | 챌린지 검색 | Query: `challTitle,challCategoryIdx,page,size,sort` | ❌ |

---

## 응답 상세

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/challenges/latest` | `{ challenges: List<ChallengeDTO>, totalPages, currentPage, pageSize }` | 200/401 |
| GET `/challenges/{challIdx}` | `{ code, message: String, status, data: ChallengeDTO }` | 200/404 |
| GET `/challenges/verify-records/{challIdx}` | `{ code, message: String, status, data: List<VerifyRecordDTO> }` | 200/401 |
| POST `/challenges/auth/join/{challIdx}` | `{ code, message: String, status, data: ChallengeDTO }` | 200/400/403/500 |
| POST `/challenges/auth/verify/{challIdx}` | `{ code, message: String, status, data: { timestamp: LocalDateTime } }` | 200/400/500 |
| GET `/challenges/search` | `{ code, message: String, status, data: List<ChallengeDTO> }` | 200 |

---

## 관련 DTO

### ChallengeDTO

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
