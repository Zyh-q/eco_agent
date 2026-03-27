package com.carbon.controller;

import com.carbon.dto.AiSuggestionDTO;
import com.carbon.dto.BatchConfirmDTO;
import com.carbon.dto.CarbonChatDTO;
import com.carbon.dto.ImageParseDTO;
import com.carbon.entity.CarbonRecord;
import com.carbon.service.AiService;
import com.carbon.service.CarbonRecordService;
import com.carbon.service.IntegralService;
import com.carbon.service.UserService;
import com.carbon.util.CarbonCalculationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.carbon.entity.User;

import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/carbon")
public class CarbonRecordController {

    private static final Logger logger = LoggerFactory.getLogger(CarbonRecordController.class);

    @Autowired
    private CarbonRecordService carbonRecordService;
    @Autowired
    private IntegralService integralService;
    @Autowired
    private AiService aiService;
    @Autowired
    private UserService userService;

    @Autowired
    private CarbonCalculationUtil carbonCalculationUtil;

    /**
     * 新增碳足迹记录（手动输入）
     */
    @PostMapping("/add")
    public Map<String, Object> addCarbonRecord(
            @RequestBody CarbonRecord carbonRecord,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        Map<String, Object> result = new HashMap<>();
        try {
            BigDecimal carbonEmission = carbonCalculationUtil.calculateCarbon(
                    carbonRecord.getType(),
                    carbonRecord.getDetail(),
                    carbonRecord.getAmount(),
                    carbonRecord.getUnit()
            );
            carbonRecord.setCarbonEmission(carbonEmission);
            boolean flag = carbonRecordService.addCarbonRecord(carbonRecord);
            if (!flag) {
                result.put("code", 500);
                result.put("msg", "记录失败");
                return result;
            }
            double emission = carbonEmission.doubleValue();
            int integral = emission < 1 ? 10 : (emission < 5 ? 5 : 1);
            String reason = "手动记录：" + carbonRecord.getDetail() + " 奖励积分";
            integralService.addIntegralRecord(carbonRecord.getUserId(), integral, reason);

            String weather;
            if (latitude != null && longitude != null) {
                weather = aiService.getWeatherByLocation(latitude, longitude);
            } else {
                weather = aiService.getCurrentWeather();
            }

            List<CarbonRecord> history = carbonRecordService.getCarbonRecordByUserId(carbonRecord.getUserId(), 3);

            User user = userService.getUserById(carbonRecord.getUserId());
            String nickname = (user != null && StringUtils.hasText(user.getNickname())) ? user.getNickname() : "小伙伴";

            CarbonChatDTO warmChat = aiService.generateWarmCarbonChat(emission, history, weather, null, nickname);

            List<CarbonRecord> allRecords = carbonRecordService.getCarbonRecordByUserId(carbonRecord.getUserId());
            List<AiSuggestionDTO> suggestionList = aiService.generateCarbonSuggestion(allRecords);

            Map<String, Object> data = new HashMap<>();
            data.put("carbonAmount", emission);
            data.put("integral", integral);
            data.put("warmChat", warmChat);
            data.put("record", carbonRecord);
            data.put("suggestionList", suggestionList);

            result.put("code", 200);
            result.put("msg", "记录成功，获得" + integral + "绿芽积分");
            result.put("data", data);
        } catch (Exception e) {
            logger.error("手动录入失败", e);
            result.put("code", 500);
            result.put("msg", "记录失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 批量提交（同时提交图片和文本解析结果）
     */
    @PostMapping("/confirmBatch")
    public Map<String, Object> confirmBatch(@RequestBody BatchConfirmDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = dto.getUserId();
            LocalDate recordTime = LocalDate.parse(dto.getRecordTime(), DateTimeFormatter.ISO_LOCAL_DATE);
            List<ImageParseDTO> items = dto.getItems();
            Double latitude = dto.getLatitude();
            Double longitude = dto.getLongitude();

            if (items == null || items.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "至少需要一个行为");
                return result;
            }

            double totalCarbon = 0;
            int totalIntegral = 0;
            StringBuilder behaviorDesc = new StringBuilder("批量记录：");

            for (ImageParseDTO parseDTO : items) {
                Double carbonAmount = aiService.calculateCarbonWithParseDTO(parseDTO);
                BigDecimal carbon = BigDecimal.valueOf(carbonAmount);

                CarbonRecord carbonRecord = new CarbonRecord();
                carbonRecord.setUserId(userId);
                carbonRecord.setType(parseDTO.getType());
                carbonRecord.setDetail(parseDTO.getDetail());
                carbonRecord.setAmount(BigDecimal.valueOf(parseDTO.getAmount()));
                carbonRecord.setUnit(parseDTO.getUnit());
                carbonRecord.setCarbonEmission(carbon);
                carbonRecord.setRecordTime(recordTime);

                boolean flag = carbonRecordService.addCarbonRecord(carbonRecord);
                if (!flag) {
                    result.put("code", 500);
                    result.put("msg", "记录保存失败：" + parseDTO.getDetail());
                    return result;
                }

                totalCarbon += carbonAmount;
                int integral = carbonAmount < 1 ? 10 : (carbonAmount < 5 ? 5 : 1);
                totalIntegral += integral;
                behaviorDesc.append(parseDTO.getDetail()).append("、");
            }

            if (behaviorDesc.length() > 5) {
                behaviorDesc.setLength(behaviorDesc.length() - 1);
            }
            behaviorDesc.append(" 奖励积分");
            integralService.addIntegralRecord(userId, totalIntegral, behaviorDesc.toString());

            String weather;
            if (latitude != null && longitude != null) {
                weather = aiService.getWeatherByLocation(latitude, longitude);
            } else {
                weather = aiService.getCurrentWeather();
            }

            User user = userService.getUserById(userId);
            String nickname = (user != null && StringUtils.hasText(user.getNickname())) ? user.getNickname() : "小伙伴";

            // 生成综合温馨对话
            ImageParseDTO first = items.get(0);
            List<CarbonRecord> history = carbonRecordService.getCarbonRecordByUserId(userId, 3);
            CarbonChatDTO warmChat = aiService.generateWarmCarbonChat(totalCarbon, history, weather, first, nickname);

            List<CarbonRecord> allRecords = carbonRecordService.getCarbonRecordByUserId(userId);
            List<AiSuggestionDTO> suggestionList = aiService.generateCarbonSuggestion(allRecords);

            Map<String, Object> data = new HashMap<>();
            data.put("totalCarbon", totalCarbon);
            data.put("totalIntegral", totalIntegral);
            data.put("warmChat", warmChat);
            data.put("suggestionList", suggestionList);

            result.put("code", 200);
            result.put("msg", "记录成功，获得" + totalIntegral + "绿芽积分");
            result.put("data", data);
        } catch (Exception e) {
            logger.error("批量提交失败", e);
            result.put("code", 500);
            result.put("msg", "提交失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 解析文本
     */
    @PostMapping("/parseText")
    public Map<String, Object> parseText(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            String description = params.get("description").toString();
            logger.info("解析文本，用户ID：{}，描述：{}", userId, description);
            List<ImageParseDTO> parseResults = aiService.parseCarbonFromText(description);
            logger.info("解析出 {} 个行为", parseResults.size());
            double totalCarbon = 0;
            int totalIntegral = 0;
            List<Map<String, Object>> items = new ArrayList<>();
            for (ImageParseDTO parseResult : parseResults) {
                double carbonAmount = aiService.calculateCarbonWithParseDTO(parseResult);
                int integral = carbonAmount < 1 ? 10 : (carbonAmount < 5 ? 5 : 1);
                totalCarbon += carbonAmount;
                totalIntegral += integral;
                Map<String, Object> item = new HashMap<>();
                item.put("parseResult", parseResult);
                item.put("carbonAmount", carbonAmount);
                item.put("integral", integral);
                items.add(item);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("totalCarbon", totalCarbon);
            data.put("totalIntegral", totalIntegral);

            result.put("code", 200);
            result.put("msg", "文本解析成功");
            result.put("data", data);
        } catch (Exception e) {
            logger.error("文本解析失败", e);
            result.put("code", 500);
            result.put("msg", "文本解析失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 保存文本解析结果
     */
    @PostMapping("/confirmText")
    public Map<String, Object> confirmTextResult(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("confirmText 请求参数: {}", params);

            Long userId = Long.valueOf(params.get("userId").toString());
            String recordTimeStr = params.get("recordTime").toString();
            LocalDate recordTime = LocalDate.parse(recordTimeStr, DateTimeFormatter.ISO_LOCAL_DATE);
            List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");

            Double latitude = params.get("latitude") != null ? Double.valueOf(params.get("latitude").toString()) : null;
            Double longitude = params.get("longitude") != null ? Double.valueOf(params.get("longitude").toString()) : null;

            if (items == null || items.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "请至少选择一个行为");
                return result;
            }

            StringBuilder behaviorBuilder = new StringBuilder("文本解析：");
            int totalIntegral = 0;
            double totalCarbon = 0.0;
            ImageParseDTO firstParseResult = null;

            for (Map<String, Object> item : items) {
                Map<String, Object> parseResultMap = (Map<String, Object>) item.get("parseResult");
                if (parseResultMap == null) {
                    logger.error("parseResult 为空");
                    continue;
                }

                ImageParseDTO parseDTO = new ImageParseDTO();
                parseDTO.setType(parseResultMap.get("type").toString());
                parseDTO.setDetail(parseResultMap.get("detail").toString());
                parseDTO.setAmount(((Number) parseResultMap.get("amount")).doubleValue());
                parseDTO.setUnit(parseResultMap.get("unit").toString());
                if (parseResultMap.containsKey("confidence")) parseDTO.setConfidence(((Number) parseResultMap.get("confidence")).doubleValue());
                if (parseResultMap.containsKey("emissionFactor")) parseDTO.setEmissionFactor(((Number) parseResultMap.get("emissionFactor")).doubleValue());
                if (parseResultMap.containsKey("powerKw")) parseDTO.setPowerKw(((Number) parseResultMap.get("powerKw")).doubleValue());

                Double carbonAmount = aiService.calculateCarbonWithParseDTO(parseDTO);
                BigDecimal carbon = BigDecimal.valueOf(carbonAmount);

                CarbonRecord carbonRecord = new CarbonRecord();
                carbonRecord.setUserId(userId);
                carbonRecord.setType(parseDTO.getType());
                carbonRecord.setDetail(parseDTO.getDetail());
                carbonRecord.setAmount(BigDecimal.valueOf(parseDTO.getAmount()));
                carbonRecord.setUnit(parseDTO.getUnit());
                carbonRecord.setCarbonEmission(carbon);
                carbonRecord.setRecordTime(recordTime);

                carbonRecordService.addCarbonRecord(carbonRecord);

                double carbonVal = carbon.doubleValue();
                totalCarbon += carbonVal;
                int integral = carbonVal < 1 ? 10 : (carbonVal < 5 ? 5 : 1);
                totalIntegral += integral;

                behaviorBuilder.append(parseDTO.getDetail()).append("、");
                if (firstParseResult == null) firstParseResult = parseDTO;
            }

            if (behaviorBuilder.length() > 5) behaviorBuilder.setLength(behaviorBuilder.length() - 1);
            behaviorBuilder.append(" 奖励积分");
            integralService.addIntegralRecord(userId, totalIntegral, behaviorBuilder.toString());

            List<CarbonRecord> history = carbonRecordService.getCarbonRecordByUserId(userId, 3);

            String weather;
            if (latitude != null && longitude != null) {
                weather = aiService.getWeatherByLocation(latitude, longitude);
                logger.info("使用用户所在城市天气：{}", weather);
            } else {
                weather = aiService.getCurrentWeather();
                logger.info("使用默认天气：{}", weather);
            }

            User user = userService.getUserById(userId);
            String nickname = (user != null && StringUtils.hasText(user.getNickname())) ? user.getNickname() : "小伙伴";

            CarbonChatDTO warmChat = aiService.generateWarmCarbonChat(totalCarbon, history, weather, firstParseResult, nickname);

            List<CarbonRecord> allRecords = carbonRecordService.getCarbonRecordByUserId(userId);
            List<AiSuggestionDTO> suggestionList = aiService.generateCarbonSuggestion(allRecords);

            Map<String, Object> data = new HashMap<>();
            data.put("totalCarbon", totalCarbon);
            data.put("totalIntegral", totalIntegral);
            data.put("warmChat", warmChat);
            data.put("suggestionList", suggestionList);

            result.put("code", 200);
            result.put("msg", "记录成功，获得" + totalIntegral + "绿芽积分");
            result.put("data", data);
        } catch (Exception e) {
            logger.error("确认文本解析失败", e);
            result.put("code", 500);
            result.put("msg", "确认解析结果失败：" + e.getMessage());
        }
        return result;
    }

    // 查询接口
    @GetMapping("/list")
    public Map<String, Object> getCarbonRecord(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate start;
            LocalDate end = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

            if (startDate == null || startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
                start = LocalDate.now().minusDays(30);
            } else {
                start = LocalDate.parse(startDate, formatter);
                end = LocalDate.parse(endDate, formatter);
            }
            List<CarbonRecord> list = carbonRecordService.getCarbonRecordByUserIdAndDateAndType(userId, start, end, type);
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", list);
        } catch (Exception e) {
            logger.error("查询碳记录失败", e);
            result.put("code", 500);
            result.put("msg", "查询碳记录失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    @GetMapping("/list/all")
    public Map<String, Object> getAllCarbonRecord(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<CarbonRecord> list = carbonRecordService.getCarbonRecordByUserId(userId);
            result.put("code", 200);
            result.put("msg", "全量记录查询成功");
            result.put("data", list);
        } catch (Exception e) {
            logger.error("查询全量碳记录失败", e);
            result.put("code", 500);
            result.put("msg", "查询全量碳记录失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    @GetMapping("/group")
    public Map<String, Object> getCarbonRecordGroupByType(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<CarbonRecord> list = carbonRecordService.getCarbonRecordGroupByType(userId);
            result.put("code", 200);
            result.put("msg", "分组查询成功");
            result.put("data", list);
        } catch (Exception e) {
            logger.error("分组查询失败", e);
            result.put("code", 500);
            result.put("msg", "分组查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    @GetMapping("/get/{id}")
    public Map<String, Object> getCarbonRecordById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            CarbonRecord record = carbonRecordService.getCarbonRecordById(id);
            if (record != null) {
                result.put("code", 200);
                result.put("msg", "查询成功");
                result.put("data", record);
            } else {
                result.put("code", 500);
                result.put("msg", "记录不存在");
                result.put("data", null);
            }
        } catch (Exception e) {
            logger.error("根据ID查询记录失败", e);
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
    @PostMapping("/update")
    public Map<String, Object> updateCarbonRecord(@RequestBody CarbonRecord carbonRecord) {
        Map<String, Object> result = new HashMap<>();
        try {
            CarbonRecord oldRecord = carbonRecordService.getCarbonRecordById(carbonRecord.getId());
            if (oldRecord == null) {
                result.put("code", 404);
                result.put("msg", "记录不存在");
                return result;
            }
            if (carbonRecord.getUnit() == null || carbonRecord.getUnit().trim().isEmpty()) {
                carbonRecord.setUnit(oldRecord.getUnit());
            }
            if (carbonRecord.getType() == null || carbonRecord.getType().trim().isEmpty()) {
                carbonRecord.setType(oldRecord.getType());
            }
            double oldEmission = oldRecord.getCarbonEmission().doubleValue();
            int oldIntegral = oldEmission < 1 ? 10 : (oldEmission < 5 ? 5 : 1);
            String deductReason = "修改记录：扣减【" + oldRecord.getDetail() + "】积分";
            integralService.deductIntegralRecord(oldRecord.getUserId(), oldIntegral, deductReason);
            boolean updateFlag = carbonRecordService.updateCarbonRecord(carbonRecord);
            if (updateFlag) {
                double newEmission = carbonRecord.getCarbonEmission().doubleValue();
                int newIntegral = newEmission < 1 ? 10 : (newEmission < 5 ? 5 : 1);
                String addReason = "修改记录：新增【" + carbonRecord.getDetail() + "】积分";
                integralService.addIntegralRecord(carbonRecord.getUserId(), newIntegral, addReason);
                result.put("code", 200);
                result.put("msg", "修改成功，积分调整：扣减" + oldIntegral + "分，新增" + newIntegral + "分");
                result.put("data", carbonRecord);
            } else {
                result.put("code", 500);
                result.put("msg", "修改失败");
            }
        } catch (Exception e) {
            logger.error("修改碳记录失败", e);
            result.put("code", 500);
            result.put("msg", "修改异常：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteCarbonRecord(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            CarbonRecord record = carbonRecordService.getCarbonRecordById(id);
            if (record == null) {
                result.put("code", 500);
                result.put("msg", "记录不存在");
                return result;
            }
            double emission = record.getCarbonEmission().doubleValue();
            int integral = emission < 1 ? 10 : (emission < 5 ? 5 : 1);
            String deleteReason = "删除记录：【" + record.getDetail() + "】扣减积分";
            integralService.deductIntegralRecord(record.getUserId(), integral, deleteReason);
            boolean deleteFlag = carbonRecordService.deleteCarbonRecord(id);
            if (deleteFlag) {
                result.put("code", 200);
                result.put("msg", "删除成功，扣减" + integral + "绿芽积分");
            } else {
                result.put("code", 500);
                result.put("msg", "删除失败");
            }
        } catch (Exception e) {
            logger.error("删除碳记录失败", e);
            result.put("code", 500);
            result.put("msg", "删除异常：" + e.getMessage());
        }
        return result;
    }
}