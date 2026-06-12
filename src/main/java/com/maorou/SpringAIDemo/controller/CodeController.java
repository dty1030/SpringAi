package com.maorou.SpringAIDemo.controller;


import com.maorou.SpringAIDemo.auth.AuthService;
import com.maorou.SpringAIDemo.auth.CurrentUser;
import com.maorou.SpringAIDemo.auth.ToolAccessPolicy;
import com.maorou.SpringAIDemo.functions.CodeSandboxTools;
import com.maorou.SpringAIDemo.utils.CodeRunRequest;
import com.maorou.SpringAIDemo.utils.CodeRunResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/code")
public class CodeController {

    private final CodeSandboxTools codeSandboxTools;
    private final AuthService authService;
    private final ToolAccessPolicy toolAccessPolicy;

    public CodeController(CodeSandboxTools codeSandboxTools,
                          AuthService authService,
                          ToolAccessPolicy toolAccessPolicy) {
        this.codeSandboxTools = codeSandboxTools;
        this.authService = authService;
        this.toolAccessPolicy = toolAccessPolicy;
    }

    @PostMapping("/run")
    public CodeRunResult run(@RequestBody CodeRunRequest request,
                             HttpServletRequest httpRequest) {
        CurrentUser currentUser = authService.authenticate(httpRequest);
        if (!toolAccessPolicy.isToolAllowed(currentUser.role(), "code")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: role " + currentUser.role() + " cannot use code tool"
            );
        }
        return codeSandboxTools.runJavaCode(request.code());
    }
}
