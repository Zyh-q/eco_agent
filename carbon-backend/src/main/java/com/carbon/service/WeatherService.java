package com.carbon.service;

public interface WeatherService {
    String getCurrentWeather();                          // 随机天气（降级用）
    String getWeatherByLocation(Double latitude, Double longitude);  // 根据经纬度获取实时天气
}