# Prizm

팀원을 초대한 공유 스페이스에 각자 결과물을 올리면, AI가 자동으로 분석·비교해 하나의 시각화된 정리본(마인드맵 트리)으로 만들어주는 협업 플랫폼.

자료를 한 사람이 모을 필요가 없다. 각자 자기 것만 올리면 통합이 자동으로 일어난다. 아무도 "정리해줘"라고 요청하지 않았는데 화면이 스스로 갱신된다.

## 사전 요구사항

- JDK 17+
- Node.js 20+ (npm)
- MySQL 8

```sql
CREATE DATABASE prizm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 환경변수

루트 `.env.example`과 `backend/application-example.yml`을 참고합니다. 시크릿은 커밋하지 마세요.

| 변수 | 설명 |
| --- | --- |
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | MySQL 사용자 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `GEMINI_API_KEY` | Gemini API 키. 시드 데이터는 키 없이 READY 상태로 들어갑니다. 실시간 시연 1건에만 필요합니다. |
| `GEMINI_TAG_MODEL` | 기본값 `gemini-2.5-flash` |
| `GEMINI_EMBED_MODEL` | 기본값 `gemini-embedding-001` |
| `VITE_API_BASE_URL` | 기본값 `http://localhost:8080` |

PowerShell 예시:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:DB_URL = "jdbc:mysql://localhost:3306/prizm?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
$env:GEMINI_API_KEY = "your-key"
```

## Backend

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
./mvnw spring-boot:run
```

헬스체크: http://localhost:8080/api/health → `{ "status": "ok" }`

기동 시 데모 스페이스 `CAFE01` / `교내 카페 개선안` 이 자동 시드됩니다.

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

브라우저: http://localhost:5173

## 데모

- 참여 코드: `CAFE01`
- 메인 노트북: 시드된 트리(대기시간 그룹은 접힌 상태)를 보여 주는 화면
- 서브 노트북: `/join` 에서 다른 닉네임·전공으로 같은 코드 입장 후 `demo/live-upload.md` 업로드
- 시드 멤버 예: 민수(산업공학), 지현(컴퓨터공학), 수아(시각디자인)

시드 실행은 Gemini를 호출하지 않습니다. 실시간 분석은 시연 1건만 호출합니다.

## 시연 순서

1. 메인: 채워진 마인드맵. 전공 색 범례가 보이는지 확인
2. 대기시간 그룹 펼치기 — 공통점 / 차이점 / 비고
3. 서브에서 live-upload.md 업로드. 메인에서 요청 없이 노드가 생김
4. 분석 후 "업데이트됨"과 차이점 문구 변화
5. 새 노드 클릭 — 원문 패널

## 데모 리허설 체크리스트

- [ ] 메인 노트북: 시드 스페이스 입장, 대기시간 그룹은 접힌 상태
- [ ] 서브 노트북: 다른 member로 같은 코드 입장, live-upload.md 준비
- [ ] 메인에서 그룹 펼침 — 공통점/차이점/비고가 읽힌다
- [ ] 서브에서 업로드 — 메인에 요청 없이 노드가 생긴다
- [ ] 분석 후 배지와 차이점 문구가 바뀐다
- [ ] 새 노드 클릭 — 원문이 열린다
- [ ] 네트워크를 잠깐 꺼도 시드 트리는 이미 보인다

## 심사 대응

**Q. NotebookLM으로 되지 않나**  
A. 자료를 한 명이 다 모아야 한다. 각자 올리고 자동 통합되는 게 차이다.

**Q. GPT에 붙여넣으면 되는데**  
A. 요청해야 답하는 것과, 요청 없이 갱신되는 건 다른 경험이다.

**Q. 비슷한 협업툴 많은데**  
A. 기존 툴은 저장까지. 비교·연결을 AI가 대신하는 게 핵심이다.
