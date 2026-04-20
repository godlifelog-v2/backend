# API — 인증/공통 (§1~5)

> Base URL: `/api/v1`
> 관련 섹션: Common, JWT, User, Verify, Category

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

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/api/v1/hc` | `{ serverName, serverAddress, serverPort, env: String }` | 200 |
| GET `/api/v1/env` | `String` (env 값) | 200 |

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

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/api/v1/reissue` | Header: `Authorization`, Cookie: `refresh` 갱신 / Body: 메시지 | 200, 401 |

---

## 3. 사용자 (User)

**Base Path**: `/api/v1/user`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/join` | 회원가입 | Body: `UserDTO` | ❌ |
| POST | `/login` | 로그인 | Body: `userId`, `userPw` | ❌ |
| POST | `/logout` | 로그아웃 | Cookie: `refresh` | ❌ (Refresh Cookie 필수) |
| GET | `/checkId/{userId}` | 아이디 중복 확인 | Path: `userId` | ❌ |
| POST | `/find/userId` | 아이디 찾기 (마스킹) | Body: `GetNameNEmail` | ❌ |
| POST | `/find/userId/noMask` | 아이디 찾기 (마스킹 해제) | Body: `GetNameNEmail` | ❌ (이메일 인증 필수) |
| PATCH | `/find/userPw` | 비밀번호 초기화 | Body: `GetUserPwRequestDTO` | ❌ (이메일 인증 필수) |
| GET | `/auth/profile` | 유저 프로필 조회 | Header: `Authorization` | ✅ JWT |

---

### POST `/join` — 회원가입

**요청 본문 필수 필드**

| 필드 | 타입 | 제약 조건 |
|---|---|---|
| `userId` | String | 4~15자, 영문+숫자만 (`[a-zA-Z0-9]*`), 중복 불가 |
| `userPw` | String | 8~20자, 영문/숫자/특수문자(`!@#$%^*()_+-=[]{},.?:~`) |
| `userName` | String | 3~15자 |
| `userEmail` | String | 이메일 형식, 중복 불가 |
| `userNick` | String | 2~15자, 한글/영문/숫자/-_ (`[가-힣a-zA-Z0-9-_]*`) |
| `jobIdx` | int | Min 1 (직업 카테고리 인덱스) |
| `targetIdx` | int | Min 1 (초기 관심사 인덱스) |
| `userPhone` | String | `010-XXXX-XXXX` 형식 |
| `userGender` | int | Min 1 |

**응답**

| 상태 코드 | 응답 본문 | 설명 |
|---|---|---|
| `200 OK` | `{ "code": 200, "message": "회원가입 완료", "status": 200 }` | 가입 성공 |
| `400 Bad Request` | `{ "필드명": "에러 메시지", ... }` | 유효성 검사 실패 |

---

### POST `/login` — 로그인

Spring Security 필터(`LoginFilter`)에서 처리됩니다.

**요청**
```json
{ "userId": "string", "userPw": "string" }
```

**응답 (성공 200)**
- Header: `Authorization: Bearer {accessToken}`
- Cookie: `refresh={refreshToken}; HttpOnly; Path=/`
```json
{ "userNick": "닉네임", "nickTag": "#태그", "roleStatus": false }
```

**토큰 유효 기간**
| 토큰 | 유효 기간 |
|---|---|
| Access Token | 5분 |
| Refresh Token | 24시간 |

**응답 (실패)**

| 상태 코드 | 사유 |
|---|---|
| `401 Unauthorized` | 아이디 또는 비밀번호 불일치 |
| `403 Forbidden` | 정지된 계정 |

---

### POST `/logout` — 로그아웃

Spring Security 필터(`CustomLogoutFilter`)에서 처리됩니다.

**요청**: Cookie: `refresh={refreshToken}`

**응답 (실패)**

| 상태 코드 | 사유 |
|---|---|
| `401 Unauthorized` | Refresh 토큰 쿠키 없음 / 만료 / DB 없음 |
| `400 Bad Request` | 유효하지 않은 Refresh 토큰 |

---

### POST `/find/userId` — 아이디 찾기 (마스킹)

**마스킹 규칙**

| 아이디 길이 | 예시 |
|---|---|
| 7~12자 | `"Hongs123"` → `"Hon*****"` (앞 3자 노출) |
| 6자 이하 | `"Hong"` → `"Ho**"` (앞 2자 노출) |
| 13자 이상 | `"HongGilDong1234"` → `"Hong***********"` (앞 4자 노출) |

> 프론트엔드 팁: 마스킹 아이디 표시 후 "전체 아이디 확인" 버튼 → 이메일 인증 후 `/find/userId/noMask` 호출

---

### POST `/find/userId/noMask` — 아이디 찾기 (마스킹 해제)

