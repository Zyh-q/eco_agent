package com.carbon.mapper;

import com.carbon.entity.AiSuggestion;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface AiSuggestionMapper {

    @Insert("INSERT INTO ai_suggestion(user_id,type,content,suitable_scene,carbon_reduce,priority,match_reason,create_time) " +
            "VALUES(#{userId},#{type},#{content},#{suitableScene},#{carbonReduce},#{priority},#{matchReason},#{createTime})")
    int insertSuggestion(AiSuggestion suggestion);

    // 按用户ID查询
    @Select("SELECT * FROM ai_suggestion WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<AiSuggestion> selectByUserId(Long userId);

    // 按用户ID删除
    @Delete("DELETE FROM ai_suggestion WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}