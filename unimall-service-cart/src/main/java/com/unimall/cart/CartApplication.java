package com.unimall.cart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.unimall.cart.mapper")
@EnableFeignClients
public class CartApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CartApplication.class, args);
    }
}
