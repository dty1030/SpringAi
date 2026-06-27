package com.maorou.SpringAIDemo.functions;

import com.maorou.SpringAIDemo.service.ReviewSaveService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ReviewTool {
    private final ReviewSaveService reviewSaveService;

    public ReviewTool(ReviewSaveService reviewSaveService) {
        this.reviewSaveService = reviewSaveService;
    }

    @Tool(description = "当用户想把某只股票的复盘分析/复盘结论保存或者记录到知识库时调用")
    public String saveReview(
            @ToolParam(description = "股票代码, 例如sh688082 或300001") String symbol,
            @ToolParam(description = "股票名称, 例如 盛美上海") String name,
            @ToolParam(description = "走势数据(K线/日线文本)") String marketData,
            @ToolParam(description = "用户的复盘结论/分析") String conclusion) throws Exception {
        String path = reviewSaveService.save(symbol, name, marketData, conclusion);
        return "复盘已保存到 " + path + "，RAG 已刷新";   // 这句会被喂回模型,让它回话给用户


    }

}
