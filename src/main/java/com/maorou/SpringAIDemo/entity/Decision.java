package com.maorou.SpringAIDemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("decision")
public class Decision {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String source;
    private String direction;
    private String status;
    private Integer confidence;
    private String reason;
    private LocalDateTime decidedAt;
    private BigDecimal decidedPrice;
    private LocalDateTime createdAt;
    private BigDecimal outcomePrice;
    private BigDecimal returnPct;
    private LocalDateTime evaluatedAt;



}
