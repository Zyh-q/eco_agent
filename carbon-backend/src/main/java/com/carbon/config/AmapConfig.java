package com.carbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    private String key;               // 高德地图API
    private String geoUrl;             // 地理编码URL
    private String weatherUrl;         // 天气查询URL
    private String defaultCity;        // 默认城市
}