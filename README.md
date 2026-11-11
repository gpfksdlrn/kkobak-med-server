# kkobak-med-server

꼬박약(복약 관리)의 백엔드 API 서버

프론트엔드: [kkobak-med-web](https://github.com/gpfksdlrn/kkobak-med-web)

**Stack** · Java 21 · Spring Boot 4.1 · Spring Security(JWT) · JPA · Flyway · PostgreSQL 18 · Testcontainers

## 로컬 실행

```bash
cp .env.example .env      # 최초 1회, 비밀번호 수정
docker compose up -d      # DB
./gradlew bootRun         # 서버 http://localhost:8080
```

- API 문서: `/swagger-ui.html`
- 테스트: `./gradlew build` (Docker 필요)
