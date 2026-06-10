package com.maorou.SpringAIDemo.controller;

import org.springframework.ai.chat.client.ChatClient;
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




}
