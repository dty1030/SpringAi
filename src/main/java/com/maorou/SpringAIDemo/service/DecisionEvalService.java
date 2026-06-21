package com.maorou.SpringAIDemo.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maorou.SpringAIDemo.entity.Decision;
import com.maorou.SpringAIDemo.functions.StockDataClient;
import com.maorou.SpringAIDemo.mapper.DecisionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DecisionEvalService {
    private final DecisionMapper decisionMapper;
    private final StockDataClient stockDataClient;
    private final ObjectMapper objectMapper;

    public DecisionEvalService(DecisionMapper decisionMapper, StockDataClient stockDataClient, ObjectMapper objectMapper) {
        this.decisionMapper = decisionMapper;
        this.stockDataClient = stockDataClient;
        this.objectMapper = objectMapper;
    }
    //For test
    public Long saveDummy(){
        Decision d = new Decision();
        d.setCode("贵州茅台");
        d.setName("贵州茅台");
        d.setSource("test");
        d.setDirection("看多");
        d.setConfidence(70);
        d.setReason("管道测试");
        d.setDecidedAt(LocalDateTime.now());
        d.setDecidedPrice(new BigDecimal("1275.00"));   // ⭐ 注意构造方式,见下
        d.setStatus("PENDING");
        decisionMapper.insert(d);     // ← 白送的方法,把对象插进表
        return d.getId();             // ← insert 后 id 会被自动回填进对象(下面讲)
    }
    public Long save(String code, String name,  String direction,
                     int confidence, String reason, BigDecimal decidedPrice){
        Decision d = new Decision();
        d.setCode(code);
        d.setName(name);
        d.setSource("full");
        d.setDirection(direction);       // ← 直接收 String,不碰 DecisionResult
        d.setConfidence(confidence);
        d.setReason(reason);
        d.setDecidedAt(LocalDateTime.now());
        d.setDecidedPrice(decidedPrice); // ← 直接收 BigDecimal,不碰 latestClose
        d.setStatus("PENDING");
        decisionMapper.insert(d);        // ← 别忘了插库
        return d.getId();                // ← 回填的 id 返回
    }
    private String judge(String direction, BigDecimal retPct){
        BigDecimal X = new BigDecimal("3");
        BigDecimal negX = X.negate();
        if("看多".equals(direction)){
            if (retPct.compareTo(X) >= 0)return "HIT";
            if (retPct.compareTo(negX) <= 0)return "MISS";
            return "FLAT";
        }
        else if ("看空".equals(direction)){
            if (retPct.compareTo(negX) <= 0)return "HIT";
            if (retPct.compareTo(X) >= 0)return "MISS";
            return "FLAT";
        }
        else {
            if (retPct.abs().compareTo(X) < 0)return "HIT";
            return "MISS";
        }
    }
    private BigDecimal latestClose(String indicatorsJson) throws Exception{
        JsonNode arr = objectMapper.readTree(indicatorsJson);
        JsonNode last = arr.get(arr.size() - 1);
        return new BigDecimal(last.get("close").asText());
    }

    /**
     * 设置回测时间
     * @param n
     * @return
     */
    public int settleDue(int n){
        LocalDateTime due = LocalDateTime.now().minusDays(n);
        QueryWrapper queryWrapper = new QueryWrapper<Decision>()
                .eq("status", "PENDING").le("decided_at", due);
        List<Decision> list = decisionMapper.selectList(queryWrapper);
        int count = 0;
        for (Decision d: list){
            try {
                String json = stockDataClient.getIndicators(d.getCode());
                BigDecimal outcome = latestClose(json); //现价
                BigDecimal ret = outcome.subtract( d.getDecidedPrice())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(d.getDecidedPrice(), 3, RoundingMode.HALF_UP);
                d.setOutcomePrice(outcome);
                d.setReturnPct(ret);
                d.setEvaluatedAt(LocalDateTime.now());
                d.setStatus(judge(d.getDirection(), ret));               // 复用你写的判定
                decisionMapper.updateById(d);                            // 按主键回写
                count++;
            } catch (Exception e) {
                log.warn("结算失败 id={} code={}", d.getId(), d.getCode(), e);
            }

        }
        return count;
    }
}
