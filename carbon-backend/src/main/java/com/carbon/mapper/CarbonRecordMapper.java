package com.carbon.mapper;

import com.carbon.entity.CarbonRecord;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CarbonRecordMapper {

    @Insert("INSERT INTO carbon_record(user_id, type, detail, amount, unit, carbon_emission, record_time) " +
            "VALUES(#{userId}, #{type}, #{detail}, #{amount}, #{unit}, #{carbonEmission}, #{recordTime})")
    int insertCarbonRecord(CarbonRecord carbonRecord);

    @Select("SELECT * FROM carbon_record " +
            "WHERE user_id=#{userId} AND record_time BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY record_time DESC, id DESC")
    List<CarbonRecord> selectByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Select("SELECT type, SUM(carbon_emission) as carbonEmission " +
            "FROM carbon_record WHERE user_id=#{userId} " +
            "GROUP BY type ORDER BY carbonEmission DESC")
    List<CarbonRecord> selectGroupByType(@Param("userId") Long userId);

    @Select("SELECT * FROM carbon_record " +
            "WHERE user_id=#{userId} " +
            "ORDER BY record_time DESC")
    List<CarbonRecord> selectByUserIdOrderByTimeDesc(@Param("userId") Long userId);

    @Select("SELECT * FROM carbon_record " +
            "WHERE user_id=#{userId} " +
            "ORDER BY record_time DESC " +
            "LIMIT #{limit}")
    List<CarbonRecord> selectByUserIdWithLimit(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    @Select("SELECT * FROM carbon_record WHERE id=#{id}")
    CarbonRecord selectById(@Param("id") Long id);

    @Update("UPDATE carbon_record SET " +
            "type=#{type}, detail=#{detail}, amount=#{amount}, unit=#{unit}, " +
            "carbon_emission=#{carbonEmission}, record_time=#{recordTime} " +
            "WHERE id=#{id}")
    int updateCarbonRecord(CarbonRecord carbonRecord);

    @Delete("DELETE FROM carbon_record WHERE id=#{id}")
    int deleteCarbonRecord(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT * FROM carbon_record ",
            "WHERE user_id = #{userId} ",
            "AND record_time BETWEEN #{startDate} AND #{endDate} ",
            "<if test='type != null and type != \"\"'>",
            "AND type = #{type} ",
            "</if>",
            "ORDER BY record_time DESC, id DESC",
            "</script>"
    })
    List<CarbonRecord> selectByUserIdAndDateAndType(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") String type
    );
}