# 软件工程课程团队项目 —— 游戏选题

软件工程课程的团队协作仓库，暂定选题为**游戏**。

## 团队协作流程

### 分支模型

| 分支 | 用途 | 保护策略 |
|------|------|----------|
| `main` | 稳定发布版本，仅通过 PR 合入 | 禁止直接 push，需 PR + 审查 |
| `dev` | 日常开发集成分支 | 通过 PR 合入 main |
| `feat/xxx` | 功能分支，从 `dev` 切出 | 合入 `dev` |
| `fix/xxx` | 缺陷修复分支 | 合入 `dev` |
| `release/x.y` | 发布准备分支 | 合入 `main` 与 `dev` |

### 工作流

1. 从 `dev` 切出功能分支：`git checkout -b feat/xxx dev`
2. 开发并提交，遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)（`feat:`、`fix:`、`docs:` 等）
3. 发起 Pull Request 至 `dev`，关联对应 Issue（`Closes #n`）
4. 至少 1 名队友审查通过后合入
5. 里程碑达成时将 `dev` 合入 `main` 打 tag 发布

### Issue 管理

- 所有任务/缺陷/需求以 Issue 形式跟踪，使用标签分类（见下方标签体系）
- 提交信息与 PR 标题引用 Issue 编号
- 迭代任务用 Milestone 划分

## 目录结构

```
├── docs/              # 项目文档（需求、设计、测试报告）
├── src/               # 源代码
├── .github/           # Issue/PR 模板与 CI 配置
├── CONTRIBUTING.md    # 协作贡献指南
└── README.md
```

## 文档索引

- [协作贡献指南](CONTRIBUTING.md)
- 需求规格说明书：`docs/需求规格说明书.md`
- 项目计划（WBS / 甘特图）：`docs/项目计划.md`
