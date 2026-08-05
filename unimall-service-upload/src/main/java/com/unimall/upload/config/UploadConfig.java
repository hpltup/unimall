package com.unimall.upload.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射：/upload/** → 本地存储目录（图片等文件经网关公开访问）
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer
{
    private final UploadProperties uploadProperties;

    public UploadConfig(UploadProperties uploadProperties)
    {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadProperties.getPath());
    }
}
