package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.auth.JwtUtil;
import org.springframework.web.bind.annotation.*;
import com.maorou.SpringAIDemo.auth.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    JwtProperties jwtProperties;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request){

        String role = resolveRole(request.username());

        String token = JwtUtil.createToken(
                jwtProperties.getSecret(),
                jwtProperties.getTtlMillis(),
                request.username(),
                role
        );
        return new LoginResponse(request.username(), role, token);

    }

    private String resolveRole(String username){
        if ("admin".equals(username)){
            return "admin";
        }
        if ("trusted".equals((username))){
            return "trusted";
        }
        return "guest";
    }

    public record LoginRequest(String username){}
    public record LoginResponse(String userId, String role, String token){}

}
