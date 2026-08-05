package com.unimall.goods;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.unimall.goods.mapper")
public class GoodsApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(GoodsApplication.class, args);
    }
}
