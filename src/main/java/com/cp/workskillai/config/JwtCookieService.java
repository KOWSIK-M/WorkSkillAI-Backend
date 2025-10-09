package com.cp.workskillai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class JwtCookieService {

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    @Value("${app.cookie.domain:localhost}")
    private String cookieDomain;

    public ResponseCookie createJwtCookie(String token) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }

    public ResponseCookie createLogoutCookie() {
        return ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }
}