package com.carbon.service.impl;

import com.carbon.entity.User;
import com.carbon.mapper.UserMapper;
import com.carbon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean register(User user) {
        // 基础非空校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return false;
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return false;
        }

        // 检查用户名是否已存在
        User existUser = userMapper.selectUserByUsername(user.getUsername().trim());
        if (existUser != null) {
            return false;
        }

        user.setIntegral(0);
        user.setUsername(user.getUsername().trim());

        try {
            int rows = userMapper.insertUser(user);
            return rows > 0;  // 明确根据插入结果返回
        } catch (Exception e) {
            return false;     // 任何数据库异常都返回失败
        }
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("请输入用户名");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("请输入密码");
        }

        User user = userMapper.selectUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    @Override
    public boolean updateUser(User user) {
        return userMapper.updateUser(user) > 0;
    }

    @Override
    public boolean updateIntegral(Long userId, Integer integral) {
        User user = userMapper.selectUserById(userId);
        if (user.getIntegral() == null) {
            user.setIntegral(0);
        }
        int newIntegral = user.getIntegral() + integral;
        if (newIntegral < 0) {
            return false;
        }
        return userMapper.updateIntegral(userId, integral) > 0;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }
}