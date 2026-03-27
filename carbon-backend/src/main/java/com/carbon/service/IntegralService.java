package com.carbon.service;

import com.carbon.entity.IntegralRecord;

import java.util.List;

public interface IntegralService {
    // 新增积分记录
    boolean addIntegralRecord(Long userId, Integer integral, String reason);

    // 查询用户积分记录
    List<IntegralRecord> getIntegralRecordByUserId(Long userId);

    // 扣减积分记录
    boolean deductIntegralRecord(Long userId, Integer integral, String reason);
}