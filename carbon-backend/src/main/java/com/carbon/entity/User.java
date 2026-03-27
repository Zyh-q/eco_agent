package com.carbon.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;             // 用户ID
    private String username;     // 用户名
    private String password;     // 密码
    private String nickname;     // 昵称
    private Integer integral;    // 积分
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;  // 更新时间

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getIntegral() { return integral; }
    public void setIntegral(Integer integral) { this.integral = integral; }
}