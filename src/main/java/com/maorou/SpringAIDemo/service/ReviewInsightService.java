package com.maorou.SpringAIDemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maorou.SpringAIDemo.functions.StockDataClient;
import com.maorou.SpringAIDemo.prompt.ReviewInsightPromptBuilder;
import com.maorou.SpringAIDemo.utils.ReviewInsightJsonReport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import static com.maorou.SpringAIDemo.utils.JsonTextUtils.extractJsonObject;

@Service
public class ReviewInsightService {

    @Autowired
    ChatClient myStrategyAnalystAgent;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StockDataClient stockDataClient;

    @Autowired
    RagService ragService;


    @Autowired
    ReviewInsightPromptBuilder reviewInsightPromptBuilder;


    public String analyzeMarkdown(String symbol) throws IOException {
        ReviewInsightContext ctx = buildReviewInsightContext(symbol);
        String prompt = reviewInsightPromptBuilder.buildMarkdownPrompt(symbol, ctx);
        return myStrategyAnalystAgent.prompt(prompt).call().content();
    }

    public ReviewInsightJsonReport analyzeJson(String symbol) throws IOException {
        ReviewInsightContext ctx = buildReviewInsightContext(symbol);
        ReviewInsightJsonReport.CurrentFacts currentFacts = buildCurrentFacts(ctx);

        String prompt = reviewInsightPromptBuilder.buildMarkdownPrompt(symbol, ctx)
                + reviewInsightPromptBuilder.buildJsonOutputInstruction();

        String raw = myStrategyAnalystAgent.prompt(prompt).call().content();
        String json = extractJsonObject(raw);

        ReviewInsightAiPart aiPart =
                objectMapper.readValue(json, ReviewInsightAiPart.class);

        return new ReviewInsightJsonReport(
                currentFacts,
                aiPart.similarCases(),
                aiPart.similarityLevel(),
                aiPart.riskNotes(),
                aiPart.conclusion()
        );
    }

    /**
     * 内部方法---------
     */
    private ReviewInsightContext buildReviewInsightContext(String symbol) throws IOException {


        String data = stockDataClient.getIndicators(symbol);
        String signals = stockDataClient.getStrategySignals(symbol);
        String snapshot = stockDataClient.getMarketSnapshot(symbol);
        String technicalFacts = stockDataClient.getTechnicalFacts(symbol);

        String reviewQuery = symbol
                + "\n当前技术事实:\n" + technicalFacts
                + "\n当前策略信号:\n" + signals
                + "\n检索目标：历史复盘中相似的均线位置、量能变化、连续阴线、反弹受阻、趋势修复、出货或洗盘案例";
        List<Document> reviewDocs = ragService.search(reviewQuery, 5);

        String reviewContext = reviewDocs.stream()
                .map(document -> "来源: " + document.getMetadata().get("source")
                        + "\n内容: \n" + document.getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        return new ReviewInsightContext(
                data,
                signals,
                snapshot,
                technicalFacts,
                reviewContext
        );
    }


    private ReviewInsightJsonReport.CurrentFacts buildCurrentFacts(ReviewInsightContext ctx)
            throws IOException {

        // 1. 用 ObjectMapper 把 ctx.technicalFacts() 转成 Map
        Map<String, Object> technical = objectMapper.readValue(ctx.technicalFacts(), Map.class);
        // 2. 用 ObjectMapper 把 ctx.signals() 转成 Map
        Map<String, Object> signals =
                objectMapper.readValue(ctx.signals(), Map.class);
        // 3. 从 technicalFacts 里提取 ma5/above_ma5/close_ma5_gap_pct ...
        int[] periods = {5, 10, 20, 30, 60, 120, 240};
        List<ReviewInsightJsonReport.MovingAverageFact> movingAverages =
                new ArrayList<>();
        for (int period : periods) {
            String maKey = "ma" + period;
            String aboveKey = "above_ma" + period;
            String gapKey = "close_ma" + period + "_gap_pct";

            if (!technical.containsKey(maKey)
                    || !technical.containsKey(aboveKey)
                    || !technical.containsKey(gapKey)) {
                continue;
            }

            double value = ((Number) technical.get(maKey)).doubleValue();
            boolean above = (Boolean) technical.get(aboveKey);
            double gapPct = ((Number) technical.get(gapKey)).doubleValue();

            movingAverages.add(new ReviewInsightJsonReport.MovingAverageFact(
                    period,
                    value,
                    above,
                    gapPct
            ));
            // 4. 从 signals 里提取 是否明显放量 / 是否温和放量 / 连续阴线天数
            // 5. new CurrentFacts(...)
        }
        boolean volumeClearlyExpanded = (Boolean) signals.get("是否明显放量");
        boolean volumeMildlyExpanded = (Boolean) signals.get("是否温和放量");
        int consecutiveBearishDays = ((Number) signals.get("连续阴线天数")).intValue();

        return new ReviewInsightJsonReport.CurrentFacts(movingAverages,
                volumeClearlyExpanded,
                volumeMildlyExpanded,
                consecutiveBearishDays,
                "均线事实来自 technical-facts，量能与连续阴线来自 strategy 信号。");
    }





    public record ReviewInsightContext(  String data,
                                         String signals,
                                         String snapshot,
                                         String technicalFacts,
                                         String reviewContext){}

    private record ReviewInsightAiPart(
            List<ReviewInsightJsonReport.SimilarCase> similarCases,
            String similarityLevel,
            List<String> riskNotes,
            String conclusion
    ) {}

}
