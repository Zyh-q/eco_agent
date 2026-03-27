package com.carbon.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 碳足迹对话气泡DTO
 */
@Data
public class CarbonChatDTO {
    // 角色：user（用户）/ai（智能助手）
    private String role;
    // 对话内容（有温度的文案）
    private String content;
    // 消息创建时间
    private LocalDateTime createTime;
    // 本次碳排放量
    private Double carbonAmount;
    // 单位（默认kg）
    private String unit = "kg";
    // 天气
    private String weather;
}