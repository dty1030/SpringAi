package com.maorou.SpringAIDemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maorou.SpringAIDemo.functions.StockDataClient;
import com.maorou.SpringAIDemo.utils.ReviewInsightJsonReport;
import com.maorou.SpringAIDemo.workspace.LocalWorkspaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.maorou.SpringAIDemo.utils.*;

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
    LocalWorkspaceStrategy localWorkspaceStrategy;


    public String analyzeMarkdown(String symbol) throws IOException {
        ReviewInsightContext ctx = buildReviewInsightContext(symbol);
        String prompt = buildReviewInsightPrompt(symbol, ctx);
        return myStrategyAnalystAgent.prompt(prompt).call().content();


    }

    public ReviewInsightJsonReport analyzeJson(String symbol) throws IOException {
        ReviewInsightContext ctx = buildReviewInsightContext(symbol);
        ReviewInsightJsonReport.CurrentFacts currentFacts = buildCurrentFacts(ctx);

        String prompt = buildReviewInsightPrompt(symbol, ctx) + buildJsonOutputInstruction();

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
    private String buildReviewInsightPrompt(String symbol, ReviewInsightContext ctx) {
        return  "\n\n【实时行情快照(JSON)】\n" + ctx.snapshot() +
                "\n\n【已精确计算好的信号事实(JSON,直接采信,不要自己从价格推断)】\n" + ctx.signals() +
                "\n\n【已计算好的技术事实(JSON,直接采信,不要自己重新计算均线关系)】\n" + ctx.technicalFacts() +
                "\n必须严格按 above_maX 字段判断是否高于/低于均线。true 表示高于，false 表示低于。" +
                "\n\n【" + symbol + "近期指标数据(JSON)】\n" + ctx.data() +
                "\n\n【我的历史复盘案例(RAG检索结果,只能作为经验参考,不能当作当前事实)】\n" + ctx.reviewContext() +
                "\n\n请基于当前行情事实和历史复盘案例，做复盘相似分析。" +
                "\n注意：这不是交易建议，也不是确认当前正在发生历史案例中的结论。" +
                "\n要求：" +
                "\n1. 先说明当前事实，不要把历史案例当成当前行情。" +
                "\n2. 再列出检索到的历史复盘案例，并说明每个案例的核心模式。" +
                "\n3. 对比当前行情和历史案例的相似点。" +
                "\n4. 必须说明不相似点或缺失的关键条件。" +
                "\n5. 给出相似程度：none / partial / high，只能三选一。" +
                "\n6. 如果只是部分相似，不能使用“已经符合”“确认出货”“确认洗盘”等确定性表述。" +
                "\n7. 必须严格遵守【已精确计算好的信号事实】中的量能字段：\n" +
                "7.1 如果 是否明显放量=false，不能说明显放量。\n" +
                "7.2 如果 是否温和放量=false，不能说温和放量。\n" +
                "7.3 如果 成交量情况 表示不能称为明显放量，不要使用“放量”作为当前事实。" +
                "\n8. 历史复盘案例只能用于识别相似风险，不能直接把历史结论套到当前股票。" +
                "\n8.1 如果当前事实没有同时满足历史案例的关键条件，必须说明只是部分相似，不能下确认性结论。" +
                "\n8.2 如果量能字段显示没有明显放量，不能说当前已经符合'放量出货'模式，只能说需要警惕。" +
                "\n\n输出结构必须包含：" +
                "\n## 当前事实" +
                "\n## 相似历史案例" +
                "\n## 相似点" +
                "\n## 不相似点或缺失条件" +
                "\n## 相似程度" +
                "\n## 风险提示";
    }

    private String buildJsonOutputInstruction(){
        return """

            你必须只输出JSON，不要Markdown，不要解释。
            JSON字段必须严格符合以下结构:
            {
              "similarCases": [
                {
                  "source": "...",
                  "corePattern": "...",
                  "similarities": ["..."],
                  "missingConditions": ["..."]
                }
              ],
              "similarityLevel": "none|partial|high",
              "riskNotes": ["..."],
              "conclusion": "..."
            }

            不要输出 currentFacts 字段，currentFacts 由后端程序生成。
            similarCases 只能来自【我的历史复盘案例】。
            similarities 表示当前行情与历史案例相似的事实。
            missingConditions 表示当前行情尚未满足的历史案例关键条件。
            similarityLevel 只能是 none、partial、high 三者之一。
            不要输出交易建议，只输出相似性分析结论。
            """;
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
