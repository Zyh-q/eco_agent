package com.carbon.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CarbonRecord {
    private Long id;                // 主键ID
    private Long userId;            // 用户ID
    private String type;            // 行为类型
    private String detail;          // 行为详情
    private BigDecimal amount;      // 数量
    private String unit;            // 单位
    private BigDecimal carbonEmission; // 碳排放量
    private LocalDate recordTime;   // 记录时间
}