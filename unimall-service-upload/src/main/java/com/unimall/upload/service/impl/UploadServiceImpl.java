package com.unimall.upload.service.impl;

import com.unimall.common.exception.BusinessException;
import com.unimall.upload.config.UploadProperties;
import com.unimall.upload.service.IUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class UploadServiceImpl implements IUploadService
{
    private static final String URL_PREFIX = "/api/upload/";

    private final UploadProperties uploadProperties;

    public UploadServiceImpl(UploadProperties uploadProperties)
    {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String store(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new BusinessException(7001, "文件不能为空");
        }
        if (file.getSize() > uploadProperties.getMaxSize())
        {
            throw new BusinessException(7002, "文件大小超出限制");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains("."))
        {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!uploadProperties.getAllowedExt().contains(ext))
        {
            throw new BusinessException(7003, "不支持的文件类型");
        }

        try
        {
            Path dir = Paths.get(uploadProperties.getPath());
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            return URL_PREFIX + filename;
        }
        catch (IOException e)
        {
            throw new BusinessException(5000, "文件保存失败");
        }
    }
}
