# API — 콘텐츠/유틸 (§10~16)

> Base URL: `/api/v1`
> 관련 섹션: FAQ, 공지사항, QNA, 신고, 검색, 이미지 업로드, 분석

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

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/faq/` | `{ code, message: String, status, data: List<FaqListDTO> }` | 200/404 |
| GET `/faq/category/{faqCategory}` | `{ code, message: String, status, data: List<FaqListDTO> }` | 200/404 |
| GET `/faq/{faqIdx}` | `{ code, message: String, status, data: FaQDTO }` | 200/404 |
| GET `/faq/search` | `{ code, message: String, status, data: List<FaQDTO> }` | 200 |
| POST `/faq/admin/write` | `{ code, message: String, status }` | 200/500 |
| PATCH `/faq/admin/{faqIdx}` | `{ code, message: String, status }` | 200/500 |
| DELETE `/faq/admin/{faqIdx}` | `{ code, message: String, status }` | 200/500 |

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

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/notice/` | `{ notices: List<NoticeDTO>, totalPages, currentPage, pageSize }` | 200/500 |
| GET `/notice/{noticeIdx}` | `{ code, message: String, status, data: NoticeDTO }` | 200/404/500 |
| GET `/notice/popup` | `{ code, message: String, status, data: List<NoticeDTO> }` | 200/204 |
| PATCH `/notice/admin/popup` | `{ code, message: String, status }` | 200/400/404/500 |
| POST `/notice/admin/create` | `{ code, message: String, status }` | 201/403/409/500 |
| PATCH `/notice/admin/{noticeIdx}` | `{ code, message: String, status }` | 200/400/403/404/500 |
| DELETE `/notice/admin/{noticeIdx}` | `{ code, message: String, status }` | 200/404/500 |

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

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/qna/auth/create` | (No Content) | 201/400 |
| GET `/qna/auth/get/just/content/{qnaIdx}` | `{ code, message: String, status }` | 200 |
| POST `/qna/auth/comment/reply` | (No Content) | 201/400 |
| PATCH `/qna/auth/modify` | `{ code, message: String, status }` | 200/400 |
| PATCH `/qna/auth/modify/reply` | `{ code, message: String, status }` | 200/400 |
| DELETE `/qna/auth/delete/{qnaIdx}` | `{ code, message: String, status }` | 200 |
| DELETE `/qna/auth/delete/reply/{qnaIdx}` | `{ code, message: String, status }` | 200 |
| GET `/qna/auth/{qnaIdx}` | `{ code, message: String, status, data: QnaDetailDTO }` | 200/400 |
| PATCH `/qna/auth/complete/{qnaIdx}` | `{ code, message: String, status }` | 200/400 |

---

## 13. 신고 (Report)

**Base Path**: `/api/v1/report`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/auth/plan/{planIdx}` | 루틴 신고 | Path: `planIdx`, Body: `PlanReportDTO` | ✅ JWT |
| PATCH | `/auth/plan/cancel/{planIdx}` | 루틴 신고 취소 | Path: `planIdx` | ✅ JWT |
| POST | `/auth/user/{reportedIdx}` | 유저 신고 | Path: `reportedIdx`, Body: `UserReportDTO` | ✅ JWT |
| PATCH | `/auth/user/cancel/{reportedIdx}` | 유저 신고 취소 | Path: `reportedIdx` | ✅ JWT |

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/report/auth/plan/{planIdx}` | `{ code, message: String, status }` | 200/404/409/422/500 |
| PATCH `/report/auth/plan/cancel/{planIdx}` | `{ code, message: String, status }` | 200/404/500 |
| POST `/report/auth/user/{reportedIdx}` | `{ code, message: String, status }` | 200/400/404/409/410/422/500 |
| PATCH `/report/auth/user/cancel/{reportedIdx}` | `{ code, message: String, status }` | 200/404/500 |

---

## 14. 검색 기록 (Search)

**Base Path**: `/api/v1/search`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| GET | `/log` | 검색 기록 조회/저장 | Query: `keyword` (optional) | 선택 |
| PATCH | `/log/{logIdx}` | 검색 기록 삭제 | Path: `logIdx` | 선택 |

> 비로그인 시 쿠키 기반, 로그인 시 사용자 단위로 저장됩니다.

**응답 상세**

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| GET `/search/log` | `{ code, message: String, status, data: List<SearchLogsResponseDTO> }` | 200/204/500 |
| PATCH `/search/log/{logIdx}` | `{ code, message: String, status }` | 200/400/412/422/500 |

---

## 15. 이미지 업로드 (Image)

**Base Path**: `/api/v1/upload/auth`

| Method | Path | 설명 | 파라미터 | 인증 |
|---|---|---|---|---|
| POST | `/image-upload/{category}` | 이미지 업로드 (multipart) | Path: `category`, Form: `image` (`MultipartFile`) | ✅ JWT |

**응답**
```json
{ "code": 200, "status": 200, "message": "이미지 업로드 성공", "data": { "url": "https://.../uploaded.png" } }
```

| 엔드포인트 | 응답 본문 | 상태 |
|---|---|---|
| POST `/upload/auth/image-upload/{category}` | `{ code, message: String, status, data: { url: String } }` | 200/400/500 |

---

## 16. 분석 (Analysis)

**Base Path**: `/api/v1/analysis`

> 현재 정의된 엔드포인트 없음 (레이아웃 컨트롤러).
