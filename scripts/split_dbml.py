#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 docs/数据建模.dbml（唯一模型源）拆成按业务域出图的只读视图。

用法：
    python3 scripts/split_dbml.py

产物（docs/diagrams/，与《数据建模》§4.1 分组一致）：
    er-1-auth-checkin.dbml    图4-1 ER-1 身份与报到
    er-2-qa.dbml              图4-2 ER-2 智能问答
    er-4-aid-life.dbml        图4-3 ER-4 资助与生活
    er-5-content-admin.dbml   图4-4 ER-5 内容与后台
    （ER-3 导航无库表，不出图）

关键点：
    * 表结构取自 docs/数据建模.dbml；**关系取自 docs/数据建模.md §4.1 的 mermaid**
      （dbml 只声明了 7 条 Ref，mermaid 有 20 条，后者才是 ER 图的权威表达）。
    * 每表只保留出图用关键字段（KEY_COLS）；仅作为关系端点出现的表退化为"薄表"（只留引用列）。
    * 视图自动生成、勿手改；**不要从视图导出 DDL**（DDL 只从源文件全量导出）。
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "docs" / "数据建模.dbml"
MD = ROOT / "docs" / "数据建模.md"
OUT_DIR = ROOT / "docs" / "diagrams"

# (文件名, §4.1 中的 mermaid 块序号, 图号, 标题, 本域实体, 图注)
VIEWS = [
    ("er-1-auth-checkin.dbml", 1, "图4-1", "ER-1 身份与报到（auth + checkin）",
     ["auth_user", "auth_identity", "checkin_step_template", "checkin_progress", "checkin_certificate"],
     "说明：checkin_* 通过 user_id / step_code 逻辑引用认证域与步骤模板，无物理外键。"),
    ("er-2-qa.dbml", 2, "图4-2", "ER-2 智能问答（qa）",
     ["qa_question", "qa_session", "qa_message", "qa_unanswered", "qa_ask_log"],
     "说明：消息归属会话；未解答与提问日志按用户维度逻辑关联 auth_user。"),
    ("er-4-aid-life.dbml", 4, "图4-3", "ER-4 资助与生活服务（aid + life）",
     ["aid_policy", "aid_application", "life_repair_order"],
     "说明：资助申请引用政策；报修单通过 user_id 逻辑关联认证域，无物理外键。"),
    ("er-5-content-admin.dbml", 5, "图4-4", "ER-5 内容服务与管理后台（content + admin）",
     ["content_article", "admin_user", "admin_role", "admin_user_role", "admin_behavior_event"],
     "说明：RBAC 由 admin_user_role 关联；埋点事件按 user_id 逻辑关联，无物理外键。"),
]

# 出图用关键字段（每表 3–5 列）：id + 唯一键 + 引用列 + 状态列。全字段明细见《数据库设计说明书》4.2。
KEY_COLS = {
 "auth_user": ["id", "student_no", "openid", "status"],
 "auth_identity": ["id", "user_id", "edu_level", "college", "batch_id"],
 "auth_push_record": ["id", "user_id", "channel", "status"],
 "checkin_step_template": ["id", "step_code", "seq", "enabled"],
 "checkin_material_item": ["id", "step_code", "name", "required"],
 "checkin_progress": ["id", "user_id", "step_code", "status", "checkin_time"],
 "checkin_certificate": ["id", "user_id", "batch_id", "cert_no", "verify_status"],
 "qa_question": ["id", "question", "category", "status"],
 "qa_session": ["id", "user_id", "channel", "start_time"],
 "qa_message": ["id", "session_id", "role", "question_id", "confidence"],
 "qa_unanswered": ["id", "question", "status", "handler_id"],
 "qa_ask_log": ["id", "user_id", "hit_type", "cost_ms"],
 "aid_policy": ["id", "name", "category", "need_apply", "status"],
 "aid_application": ["id", "user_id", "policy_id", "batch_id", "status"],
 "life_repair_order": ["id", "user_id", "category", "status", "finish_time"],
 "life_service_info": ["id", "category", "title", "poi_code", "status"],
 "content_article": ["id", "module", "title", "status", "poi_code"],
 "admin_user": ["id", "username", "dept", "status"],
 "admin_role": ["id", "role_code", "role_name", "status"],
 "admin_user_role": ["id", "admin_user_id", "role_id"],
 "admin_announcement": ["id", "title", "status", "publish_time"],
 "admin_config": ["id", "config_key", "config_group", "value_type"],
 "admin_operation_log": ["id", "admin_user_id", "module", "action", "result"],
 "admin_behavior_event": ["id", "event_type", "user_id", "service", "event_time"],
}

