package com.maorou.SpringAIDemo.auth;

import com.maorou.SpringAIDemo.ChatRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    CurrentUser authenticate(HttpServletRequest httpRequest,
                             ChatRequest chatRequest);
}
