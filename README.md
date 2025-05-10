# Spring JWT 심화

#### 사용 기술 스택

| 구성 요소       | 사용 기술 / 버전                  | 비고                                   |
|----------------|-----------------------------------|----------------------------------------|
| 백엔드 프레임워크 | Spring Boot 3.4.4                  | 전체 애플리케이션 구성                 |
| 인증 라이브러리 | jjwt (Java JWT) 0.12.3            | Access / Refresh 토큰 생성 및 검증    |
| 인메모리 저장소 | Redis                             | Refresh 토큰 저장 및 TTL 설정          |
| 관계형 DB       | MySQL                             | 사용자 계정 및 기타 정보 저장           |

> Redis는 Refresh 토큰을 저장하고, TTL을 통해 만료된 토큰을 자동 삭제
> jjwt 0.12.3 버전은 JWT 구조 분리 방식 (header, payload, signature) 사용

#### 기존의 단일 JWT 사용 시 문제점
- Access 토큰은 로그인 후 발급되어 모든 요청 헤더에 포함됨.
- 클라이언트에서 XSS 또는 HTTP 스니핑 등을 통해 토큰이 탈취될 위험이 있음.
- 탈취 시 공격자는 권한이 필요한 API에 자유롭게 접근 가능.
---
#### 다중 토큰 구조 도입

Access / Refresh 토큰 구성:

| 토큰 | 역할 | 생명주기 | 전송 빈도 | 저장 위치 권장 |
| --- | --- | --- | --- | --- |
| Access | 권한 인증 | 짧음 (약 10분) | 매우 잦음 | LocalStorage |
| Refresh | Access 갱신용 | 김 (24시간 이상) | 낮음 | HttpOnly Cookie |

> Access 토큰은 유효 기간이 짧아 자주 갱신되고, Refresh 토큰은 새로운 Access 토큰을 발급받을 때만 사용되므로 서버에서 더 철저히 검증하고 안전하게 보관
>

---
#### 흐름 요약

1. 로그인 성공 시 Access + Refresh 토큰 동시 발급
2. 일반 요청은 Access 토큰으로 처리
3. Access 토큰 만료 시 → Refresh 토큰으로 Access 재발급 요청
4. 서버는 Refresh를 검증하고 새로운 Access를 발급
5. 보안을 위해 Refresh도 회전(Rotate)하여 새로 발급 가능
6. Refresh 토큰 관리 전략
---
#### Refresh Rotate
- Access 재발급 시 Refresh도 함께 갱신.
- 사용된 Refresh는 재사용 불가 처리.

**저장소 선택 기준**

| 저장소 | 장점 | 단점 |
| --- | --- | --- |
| LocalStorage | 구현 간단, 직관적 | XSS 공격에 취약 |
| HttpOnly Cookie | XSS 방어 가능, 서버 전송 자동화 | CSRF 방어 위한 추가 조치 필요 |

---

#### 보안 강화 팁
- Access는 짧게, Refresh는 길게 설정.
- Refresh는 DB에 저장하여 로그아웃 시 무효화 (토큰 블랙리스트 처리)
- HTTPS 통신 필수 적용.
