package com.maorou.SpringAIDemo.service;


import com.maorou.SpringAIDemo.entity.Decision;
import com.maorou.SpringAIDemo.mapper.DecisionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Service
public class DecisionEvalService {
    private final DecisionMapper decisionMapper;

    public DecisionEvalService(DecisionMapper decisionMapper) {
        this.decisionMapper = decisionMapper;
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
}
