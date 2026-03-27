package com.carbon.controller;

import com.carbon.dto.AiSuggestionDTO;
import com.carbon.dto.CarbonChatDTO;
import com.carbon.dto.ImageParseDTO;
import com.carbon.entity.CarbonRecord;
import com.carbon.service.AiService;
import com.carbon.service.CarbonRecordService;
import com.carbon.service.IntegralService;
import com.carbon.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import com.carbon.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/image")
public class ImageController {
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private AiService aiService;
    @Autowired
    private CarbonRecordService carbonRecordService;
    @Autowired
    private IntegralService integralService;
    @Autowired
    private UserService userService;

    /**
     * 图片上传解析
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (userId == null || userId <= 0) {
            result.put("code", 400);
            result.put("msg", "用户ID不能为空且必须为正数");
            return result;
        }
        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "请选择要上传的图片");
            return result;
        }
        try {
            logger.info("解析用户[{}]的图片：{}", userId, file.getOriginalFilename());
            ImageParseDTO parseResult = aiService.parseCarbonFromImage(file);
            Double carbonAmount = aiService.calculateCarbonWithParseDTO(parseResult);
            int integral = carbonAmount < 1 ? 10 : (carbonAmount < 5 ? 5 : 1);
            result.put("code", 200);
            result.put("msg", "图片解析成功");
            Map<String, Object> data = new HashMap<>();
            data.put("parseResult", parseResult);
            data.put("carbonAmount", carbonAmount);
            data.put("integral", integral);
            result.put("data", data);
        } catch (Exception e) {
            logger.error("图片解析失败", e);
            result.put("code", 500);
            result.put("msg", "图片解析失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 确认图片解析结果
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirmImageResult(
            @RequestBody ImageParseDTO parseDTO,
            @RequestParam Long userId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        Map<String, Object> result = new HashMap<>();

        if (userId == null || userId <= 0) {
            result.put("code", 400);
            result.put("msg", "用户ID不能为空且必须为正数");
            return result;
        }
        if (parseDTO == null || !parseDTO.checkValid()) {
            result.put("code", 400);
            result.put("msg", "解析结果不能为空且参数必须合法");
            return result;
        }

        try {
            Double carbonAmount = aiService.calculateCarbonWithParseDTO(parseDTO);

            CarbonRecord carbonRecord = new CarbonRecord();
            carbonRecord.setUserId(userId);
            carbonRecord.setType(parseDTO.getType());
            carbonRecord.setDetail(parseDTO.getDetail());
            carbonRecord.setAmount(BigDecimal.valueOf(parseDTO.getAmount()));
            carbonRecord.setUnit(parseDTO.getUnit());
            carbonRecord.setCarbonEmission(BigDecimal.valueOf(carbonAmount));
            carbonRecord.setRecordTime(LocalDate.now());

            boolean flag = carbonRecordService.addCarbonRecord(carbonRecord);
            if (!flag) {
                result.put("code", 500);
                result.put("msg", "记录保存失败");
                return result;
            }

            int integral = carbonAmount < 1 ? 10 : (carbonAmount < 5 ? 5 : 1);
            String reason = "图片解析：" + parseDTO.getDetail() + " 奖励积分";
            integralService.addIntegralRecord(userId, integral, reason);

            List<CarbonRecord> historyRecords = carbonRecordService.getCarbonRecordByUserId(userId, 3);
            String weather;
            if (latitude != null && longitude != null) {
                weather = aiService.getWeatherByLocation(latitude, longitude);
            } else {
                weather = aiService.getCurrentWeather();
            }

            // 获取用户昵称
            User user = userService.getUserById(userId);
            String nickname = (user != null && org.springframework.util.StringUtils.hasText(user.getNickname()))
                    ? user.getNickname() : "小伙伴";

            CarbonChatDTO warmChat = aiService.generateWarmCarbonChat(carbonAmount, historyRecords, weather, parseDTO, nickname);

            List<CarbonRecord> allRecords = carbonRecordService.getCarbonRecordByUserId(userId);
            List<AiSuggestionDTO> suggestionList = aiService.generateCarbonSuggestion(allRecords);

            Map<String, Object> data = new HashMap<>();
            data.put("carbonAmount", carbonAmount);
            data.put("integral", integral);
            data.put("warmChat", warmChat);
            data.put("record", carbonRecord);
            data.put("suggestionList", suggestionList);

            result.put("code", 200);
            result.put("msg", "记录成功，获得" + integral + "绿芽积分");
            result.put("data", data);

        } catch (Exception e) {
            logger.error("确认解析结果失败", e);
            result.put("code", 500);
            result.put("msg", "确认解析结果失败：" + e.getMessage());
        }
        return result;
    }
}