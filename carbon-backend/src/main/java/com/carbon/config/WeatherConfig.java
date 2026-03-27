package com.carbon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherConfig {
    private String defaultWeather;     // 降级处理
}