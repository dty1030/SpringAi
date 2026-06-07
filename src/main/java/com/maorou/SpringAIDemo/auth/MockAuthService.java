package com.maorou.SpringAIDemo.auth;

import com.maorou.SpringAIDemo.ChatRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "mock")
public class MockAuthService implements AuthService {

    @Override
    public CurrentUser authenticate(HttpServletRequest httpRequest, ChatRequest chatRequest) {
        return new CurrentUser("mock-user", "admin");
    }
}
