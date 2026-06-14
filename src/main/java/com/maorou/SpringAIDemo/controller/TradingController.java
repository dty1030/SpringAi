package com.maorou.SpringAIDemo.controller;

import com.maorou.SpringAIDemo.functions.StockDataClient;
import com.maorou.SpringAIDemo.service.RagService;
import com.maorou.SpringAIDemo.workspace.LocalWorkspaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    ChatClient riskControlAgent;
    @Autowired
    ChatClient retrospectiveAgent;
    @Autowired
    ChatClient rerankerAgent;
    @Autowired
    ChatClient myStrategyAnalystAgent;
    @Autowired
    LocalWorkspaceStrategy localWorkspaceStrategy;

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
        String snapshot = stockDataClient.getMarketSnapshot(code);
        String technicalFacts = stockDataClient.getTechnicalFacts(code);
        String technical = technicalAnalystAgent.prompt(
                "股票" + name + " 实时行情快照(JSON):\n" + snapshot +
                        "\n\n股票" + name +
                        " 近期真实指标(JSON):\n" + data +
                        "\n\n【已计算好的技术事实(JSON,直接采信,不要自己重新计算均线关系)】\n" + technicalFacts +
                "\n请结合实时行情快照、近期指标数据和已计算好的技术事实做技术面分析，并引用具体数值。" +
                        "\n必须逐项说明 above_ma5、above_ma10、above_ma20、above_ma30、above_ma60、above_ma120、above_ma240 的 true/ false 结果。" +
                        "\n如果某个 above_maX 是 false， 必须明确说价格低于该均线，不要说成站上或高于。" +
        "\n不要自己重新计算均线关系，以技术事实 JSON 为准。" +   "\n每一条均线结论必须与对应 above_maX 字段一致： true 表示高于该均线，false 表示低于该均线。")
                .call().content();

        //2. 基本面: RAG 研报
        String fundamental = fundamentalAnalystAgent.prompt("请基于研报资料分析 " + name + " 的基本面")
                .advisors(QuestionAnswerAdvisor.builder(ragService.getVectorStore()).build())
                .call().content();
        //3. 新闻面
        String newsJson = stockDataClient.getNews(code, name);
        String news = newsAnalystAgent.prompt(
                "以下是股票 " + name + " 最近的真实新闻(JSON):\n" + newsJson +
                        "\n请基于这些真实新闻做新闻面与市场情绪分析,引用具体事件。")
                .call().content();
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

    private int rerankScore(String question, String docText){
        String resp = rerankerAgent.prompt(
                "问题:" + question +
                        "\n文档:" + docText
        ).call().content();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(resp);
        return m.find() ? Integer.parseInt(m.group()) : 0;
    }

    record Scored(int rerankScore, double cosine, String text){}

    @GetMapping("/api/trading/fundamental-rerank")
    public Map<String, Object> fundamentalRerank(@RequestParam String question){
        List<Document> candidates = ragService.search(question, 10);
        List<Scored> scored = new ArrayList<>();
        for (Document d : candidates){
            int score = rerankScore(question, d.getText());
            scored.add(new Scored(score, d.getScore(), d.getText()));
        }
        scored.sort(Comparator.comparingInt(Scored::rerankScore).reversed());
        List<Scored> top3 = scored.stream()
                .limit(3).toList();

        String context = top3
                .stream()
                .map(Scored::text)
                .collect(Collectors.joining("\n"));
        String answer = fundamentalAnalystAgent.prompt(
                "请基于以下资料分析。 \n问题:" +
                        question + "\n资料:\n" + context)
                .call().content();
        return Map.of("rerank明细", scored, "分析", answer);

    }


    @GetMapping("/api/trading/my-view")
    public String myView(@RequestParam String symbol) throws IOException {
        //读取现原则文件
        String principles = Files.readString(
                localWorkspaceStrategy.allowedFileBaseDir().resolve("my-strategy.txt") );
        String data = stockDataClient.getIndicators(symbol);
        String signals = stockDataClient.getStrategySignals(symbol);
        String snapshot = stockDataClient.getMarketSnapshot(symbol);

        return myStrategyAnalystAgent.prompt(
                "[我的交易原则]\n" + principles +
                        "\n\n【实时行情快照(JSON)】\n" + snapshot +
                        "\n\n【已精确计算好的信号事实(JSON,直接采信,不要自己从价格推断)】\n" + signals +
                        "\n\n【" + symbol + "近期指标数据(JSON)】\n" +
                        data + "\n请严格按我上面我的原则分析这只股票, 逐条说明符合或违反了哪条。"
        ).call().content();
    }
}
