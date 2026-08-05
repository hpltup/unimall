# unimall-service-upload — 文件上传服务

文件上传服务（端口 **10018**）：本地磁盘存储，返回可访问 URL。**不落库**（文件元信息后续需要时再加表）。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /upload`（multipart `file` 字段） | 单文件上传：UUID 重命名 + 扩展名白名单 + 大小限制 → 存本地 → 返回 `/api/upload/{文件名}` | 需登录 |
| `GET /upload/{文件名}` | 静态资源访问（`UploadConfig` 资源映射到本地目录） | **公开**（网关白名单 `/api/upload/` 带尾斜杠） |

## 分层

```
com.unimall.upload/
├── UploadApplication.java        # @SpringBootApplication
├── controller/UploadController.java   # POST /upload
├── service/IUploadService.java + impl/UploadServiceImpl.java
└── config/
    ├── UploadProperties.java     # @ConfigurationProperties(unimall.upload.*)：path/maxSize/allowedExt
    ├── UploadConfig.java         # WebMvcConfigurer：/upload/** → file:{path}
    └── GlobalExceptionHandler.java
```

## 要点

- 错误码：`7001 文件不能为空` / `7002 文件大小超出限制` / `7003 不支持的文件类型` / `5000 文件保存失败`
- 配置（配置中心 `upload-dev.yml`）：存储目录 `D:/unimall-upload/`、单文件 5MB、扩展名白名单（jpg/jpeg/png/gif/webp）、`spring.servlet.multipart` 上限 10MB/20MB
- **网关白名单用 `/api/upload/`（带尾斜杠）**：只放行 `/api/upload/xxx` 资源路径（图片公开访问），`POST /api/upload`（无尾斜杠）不匹配 → 上传仍需登录
- 文件名：UUID（去横线）+ 原扩展名（小写），防重名/路径穿越
- 返回 URL 为相对路径 `/api/upload/{文件名}`（走网关），前端直接拼域名使用
- 无 MyBatis-Plus / MySQL 依赖（本服务不落库）

## 已知问题

- 本地磁盘存储（单机）：多实例部署时文件不共享，生产需换对象存储（OSS）或共享文件系统
- 无文件清理策略（孤儿文件会累积）
