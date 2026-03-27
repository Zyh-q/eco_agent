package com.carbon.service.impl;

import com.carbon.entity.IntegralRecord;
import com.carbon.mapper.IntegralRecordMapper;
import com.carbon.service.IntegralService;
import com.carbon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IntegralServiceImpl implements IntegralService {
    @Autowired
    private IntegralRecordMapper integralRecordMapper;
    @Autowired
    private UserService userService;

    @Transactional
    @Override
    public boolean addIntegralRecord(Long userId, Integer integral, String reason) {
        if (userId == null || integral <= 0 || userService.getUserById(userId) == null) {
            return false;
        }

        // 1. 更新用户总积分
        boolean updateFlag = userService.updateIntegral(userId, integral);
        // 2. 插入积分记录
        IntegralRecord integralRecord = new IntegralRecord();
        integralRecord.setUserId(userId);
        integralRecord.setIntegral(integral);
        integralRecord.setReason(reason);
        integralRecord.setCreateTime(LocalDateTime.now());
        boolean insertFlag = integralRecordMapper.insertIntegralRecord(integralRecord) > 0;

        return updateFlag && insertFlag;
    }

    @Transactional
    @Override
    public boolean deductIntegralRecord(Long userId, Integer integral, String reason) {
        // 前置校验：用户存在+积分正数
        if (userId == null || integral <= 0 || userService.getUserById(userId) == null) {
            return false;
        }

        // 1. 扣减用户总积分（传入负数）
        boolean updateFlag = userService.updateIntegral(userId, -integral);
        // 2. 插入积分记录（积分值为负数）
        IntegralRecord integralRecord = new IntegralRecord();
        integralRecord.setUserId(userId);
        integralRecord.setIntegral(-integral); // 负数表示扣减
        integralRecord.setReason(reason);
        integralRecord.setCreateTime(LocalDateTime.now());
        boolean insertFlag = integralRecordMapper.insertIntegralRecord(integralRecord) > 0;

        return updateFlag && insertFlag;
    }

    @Override
    public List<IntegralRecord> getIntegralRecordByUserId(Long userId) {
        return integralRecordMapper.selectByUserId(userId);
    }
}