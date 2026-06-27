package com.maorou.SpringAIDemo.controller;


import com.maorou.SpringAIDemo.ChatRequest;
import com.maorou.SpringAIDemo.auth.AuthService;
import com.maorou.SpringAIDemo.auth.CurrentUser;
import com.maorou.SpringAIDemo.auth.ToolAccessPolicy;
import com.maorou.SpringAIDemo.functions.*;
import com.maorou.SpringAIDemo.service.RagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    @Autowired
    ChatClient chatClient;

    @Autowired
    LocalFileTools localFileTools;

    @Autowired
    TimeTools timeTools;

    @Autowired
    CodeSandboxTools codeSandboxTools;

    @Autowired
    AuthService authService;

    @Autowired
    ToolAccessPolicy toolAccessPolicy;

    @Autowired
    private RagService ragService;

    @Autowired
    ToolCallbackProvider toolCallbackProvider;



    @Autowired
    MathToolFactory mathToolFactory;
    @Autowired
    ReviewTool reviewTool;

    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest req, HttpServletRequest httpRequest) {

        CurrentUser currentUser = authService.authenticate(httpRequest, req);
        String toolMode = req.toolMode();
        String role = currentUser.role();
        if (!toolAccessPolicy.isToolAllowed(role, toolMode)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: role " + role + " cannot use toolMode " + toolMode
            );
        }
        ChatClient.ChatClientRequestSpec prompt =
                chatClient.prompt()
                        .user(req.message())
                        .advisors(advisorSpec ->
                                advisorSpec.param(ChatMemory.CONVERSATION_ID,
                                        req.conversationId()));
        if ("time".equals(req.toolMode())) {
            prompt = prompt.tools(timeTools);
        }
        else if ("file".equals(req.toolMode())) {
            prompt = prompt.tools(localFileTools);
        }
        else if ("all".equals(req.toolMode())) {
            prompt = prompt.tools(timeTools,
                    localFileTools);
        } else if ("rag".equals(req.toolMode())) {
            prompt = prompt.advisors(QuestionAnswerAdvisor.builder(ragService.getVectorStore())
                    .build());
        }
        else if ("code".equals(req.toolMode())) {
            prompt = prompt.tools(codeSandboxTools);
        } else if ("dyn".equals(req.toolMode())) {
            prompt = prompt.toolCallbacks(mathToolFactory.buildTools());
        } else if ("mcp".equals(req.toolMode())) {
            prompt = prompt.toolCallbacks(toolCallbackProvider.getToolCallbacks());
        } else if ("review".equals(req.toolMode())) {
            prompt = prompt.tools(reviewTool);
        }
        return prompt.stream().content();
    }
}
