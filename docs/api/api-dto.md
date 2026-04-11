# API — 핵심 DTO 필드 정의

> 각 도메인에서 공통적으로 참조하는 DTO들을 정리한 문서.
> 도메인별 DTO(ChallengeDTO 등)는 해당 도메인 파일에서 확인할 것.

---

## v1 DTO

### PlanDTO (루틴 작성/수정 요청용)

> 루틴 **작성·수정·조회** 공용. v2 조회 응답은 `PlanDetailDTO` 사용.

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

---

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

---

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

---

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

---

### FaQDTO

| 필드 | 타입 |
|---|---|
| faqIdx | int |
| faqTitle / faqAnswer | String |
| faqCategory | Integer |
| faqCategoryName | String |

---

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

---

### ModifyPersonalRequestDTO

| 필드 | 타입 | 비고 |
|---|---|---|
| userIdx | int | |
| userName | String | 3~15 |
| userPhone | String | 010-... |
| userGender | int | Min 1 |

---

### ModifyEmailRequestDTO

| 필드 | 타입 |
|---|---|
| userIdx | int |
| userEmail | String (Email, Unique) |

---

### GetUserPwRequestDTO

| 필드 | 타입 |
|---|---|
| userIdx | int |
| userPw | String (4~15) |
| originalPw | String |
| userPwConfirm | String |

---

### TargetCateDTO / JobCateDTO / ChallengeCateDTO

| 필드 | 타입 |
|---|---|
| idx (또는 challCateIdx) | int |
| name (또는 challName) | String |
| iconKey / icon / color | String |

---

### FireDTO

| 필드 | 타입 |
|---|---|
| lvIdx | Long |
| minExp / maxExp | int |
| fireName / fireColor / fireEffect | String |

---

### IconDTO

| 필드 | 타입 |
|---|---|
| iconKey / icon / color | String |
| visible | int |
| originalIconKey | String |

---

### FaqCateDTO

| 필드 | 타입 |
|---|---|
| faqCategoryIdx | int |
| faqCategoryName | String |

---

### QnaCateDTO

| 필드 | 타입 |
|---|---|
| categoryIdx | int |
| parentIdx | Integer |
| categoryName | String |
| categoryLevel | int |
| createDate / updateDate | LocalDateTime |
| deprecated | int |

---

### TopMenu / TopCateDTO

| 필드 | 타입 |
|---|---|
| topIdx | int |
| topName | String |
| iconKey / icon / color | String |
| ord | int |

---

## v2 DTO

### PlanDetailDTO (루틴 상세 조회 응답 전용)

> `GET /api/v2/plan/detail/{planIdx}` 응답 DTO. 조회에 불필요한 필드 제거 + 플래그 boolean 통일.

| 필드 | 타입 | 비고 |
|---|---|---|
| planIdx | int | |
| planTitle | String | |
| endTo | int | |
| repeatDays | List\<String\> | |
| planImp | int | |
| certExp / verifyCount / viewCount / likeCount / forkCount | int | |
| isShared / isActive / isCompleted / isWriter | **boolean** | int → boolean 변환 |
| fireState / forked | boolean | |
| planSubDate / planSubStart / planSubEnd | LocalDateTime | |
| forkIdx | Integer | |
| forkTitle | String | |
| review | String | |
| activities | List\<ActivityDTO\> | |
| jobCateDTO / jobEtcCateDTO / targetCateDTO | DTO | |
| fireInfo | FireDTO | |
| **제거된 필드** | | `userIdx`, `targetIdx`, `jobIdx`, `lastExp`, `isDeleted`, `deleteActivityIdx`, `planSubMod` |

---

### MyPlanV2DTO (내 루틴 목록 응답 전용)

> `GET /api/v2/list/auth/myPlans`, `GET /api/v2/list/auth/todayPlans` 응답 DTO.

| 필드 | 타입 | 비고 |
|---|---|---|
| planInfos | CustomPlanV2DTO | 루틴 기본 정보 |
| activities | List\<ActivityDTO\> | |
| jobCateDTO | JobCateDTO | 직업(기본) |
| jobEtcCateDTO | JobEtcCateDTO | 직업(직접입력) |
| targetCateDTO | TargetCateDTO | 관심사 |
| fireInfo | FireDTO | 불꽃 등급 |

---

### CustomPlanV2DTO (루틴 기본 정보)

> `MyPlanV2DTO.planInfos` 안에 포함.

| 필드 | 타입 | 비고 |
|---|---|---|
| planIdx | int | |
| planTitle | String | |
| endTo | int | |
| planSubEnd | LocalDateTime | |
| isShared | **boolean** | 구: `int` |
| isActive | **boolean** | 구: `int` |
| planImp | int | |
| certExp | int | |
| repeatDays | **List\<String\>** | 구: `String("mon,tue")` |
| fireState | boolean | |
| color | String | 루틴 색상 헥사코드 (DB 컬럼 추가 후 활성화) |

---

### TodayStatsDTO (오늘 요약 통계)

> `GET /api/v2/list/auth/todayStats` 응답 DTO.

| 필드 | 타입 | 비고 |
|---|---|---|
| totalToday | int | 오늘 요일 해당 활성 루틴 수 |
| completedToday | int | 오늘 모든 activity 인증 완료 루틴 수 |
| combo | int | 연속 달성 일수 (`USER_TABLE.COMBO`) |
