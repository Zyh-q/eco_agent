package com.carbon.mapper;

import com.carbon.entity.IntegralRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegralRecordMapper {

    @Insert("INSERT INTO integral_record(user_id, integral, reason) VALUES(#{userId}, #{integral}, #{reason})")
    int insertIntegralRecord(IntegralRecord integralRecord);

    @Select("SELECT * FROM integral_record WHERE user_id=#{userId} ORDER BY create_time DESC")
    List<IntegralRecord> selectByUserId(@Param("userId") Long userId);
}