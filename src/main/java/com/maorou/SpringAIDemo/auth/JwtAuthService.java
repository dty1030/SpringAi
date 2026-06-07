package com.maorou.SpringAIDemo.auth;

import com.maorou.SpringAIDemo.ChatRequest;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing Authorization Bearer token"
            );
        }

        // Reject an empty Bearer token.
        String token = authorization.substring("Bearer ".length()).trim();
        if(token.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing Authorization Bearer token"
            );
        }


        try {
            return JwtUtil.parseToken(jwtProperties.getSecret(), token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token"
            );
        }
    }
}
