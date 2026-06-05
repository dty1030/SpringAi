package com.maorou.SpringAIDemo.controller;


import com.maorou.SpringAIDemo.ChatRequest;
import com.maorou.SpringAIDemo.functions.LocalFileTools;
import com.maorou.SpringAIDemo.functions.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(ChatController.class);

    @Autowired
    VectorStore vectorStore;

    @Autowired
    ChatClient chatClient;

    @Autowired
    LocalFileTools localFileTools;

    @Autowired
    TimeTools timeTools;


    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest req){

        ChatClient.ChatClientRequestSpec prompt =
                chatClient.prompt()
                        .user(req.message())
                        .advisors(advisorSpec ->
                                advisorSpec.param(ChatMemory.CONVERSATION_ID,
                                        req.conversationId()));
        if ("time".equals(req.toolMode())){
            prompt = prompt.tools(timeTools);
        }
        else if ("file".equals(req.toolMode())){
            prompt = prompt.tools(localFileTools);
        }
        else if ("all".equals(req.toolMode())) {
            prompt = prompt.tools(timeTools,
                    localFileTools);
        } else if ("rag".equals(req.toolMode())) {
            prompt = prompt.advisors(QuestionAnswerAdvisor.builder(vectorStore)
                    .build());
        }
        return prompt.stream().content();


    }
}
