package com.carbon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_suggestion")
public class AiSuggestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private String suitableScene;
    private BigDecimal carbonReduce;
    private Integer priority;
    private String matchReason;
    private LocalDateTime createTime;
}