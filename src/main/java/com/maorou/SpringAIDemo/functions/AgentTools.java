package com.maorou.SpringAIDemo.functions;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AgentTools {

    private final ChatClient translatorAgent;
    private final ChatClient poetAgent;

    public AgentTools(ChatClient translatorAgent, ChatClient poetAgent){
        this.translatorAgent = translatorAgent;
        this.poetAgent = poetAgent;
    }

    @Tool(description = "把一段文本翻译成英文")
    public String translateToEnglish(@ToolParam(description = "要翻译的原文")String text){
        return translatorAgent.prompt(text).call().content();
    }
    @Tool(description = "根据一个主题写一首中文诗")
    public String writePoem(@ToolParam(description = "诗的主题") String topic) {
        return poetAgent.prompt(topic).call().content();
    }

}


