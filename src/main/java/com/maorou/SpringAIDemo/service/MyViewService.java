package com.maorou.SpringAIDemo.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.maorou.SpringAIDemo.controller.TradingController;
import com.maorou.SpringAIDemo.functions.StockDataClient;
import com.maorou.SpringAIDemo.utils.MyViewContext;
import com.maorou.SpringAIDemo.utils.MyViewJsonReport;
import com.maorou.SpringAIDemo.workspace.LocalWorkspaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MyViewService {

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
        MyViewContext ctx = buildMyViewContext(symbol);
        String prompt = buildMyViewPrompt(symbol, ctx);
        return myStrategyAnalystAgent.prompt(prompt).call().content();
    }

    public MyViewJsonReport analyzeJson(String symbol) throws IOException {
        MyViewContext ctx = buildMyViewContext(symbol);
        String prompt = buildMyViewPrompt(symbol, ctx) + buildJsonOutputInstruction();
        String raw = myStrategyAnalystAgent.prompt(prompt).call().content();
        String json = extractJsonObject(raw);
        MyViewJsonReport report = objectMapper.readValue(json, MyViewJsonReport.class);
        MyViewJsonReport normalizedReport = normalizeMyViewReport(report);
        List<String> validationWarnings = validateMyViewReport(normalizedReport);

        return new MyViewJsonReport(
                normalizedReport.currentFacts(),
                normalizedReport.matchedReviewPatterns(),
                normalizedReport.principleChecks(),
                normalizedReport.decision(),
                validationWarnings
        );

    }


    /**
     * 内部方法---------
     */
    private MyViewContext buildMyViewContext(String symbol) throws IOException {
        String principles = Files.readString(
                localWorkspaceStrategy
                        .allowedFileBaseDir()
                        .resolve("my-strategy.txt"));

        String data = stockDataClient.getIndicators(symbol);
        String signals = stockDataClient.getStrategySignals(symbol);
        String snapshot = stockDataClient.getMarketSnapshot(symbol);
        String technicalFacts = stockDataClient.getTechnicalFacts(symbol);

        List<Document> reviewDocs = ragService.search(
                symbol + " 主力出货 洗盘 MA20 成交额 放大 复盘", 5
        );

        String reviewContext = reviewDocs.stream()
                .map(document -> "来源: " + document.getMetadata().get("source")
                        + "\n内容: \n" + document.getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        return new MyViewContext(
                principles,
                data,
                signals,
                snapshot,
                technicalFacts,
                reviewContext
        );
    }

    private String buildMyViewPrompt(String symbol, MyViewContext ctx) {
        return "[我的交易原则]\n" + ctx.principles() +
                "\n\n【实时行情快照(JSON)】\n" + ctx.snapshot() +
                "\n\n【已精确计算好的信号事实(JSON,直接采信,不要自己从价格推断)】\n" + ctx.signals() +
                "\n\n【已计算好的技术事实(JSON,直接采信,不要自己重新计算均线关系)】\n" + ctx.technicalFacts() +
                "\n必须严格按 above_maX 字段判断是否高于/低于均线。true 表示高于，false 表示低于。" +
                "\n\n【" + symbol + "近期指标数据(JSON)】\n" + ctx.data() +
                "\n\n【我的历史复盘案例(RAG检索结果,只能作为经验参考,不能当作当前事实)】\n" + ctx.reviewContext() +
                "\n\n请严格按我的交易原则，并参考历史复盘案例分析这只股票。" +
                "\n要求：" +
                "\n1. 先说明当前事实，不要把历史案例当成当前行情。" +
                "\n2. 再说明命中了哪些历史复盘模式。" +
                "\n3. 最后逐条说明符合或违反了哪些交易原则。" +
                "\n4. 必须严格遵守【已精确计算好的信号事实】中的量能字段：\n" +
                "4.1 如果 是否明显放量=false，不能说明显放量。\n" +
                "4.2 如果 是否温和放量=false，不能说温和放量。\n" +
                "4.3 如果 成交量情况 表示不能称为明显放量，不要使用“放量”作为当前事实。" +
                "\n5. 历史复盘案例只能用于识别相似风险，不能直接把历史结论套到当前股票。" +
                "\n5.1 如果当前事实没有同时满足历史案例的关键条件，必须说明只是部分相似，不能下确认性结论。" +
                "\n5.2 如果量能字段显示没有明显放量，不能说当前已经符合'放量出货'模式，只能说需要警惕。";
    }

    private String buildJsonOutputInstruction() {
        return """

              6. 你必须只输出JSON格式，不要Markdown，不要解释。
              字段必须严格符合以下结构：
              {
                "currentFacts": {
                  "priceBelowMa20": true,
                  "volumeClearlyExpanded": false,
                  "volumeMildlyExpanded": false,
                  "consecutiveBearishDays": 2,
                  "evidence": "..."
                },
                "matchedReviewPatterns": [
                  {
                    "pattern": "...",
                    "matchLevel": "partial",
                    "reason": "..."
                  }
                ],
                "principleChecks": [
                  {
                    "principle": "...",
                    "status": "triggered",
                    "evidence": "..."
                  }
                ],
                "decision": {
                  "action": "watch",
                  "confidence": "medium",
                  "reason": "..."
                },
                "warnings": []
              }
              """;
    }
    private List<String> validateMyViewReport(MyViewJsonReport report) {
        List<String> warnings = new ArrayList<>();

        if (!report.currentFacts().volumeClearlyExpanded()
                && !report.currentFacts().volumeMildlyExpanded()) {
            for (MyViewJsonReport.PrincipleCheck check : report.principleChecks()) {
                if (check.principle().contains("成交")
                        && "triggered".equals(check.status())) {
                    warnings.add("量能事实显示未放量，但模型触发了成交额放大相关原则。");
                }
            }
        }

        return warnings;
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("模型没有返回合法 JSON 对象: " + text);
        }

        return text.substring(start, end + 1);
    }

    private MyViewJsonReport normalizeMyViewReport(MyViewJsonReport report) {
        List<MyViewJsonReport.PrincipleCheck> normalizedChecks = new ArrayList<>();

        boolean noVolumeExpansion =
                !report.currentFacts().volumeClearlyExpanded()
                        && !report.currentFacts().volumeMildlyExpanded();

        for (MyViewJsonReport.PrincipleCheck check : report.principleChecks()) {
            String normalizedStatus = normalizeStatus(check.status());

            if (noVolumeExpansion
                    && check.principle().contains("成交")
                    && "triggered".equals(normalizedStatus)) {

                normalizedChecks.add(new MyViewJsonReport.PrincipleCheck(
                        check.principle(),
                        "not_triggered",
                        "后端校验修正：量能事实显示未明显放量，不能触发成交额放大相关原则。原模型依据：" + check.evidence()
                ));
            } else {
                normalizedChecks.add(new MyViewJsonReport.PrincipleCheck(
                        check.principle(),
                        normalizedStatus,
                        check.evidence()
                ));
            }
        }

        return new MyViewJsonReport(
                report.currentFacts(),
                report.matchedReviewPatterns(),
                normalizedChecks,
                report.decision(),
                report.warnings()
        );
    }
    private String normalizeStatus(String status) {
        if (status == null) {
            return "not_triggered";
        }

        return switch (status.trim()) {
            case "triggered" -> "triggered";
            case "violated" -> "violated";
            case "not triggered", "not-triggered", "not_triggered" -> "not_triggered";
            default -> status;
        };
    }

}
