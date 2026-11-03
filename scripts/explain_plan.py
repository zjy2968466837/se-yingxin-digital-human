#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 MySQL 的 EXPLAIN FORMAT=JSON 输出渲染成 Mermaid 流程图（查询计划可视化）。

为什么单独写脚本：
    MySQL Workbench 的 Visual Explain 只能在桌面端点，截图无法进仓库、也无法进 CI；
    EXPLAIN FORMAT=JSON 是稳定文本，转成 Mermaid 后可以直接嵌进《MySQL存储引擎与执行计划》
    并在 GitHub / IDE 里渲染，还能随查询一起 code review。

用法：
    # 1) 先从库里导出计划（注意 --raw，否则换行会被转义成 \\n）
    mysql -h127.0.0.1 -P3306 -uyingxin -p yingxin -r -N --raw \
        -e "EXPLAIN FORMAT=JSON SELECT ..." > plan.json

    # 2) 渲染（Markdown 表格 + Mermaid；也可用 --mermaid-only 只要图）
    python3 scripts/explain_plan.py plan.json
    python3 scripts/explain_plan.py plan.json --mermaid-only > plan.mmd
    cat plan.json | python3 scripts/explain_plan.py -

依赖：仅标准库。JSON 结构以 MySQL 8.0 为准（query_block / nested_loop / table），
遇到未识别的节点类型不会报错，只会退化成该节点的表名。
"""
import json
import sys

# 访问类型从好到差，用于在表格里给出"是否可接受"的判断
ACCESS_ORDER = ["system", "const", "eq_ref", "ref", "fulltext", "ref_or_null",
                "index_merge", "range", "index", "ALL"]

# 包装节点：本身不产生表，只记录优化器在这一层做的事
WRAPPERS = {
    "grouping_operation": "分组",
    "duplicates_removal": "去重",
    "buffer_result": "缓冲结果",
    "union_result": "UNION",
}


class PlanRenderer:
    def __init__(self):
        self.nodes = []   # (id, label, table_info|None)
        self.edges = []   # (from, to, label)

    def _add(self, label, info=None):
        nid = "N%d" % (len(self.nodes) + 1)
        self.nodes.append((nid, label, info))
        return nid

    def _table_label(self, t, anns):
        rows = t.get("rows_examined_per_scan", t.get("rows_produced_per_join", "?"))
        filtered = t.get("filtered", "?")
        parts = [
            t.get("table_name", "?"),
            "%s · %s" % (t.get("access_type", "?"), t.get("key") or "无索引"),
            "扫描≈%s 过滤=%s%%" % (rows, filtered),
        ]
        if anns:
            parts.append("⚠ " + " / ".join(anns))
        return "<br/>".join(parts)

    def walk(self, node, parents, anns=None, edge_label="", dashed=False):
        """返回本层产生的末端节点 id 列表，供调用方继续串链。"""
        anns = list(anns or [])

        if "query_block" in node:
            return self.walk(node["query_block"], parents, anns, edge_label, dashed)

        if "ordering_operation" in node:
            sub = node["ordering_operation"]
            if sub.get("using_filesort"):
                extra = ["filesort"]
            else:
                table = sub.get("table") or {}
                extra = ["倒序索引扫描（无需额外排序）" if table.get("backward_index_scan")
                         else "索引有序（无需额外排序）"]
            return self.walk(sub, parents, anns + extra, edge_label, dashed)

        for wrapper, zh in WRAPPERS.items():
            if wrapper in node:
                sub = node[wrapper]
                extra = [zh]
                if sub.get("using_filesort"):
                    extra.append("filesort")
                if sub.get("using_temporary_table"):
                    extra.append("temporary")
                return self.walk(sub, parents, anns + extra, edge_label, dashed)

        if "nested_loop" in node:
            produced, prev = [], None
            for i, element in enumerate(node["nested_loop"]):
                ids = self.walk(
                    element,
                    parents if i == 0 else [prev],
                    anns if i == 0 else [],
                    "驱动表" if i == 0 else "被驱动表",
                    dashed,
                )
                if ids:
                    produced.extend(ids)
                    prev = ids[-1]
            return produced

        if "table" in node:
            t = node["table"]
            nid = self._add(self._table_label(t, anns), t)
            for p in parents:
                self.edges.append((p, nid, edge_label, dashed))
            # 物化子查询：作为该表的子节点挂在下面
            if "materialized_from_subquery" in t:
                self.walk(t["materialized_from_subquery"], [nid], ["物化子查询"], "物化", True)
            for sq in t.get("attached_subqueries") or []:
                self.walk(sq, [nid], ["关联子查询"], "子查询", True)
            return [nid]

        if "union_result" in node:
            nid = self._add(node["union_result"].get("table_name", "UNION 结果"), node["union_result"])
            for p in parents:
                self.edges.append((p, nid, edge_label, dashed))
            for spec in node["union_result"].get("query_specifications") or []:
                self.walk(spec, [nid], [], "分支", True)
            return [nid]

        if "attached_subqueries" in node:
            for sq in node["attached_subqueries"]:
                self.walk(sq, parents, ["关联子查询"], "子查询", True)
        return []

    def render(self, doc):
        qb = doc.get("query_block", {})
        cost = qb.get("cost_info", {}).get("query_cost", "?")
        root = self._add("SELECT #%s<br/>query_cost=%s" % (qb.get("select_id", 1), cost))
        self.walk(qb, [root])
        return root

    def tables(self):
        return [n for n in self.nodes if n[2] is not None]

    def markdown_table(self):
        lines = ["| 顺序 | 表 | 访问类型 | 使用的索引 | 扫描行数 | 过滤率 | 判读 |",
                 "|---:|---|---|---|---:|---:|---|"]
        for i, (_, label, info) in enumerate(self.tables(), 1):
            cells = label.split("<br/>")
            name = cells[0]
            access, key = ("%s · %s" % (info.get("access_type"), info.get("key") or "无索引")).split(" · ", 1)
            rows = info.get("rows_examined_per_scan", info.get("rows_produced_per_join", "?"))
            filt = info.get("filtered", "?")
            warn = cells[3][2:] if len(cells) > 3 else ""
            lines.append("| %d | `%s` | %s | %s | %s | %s%% | %s |"
                         % (i, name, access, key, rows, filt, warn or "—"))
        return "\n".join(lines)

    def mermaid(self):
        lines = ["flowchart TD"]
        for nid, label, info in self.nodes:
            lines.append('    %s["%s"]' % (nid, label.replace('"', "'")))
        for src, dst, label, dashed in self.edges:
            arrow = "-.->" if dashed else "-->"
            if label:
                lines.append('    %s %s|"%s"| %s' % (src, arrow, label, dst))
            else:
                lines.append("    %s %s %s" % (src, arrow, dst))
        return "\n".join(lines)

    def verdict_notes(self):
        """给出机制性提示：这些信号与具体数据量无关，可稳定用于评审。"""
        notes = []
        for _, label, info in self.tables():
            acc = info.get("access_type")
            if acc in ("ALL", "index"):
                notes.append("`%s` 使用 %s：全表/全索引扫描，行数增长后必然变慢，需核对过滤条件能否命中索引。"
                             % (info.get("table_name"), acc))
            extra = label.split("<br/>")[3][2:] if len(label.split("<br/>")) > 3 else ""
            extra = "" if "无需额外排序" in extra else extra
            if "filesort" in extra or "temporary" in extra:
                notes.append("`%s` 出现 %s：排序/临时表在内存不足时会落盘，需核对排序字段是否能借索引有序性。"
                             % (info.get("table_name"), extra))
        return notes


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    mermaid_only = "--mermaid-only" in sys.argv
    if not args:
        print(__doc__)
        return 1
    raw = sys.stdin.read() if args[0] == "-" else open(args[0], encoding="utf-8").read()
    doc = json.loads(raw)
    r = PlanRenderer()
    r.render(doc)
    if mermaid_only:
        print(r.mermaid())
        return 0
    print("## 执行计划\n")
    print(r.markdown_table())
    print("\n```mermaid")
    print(r.mermaid())
    print("```")
    notes = r.verdict_notes()
    if notes:
        print("\n**判读提示**：")
        for n in notes:
            print("- " + n)
    return 0


if __name__ == "__main__":
    sys.exit(main())
