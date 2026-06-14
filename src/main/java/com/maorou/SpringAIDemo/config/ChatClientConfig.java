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

    @Bean
    public ChatClient drafterAgent(ChatClient.Builder builder){
        return builder.defaultSystem("针对用户的问题,直接写出一份简洁的答案初稿")
                .build();
    }

    @Bean
    public ChatClient reviewerAgent(ChatClient.Builder
                                            builder) {
        return builder
                .defaultSystem("You are a strict supervisor, the user " +
                        "will give you a draft and a question,你要挑出初稿里的错误/遗漏/含糊之处,然后输出一份改进后 的最终答案")
                                .build();
    }

    @Bean
    public ChatClient translatorAgent(ChatClient.Builder builder){
        return builder.defaultSystem("你是翻译专家,把收到的文本准确翻译成英文,只输出译文").build();
    }

    @Bean
    public ChatClient poetAgent(ChatClient.Builder
                                        builder) {
        return builder.defaultSystem("你是诗人,根据收到的主题写一首简短的中文现代诗").build();
    }

    @Bean
    public ChatClient orchestratorAgnet(ChatClient.Builder builder){
        return builder.defaultSystem("你是一个任务协调者。你手下有若干专家工具,请根 据用户的需求," +
                "选择并调用最合适的专家工具来完成任务,然后把专家的结果整理后返回。" +
                "需要专家处理的部分,不要自己直接回答,必须交给对应的工具。").build();
    }

    @Bean
    public ChatClient
    technicalAnalystAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("你是一个技术面分析师:从均线/趋势/量价/支撑阻力等技术角度,简短分析给定股票,只谈技术面").build();
    }

    @Bean
    public ChatClient
    fundamentalAnalystAgent(ChatClient.Builder builder) {
        return
                builder.defaultSystem("你是一个基本面分析师, 从业绩/估值/盈利能力/行业地位角度分析,只谈基本面").build();
    }

    @Bean
    public ChatClient newsAnalystAgent(ChatClient.Builder
                                               builder) {
        return builder.defaultSystem("从近期事件、政策,市场情绪角度分析一下这只股票").build();
    }

    @Bean
    public ChatClient
    bullResearcherAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("【看多研究员:你会收到一只股票和几份分析。你的唯一职责是【只找上涨/看多的 理由】,尽力论证它会涨,绝不提任何利空").build();
    }

    @Bean
    public ChatClient
    bearResearcherAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("你会收 到一只股票和几份分析。你的唯一职责是只找下跌/看空的理由,尽力论证它有风险会跌,绝不提任何利好").build();
    }

    @Bean
    public ChatClient
    tradingDecisionAgent(ChatClient.Builder builder) {
        return builder.defaultSystem("你会收到三份分析 + 看多看空双方的辩论。综合所有信息,给出最终结论:方向(看 多/看空/观望)、核心理由、主要风险、置信度】").build();
    }

    @Bean
    public ChatClient
    riskControlAgent(ChatClient.Builder builder)
    {
        return builder.defaultSystem("你会收到分析和多空辩论。专门评估这笔交易的风险——最大回撤、需注意的风险点、建议仓位上限。不谈 机会,只谈风险控制").build();
    }

    @Bean
    public ChatClient
    retrospectiveAgent(ChatClient.Builder
                               builder) {
        return
                builder.defaultSystem("复盘官:你会收到一条完整的决策链(分析→辩论→风控→决策)。你的职责是[评价这次决策的逻辑质量]——推理是否严谨?有没 有自相矛盾或遗漏的角度?证据是否支撑结论?不重 新做决策,只评判决策的过程好不好").build()
                ;
    }

    @Bean
    public ChatClient rerankerAgent(ChatClient.Builder builder){
        return builder.defaultSystem("你是一个相关性打分器。用户会给你一个【问题】和一份【文档】," +
                        "你只需判断这份文档对回答该问题的相关性有多高," +
                        "输出一个 0 到 10 的整数,只输出数字,不要任何其他文字。"

        ).build();
    }

    @Bean
    public ChatClient
    myStrategyAnalystAgent(ChatClient.Builder
                                   builder) {
        return builder.defaultSystem(
                "你是严格遵循用户个人交易体系的分析师。用户会给你[他的交易原则]和[行情数据]," +
                        "你必须只依据这些原则分析,不要套用通用教科书逻辑," +
                        "并明确指出当前行情符合或违反了哪几条原则。"
        ).build();
    }

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
