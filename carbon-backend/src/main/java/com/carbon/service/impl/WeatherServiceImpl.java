package com.carbon.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.carbon.config.AmapConfig;
import com.carbon.config.WeatherConfig;
import com.carbon.service.WeatherService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class WeatherServiceImpl implements WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherServiceImpl.class);

    @Autowired
    private AmapConfig amapConfig;

    @Autowired
    private WeatherConfig weatherConfig;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    // 天气缓存（按经纬度）
    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE = 60 * 60 * 1000; // 1小时

    private static class CacheEntry {
        String weather;
        long timestamp;
        CacheEntry(String weather, long timestamp) {
            this.weather = weather;
            this.timestamp = timestamp;
        }
    }

    // 默认城市天气缓存
    private CacheEntry defaultCityCache;

    @Override
    public String getCurrentWeather() {
        // 优先尝试获取默认城市的真实天气，如果失败则返回配置的默认文本
        String weather = fetchDefaultCityWeather();
        if (weather != null) {
            logger.info("getCurrentWeather() 返回默认城市实时天气：{}", weather);
            return weather;
        }
        String fallback = weatherConfig.getDefaultWeather();
        logger.info("getCurrentWeather() 返回配置默认天气：{}", fallback);
        return fallback;
    }

    @Override
    public String getWeatherByLocation(Double latitude, Double longitude) {
        logger.info("getWeatherByLocation() 被调用，参数：latitude={}, longitude={}", latitude, longitude);

        if (latitude == null || longitude == null) {
            logger.warn("经纬度为null，尝试返回默认城市天气");
            return getCurrentWeather(); // 复用默认城市逻辑
        }

        String cacheKey = latitude + "," + longitude;
        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRE) {
            logger.info("命中缓存，返回缓存的天气：{}", cached.weather);
            return cached.weather;
        }

        try {
            String locationId = getLocationId(latitude, longitude);
            if (locationId == null) {
                logger.warn("根据经纬度获取locationId失败，返回默认城市天气");
                return getCurrentWeather();
            }
            String weather = fetchWeather(locationId);
            if (weather != null) {
                logger.info("成功获取实时天气：{}", weather);
                weatherCache.put(cacheKey, new CacheEntry(weather, System.currentTimeMillis()));
                return weather;
            } else {
                logger.warn("获取实时天气失败，返回默认城市天气");
                return getCurrentWeather();
            }
        } catch (Exception e) {
            logger.error("获取天气异常", e);
            return getCurrentWeather();
        }
    }

    /**
     * 获取默认城市的实时天气（用于降级）
     */
    private String fetchDefaultCityWeather() {
        if (amapConfig.getDefaultCity() == null || amapConfig.getDefaultCity().isEmpty()) {
            return null;
        }
        // 检查默认城市缓存
        if (defaultCityCache != null && System.currentTimeMillis() - defaultCityCache.timestamp < CACHE_EXPIRE) {
            return defaultCityCache.weather;
        }
        String weather = fetchWeather(amapConfig.getDefaultCity());
        if (weather != null) {
            defaultCityCache = new CacheEntry(weather, System.currentTimeMillis());
        }
        return weather;
    }

    /**
     * 根据经纬度获取城市adcode
     */
    private String getLocationId(double lat, double lon) {
        logger.debug("开始获取locationId，坐标：{},{}", lat, lon);
        HttpUrl url = HttpUrl.parse(amapConfig.getGeoUrl()).newBuilder()
                .addQueryParameter("key", amapConfig.getKey())
                .addQueryParameter("location", lon + "," + lat)
                .addQueryParameter("output", "json")
                .build();

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("地理编码API响应失败: {}", response.code());
                return null;
            }
            String body = response.body().string();
            JSONObject json = JSON.parseObject(body);
            if ("1".equals(json.getString("status"))) {
                JSONObject regeocode = json.getJSONObject("regeocode");
                if (regeocode != null) {
                    JSONObject addressComponent = regeocode.getJSONObject("addressComponent");
                    if (addressComponent != null) {
                        String adcode = addressComponent.getString("adcode");
                        logger.debug("获取到locationId：{}", adcode);
                        return adcode;
                    }
                }
            } else {
                logger.warn("地理编码返回错误: {}", json.getString("info"));
            }
        } catch (IOException e) {
            logger.error("地理编码请求异常", e);
        }
        return null;
    }

    /**
     * 获取实时天气
     */
    private String fetchWeather(String cityCode) {
        logger.debug("开始获取天气，城市code：{}", cityCode);
        HttpUrl url = HttpUrl.parse(amapConfig.getWeatherUrl()).newBuilder()
                .addQueryParameter("key", amapConfig.getKey())
                .addQueryParameter("city", cityCode)
                .addQueryParameter("extensions", "base")
                .build();

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("天气API响应失败: {}", response.code());
                return null;
            }
            String body = response.body().string();
            JSONObject json = JSON.parseObject(body);
            if ("1".equals(json.getString("status"))) {
                JSONArray lives = json.getJSONArray("lives");
                if (lives != null && !lives.isEmpty()) {
                    JSONObject live = lives.getJSONObject(0);
                    String weather = live.getString("weather") + " " + live.getString("temperature") + "℃";
                    logger.debug("获取到天气：{}", weather);
                    return weather;
                }
            } else {
                logger.warn("天气查询返回错误: {}", json.getString("info"));
            }
        } catch (IOException e) {
            logger.error("天气请求异常", e);
        }
        return null;
    }
}