package com.example.springjwt.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
// JWT 0.12.3 버전
// 서비스의 요구 사항에 따라 선택적으로 JWT 발급 클래스를 구현해야 하며 현재는 실습을 위해 단순히 인증만 필요한 시스템을 가정
public class JWTUtil {

    private SecretKey secretKey;

    // 생성자: 비밀 키(secret)를 사용하여 SecretKey 객체를 생성
    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    // 토큰에서 username을 추출하는 메소드
    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    // 토큰에서 역할(role)을 추출하는 메소드
    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    // 토큰에서 카테고리(category)를 추출하는 메소드 (access/refresh 판단을 위한 카테고리)
    public String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
    }

    // 토큰의 만료 여부를 체크하는 메소드
    public Boolean isExpired(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    // 사용자 이름, 역할 및 만료 시간을 기준으로 JWT를 생성하는 메소드
    public String createJwt(String category, String username, String role, Long expiredMs) {
        return Jwts.builder()
                // claim() = JWT의 페이로드에 데이터를 추가하는 메서드
                .claim("category", category)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                // JWT의 만료 시간을 설정
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey) // 서명
                .compact();
    }
}