# content-service（内容管理）

安全教育、社团、公告、话术/问答库发布。端口 `8106`，路径前缀 `/api/content`。

## 自检与文档

```bash
curl http://localhost:8106/api/content/ping          # 直连
curl http://localhost:8080/api/content/ping            # 经网关
```

Swagger：<http://localhost:8106/swagger-ui.html>

## 结构

```
src/main/java/com/sicnu/yingxin/content/
├── ContentApplication.java        # 启动类
└── controller/                  # 接口层
src/main/resources/
├── application.yml              # 连接信息走环境变量，本地有默认值
└── mapper/                      # MyBatis XML 映射文件
```

数据库表统一建在 `yingxin` 库（按服务分表），Redis 用于缓存。

约定与版本基线见 [../README.md](../README.md)，部署见 [../../docs/部署指南.md](../../docs/部署指南.md)。
