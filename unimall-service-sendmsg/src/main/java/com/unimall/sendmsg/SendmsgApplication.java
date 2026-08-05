package com.unimall.sendmsg;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.unimall.sendmsg.mapper")
public class SendmsgApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SendmsgApplication.class, args);
    }
}
