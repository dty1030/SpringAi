package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.functions.StockDataClient;
import com.maorou.SpringAIDemo.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradingController {

    @Autowired
    ChatClient technicalAnalystAgent;
    @Autowired
    ChatClient fundamentalAnalystAgent;
    @Autowired
    ChatClient newsAnalystAgent;
    @Autowired
    ChatClient bullResearcherAgent;
    @Autowired
    ChatClient bearResearcherAgent;
    @Autowired
    ChatClient tradingDecisionAgent;
    @Autowired
    StockDataClient stockDataClient;
    @Autowired
    RagService ragService;
    @Autowired ChatClient riskControlAgent;
    @Autowired ChatClient retrospectiveAgent;

    @GetMapping("api/trading/analyze")
    public TradingReport analyze(@RequestParam String symbol){
        String technical = technicalAnalystAgent.prompt(symbol)
                .call().content();
        String fundamental = fundamentalAnalystAgent.prompt(symbol)
                .call().content();
        String news = newsAnalystAgent.prompt(symbol)
                .call().content();

        //Layer 2
        String analyses = "Stock" + symbol
                + "\n【技术面】" + technical
                + "\n【基本面】" + fundamental
                + "\n【新闻面】" + news;
        String bull = bullResearcherAgent.prompt(analyses).call().content();
        String bear = bearResearcherAgent.prompt(analyses).call().content();
        //Layer 3
        String context = analyses
                + "\n【看多方】" + bull
                + "\n【看空方】" + bear;

        String decision = tradingDecisionAgent.prompt(context).call().content();

        return new TradingReport(technical, fundamental, news, bull, bear, decision);

    }


    record TradingReport(String technical, String fundamental, String news,
                              String bull, String bear, String decision){}



    @GetMapping("/api/trading/technical-real")
    public TechnicalReport technicalReal(@RequestParam String symbol){

        //1.拿真实JSON
        String data = stockDataClient.getIndicators(symbol);
        //2.塞进Agent
        String analysis = technicalAnalystAgent.prompt(
                "这是股票 " + symbol +
                        "最近的真实指标数据(JSON格式):\n" + data +
                        "\n请基于这些真实数字做完整的技术面分析,并且在分析中引用具体数值。")
                .call().content();
        return new TechnicalReport(data, analysis);
    }


    record TechnicalReport(String data, String analysis)
    {}


    @GetMapping("/api/trading/fundamental-rag")
    public String fundamentalRag(@RequestParam String symbol){
        return fundamentalAnalystAgent.prompt("请基于研报资料,分析 " + symbol + "的基本面投资价值")
                .advisors(QuestionAnswerAdvisor.builder(ragService.getVectorStore()).build())
                .call().content();
    }

    @GetMapping("/api/trading/full")
    public FullReport full(@RequestParam String code, @RequestParam String name){
        String data = stockDataClient.getIndicators(code);
        String technical = technicalAnalystAgent.prompt(
                "股票" + name + " 近期真实指标(JSON):\n" +
                        data + "\n请基于这些真实数字做技术面分析")
                .call().content();

        //2. 基本面: RAG 研报
        String fundamental = fundamentalAnalystAgent.prompt("请基于研报资料分析 " + name + " 的基本面")
                .advisors(QuestionAnswerAdvisor.builder(ragService.getVectorStore()).build())
                .call().content();
        //3. 新闻面 TODO : 接入真实新闻源
        String news = newsAnalystAgent.prompt(name).call().content();
        //4. 汇总三部分分析
        String analyses = "股票:" + name
                + "\n【技术面】" + technical
                + "\n【基本面】" + fundamental
                + "\n【新闻面】" + news;
        //5. 多空辩论
        String bull = bullResearcherAgent.prompt(analyses).call().content();
        String bear = bearResearcherAgent.prompt(analyses).call().content();
        String debate = analyses +"\n【看多方】" + bull +"\n【看空方】" + bear;
        //6. 风控
        String risk = riskControlAgent.prompt(debate).call().content();
        // 7. 决策:综合辩论 + 风控
        String decision = tradingDecisionAgent.prompt(debate + "\n【风控】" + risk).call().content();
        //8. 复盘
        String retrospective = retrospectiveAgent.prompt("辩论: " + debate
                + "\n风控: " + risk
                + "\n决策: " + decision)
                .call().content();
        return new FullReport(technical, fundamental, news,
                bull, bear, risk,
                decision, retrospective);

    }



    record FullReport(String technical, String fundamental, String news,
                      String bull, String bear, String risk,
                      String decision, String retrospective){}


    @GetMapping("/api/trading/debate")
    public DebateReport debate(@RequestParam String name,
                               @RequestParam(defaultValue = "2") int rounds){
        //简略版
        String technical = technicalAnalystAgent.prompt(name).call().content();

        String fundamental = fundamentalAnalystAgent.prompt(name).call().content();

        String news = newsAnalystAgent.prompt(name).call().content();

        String analyses = "股票:" + name
                + "\n[技术面] " + technical
                + "\n[基本面] " + fundamental
                + "\n[新闻面] " + news;

        //transcript : 一份不断增长的辩论记录
        String transcript = analyses;

        for(int round = 1; round <= rounds; round++){
            String task = (round == 1) ? "请基于以上分析,提出你的看多核心论点(开场陈述)"
                    : "请反驳看空方最新的观点,并强化你的看多立场";
            String bull =
                    bullResearcherAgent.prompt(transcript + "\n\n第"
                                    + round + "轮 看多发言:" + task)
                            .call().content();
            transcript += "\n\n[看多·第" + round + "轮]" + bull;
            // 【你写】看空方:读(已含看多本轮的)transcript,反驳看多
            String bear = bearResearcherAgent.prompt(
                    transcript + "这是股票的相关技术面, 基本面, 新闻面的分析第" + round
                            + "轮,\n" + "请你根据分析,反驳看多").call().content();
            transcript += "\n\n[看空·第" + round + "轮]"+ bear;



        }
        // 决策读完整多轮辩论记录
        String decision = tradingDecisionAgent.prompt(
                        transcript + "\n\n请基于以上多轮辩论,给出最终决策(方向/理由/风险/置信度)")
                .call().content();
        return new DebateReport(transcript, decision);}


    record DebateReport(String transcript, String decision) {}
}
