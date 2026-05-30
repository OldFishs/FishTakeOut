package com.fish;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.fish.mapper")
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@Slf4j
public class FishApplication {
    public static void main(String[] args) {
        SpringApplication.run(FishApplication.class, args);
        log.info("server started");
    }
}
