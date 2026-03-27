package com.carbon.service;

import com.carbon.entity.User;

public interface UserService {
    // 注册
    boolean register(User user);

    // 登录
    User login(String username, String password);

    // 更新用户信息
    boolean updateUser(User user);

    // 更新用户积分
    boolean updateIntegral(Long userId, Integer integral);

    // 根据ID查询用户
    User getUserById(Long userId);
}