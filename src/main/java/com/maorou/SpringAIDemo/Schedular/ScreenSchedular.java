package com.maorou.SpringAIDemo.Schedular;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maorou.SpringAIDemo.functions.ServerChanNotifier;
import com.maorou.SpringAIDemo.functions.StockDataClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScreenSchedular {

    StockDataClient stockDataClient;
    private final ServerChanNotifier serverChanNotifier;
    private final ObjectMapper objectMapper;
    public ScreenSchedular(StockDataClient stockDataClient, ServerChanNotifier serverChanNotifier, ObjectMapper objectMapper){
        this.stockDataClient = stockDataClient;
        this.serverChanNotifier = serverChanNotifier;
        this.objectMapper = objectMapper;
    }


    @Scheduled(cron = "0 30 20 * * MON-FRI")
    //@Scheduled(fixedRate = 30000)
    public void autoScreen() throws Exception {
        String hits = stockDataClient.screen();
        log.info("收盘选股结果: {}", hits);
        int count = objectMapper.readTree(hits).get("命中数").asInt();
        if (count > 0){
            serverChanNotifier.push(
                    "收盘选股: 命中 " + count + "只", hits
            );
        }

    }

}
