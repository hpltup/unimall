package com.unimall.comments;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.unimall.comments.mapper")
public class CommentsApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CommentsApplication.class, args);
    }
}
