package com.maorou.SpringAIDemo.config;

import com.maorou.SpringAIDemo.functions.LocalFileTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class ChatClientConfig {


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory){
        return builder
                .defaultSystem("你是一个友好的中文助手,回答简洁、清晰、有条理。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultFunctions("myLocalFileScanner")
                .build();
    }
    /**
     * 🎯 核心改变 3：把你的 Java 苦力逻辑注册成 Spring AI 能识别的工具
     * * ⚠️ 【避坑考点 1】：@Description 注解里的这一段话，是直接打包当成“隐形提示词”发给 3090 里的模型的！
     * 大模型就是靠这段大白话来判断“什么时候该用这个工具”。如果你写得含糊，模型就会“无视”你的工具。
     * * ⚠️ 【避坑考点 2】：这里的 Function<输入, 输出> 必须精准对应你之前写的两个 record 类！
     */
    @Bean(name = "myLocalFileScanner")
    @Description("用于读取并列出本地电脑中指定绝对路径（文件夹/目录）下的所有文件和子文件夹的名称")
    public Function<LocalFileTools.Request, LocalFileTools.Response> myLocalFileScanner() {
        // new 出你刚才写好的执行器，当大模型发信号时，Spring AI 会自动来调用它的 apply() 方法
        return new LocalFileTools.FileScannerFunction();
    }
}
