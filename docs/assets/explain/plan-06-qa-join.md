## 执行计划

| 顺序 | 表 | 访问类型 | 使用的索引 | 扫描行数 | 过滤率 | 判读 |
|---:|---|---|---|---:|---:|---|
| 1 | `s` | ref | idx_user_time | 2 | 100.00% | filesort |
| 2 | `m` | ref | idx_session | 9 | 100.00% | — |

```mermaid
flowchart TD
    N1["SELECT #1<br/>query_cost=27.41"]
    N2["s<br/>ref · idx_user_time<br/>扫描≈2 过滤=100.00%<br/>⚠ filesort"]
    N3["m<br/>ref · idx_session<br/>扫描≈9 过滤=100.00%"]
    N1 -->|"驱动表"| N2
    N2 -->|"被驱动表"| N3
```

**判读提示**：
- `s` 出现 filesort：排序/临时表在内存不足时会落盘，需核对排序字段是否能借索引有序性。
