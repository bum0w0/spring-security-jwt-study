package com.example.springjwt.jwt;

import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.entity.RefreshEntity;
import com.example.springjwt.repository.RefreshRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

@Slf4j
@RequiredArgsConstructor
// 로그인 요청을 처리하는 커스텀 필터 (Form 로그인 방식은 사용하지 않음)
public class LoginFilter extends UsernamePasswordAuthenticationFilter { // 이 필터는 기본적으로 "/login" 경로에 대한 POST 요청을 감지

    private final AuthenticationManager authenticationManager;
    // JWTUtil 주입
    private final JWTUtil jwtUtil;

    private final RefreshRepository refreshRepository;

    @Override
    public Authentication attemptAuthentication( // 로그인 시도가 발생하면 실행되는 메소드
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException {

        // 클라이언트 요청에서 사용자의 username, password 추출
        // 내부적으로 request.getParameter("username") 또는 request.getParameter("password")와 동일
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        /*
        스프링 시큐리티에서 username과 password를 검증할 때는 Token에 담아서 AuthenticationManager로 전달하여 내부에서 검증 진행 (AuthenticationProvider가 검증)
        이름은 Token이지만, Spring Security 내부에서 사용하는 인증 정보 객체라고 보면 됨 (Authentication 객체)
        1. 인증 요청용 객체로써 사용 : 사용자가 로그인할 때, ID와 비밀번호를 담아서 인증 요청을 만들기 위해 사용
        2. 인증 완료된 결과 객체로써 사용 : AuthenticationManager가 인증을 마친 후, UserDetails와 권한 정보를 담아 새로 생성한 인증 객체
            → 인증 완료 시 이 객체가 SecurityContextHolder에 저장됨
         */
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null); // 로그인 요청 시점에는 아직 인증이 되지 않은 상태이기 때문에, 사용자의 권한 정보가 없음. 인증이 완료되면 Authentication 객체 안에 실제 권한 정보가 채워짐 (Ex. User, Admin)

        // AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
    }

    // authenticate()의 성공 여부가 로그인 성공/실패를 판별하는 기준, Spring Security가 그 결과에 따라 성공/실패 메서드를 자동으로 실행

    // 로그인 성공 시 실행하는 메소드
    // 매개변수에 filterchain : 인증 성공 후, 다음 필터로 요청을 넘길 수 있게 하기 위한 매개변수 (인증 성공 후에도 다른 필터에서 CORS 설정, 리스폰스 헤더 추가 등이 필요할 수 있음)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, Authentication authentication) {
        // Spring Security에서 Authentication 객체는 로그인 인증 후 사용자 정보를 담고 있음. 이 객체에서 사용자의 정보를 getPrincipal() 메소드를 통해 꺼낼 수 있고 이는 UserDetails 타입임
        // 하지만 현재 실습에서 사용자 정보를 담는 클래스로 CustomUserDetails를 만들었기 때문에 형변환해서 사용
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        // 로그인에 성공한 사용자 정보를 활용해서 이후 작업
        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities(); // 권한 목록 가져오기
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator(); // 반복자 생성
        GrantedAuthority auth = iterator.next(); // 첫 번째 권한 가져오기

        String role = auth.getAuthority(); // 권한 이름 추출

        String access = jwtUtil.createJwt("access", username, role, 600000L); // 생명 주기 : 10분
        String refresh = jwtUtil.createJwt("refresh", username, role, 86400000L);  // 생명 주기 : 24시간

        // RefreshToken 저장
        addRefreshEntity(username, refresh);

        // AccessToken은 응답 헤더에 담아서 클라이언트에게 전달, 프론트에서 로컬 스토리지에 저장
        // RefreshToken은 쿠키에 저장
        response.addHeader("Authorization", "Bearer " + access);
        response.addCookie(createCookie("refresh", refresh));
        response.setStatus(HttpStatus.OK.value());
    }

    // 로그인 실패 시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }

    private void addRefreshEntity(String username, String refresh) {

        RefreshEntity refreshEntity = new RefreshEntity();
        refreshEntity.setUsername(username);
        refreshEntity.setRefresh(refresh);

        refreshRepository.save(refreshEntity);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        //cookie.setSecure(true);
        //cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

}
