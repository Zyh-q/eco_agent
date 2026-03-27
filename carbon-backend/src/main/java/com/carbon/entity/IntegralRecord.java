package com.carbon.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IntegralRecord {
    private Long id;             // 记录ID
    private Long userId;         // 用户ID
    private Integer integral;    // 积分变动
    private String reason;       // 变动原因
    private LocalDateTime createTime;  // 创建时间
}