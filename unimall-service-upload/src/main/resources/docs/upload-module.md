# unimall-service-upload 上传模块说明

> 文件上传服务：本地磁盘存储，返回可访问 URL。**不落库**。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、接口清单](#三接口清单)
- [四、功能实现思路](#四功能实现思路)
- [五、目录结构](#五目录结构)
- [六、配置说明](#六配置说明)
- [七、启动前提与已知问题](#七启动前提与已知问题)

---

## 一、模块定位

上传服务（端口 **10018**）：单文件上传（图片为主），UUID 重命名存本地磁盘，返回 `/api/upload/{文件名}`（走网关公开访问）。供商品图、头像、评论图等使用。

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config。**无 MyBatis-Plus / MySQL**（不落库，最小依赖）。

## 三、接口清单

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /upload`（multipart `file` 字段） | 单文件上传：校验 → UUID 重命名 → 存本地 → 返回 `/api/upload/{文件名}` | 需登录 |
| `GET /upload/{文件名}` | 静态资源（`UploadConfig` 资源映射到本地目录） | **公开** |

错误码：`7001 文件不能为空` / `7002 文件大小超出限制` / `7003 不支持的文件类型` / `5000 文件保存失败`。

## 四、功能实现思路

```
store(file)
  1. 空/大小校验（max-size）
  2. 扩展名白名单校验（jpg/jpeg/png/gif/webp）
  3. UUID（去横线）+ 小写扩展名命名（防重名/路径穿越）
  4. Files.createDirectories 确保目录存在 → transferTo 保存
  5. 返回 "/api/upload/" + filename
```

**静态资源映射**（`UploadConfig`）：`/upload/**` → `file:{unimall.upload.path}`。

**网关白名单技巧**：白名单配置 `/api/upload/`（**带尾斜杠**）——只放行 `/api/upload/xxx` 资源路径（图片公开），`POST /api/upload`（无尾斜杠）不匹配 → **上传仍需登录**。

## 五、目录结构

```
com.unimall.upload/
├── UploadApplication.java         # @SpringBootApplication
├── controller/UploadController.java
├── service/IUploadService.java + impl/UploadServiceImpl.java
└── config/
    ├── UploadProperties.java      # @ConfigurationProperties(unimall.upload.*)
    ├── UploadConfig.java          # 静态资源映射
    └── GlobalExceptionHandler.java
```

## 六、配置说明

- 本地 `application.yml`：端口 10018、Nacos、config 客户端
- 配置中心 `config-repo/upload-dev.yml`：
  - `spring.servlet.multipart`：max-file-size 10MB / max-request-size 20MB
  - `unimall.upload`：`path: D:/unimall-upload/`（尾斜杠）、`max-size: 5242880`（5MB）、`allowed-ext` 图片白名单

## 七、启动前提与已知问题

- 前提：Nacos、config 启动（git 模式）、存储目录可写
- 已知问题：本地磁盘存储（单机，多实例文件不共享，生产需换 OSS/共享文件系统）；无文件清理策略（孤儿文件累积）
