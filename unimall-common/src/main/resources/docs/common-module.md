# unimall-common 公共模块说明

> 微服务集群公共层：统一返回体、业务异常、JWT 工具。**纯 Java，零 Spring 依赖**，网关（WebFlux）与业务服务（MVC）均可依赖。

## 目录

- [一、模块定位](#一模块定位)
- [二、设计原则](#二设计原则)
- [三、组件清单](#三组件清单)
- [四、使用示例](#四使用示例)
- [五、目录结构](#五目录结构)
- [六、依赖说明](#六依赖说明)

---

## 一、模块定位

`unimall-common` 存放所有微服务共用的基础能力，避免各模块重复造轮子：

| 组件 | 被谁使用 |
|---|---|
| `JwtUtil` | user 服务（签发）、gateway（校验） |
| `Result` | 所有服务接口 + 网关 401 响应 |
| `BusinessException` | 各业务服务的业务异常 |

## 二、设计原则

1. **纯 Java，无 Spring 依赖**——不引入 `spring-boot-starter-web` 等框架依赖，因此 gateway（WebFlux 应用）和 user（MVC 应用）依赖它都不会带来类路径污染
2. **无数据库/中间件访问**——只做无状态工具与数据结构
3. **被所有业务模块依赖**——新模块开发时在 pom 引入 `unimall-common` 即可

## 三、组件清单

### 1. `com.unimall.common.utils.JwtUtil` — JWT 工具（HS256）

构造：`new JwtUtil(String secret, long expireSeconds)`

- 密钥要求：HS256 对称密钥，**>= 32 字节（256 bit）**，由 `Keys.hmacShaKeyFor` 生成 `SecretKey`
- token payload：`sub` = userId、`jti` = UUID（Redis 白名单 key）、`iat`、`exp`

| 方法 | 说明 |
|---|---|
| `String generateToken(Long userId)` | 签发 token |
| `Claims parseToken(String token)` | 验签 + 解析，失败抛 `JwtException` |
| `String getJti(Claims)` | 取 `jti`（Redis key 用） |
| `Long getUserId(Claims)` | 取 `sub`（userId） |
| `long getExpireSeconds()` | token 有效期（秒），签发端存 Redis TTL 时保证与 JWT 一致 |

### 2. `com.unimall.common.result.Result<T>` — 统一返回体

结构：`{ code, message, data }`

| 静态工厂 | 说明 |
|---|---|
| `Result.ok()` / `Result.ok(data)` | 成功（code=0） |
| `Result.fail(code, message)` | 失败 |

### 3. `com.unimall.common.exception.BusinessException` — 业务异常

```java
throw new BusinessException(1001, "用户名已存在");
```

携带 `code` + `message`，由各服务的 `@RestControllerAdvice` 统一捕获转 `Result`。

## 四、使用示例

```java
// 签发（user 服务）
JwtUtil jwtUtil = new JwtUtil(secret, 1800);
String token = jwtUtil.generateToken(userId);

// 校验（网关）
Claims claims = jwtUtil.parseToken(token);
Long userId = jwtUtil.getUserId(claims);

// 统一响应
return Result.ok(data);
throw new BusinessException(1001, "用户名已存在");
```

## 五、目录结构

```
unimall-common/src/main/java/com/unimall/common/
├── utils/JwtUtil.java
├── result/Result.java
└── exception/BusinessException.java
```

## 六、依赖说明

`pom.xml` 仅引入 `jjwt 0.12.6`（api + impl + jackson）：

- `jjwt-api`：编译期 API
- `jjwt-impl` / `jjwt-jackson`：runtime 实现与 JSON 序列化
- 版本显式写死 0.12.6（根 pom 未管理 jjwt 版本）