> ⚠️ 선행 조건: 이메일 인증 완료 필수 (하단 [이메일 인증 플로우](#이메일-인증-플로우) 참고)

| 상태 코드 | 설명 |
|---|---|
| `200 OK` | 아이디 원문 반환. Redis 인증 플래그 자동 삭제 |
| `412 Precondition Failed` | 이메일 인증 미완료 또는 만료(10분) |

---

### PATCH `/find/userPw` — 비밀번호 초기화

> ⚠️ 선행 조건: 이메일 인증 완료 필수

**요청 본문**: `{ userEmail, userPw, userPwConfirm }`

| 상태 코드 | 설명 |
|---|---|
| `200 OK` | 초기화 성공. 모든 Refresh Token 무효화 |
| `412 Precondition Failed` | 이메일 인증 미완료 또는 만료 |
| `422 Unprocessable Entity` | `userPw`와 `userPwConfirm` 불일치 |

---

### 이메일 인증 플로우

아이디 찾기(마스킹 해제) / 비밀번호 초기화 공통 선행 절차:

```
[Step 1] POST /api/v1/verify/emails/send/just/verification-requests
         Body: { "userEmail": "hong@example.com" }

[Step 2] POST /api/v1/verify/emails/just/verifications?code=123456
         Body: { "userEmail": "hong@example.com" }
         → 응답: { "verified": true }  ← 10분간 유효

[Step 3-A] POST /api/v1/user/find/userId/noMask
[Step 3-B] PATCH /api/v1/user/find/userPw
```

**Redis 키 흐름**

| 단계 | Redis Key | TTL |
|---|---|---|
| 인증코드 발송 후 | `AuthCode {email}` | 5분 |
| 검증 성공 후 | `EMAIL_VERIFIED: {email}` | 10분 |
| Step 3 완료 후 | 자동 삭제 | — |

> Step 2 응답 `verified: false` → "인증코드가 올바르지 않습니다" 재입력 유도  
> Step 3에서 `412` → "인증이 만료되었습니다" 안내 후 Step 1로 복귀

---

### GET `/auth/profile` — 유저 프로필 조회

**응답 (200)**
```json
{
  "code": 200,
  "status": 200,
  "message": "프로필 조회 성공",
  "data": {
    "userNick": "철수짱", "nickTag": "#1",
    "jobIdx": 1, "targetIdx": 1,
    "combo": 18, "userExp": 1770, "userLv": 6
  }
}
```

---

## 4. 인증/검증 (Verify)

**Base Path**: `/api/v1/verify`

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/auth/routine` | 루틴 활동 인증 | ✅ JWT |
| POST | `/emails/send/verification-requests` | 이메일 인증코드 전송 (가입/수정용) | ❌ |
| POST | `/emails/send/just/verification-requests` | 이메일 인증코드 전송 (단순인증용) | ❌ |
| POST | `/emails/verifications` | 이메일 인증코드 검증 (가입/수정용) | ❌ |
| POST | `/emails/just/verifications` | 이메일 인증코드 검증 (단순인증용) | ❌ |

**이메일 인증 API 두 가지 종류**

| 구분 | 가입/수정용 | 단순인증용 |
|---|---|---|
| 사용 DTO | `ModifyEmailRequestDTO` | `GetEmailRequestDTO` |
| DB 이메일 존재 시 | 요청 거부 (중복 방지) | 200 반환 후 발송 |
| DB 이메일 없을 시 | 요청 허용 | 발송 없이 200 반환 |
| 사용 시나리오 | 회원가입, 이메일 변경 | 아이디 찾기, 비밀번호 초기화 |

**POST `/emails/just/verifications` 응답**
```json
{ "verified": true }
```

**Brute Force 방어**

| 상황 | 동작 |
|---|---|
| 코드 불일치 1~4회 | 실패 횟수 증가 (TTL 5분) |
| 코드 불일치 5회 이상 | 인증코드 즉시 무효화, 재발송 필요 |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/verify/auth/routine` | `{ code, message: String, status }` | 200/400/403/404/409/410/412/500 |
| POST `/verify/emails/send/verification-requests` | (No content) | 200, 400, 429 |
| POST `/verify/emails/send/just/verification-requests` | (No content) | 200, 400, 429 |
| POST `/verify/emails/verifications` | `{ verified: Boolean }` | 200, 400 |
| POST `/verify/emails/just/verifications` | `{ verified: Boolean }` | 200, 400 |

> 429: 1분 이내 재발송 요청 시. 단순인증용은 미등록 이메일이어도 400 없이 200/429만 반환

---

## 5. 카테고리 (Category)

**Base Path**: `/api/v1/categories` (전체 인증 불필요)

| Method | Path | 응답 타입 | 인증 |
|---|---|---|---|
| GET | `/topMenu` | `List<TopMenu>` | ❌ |
| GET | `/job` | `List<JobCateDTO>` | ❌ |
| GET | `/target` | `List<TargetCateDTO>` | ❌ |
| GET | `/challenge` | `List<ChallengeCateDTO>` | ❌ |
| GET | `/icon` | `List<IconDTO>` | ❌ |
| GET | `/qna` | `List<QnaParent>` | ❌ |
| GET | `/faq` | `List<FaqCateDTO>` | ❌ |
| GET | `/shortcut` | `List<ShortCutCateDTO>` (Deprecated) | ❌ |
| GET | `/admin/authority` | `List<AuthorityCateDTO>` | ✅ Admin |
| GET | `/admin/icon` | `List<IconDTO>` | ✅ Admin |
| GET | `/admin/fireInfos` | `List<FireDTO>` | ✅ Admin |
| GET | `/admin/userLevelInfos` | `List<UserLevelDTO>` | ✅ Admin |

모두 200 응답, 본문은 해당 DTO 배열.
