package com.maorou.SpringAIDemo.prompt;


import com.maorou.SpringAIDemo.service.ReviewInsightService;
import org.springframework.stereotype.Component;

@Component
public class ReviewInsightPromptBuilder {

    public String buildMarkdownPrompt(
            String symbol,
            ReviewInsightService.ReviewInsightContext ctx
    ){
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

    public String buildJsonOutputInstruction() {
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
}
