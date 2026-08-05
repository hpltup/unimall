package com.unimall.upload.service;

import org.springframework.web.multipart.MultipartFile;

public interface IUploadService
{
    /**
     * 保存文件到本地磁盘，返回可访问 URL（/api/upload/{文件名}，走网关）
     */
    String store(MultipartFile file);
}
