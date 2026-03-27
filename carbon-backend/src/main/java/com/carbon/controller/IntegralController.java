package com.carbon.controller;

import com.carbon.entity.IntegralRecord;
import com.carbon.service.IntegralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/integral")
public class IntegralController {
    @Autowired
    private IntegralService integralService;

    // 查询用户积分记录
    @GetMapping("/list")
    public Map<String, Object> getIntegralRecord(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<IntegralRecord> list = integralService.getIntegralRecordByUserId(userId);
        result.put("code", 200);
        result.put("data", list);
        return result;
    }
}