# 关系列映射：(左表, 右表) -> (左列, 右列)，与 §4.1 mermaid 的每条关系一一对应。
# mermaid 只给实体对，不给列名，故在此显式登记。
REL_COLS = {
 ("auth_identity", "auth_user"): ("user_id", "id"),
 ("auth_user", "auth_push_record"): ("id", "user_id"),
 ("checkin_step_template", "checkin_progress"): ("step_code", "step_code"),
 ("checkin_step_template", "checkin_material_item"): ("step_code", "step_code"),
 ("auth_user", "checkin_progress"): ("id", "user_id"),
 ("auth_user", "checkin_certificate"): ("id", "user_id"),
 ("auth_user", "qa_session"): ("id", "user_id"),
 ("qa_session", "qa_message"): ("id", "session_id"),
 ("qa_question", "qa_message"): ("id", "question_id"),
 ("auth_user", "qa_unanswered"): ("id", "user_id"),
 ("auth_user", "qa_ask_log"): ("id", "user_id"),
 ("aid_policy", "aid_application"): ("id", "policy_id"),
 ("auth_user", "aid_application"): ("id", "user_id"),
 ("auth_user", "life_repair_order"): ("id", "user_id"),
 ("admin_user", "admin_user_role"): ("id", "admin_user_id"),
 ("admin_role", "admin_user_role"): ("id", "role_id"),
 ("admin_user", "admin_announcement"): ("id", "publisher_id"),
 ("admin_user", "admin_operation_log"): ("id", "admin_user_id"),
 ("auth_user", "admin_behavior_event"): ("id", "user_id"),
}

HEADER = """// ============================================================
// 自动生成，请勿手改 —— 源：docs/数据建模.dbml（表）+ docs/数据建模.md §4.1（关系）
// 生成脚本：scripts/split_dbml.py
// 用途：仅用于出图（{fig} {title}）
// 字段：出图用关键字段（每表 3–5 列），全字段明细见《数据库设计说明书》4.2
// 注意：不要从本文件导出 DDL；DDL 只从 docs/数据建模.dbml 全量导出
// {note}
// ============================================================
"""


def parse_tables(src: str):
    return {m.group(1): m.group(0)
            for m in re.finditer(r"^Table\s+(\w+)\s*\{(.*?)^\}", src, re.S | re.M)}


def parse_mermaid(md: str):
    """返回 {块序号: [(左表, 右表, 标签)]}"""
    blocks = re.findall(r"```mermaid\n(.*?)```", md, re.S)
    out = {}
    for i, b in enumerate(blocks, 1):
        rels = []
        for line in b.splitlines():
            if "--" not in line or ":" not in line:
                continue
            left, label = line.split(":", 1)
            toks = re.findall(r"\b[A-Z][A-Z_0-9]+\b", left)
            if len(toks) >= 2:
                rels.append((toks[0].lower(), toks[-1].lower(), label.strip().strip('"')))
        out[i] = rels
    return out


def trim_block(block: str, keep_cols):
    lines = block.splitlines()
    out = [lines[0]]
    in_indexes = False
    for ln in lines[1:-1]:
        s = ln.strip()
        if s.startswith("indexes"):
            in_indexes = True
            continue
        if in_indexes:
            if s == "}":
                in_indexes = False
            continue
        if not s:
            continue
        if s.startswith("Note:"):
            out.append(ln)
            continue
        if s.split()[0] in keep_cols:
            out.append(ln)
    out.append("}")
    return "\n".join(out)


def rel_cols(a, b):
    """返回 (a 侧列, b 侧列)；REL_COLS 只登记了一个方向，反向时交换列。"""
    if (a, b) in REL_COLS:
        return REL_COLS[(a, b)]
    cb, ca = REL_COLS[(b, a)]
    return ca, cb


def main():
    if not SRC.exists() or not MD.exists():
        sys.exit("缺少 docs/数据建模.dbml 或 docs/数据建模.md")
    tables = parse_tables(SRC.read_text(encoding="utf-8"))
    mrels = parse_mermaid(MD.read_text(encoding="utf-8"))
    if not tables:
        sys.exit("源文件里没有解析到 Table 块")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    print(f"源：{SRC.name}（{len(tables)} 表） + {MD.name}（{sum(len(v) for v in mrels.values())} 条关系）\n")

    for fname, idx, fig, title, base_tables, note in VIEWS:
        rels = [(a, b, l) for a, b, l in mrels.get(idx, [])
                if a in tables and b in tables and ((a, b) in REL_COLS or (b, a) in REL_COLS)]
        skipped = [f"{a}->{b}" for a, b, _ in mrels.get(idx, []) if a not in tables or b not in tables]

        tabs = list(dict.fromkeys(base_tables + [x for r in rels for x in r[:2]]))
        thin_cols = {}
        for a, b, _l in rels:
            ca, cb = rel_cols(a, b)
            thin_cols.setdefault(a, set()).add(ca)
            thin_cols.setdefault(b, set()).add(cb)

        body = []
        for t in tabs:
            keep = set(KEY_COLS.get(t, [])) | thin_cols.get(t, set())
            body.append(trim_block(tables[t], keep))

        content = HEADER.format(fig=fig, title=title, note=note)
        content += "\n" + "\n\n".join(body) + "\n"
        if rels:
            content += "\n// ---- 关系（取自《数据建模》§4.1）----\n"
            for a, b, label in rels:
                ca, cb = rel_cols(a, b)
                content += f"// {label}\nRef: {a}.{ca} > {b}.{cb}\n"
        if skipped:
            content += "\n// 注意：§4.1 中以下关系引用的表已被裁剪，本视图不画：" + "、".join(skipped) + "\n"

        (OUT_DIR / fname).write_text(content, encoding="utf-8")
        print(f"  ✓ {fname:26s} 表 {len(tabs):2d} 张，关系 {len(rels):2d} 条"
              + (f"（跳过 {len(skipped)} 条：{', '.join(skipped)}）" if skipped else ""))

    print(f"\n产物目录：{OUT_DIR}")


if __name__ == "__main__":
    main()
