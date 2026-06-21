package com.maorou.SpringAIDemo.Schedular;

import com.maorou.SpringAIDemo.service.DecisionEvalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DecisionEvalScheduler {
    private final DecisionEvalService decisionEvalService;

    public DecisionEvalScheduler(DecisionEvalService decisionEvalService) {
        this.decisionEvalService = decisionEvalService;
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI")
    //@Scheduled(fixedRate = 30000)   // 每 30 秒跑一次,纯为看它真的在自动触发
    public void autoSettle(){
        int n = decisionEvalService.settleDue(5);   // N=5 个自然日(暂时;交易日精确版留 V2)
        log.info("定时结算完成,本次结算 {} 条", n);
    }
}
