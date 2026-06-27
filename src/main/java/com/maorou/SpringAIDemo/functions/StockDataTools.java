package com.maorou.SpringAIDemo.functions;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StockDataTools {

    @Autowired
    StockDataClient stockDataClient;


    @Tool(description="获取某股票近期真实新闻。需要了解消息面/利空利好时调用")
    public String getNews(@ToolParam(description="股票代码，带交易所前缀，如 sh600519") String symbol,
                          @ToolParam(description="股票中文名称，如 贵州茅台") String name){

        return stockDataClient.getNews(symbol, name);

    }
    @Tool(description="获取某股票的均线技术指标(是否站上某均线，距离某均线的百分比) 需要判断价格趋势时调用")
    public String getTechnicalFacts(
            @ToolParam(description="股票代码，带交易所前缀，如 sh600519") String symbol){
        return stockDataClient.getTechnicalFacts(symbol);
    }
    @Tool(description = "获取某股票由 Python 精确算好的量价信号事实：量比、是否放量、是否倍量柱、当日阴/阳线、连续阴线天数。需要从成交量和 K 线形态判断主力是否控盘、是否放量突破时调用")
    public String getStrategySignals(
            @ToolParam(description="股票代码，带交易所前缀，如 sh600519") String symbol){
        return stockDataClient.getStrategySignals(symbol);
    }


    @Tool(description = "获取某股票最近若干交易日的原始行情：每日收盘价和涨跌幅。需要知道当前价格水平、近期涨跌幅度时调用（不含均线分析）")
    public String getIndicators(@ToolParam(description = "股票代码，带交易所前缀，如 sh600519") String symbol){
        return stockDataClient.getIndicators(symbol);

    }
}
