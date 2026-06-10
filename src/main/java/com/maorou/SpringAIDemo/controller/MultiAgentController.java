package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.functions.AgentTools;
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

    @Autowired
    ChatClient orchestratorAgnet;

    @Autowired
    AgentTools agentTools;

    @GetMapping("/api/multiagent/ask")
    public output ask(@RequestParam String question){

        String draft = drafterAgent.prompt(question).call().content();
        String reviewInput = "Primary question: " + question
                + "\ndraft: " + draft
                + "\nPlease review and give the improved final answer";
        String finalAnswer = reviewerAgent.prompt(reviewInput).call().content();
        return new output(draft, finalAnswer);

    }

    @GetMapping("/api/multiagent/orchestrate")
    public String orchestrate(@RequestParam String question){
        return orchestratorAgnet.prompt(question)
                .tools(agentTools)
                .call()
                .content();
    }




    public record output(String draft, String finalAnswer){};

}
