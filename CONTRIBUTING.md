# 贡献指南（CONTRIBUTING）

## 环境准备

从零搭建开发环境（JDK / Node / pnpm / Docker 的版本要求与安装步骤）见
[环境搭建指南](docs/环境搭建指南.md)，环境一致性约定见 [开发环境版本](docs/开发环境版本.md)。

```bash
git clone https://github.com/zjy2968466837/se-yingxin-digital-human.git
cd se-yingxin-digital-human
git checkout dev
git checkout -b feat/你的功能名
```

> 仓库已用 `.tool-versions` / `.nvmrc` 声明工具链版本：装了 mise 或 nvm 的成员执行
> `mise install` 或 `nvm use` 即可对齐。**Maven 不用自己装**，统一用仓库自带的 `./mvnw`。

## 提交规范

采用 Conventional Commits：

- `feat: 新增报到流程进度查询接口` —— 新功能
- `fix: 修复网关转发裁剪前缀导致 404` —— 缺陷修复
- `docs: 补充 WSL 环境搭建说明` —— 文档
- `refactor:` `test:` `chore:` 同理

一次提交只做一件事，信息用祈使句、不超过 72 字符。

## Pull Request 要求

1. 标题格式：`类型: 简述 (#Issue号)`
2. 描述中写明：改了什么、为什么改、如何验证
3. 自查清单：本地测试通过、无调试残留代码、文档同步更新
4. 指派至少 1 名队友 Review，通过后由 Reviewer 合并（squash merge）

## 分支规则

- 禁止向 `main` / `dev` 直接 push
- 分支命名：`feat/`、`fix/`、`docs/` 前缀 + 短横线小写
- 合并后删除功能分支

## 沟通与任务

- 任务认领：在 Issue 下回复认领，避免重复劳动
- 有阻塞及时在 Issue 或群内同步，不要憋到截止日
