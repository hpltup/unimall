package com.unimall.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "unimall.upload")
public class UploadProperties
{
    /** 文件存储目录（结尾带 /，如 D:/unimall-upload/） */
    private String path;

    /** 单文件大小上限（字节） */
    private Long maxSize;

    /** 允许的扩展名（不含点） */
    private List<String> allowedExt = new ArrayList<>();
}
