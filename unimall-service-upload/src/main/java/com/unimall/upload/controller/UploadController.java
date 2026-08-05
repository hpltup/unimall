package com.unimall.upload.controller;

import com.unimall.common.result.Result;
import com.unimall.upload.service.IUploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController
{
    private final IUploadService uploadService;

    public UploadController(IUploadService uploadService)
    {
        this.uploadService = uploadService;
    }

    /**
     * 单文件上传（需登录），返回 /api/upload/{文件名}
     */
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file)
    {
        return Result.ok(uploadService.store(file));
    }
}
