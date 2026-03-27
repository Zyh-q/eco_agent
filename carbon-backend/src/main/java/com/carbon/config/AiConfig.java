package com.carbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String apiKey;
    private String apiUrl;
    private String model;
    private double temperature;
    // 多模态模型配置
    private String multimodalModel;
    private String multimodalApiUrl;
    private String multimodalApiKey;
}