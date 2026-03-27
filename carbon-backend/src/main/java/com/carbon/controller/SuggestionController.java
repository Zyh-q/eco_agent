package com.carbon.controller;

import com.carbon.dto.AiSuggestionDTO;
import com.carbon.entity.CarbonRecord;
import com.carbon.service.AiService;
import com.carbon.service.CarbonRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/suggestion")
public class SuggestionController {
    @Autowired
    private CarbonRecordService carbonRecordService;
    @Autowired
    private AiService aiService;

    /**
     * AI生成个性化减碳建议
     */
    @GetMapping("/ai")
    public Map<String, Object> getAiSuggestion(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (userId == null || userId <= 0) {
            result.put("code", 400);
            result.put("msg", "用户ID不能为空");
            result.put("data", null);
            return result;
        }

        try {
            // 查询用户全量碳记录
            List<CarbonRecord> carbonRecords = carbonRecordService.getCarbonRecordByUserId(userId);

            // 无碳记录时直接返回空数组
            if (carbonRecords == null || carbonRecords.isEmpty()) {
                result.put("code", 200);
                result.put("msg", "用户暂无碳足迹记录，暂无减碳建议");
                result.put("data", List.of());
                return result;
            }

            // 有碳记录时才调用AI生成建议
            List<AiSuggestionDTO> suggestions = aiService.generateCarbonSuggestion(carbonRecords);

            result.put("code", 200);
            result.put("msg", "获取AI减碳建议成功");
            result.put("data", suggestions);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "生成AI建议失败：" + e.getMessage());
            result.put("data", List.of());
        }
        return result;
    }

    @GetMapping("/personal")
    public Map<String, Object> getPersonalSuggestion(@RequestParam Long userId) {
        return getAiSuggestion(userId);
    }
}