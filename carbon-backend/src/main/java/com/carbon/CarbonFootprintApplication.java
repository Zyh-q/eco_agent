package com.carbon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.carbon.mapper")
public class CarbonFootprintApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarbonFootprintApplication.class, args);
    }
}