package com.carbon.service.impl;

import com.carbon.entity.CarbonRecord;
import com.carbon.mapper.CarbonRecordMapper;
import com.carbon.service.CarbonRecordService;
import com.carbon.util.CarbonCalculationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CarbonRecordServiceImpl implements CarbonRecordService {

    @Autowired
    private CarbonRecordMapper carbonRecordMapper;

    @Autowired
    private CarbonCalculationUtil carbonCalculationUtil;


    @Override
    public boolean addCarbonRecord(CarbonRecord carbonRecord) {
        // 使用注入的工具类计算
        BigDecimal carbon = carbonCalculationUtil.calculateCarbon(
                carbonRecord.getType(),
                carbonRecord.getDetail(),
                carbonRecord.getAmount(),
                carbonRecord.getUnit()
        );
        carbonRecord.setCarbonEmission(carbon);
        if (carbonRecord.getRecordTime() == null) {
            carbonRecord.setRecordTime(LocalDate.now());
        }
        return carbonRecordMapper.insertCarbonRecord(carbonRecord) > 0;
    }

    @Override
    public List<CarbonRecord> getCarbonRecordByUserIdAndDate(Long userId, LocalDate startDate, LocalDate endDate) {
        return carbonRecordMapper.selectByUserIdAndDate(userId, startDate, endDate);
    }

    @Override
    public List<CarbonRecord> getCarbonRecordByUserId(Long userId) {
        return carbonRecordMapper.selectByUserIdOrderByTimeDesc(userId);
    }

    @Override
    public List<CarbonRecord> getCarbonRecordByUserId(Long userId, int limit) {
        return carbonRecordMapper.selectByUserIdWithLimit(userId, limit);
    }

    @Override
    public List<CarbonRecord> getCarbonRecordGroupByType(Long userId) {
        return carbonRecordMapper.selectGroupByType(userId);
    }

    @Override
    public CarbonRecord getCarbonRecordById(Long id) {
        return carbonRecordMapper.selectById(id);
    }


    @Override
    public boolean updateCarbonRecord(CarbonRecord carbonRecord) {
        BigDecimal carbon = carbonCalculationUtil.calculateCarbon(
                carbonRecord.getType(),
                carbonRecord.getDetail(),
                carbonRecord.getAmount(),
                carbonRecord.getUnit()
        );
        carbonRecord.setCarbonEmission(carbon);
        return carbonRecordMapper.updateCarbonRecord(carbonRecord) > 0;
    }

    @Override
    public boolean deleteCarbonRecord(Long id) {
        return carbonRecordMapper.deleteCarbonRecord(id) > 0;
    }

    // 组合查询实现
    @Override
    public List<CarbonRecord> getCarbonRecordByUserIdAndDateAndType(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String type
    ) {
        return carbonRecordMapper.selectByUserIdAndDateAndType(userId, startDate, endDate, type);
    }
}