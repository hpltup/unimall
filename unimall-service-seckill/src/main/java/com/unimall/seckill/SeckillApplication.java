package com.unimall.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.unimall.seckill.mapper")
@EnableFeignClients
public class SeckillApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
