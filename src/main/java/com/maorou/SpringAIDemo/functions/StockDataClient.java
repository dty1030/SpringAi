package com.maorou.SpringAIDemo.functions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StockDataClient {

    private final String baseUrl;
    public StockDataClient(
            @Value("${app.trading-data.base-url:http://localhost:8000}") String baseUrl
    ){
        this.baseUrl = baseUrl;
    }

    private final RestTemplate restTemplate = new RestTemplate();


    public String getNews(String symbol, String name){
        return restTemplate.getForObject(
                baseUrl + "/news?symbol={symbol}&name={name}",
                String.class,
                symbol, name

        );
    }
    public String getIndicators(String symbol) {
        return restTemplate.getForObject(
                baseUrl + "/indicators?symbol={symbol}",
                String.class,
                symbol);
    }

    public String getStrategySignals(String symbol) {
        return restTemplate.getForObject(
                baseUrl + "/strategy?symbol={symbol}",
                String.class,
                symbol);

    }

    public String getMarketSnapshot(String symbol){
        return restTemplate.getForObject(
                baseUrl + "/market-snapshot?symbol={symbol}",
                String.class,
                symbol
        );
    }

    /**
     *   MA5/10/20/30/60/120/240
     *   是否站上均线
     *   距离均线百分比
     * @param symbol
     * @return
     */
    public String getTechnicalFacts(String symbol){
        return restTemplate.getForObject(
                baseUrl + "/technical-facts?symbol={symbol}",
                String.class,
                symbol
        );

    }


}
