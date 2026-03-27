package com.carbon.controller;

import com.carbon.entity.User;
import com.carbon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        boolean flag = userService.register(user);
        if (flag) {
            result.put("code", 200);
            result.put("msg", "注册成功");
        } else {
            result.put("code", 500);
            result.put("msg", "用户名已存在或注册失败");
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = params.get("username");
            String password = params.get("password");
            User user = userService.login(username, password);

            result.put("code", 200);
            result.put("msg", "登录成功");
            user.setPassword(null);
            result.put("data", user);
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            result.put("code", 500);
            result.put("msg", errorMsg);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "登录失败，请稍后重试");
        }
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> updateUser(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        boolean flag = userService.updateUser(user);
        if (flag) {
            result.put("code", 200);
            result.put("msg", "更新成功");
        } else {
            result.put("code", 500);
            result.put("msg", "更新失败");
        }
        return result;
    }

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.getUserById(userId);
        if (user != null) {
            user.setPassword(null);
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", user);
        } else {
            result.put("code", 500);
            result.put("msg", "用户不存在");
        }
        return result;
    }
}