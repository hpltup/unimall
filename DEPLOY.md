# UniMall Docker 部署说明（方案 A：仅容器化服务）

> 15 个模块中的 **13 个服务**容器化部署（`unimall-common` 是库、`unimall-registry` 是 Nacos 部署说明壳，均不部署）。
> 中间件继续用现有的：Redis / ES / RabbitMQ 在虚拟机本机，Nacos / MySQL 在 Windows 宿主。

## 一、部署拓扑

```
┌─ Windows 宿主机 ─────────────────────────────┐
│  Nacos (127.0.0.1:8848)     MySQL (127.0.0.1:3306)  │
└──────────────┬────────────────────────────────┘
               │ 宿主 IP（虚拟机可达，.env 中 NACOS_ADDR / MYSQL_HOST）
┌─ 虚拟机 192.168.89.101 ──────────────────────┐
│  Docker 29.5.2                               │
│  ┌─ unimall-net (bridge) ─────────────────┐  │
│  │  unimall-config (10010) ← healthcheck  │  │
│  │  unimall-gateway  (10011) 对外入口      │  │
│  │  11 个业务服务 (10012~10022)           │  │
│  │  服务间用 compose 服务名通信            │  │
│  └───────────────────────────────────────┘  │
│  Redis:6379 / ES:9200 / RabbitMQ:5672（本机） │
└──────────────────────────────────────────────┘
```

## 二、网络前提（关键，容器连 Windows 宿主）

Nacos 与 MySQL 在 Windows 上，容器要访问它们必须满足：

1. **虚拟机 → Windows 宿主 IP 可达**：在虚拟机上 `ping <宿主IP>` 确认（NAT 模式宿主一般是虚拟机网关，如 `192.168.89.1` 或 `192.168.89.2`；桥接模式用 Windows 的局域网 IP）
2. **MySQL 允许远程连接**：Windows 的 MySQL 执行
   ```sql
   ALTER USER 'root'@'%' IDENTIFIED BY 'SQL123456';
   FLUSH PRIVILEGES;
   ```
   （或建专用账号），并确认 Windows 防火墙放行 3306
3. **Nacos 默认监听 0.0.0.0**，确认 Windows 防火墙放行 8848（HTTP）/ 9848（gRPC）

> 其余中间件（Redis/ES/RabbitMQ）就在虚拟机本机，配置中心里已是 `192.168.89.101`，容器直接可达，无需处理。

## 三、构建前准备

```bash
# 1. 生成各服务 jar（本项目已生成过，改代码后需重跑）
mvn -q -o clean package -DskipTests
```

## 四、配置 .env

```bash
cp .env.example .env
# 编辑 .env，至少填：
#   GITEE_TOKEN   —— gitee 私人令牌（config 拉私有配置仓库）
#   NACOS_ADDR    —— Windows 宿主 IP:8848
#   MYSQL_HOST    —— Windows 宿主 IP
#   DEEPSEEK_API_KEY —— AI 客服用（可选）
```

> 配置中心（gitee `unimall-config-dev`）里的 `jdbc:mysql://127.0.0.1:3306` 等地址在容器内**不适用**，compose 已通过 `SPRING_DATASOURCE_URL` 等环境变量覆盖（env 优先级高于配置中心）。

## 五、构建与启动

```bash
# 在项目根目录（含 docker-compose.yml）
docker compose build        # 构建 13 个镜像（拉取 eclipse-temurin:21-jre）
docker compose up -d        # 按依赖顺序启动（业务服务等待 config 健康）
docker compose ps           # 查看状态
docker compose logs -f unimall-config   # 看 config 是否成功拉取 gitee 配置
```

验证：

```bash
# config 健康
curl http://192.168.89.101:10010/actuator/health
# 网关（转发到 user）
curl http://192.168.89.101:10011/api/user/register -X POST ...   # 注册/登录
# Nacos 控制台看服务注册：http://<宿主IP>:8848/nacos
```

## 六、服务清单与端口

| 服务 | 端口 | 数据源覆盖 | 备注 |
|---|---|---|---|
| unimall-config | 10010 | - | 需 GITEE_TOKEN |
| unimall-gateway | 10011 | - | 对外入口 |
| unimall-service-user | 10012 | ✅ | |
| unimall-service-goods | 10013 | ✅ | |
| unimall-service-cart | 10014 | ✅ | |
| unimall-service-order | 10015 | ✅ | |
| unimall-service-seckill | 10016 | ✅ | |
| unimall-service-comments | 10017 | ✅ | |
| unimall-service-upload | 10018 | - | 挂载 `./data/upload:/app/upload` |
| unimall-service-sendmsg | 10019 | ✅ | |
| unimall-service-search | 10020 | - | 连 ES（虚拟机） |
| unimall-service-admin | 10021 | ✅ | |
| unimall-service-ai | 10022 | - | 需 DEEPSEEK_API_KEY |

## 七、常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| config 拉取失败 | GITEE_TOKEN 未填/失效、仓库分支不对 | 检查 .env；`docker compose logs unimall-config` |
| 业务服务反复重启 | 等 config 健康；或 Nacos/MySQL 不可达 | `depends_on` 已串行；检查 NACOS_ADDR/MYSQL_HOST |
| 服务注册到 Nacos 显示容器 IP | 正常（compose 网络内互通，gateway 通过 lb 调用可达） | 无需处理 |
| MySQL 连接拒绝 | root 不允许远程 / 防火墙未放行 3306 | 见第二节 |
| 时间显示差 8 小时 | 容器时区 | Dockerfile 已加 `-Duser.timezone=Asia/Shanghai` |
| 上传文件重启丢失 | 未挂载 volume | 已挂载 `./data/upload`，确认目录存在 |
| AI 对话 401 | DEEPSEEK_API_KEY 未填 | .env 配置后 `docker compose up -d unimall-service-ai` |
| 端口占用 | 本机已跑同名服务 | 先停 IDEA 里的服务再起容器，或改端口映射 |

## 八、日常操作

```bash
docker compose up -d                # 启动全部
docker compose restart unimall-gateway   # 重启单个
docker compose logs -f <服务名>      # 看日志
docker compose down                 # 停止（保留数据卷）
docker compose down -v              # 停止并删数据（upload 数据会丢）
```

> 改配置流程不变：改 gitee 仓库 → push → `POST http://192.168.89.101:10010/actuator/busrefresh`（204）广播热刷新。
