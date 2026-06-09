package com.maorou.SpringAIDemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MultiAgentController {

    @Autowired
    ChatClient drafterAgent;

    @Autowired
    ChatClient reviewerAgent;

    @GetMapping("/api/multiagent/ask")
    public output ask(@RequestParam String question){

        String draft = drafterAgent.prompt(question).call().content();
        String reviewInput = "Primary question: " + question
                + "\ndraft: " + draft
                + "\nPlease review and give the improved final answer";
        String finalAnswer = reviewerAgent.prompt(reviewInput).call().content();
        return new output(draft, finalAnswer);

    }




    public record output(String draft, String finalAnswer){};

}
