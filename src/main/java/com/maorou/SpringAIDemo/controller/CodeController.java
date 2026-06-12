package com.maorou.SpringAIDemo.controller;


import com.maorou.SpringAIDemo.auth.AuthService;
import com.maorou.SpringAIDemo.auth.CurrentUser;
import com.maorou.SpringAIDemo.auth.ToolAccessPolicy;
import com.maorou.SpringAIDemo.functions.CodeSandboxTools;
import com.maorou.SpringAIDemo.utils.CodeRunRequest;
import com.maorou.SpringAIDemo.utils.CodeRunResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/code")
@Slf4j
public class CodeController {

    private static final int CODE_PREVIEW_LENGTH = 200;
    private static final int ERROR_PREVIEW_LENGTH = 200;

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
            log.warn("Code sandbox denied userId={} role={} codeLength={} codePreview={}",
                    currentUser.userId(),
                    currentUser.role(),
                    codeLength(request),
                    codePreview(request));
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: role " + currentUser.role() + " cannot use code tool"
            );
        }

        long startTime = System.currentTimeMillis();
        CodeRunResult result = codeSandboxTools.runJavaCode(request.code());
        long durationMillis = System.currentTimeMillis() - startTime;

        log.info("Code sandbox run userId={} role={} success={} stage={} exitCode={} durationMillis={} codeLength={} codePreview={} errorPreview={}",
                currentUser.userId(),
                currentUser.role(),
                result.success(),
                result.stage(),
                result.exitCode(),
                durationMillis,
                codeLength(request),
                codePreview(request),
                errorPreview(result));

        return result;
    }

    private int codeLength(CodeRunRequest request) {
        if (request == null || request.code() == null) {
            return 0;
        }
        return request.code().length();
    }

    private String codePreview(CodeRunRequest request) {
        if (request == null || request.code() == null) {
            return "";
        }
        return preview(request.code(), CODE_PREVIEW_LENGTH);
    }

    private String errorPreview(CodeRunResult result) {
        if (result == null || result.error() == null) {
            return "";
        }
        return preview(result.error(), ERROR_PREVIEW_LENGTH);
    }

    private String preview(String text, int maxLength) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
