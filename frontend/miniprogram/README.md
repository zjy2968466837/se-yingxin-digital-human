# 学生端网页工程（uni-app，原小程序端）

> **一期范围**：只做网页端（见 [需求分析文档](../../docs/需求分析文档.md) §8.4.1）。
> 本工程一期**只使用 H5 构建产物**作为学生端网页；**小程序端已裁剪**，`dev:mp-weixin` / `build:mp-weixin` 仅作存档。
> 是否另建轻量 Vue 网页工程替代本工程，见该文档附录 B Q-9（未决）。
>
> 目录名仍是 `miniprogram`（历史原因，避免改动 CI 与文档中的大量路径引用）。

技术栈：uni-app 3 + Vue 3 + Vite **5.2.8**（`@dcloudio/vite-plugin-uni` 的 peerDependency 精确锁定，**不可升级**；
`admin-web` 用的是 Vite 8，两者版本本就不同）。

初始化：`pnpm dlx degit dcloudio/uni-preset-vue#vite-ts .` 后补齐。

```bash
pnpm install

# 一期：学生端网页
pnpm dev:h5        # 开发
pnpm build:h5      # 构建，产物 dist/build/h5，可直接部署为静态站点

# 小程序（已裁剪，仅存档）
pnpm dev:mp-weixin          # 产物 dist/dev/mp-weixin，用微信开发者工具导入
pnpm build:mp-weixin
```

请求基地址不得硬编码（当前实现是硬编码 `http://localhost:8080/api`，属待修项）；
统一走网关 `/api/<服务>/**`，前缀不裁剪。约定见 [开发规范](../../docs/开发规范.md) §7。
