package com.carbon.service;

import com.carbon.entity.CarbonRecord;
import java.time.LocalDate;
import java.util.List;

public interface CarbonRecordService {
    // 新增碳记录
    boolean addCarbonRecord(CarbonRecord carbonRecord);

    // 查询用户指定时间范围的记录
    List<CarbonRecord> getCarbonRecordByUserIdAndDate(Long userId, LocalDate startDate, LocalDate endDate);

    // 查询用户按类型分组的记录
    List<CarbonRecord> getCarbonRecordGroupByType(Long userId);

    // 查询用户所有碳记录（按时间倒序）
    List<CarbonRecord> getCarbonRecordByUserId(Long userId);

    // 查询用户指定条数的历史碳记录
    List<CarbonRecord> getCarbonRecordByUserId(Long userId, int limit);

    // 根据ID查询单条记录
    CarbonRecord getCarbonRecordById(Long id);

    // 修改碳记录
    boolean updateCarbonRecord(CarbonRecord carbonRecord);

    // 删除碳记录
    boolean deleteCarbonRecord(Long id);

    // 时间+类型组合查询
    List<CarbonRecord> getCarbonRecordByUserIdAndDateAndType(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String type
    );
}