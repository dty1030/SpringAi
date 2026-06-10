package com.maorou.SpringAIDemo.functions;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StockDataClient {

    private final RestTemplate restTemplate = new RestTemplate();
    public String getIndicators(String symbol){
        return restTemplate.getForObject(
                "http://localhost:8000/indicators?symbol={symbol}",
                String.class,
                symbol

        );
    }
}
