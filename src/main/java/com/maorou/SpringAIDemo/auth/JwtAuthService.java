package com.maorou.SpringAIDemo.auth;

import com.maorou.SpringAIDemo.ChatRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "jwt")
public class JwtAuthService implements AuthService {

    private final JwtProperties jwtProperties;

    public JwtAuthService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public CurrentUser authenticate(HttpServletRequest httpServletRequest, ChatRequest chatRequest) {
        String authorization = httpServletRequest.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Authorization Bearer token");
        }

        String token = authorization.substring("Bearer ".length());
        return JwtUtil.parseToken(jwtProperties.getSecret(), token);
    }
}
