# navigation-service（校园导航）

室内外地图、路径规划、数字人带路。端口 `8103`，路径前缀 `/api/navigation`。

## 自检与文档

```bash
curl http://localhost:8103/api/navigation/ping          # 直连
curl http://localhost:8080/api/navigation/ping            # 经网关
```

Swagger：<http://localhost:8103/swagger-ui.html>

## 结构

```
src/main/java/com/sicnu/yingxin/navigation/
├── NavigationApplication.java        # 启动类
└── controller/                  # 接口层
src/main/resources/
├── application.yml              # 连接信息走环境变量，本地有默认值
└── mapper/                      # MyBatis XML 映射文件
```

数据库表统一建在 `yingxin` 库（按服务分表），Redis 用于缓存。

约定与版本基线见 [../README.md](../README.md)，部署见 [../../docs/部署指南.md](../../docs/部署指南.md)。
