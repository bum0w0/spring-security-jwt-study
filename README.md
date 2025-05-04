# Spring Security JWT

| 구분          | basic 브랜치                     | advanced 브랜치                      |
|---------------|---------------------------------|-------------------------------------|
| **토큰 구조**  | 단일 JWT 토큰                    | Access, Refresh Token 분리            |
| **토큰 관리**  | 분리하지 않음                    | Redis에 Refresh Token 저장           |
| **토큰 만료**  | 재로그인 필요, 자동 갱신 없음      | Refresh Token으로 재발급 가능        |
| **로그아웃**   | 없음                             | Refresh Token 삭제, Blacklist 처리    |

### 1. 회원가입 - POST /join
1. `JoinController`가 요청을 받고 DTO로 파싱
2. `JoinService`가 DTO를 `UserEntity`로 변환 후 `UserRepository`를 통해 DB에 저장
3. 완료되면 응답 반환 (※ 토큰은 발급하지 않음)

### 2. 로그인 - POST /login
1. 클라이언트가 `username`, `password`를 전송
2. `UsernamePasswordAuthenticationFilter`가 요청을 가로채고 `AuthenticationManager`에 인증 요청
3. `AuthenticationManager`가 `UserDetailsService`를 통해 DB에서 사용자 정보 로드 (`UserEntity` → `UserDetails`)
4. 인증 성공 시 `successfulAuth` 단계에서 `JWTUtil`을 통해 JWT 토큰 생성
5. 생성된 JWT를 응답으로 반환

### 3. JWT 기반 인증 처리 (토큰 유효성 검사)
1. 사용자가 인증이 필요한 API를 호출할 때 `Authorization: Bearer <token>` 헤더를 함께 전송
2. `SecurityAuthenticationFilter` 또는 `JWT Filter`가 요청을 가로채 토큰을 추출
3. `JWTUtil`을 통해 토큰 유효성 검증
4. 유효하다면 사용자 정보를 `SecurityContextHolder`에 저장
   - JWT 기반(stateless)이지만, 요청당 인증 객체를 담기 위한 일시적 보관소 역할
   - 세션처럼 동작하지만, 실제 세션 저장은 하지 않음 (서버에 상태 정보 저장 X) 
5. 이후 컨트롤러에서 인증된 사용자로 인가 처리 가능

#### 📌 현재 실습에서는 `Access Token`과 `Refresh Token`을 분리하지 않고, **단일 JWT 토큰 구조**로만 인증을 처리함
- 따라서 토큰이 만료되면 재로그인이 필요하며, 자동 갱신 로직은 없음
- 다중 토큰 방식(Access/Refresh)은 [advanced 브랜치](https://github.com/bum0w0/spring-security-jwt-study/tree/advanced)에서 진행
