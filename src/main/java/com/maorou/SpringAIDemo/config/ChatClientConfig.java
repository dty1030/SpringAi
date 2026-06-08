package com.maorou.SpringAIDemo.config;

import com.maorou.SpringAIDemo.feign.WeatherFeignClient;
import com.maorou.SpringAIDemo.functions.LocalFileTools;
import com.maorou.SpringAIDemo.functions.TimeTools;
import com.maorou.SpringAIDemo.functions.WeatherTools;
import com.maorou.SpringAIDemo.workspace.WorkspaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ChatClientConfig {


    TimeTools timeTools = new TimeTools();
    //WeatherTools weatherTools = new WeatherTools();

    @Bean
    public LocalFileTools localFileTools(WorkspaceStrategy workspaceStrategy) {
        return new LocalFileTools(workspaceStrategy);
    }

    @Bean
    public TimeTools timeTools() {
        return new TimeTools();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory,
                                 WeatherFeignClient weatherFeignClient){
        // 🎯 第一步：让它的 System 提示词知道自己是 Agent
        builder.defaultSystem("你是一个拥有多种本地能力(文件操作、查询时间等)的 AI 助手。当你需要的信息(如当前时间、文件内容、目录列表等)超出你自己已知范围时,必须主动调用对应的工具来获取，不要让用户自己去调用工具,也不要凭空编造。");

        // 🎯 第二步：把多轮对话记忆的 Advisor 挂载上去
        builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());

        WeatherTools weatherTools = new WeatherTools(weatherFeignClient);
        // 🎯 第三步：把你的查文件工具箱名字注册进去（1.x 用 defaultToolNames 按 Bean 名注册）
        //builder.defaultTools(localFileTools, timeTools, weatherTools);

        // 🎯 最后一步：一切准备就绪，单独一行执行 build() 产生真正的客户端
        return builder.build();
    }





}
