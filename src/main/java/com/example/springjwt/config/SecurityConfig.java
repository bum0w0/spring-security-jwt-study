package com.example.springjwt.config;

import com.example.springjwt.jwt.JWTUtil;
import com.example.springjwt.jwt.LoginFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // AuthenticationManager가 사용할 AuthenticationConfiguration 객체를 생성자 주입받음
    // 이 객체를 통해 Spring이 자동 구성한 AuthenticationManager를 가져올 수 있으며, 내부적으로 어떤 UserDetailsService와 PasswordEncoder를 사용할지에 대한 설정도 이미 포함되어 있음
    private final AuthenticationConfiguration authenticationConfiguration;
    // JWTUtil 주입
    private final JWTUtil jwtUtil;

    // AuthenticationManager Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CSRF disable
        // CSRF는 세션 기반 인증에서 위험하지만, 현재는 JWT 인증 → 브라우저가 자동으로 쿠키를 전송하지 않으므로 CSRF 방어는 고려 X
        http.csrf((auth) -> auth.disable());

        // Form 로그인 방식 disable
        // 기본 로그인 폼(Username/Password 입력창) 비활성화. REST API에서는 직접 클라이언트에서 로그인 데이터를 받아서 처리하므로 필요 없음
        http.formLogin((auth) -> auth.disable());

        // http basic 인증 방식 disable
        http.httpBasic((auth) -> auth.disable());

        // 경로별 인가 작업
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/login", "/", "/join").permitAll()
                .requestMatchers("/admin").hasRole("ADMIN")
                .anyRequest().authenticated());


        // UsernamePasswordAuthenticationFilter를 대체할 커스텀 필터(LoginFilter)를 등록하기 위해 addFilterAt() 사용
        // LoginFilter는 인증 처리를 위해 AuthenticationManager를 사용하며, 이 매니저는 사용자 인증을 담당하는 객체
        // AuthenticationManager는 UserDetailsService와 PasswordEncoder 등 인증에 필요한 설정을 포함하고 있으므로, 이를 생성자에 전달하여 필터가 인증을 처리할 수 있도록 설정
        http.addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil), UsernamePasswordAuthenticationFilter.class);

        // 세션 설정
        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

}
