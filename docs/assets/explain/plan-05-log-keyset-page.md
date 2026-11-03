## 执行计划

| 顺序 | 表 | 访问类型 | 使用的索引 | 扫描行数 | 过滤率 | 判读 |
|---:|---|---|---|---:|---:|---|
| 1 | `qa_ask_log` | range | idx_create_time | 49550 | 100.00% | 索引有序（无需额外排序） |

```mermaid
flowchart TD
    N1["SELECT #1<br/>query_cost=10158.75"]
    N2["qa_ask_log<br/>range · idx_create_time<br/>扫描≈49550 过滤=100.00%<br/>⚠ 索引有序（无需额外排序）"]
    N1 --> N2
```
