# aid-service（资助与绿色通道）

资助政策、绿色通道申请与审核。端口 `8104`，路径前缀 `/api/aid`。

## 自检与文档

```bash
curl http://localhost:8104/api/aid/ping          # 直连
curl http://localhost:8080/api/aid/ping            # 经网关
```

Swagger：<http://localhost:8104/swagger-ui.html>

## 结构

```
src/main/java/com/sicnu/yingxin/aid/
├── AidApplication.java        # 启动类
└── controller/                  # 接口层
src/main/resources/
├── application.yml              # 连接信息走环境变量，本地有默认值
└── mapper/                      # MyBatis XML 映射文件
```

数据库表统一建在 `yingxin` 库（按服务分表），Redis 用于缓存。

约定与版本基线见 [../README.md](../README.md)，部署见 [../../docs/部署指南.md](../../docs/部署指南.md)。
