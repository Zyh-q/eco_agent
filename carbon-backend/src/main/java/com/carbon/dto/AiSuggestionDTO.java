package com.carbon.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 减碳建议DTO（修正字段类型为BigDecimal）
 */
@Data
public class AiSuggestionDTO {
    private String type;            // 建议类型（如交通/饮食）
    private String content;         // 具体建议内容
    private String suitableScene;   // 适用场景
    private BigDecimal carbonReduce;// 预估减碳量
    private Integer priority;       // 优先级（1/2/3）
    private String matchReason;     // 匹配该用户的原因
}