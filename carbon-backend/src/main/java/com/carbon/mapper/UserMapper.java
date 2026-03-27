package com.carbon.mapper;

import com.carbon.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper {
    // 注册用户，返回受影响的行数（1表示成功）
    @Insert("INSERT INTO user (username, password, nickname, integral, create_time, update_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{integral}, NOW(), NOW())")
    int insertUser(User user);

    // 根据用户名查询用户
    @Select("SELECT id, username, password, nickname, integral, create_time as createTime, update_time as updateTime " +
            "FROM user WHERE username = #{username}")
    User selectUserByUsername(String username);

    // 根据ID查询用户
    @Select("SELECT id, username, password, nickname, integral, create_time as createTime, update_time as updateTime " +
            "FROM user WHERE id = #{userId}")
    User selectUserById(@Param("userId") Long userId);

    // 更新用户信息
    @Update("UPDATE user SET nickname=#{nickname}, update_time=NOW() WHERE id=#{id}")
    int updateUser(User user);

    // 更新积分
    @Update("UPDATE user SET integral=integral+#{integral}, update_time=NOW() WHERE id=#{userId}")
    int updateIntegral(@Param("userId") Long userId, @Param("integral") Integer integral);
}