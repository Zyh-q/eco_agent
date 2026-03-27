package com.carbon.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.carbon.config.AiConfig;
import com.carbon.config.CarbonFactorConfig;
import com.carbon.dto.AiSuggestionDTO;
import com.carbon.dto.CarbonChatDTO;
import com.carbon.dto.ImageParseDTO;
import com.carbon.entity.CarbonRecord;
import com.carbon.entity.User;
import com.carbon.service.AiService;
import com.carbon.service.CarbonRecordService;
import com.carbon.service.UserService;
import com.carbon.service.WeatherService;
import com.carbon.util.CarbonCalculationUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeParseException;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // 工具定义：支持通用碳数据查询
    private static final List<Map<String, Object>> TOOLS = List.of(
            Map.of(
                    "type", "function",
                    "function",
                    Map.of(
                            "name", "query_carbon_data",
                            "description", "查询用户的碳足迹数据，支持按时间范围、分组、聚合等。返回结果均为JSON格式。",
                            "parameters",
                            Map.of(
                                    "type", "object",
                                    "properties",
                                    Map.of(
                                            "query_type",
                                            Map.of(
                                                    "type", "string",
                                                    "enum", List.of("total", "by_type", "by_date", "trend", "top_type", "by_type_detail","integral"),
                                                    "description", "查询类型：total=总排放；by_type=按类型汇总；by_date=按日期汇总；trend=每日趋势；top_type=排放最高的类型；by_type_detail=按类型返回明细记录；integral=查询用户当前绿芽积分"
                                            ),
                                            "days",
                                            Map.of(
                                                    "type", "integer",
                                                    "description", "最近N天，如果不指定则查询全部"
                                            ),
                                            "start_date",
                                            Map.of(
                                                    "type", "string",
                                                    "description", "开始日期，格式YYYY-MM-DD"
                                            ),
                                            "end_date",
                                            Map.of(
                                                    "type", "string",
                                                    "description", "结束日期，格式YYYY-MM-DD"
                                            ),
                                            "type",
                                            Map.of(
                                                    "type", "string",
                                                    "description", "当查询特定类型时使用（如“交通出行”）"
                                            ),
                                            "limit",
                                            Map.of(
                                                    "type", "integer",
                                                    "description", "返回记录数限制（仅对 by_type_detail 有效）"
                                            )
                                    ),
                                    "required", List.of("query_type")
                            )
                    )
            )
    );

    @Autowired
    private AiConfig aiConfig;

    @Autowired
    private CarbonRecordService carbonRecordService;

    @Autowired
    private CarbonCalculationUtil carbonCalculationUtil;

    @Autowired
    private CarbonFactorConfig carbonFactorConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private WeatherService weatherService;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    //天气相关
    @Override
    public String getCurrentWeather() {
        return weatherService.getCurrentWeather();
    }

    @Override
    public String getWeatherByLocation(Double latitude, Double longitude) {
        return weatherService.getWeatherByLocation(latitude, longitude);
    }

    // 碳排放计算
    @Override
    public Double calculateCarbonWithParseDTO(ImageParseDTO dto) {
        try {
            if ("能源消耗".equals(dto.getType()) && dto.getPowerKw() != null && dto.getPowerKw() > 0) {
                double gridFactor = carbonFactorConfig.getEnergy().getGrid();
                logger.info("使用AI提供的功率：{} kW，电网因子：{}", dto.getPowerKw(), gridFactor);
                return dto.getAmount() * dto.getPowerKw() * gridFactor;
            }
            if (dto.getEmissionFactor() != null && dto.getEmissionFactor() > 0) {
                logger.info("使用AI提供的排放因子：{}", dto.getEmissionFactor());
                return dto.getAmount() * dto.getEmissionFactor();
            }
            logger.info("回退到传统计算，类型：{}，详情：{}", dto.getType(), dto.getDetail());
            BigDecimal carbon = carbonCalculationUtil.calculateCarbon(
                    dto.getType(),
                    dto.getDetail(),
                    BigDecimal.valueOf(dto.getAmount()),
                    dto.getUnit()
            );
            return carbon.doubleValue();
        } catch (Exception e) {
            logger.error("碳排放计算失败", e);
            return 0.01;
        }
    }

    // 生成温馨对话
    @Override
    public CarbonChatDTO generateWarmCarbonChat(Double carbonAmount, List<CarbonRecord> historyRecords,
                                                String weather, ImageParseDTO parseDTO, String nickname) {
        logger.info("生成温馨对话，weather：{}，昵称：{}", weather, nickname);
        CarbonChatDTO chatDTO = new CarbonChatDTO();
        try {
            double carbon = carbonAmount == null ? 0.0 : carbonAmount;
            String weatherStr = weather == null ? "晴朗" : weather;
            String behavior = parseDTO == null ? "低碳行为" : parseDTO.getDetail();
            String name = (nickname != null && !nickname.trim().isEmpty()) ? nickname : "小伙伴";

            String prompt = String.format(
                    "你是有温度的低碳生活小助手，语气亲切可爱。请按以下要求生成一条综合减碳建议（100字内）：\n" +
                            "用户昵称：%s\n" +
                            "用户本次行为：%s\n" +
                            "总碳排放：%.2f kg CO₂\n" +
                            "当前天气：%s\n" +
                            "要求：\n" +
                            "1. 直接输出一句话，必须以“你好呀%s，”开头，紧接着用当前天气问候（例如“今天晴朗，记得多喝水哦”）。\n" +
                            "2. 然后总结本次碳排放（例如“你今天因为开车产生了0.5 kg CO₂”）。\n" +
                            "3. 最后给出1条与本次行为相关的具体减碳建议。\n" +
                            "4. 整个内容为一句连贯的话，不要分点，不要额外解释。",
                    name, behavior, carbon, weatherStr, name
            );

            String aiResponse = callAiApi(prompt);
            chatDTO.setRole("ai");
            chatDTO.setContent(aiResponse);
            chatDTO.setCarbonAmount(carbon);
            chatDTO.setUnit("kg");
            chatDTO.setWeather(weatherStr);
        } catch (Exception e) {
            logger.error("生成AI建议失败", e);
            chatDTO.setRole("ai");
            chatDTO.setContent("恭喜你完成低碳记录！继续保持绿色生活~");
            chatDTO.setCarbonAmount(0.0);
            chatDTO.setUnit("kg");
            chatDTO.setWeather("晴朗");
        }
        chatDTO.setCreateTime(LocalDateTime.now());
        return chatDTO;
    }
    // 图片解析
    @Override
    public ImageParseDTO parseCarbonFromImage(MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = "你是专业碳足迹解析助手，严格按以下规则执行：\n" +
                    "\n" +
                    "1. **输出格式**：必须返回**标准 JSON 对象**，禁止任何多余文字、解释或标点。\n" +
                    "2. **字段要求**：\n" +
                    "   - `type`：必填，仅限以下枚举值之一：交通出行、能源消耗、饮食消费、购物消费、垃圾处理、其他（若无法准确识别，填“其他”）。\n" +
                    "   - `unit`：必填，仅限以下枚举值之一：公里、小时、杯、份、度、件、次（若无法准确识别，填“次”）。\n" +
                    "   - `amount`：数字，表示数量。\n" +
                    "   - `detail`：简短描述用户行为（如“使用空调”）。\n" +
                    "   - `confidence`：0～1 之间的小数，表示识别可信度。\n" +
                    "   - `emissionFactor`（可选）：若你能直接知道该行为每单位的碳排放因子（kg CO₂/单位），则填写，否则不填。\n" +
                    "   - `powerKw`（可选）：若行为是能源消耗且能识别出功率（千瓦），则填写，否则不填。\n" +
                    "3. **示例**：\n" +
                    "   ```json\n" +
                    "   {\"type\":\"能源消耗\",\"detail\":\"使用空调\",\"amount\":3,\"unit\":\"小时\",\"confidence\":0.95}";

            String aiResponse = callMultimodalApi(prompt, base64Image);
            logger.info("AI 原始响应：{}", aiResponse);
            String cleanJson = cleanAiResponse(aiResponse);
            List<ImageParseDTO> results;
            try {
                results = JSON.parseArray(cleanJson, ImageParseDTO.class);
            } catch (Exception e) {
                ImageParseDTO single = JSON.parseObject(cleanJson, ImageParseDTO.class);
                results = Collections.singletonList(single);
            }
            if (results.isEmpty()) throw new RuntimeException("图片解析结果为空");
            ImageParseDTO result = results.get(0);
            if (result.getType() == null || result.getType().trim().isEmpty()) result.setType("其他");
            if (result.getUnit() == null || result.getUnit().trim().isEmpty()) result.setUnit("次");
            if (result.getAmount() == null || result.getAmount() <= 0) result.setAmount(1.0);
            if (result.getDetail() == null || result.getDetail().trim().isEmpty()) result.setDetail("图片识别行为");
            if (result.getConfidence() == null) result.setConfidence(0.8);
            fixInvalidUnit(result);
            String unit = result.getUnit();
            if ("元".equals(unit) || unit.contains("元")) {
                result = fixAmountUnit(result);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("图片解析失败：" + e.getMessage(), e);
        }
    }

    private ImageParseDTO fixAmountUnit(ImageParseDTO dto) {
        String detail = dto.getDetail() != null ? dto.getDetail().toLowerCase() : "";
        if (detail.contains("黄焖鸡") || detail.contains("米饭") || detail.contains("套餐")) {
            dto.setAmount(1.0);
            dto.setUnit("份");
        } else if (detail.contains("奶茶") || detail.contains("咖啡")) {
            dto.setAmount(1.0);
            dto.setUnit("杯");
        } else if (detail.contains("矿泉水")) {
            dto.setAmount(1.0);
            dto.setUnit("瓶");
        } else if (detail.contains("衣服")) {
            dto.setAmount(1.0);
            dto.setUnit("件");
        }
        return dto;
    }

    // 文本解析
    @Override
    public List<ImageParseDTO> parseCarbonFromText(String text) {
        String prompt = "你是碳足迹解析助手。请严格按照以下规则解析用户的文本描述，并**只返回一个 JSON 数组**，禁止任何多余文字或解释。\n" +
                "\n" +
                "**规则**：\n" +
                "1. **数组元素结构**：每个元素必须包含以下字段：\n" +
                "   - `type`：字符串，行为类型，仅限枚举值：交通出行、能源消耗、饮食消费、购物消费、垃圾处理、其他。\n" +
                "   - `detail`：字符串，简要描述具体行为（例如“开车上班”“使用空调”“吃牛肉面”）。\n" +
                "   - `amount`：数字，行为的数量（例如 2.5）。\n" +
                "   - `unit`：字符串，行为单位，仅限枚举值：公里、小时、杯、份、度、件、次。\n" +
                "   - `confidence`：0～1 之间的小数，表示解析可信度。\n" +
                "   - `emissionFactor`（可选）：若你能直接知道该行为每单位的碳排放因子（kg CO₂/单位），则填写，否则不填。\n" +
                "   - `powerKw`（可选）：若行为是能源消耗且能识别出功率（千瓦），则填写，否则不填。\n" +
                "\n" +
                "2. **多行为处理**：\n" +
                "   - 如果文本描述中包含多个行为，必须返回多个数组元素。例如：'开车3公里去吃牛肉面' → 应返回两个元素：{交通出行:开车3公里} 和 {饮食消费:吃牛肉面}。\n" +
                "   - 即使只有一个行为，也返回包含一个元素的数组。\n" +
                "\n" +
                "3. **单位限制**：`unit` 必须使用上述枚举值，禁止使用 kg、kgCO2e、元 等非法单位。\n" +
                "\n" +
                "4. **输出格式示例**：\n" +
                "   ```json\n" +
                "   [\n" +
                "     {\"type\":\"交通出行\",\"detail\":\"开车3公里\",\"amount\":3,\"unit\":\"公里\",\"confidence\":0.95},\n" +
                "     {\"type\":\"饮食消费\",\"detail\":\"吃牛肉面\",\"amount\":1,\"unit\":\"份\",\"confidence\":0.98}\n" +
                "   ]";

        String fullPrompt = prompt + "\n\n用户描述：" + text;
        String aiResponse = callAiApi(fullPrompt);
        logger.info("AI 原始响应：{}", aiResponse);

        String cleanJson = cleanAiResponse(aiResponse);
        List<ImageParseDTO> results ;

        try {
            results = JSON.parseArray(cleanJson, ImageParseDTO.class);
        } catch (Exception e) {
            try {
                ImageParseDTO single = JSON.parseObject(cleanJson, ImageParseDTO.class);
                results = Collections.singletonList(single);
            } catch (Exception ex) {
                logger.error("解析AI响应失败，原始响应：{}", aiResponse, ex);
                return Collections.emptyList();
            }
        }
        results.forEach(this::fixInvalidUnit);
        return results;
    }

    private void fixInvalidUnit(ImageParseDTO dto) {
        String unit = dto.getUnit() == null ? "" : dto.getUnit().trim();
        if (unit.contains("kg") || unit.contains("CO2") || unit.contains("kgCO2e")) {
            String type = dto.getType() == null ? "" : dto.getType();
            switch (type) {
                case "交通出行": dto.setUnit("公里"); break;
                case "饮食消费": dto.setUnit("份"); break;
                case "能源消耗": dto.setUnit("度"); break;
                case "购物消费": dto.setUnit("件"); break;
                default: dto.setUnit("次");
            }
        }
    }

    // 减碳建议生成
    @Override
    public List<AiSuggestionDTO> generateCarbonSuggestion(List<CarbonRecord> carbonRecords) {
        String prompt = buildPrompt(carbonRecords);
        String aiResponse = callAiApi(prompt);
        return parseAiResponse(aiResponse);
    }

    // AI对话（使用千文模型，支持函数调用）
    @Override
    public String chatWithAi(String question, Long userId) {
        User user = userService.getUserById(userId);
        String nickname = (user != null && StringUtils.hasText(user.getNickname()))
                ? user.getNickname()
                : "小伙伴";
        String today = LocalDate.now().toString();
        String userMessage = "当前日期是 " + today + "。用户昵称是：" + nickname + "。请用这个昵称称呼用户，回答要亲切自然。用户问题：" + question +
                "注意：当用户问‘今天’时，请使用 start_date='" + today + "' 和 end_date='" + today + "' 来调用 query_carbon_data。";

        try {
            return callAiApiWithTools(userMessage, userId, TOOLS);
        } catch (Exception e) {
            logger.error("AI对话失败", e);
            return "哎呀，小助手暂时遇到点问题，稍后再试试吧～";
        }
    }


    private String callAiApiWithTools(String userMessage, Long userId, List<Map<String, Object>> tools) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userMessage));

        while (true) {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("temperature", aiConfig.getTemperature());
            requestBody.put("messages", messages);
            requestBody.put("tools", tools);
            requestBody.put("tool_choice", "auto");

            Request request = new Request.Builder()
                    .url(aiConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                    .post(RequestBody.create(requestBody.toJSONString(), JSON_MEDIA))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    logger.error("AI调用失败，状态码：{}，响应体：{}", response.code(), errorBody);
                    throw new RuntimeException("AI调用失败，HTTP状态码：" + response.code());
                }
                String respBody = response.body().string();
                JSONObject result = JSON.parseObject(respBody);
                JSONArray choices = result.getJSONArray("choices");
                if (choices == null || choices.isEmpty())
                    throw new RuntimeException("AI返回内容为空");
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");

                if (message.containsKey("tool_calls")) {
                    JSONArray toolCalls = message.getJSONArray("tool_calls");
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", message.getString("content"));
                    assistantMsg.put("tool_calls", message.getJSONArray("tool_calls"));
                    messages.add(assistantMsg);

                    // 处理每个工具调用
                    for (int i = 0; i < toolCalls.size(); i++) {
                        JSONObject toolCall = toolCalls.getJSONObject(i);
                        String functionName = toolCall.getJSONObject("function").getString("name");
                        String argumentsStr = toolCall.getJSONObject("function").getString("arguments");
                        Map<String, Object> arguments = JSON.parseObject(argumentsStr);
                        arguments.put("userId", userId);  // 注入 userId
                        String functionResult = callFunction(functionName, arguments);

                        Map<String, Object> toolMessage = Map.of(
                                "role", "tool",
                                "tool_call_id", toolCall.getString("id"),
                                "content", functionResult
                        );
                        messages.add(toolMessage);
                    }
                } else {
                    return message.getString("content");
                }
            } catch (IOException e) {
                throw new RuntimeException("AI调用网络异常", e);
            }
        }
    }

    private String callFunction(String functionName, Map<String, Object> arguments) {
        if ("query_carbon_data".equals(functionName)) {
            return queryCarbonData(arguments);
        }
        return "{\"error\": \"未知函数\"}";
    }

    private String queryCarbonData(Map<String, Object> arguments) {
        String queryType = (String) arguments.get("query_type");
        Long userId = ((Number) arguments.get("userId")).longValue();

        Integer days = arguments.containsKey("days") ? ((Number) arguments.get("days")).intValue() : null;
        String startDateStr = (String) arguments.get("start_date");
        String endDateStr = (String) arguments.get("end_date");
        String type = (String) arguments.get("type");
        Integer limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : null;

        LocalDate start = null, end = null;
        if (days != null) {
            end = LocalDate.now();
            start = end.minusDays(days);
        } else if (startDateStr != null && endDateStr != null) {
            boolean invalid = startDateStr.contains("{{") || startDateStr.contains("}}") ||
                    endDateStr.contains("{{") || endDateStr.contains("}}") ||
                    startDateStr.length() < 6 || endDateStr.length() < 6;
            if (!invalid) {
                try {
                    start = LocalDate.parse(startDateStr);
                    end = LocalDate.parse(endDateStr);
                } catch (DateTimeParseException e) {
                    logger.warn("日期格式解析失败: start={}, end={}", startDateStr, endDateStr);
                    start = LocalDate.MIN;
                    end = LocalDate.now();
                }
            } else {
                logger.warn("日期参数包含无效占位符，忽略日期范围: start={}, end={}", startDateStr, endDateStr);
                start = LocalDate.MIN;
                end = LocalDate.now();
            }
        } else {
            start = LocalDate.MIN;
            end = LocalDate.now();
        }

        List<CarbonRecord> records;
        if (type != null) {
            records = carbonRecordService.getCarbonRecordByUserIdAndDateAndType(userId, start, end, type);
        } else {
            records = carbonRecordService.getCarbonRecordByUserIdAndDate(userId, start, end);
        }

        JSONObject result = new JSONObject();
        switch (queryType) {
            case "total":
                double total = records.stream().mapToDouble(r -> r.getCarbonEmission().doubleValue()).sum();
                result.put("total_emission", total);
                result.put("unit", "kg");
                break;
            case "by_type":
                Map<String, Double> typeMap = new HashMap<>();
                for (CarbonRecord r : records) typeMap.merge(r.getType(), r.getCarbonEmission().doubleValue(), Double::sum);
                result.put("data", typeMap);
                break;
            case "by_date":
                Map<String, Double> dailyMap = new LinkedHashMap<>();
                for (CarbonRecord r : records) dailyMap.merge(r.getRecordTime().toString(), r.getCarbonEmission().doubleValue(), Double::sum);
                result.put("data", dailyMap);
                break;
            case "trend":
                Map<String, Double> trendMap = new LinkedHashMap<>();
                for (CarbonRecord r : records) trendMap.merge(r.getRecordTime().toString(), r.getCarbonEmission().doubleValue(), Double::sum);
                result.put("trend", trendMap);
                break;
            case "top_type":
                Map<String, Double> topMap = new HashMap<>();
                for (CarbonRecord r : records) topMap.merge(r.getType(), r.getCarbonEmission().doubleValue(), Double::sum);
                String topType = topMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
                double topValue = topMap.getOrDefault(topType, 0.0);
                result.put("top_type", topType);
                result.put("top_emission", topValue);
                break;
            case "by_type_detail":
                List<Map<String, Object>> details = new ArrayList<>();
                for (CarbonRecord r : records) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("type", r.getType());
                    detail.put("date", r.getRecordTime().toString());
                    detail.put("emission", r.getCarbonEmission().doubleValue());
                    detail.put("detail", r.getDetail());
                    detail.put("amount", r.getAmount().doubleValue());
                    detail.put("unit", r.getUnit());
                    details.add(detail);
                }
                if (limit != null && details.size() > limit) details = details.subList(0, limit);
                result.put("details", details);
                break;
            case "integral":
                User user = userService.getUserById(userId);
                int integral = (user != null && user.getIntegral() != null) ? user.getIntegral() : 0;
                result.put("integral", integral);
                break;
            default:
                result.put("error", "不支持的查询类型: " + queryType);
        }
        return result.toJSONString();
    }

    private String cleanAiResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) throw new RuntimeException("AI返回内容为空");
        return rawResponse.replaceAll("```\\s*json", "").replaceAll("```", "").trim();
    }

    private String buildPrompt(List<CarbonRecord> carbonRecords) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下用户碳足迹记录，生成个性化减碳建议，严格按照JSON数组格式返回：\n");
        for (CarbonRecord record : carbonRecords) {
            prompt.append("- 类型：").append(record.getType()).append("，碳排放：").append(record.getCarbonEmission()).append("kg\n");
        }
        return prompt.toString();
    }

    private String callAiApi(String prompt) {
        logger.info("调用文本 AI，模型：{}，API URL：{}", aiConfig.getModel(), aiConfig.getApiUrl());
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", aiConfig.getModel());
        requestBody.put("temperature", aiConfig.getTemperature());
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", prompt));
        requestBody.put("messages", messages);

        Request request = new Request.Builder()
                .url(aiConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .post(RequestBody.create(requestBody.toJSONString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                logger.error("文本AI调用失败，状态码：{}，响应体：{}", response.code(), errorBody);
                throw new RuntimeException("AI调用失败");
            }
            JSONObject result = JSON.parseObject(response.body().string());
            return result.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch (IOException e) {
            throw new RuntimeException("AI调用异常", e);
        }
    }

    private String callMultimodalApi(String prompt, String base64Image) throws IOException {
        logger.info("调用多模态 AI，模型：{}，API URL：{}", aiConfig.getMultimodalModel(), aiConfig.getMultimodalApiUrl());
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", aiConfig.getMultimodalModel());
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        JSONArray content = new JSONArray();
        content.add(new JSONObject().fluentPut("type", "text").fluentPut("text", prompt));
        JSONObject imageUrl = new JSONObject().fluentPut("url", "data:image/jpeg;base64," + base64Image);
        content.add(new JSONObject().fluentPut("type", "image_url").fluentPut("image_url", imageUrl));
        userMsg.put("content", content);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        Request request = new Request.Builder()
                .url(aiConfig.getMultimodalApiUrl())
                .addHeader("Authorization", "Bearer " + aiConfig.getMultimodalApiKey())
                .post(RequestBody.create(requestBody.toJSONString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                logger.error("多模态API调用失败，状态码：{}，响应体：{}", response.code(), errorBody);
                throw new RuntimeException("多模态调用失败");
            }
            JSONObject result = JSON.parseObject(response.body().string());
            return result.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

    private List<AiSuggestionDTO> parseAiResponse(String aiResponse) {
        try {
            return JSON.parseArray(cleanAiResponse(aiResponse), AiSuggestionDTO.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}