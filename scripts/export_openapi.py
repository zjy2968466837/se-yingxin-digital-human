#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出各服务的 OpenAPI 文档，并生成接口索引（IR-3.7）。

为什么要落盘：
    Swagger UI 只能运行期看，评审/归档/对比接口变更时需要一份可提交的 JSON。
    脚本从各服务的 /v3/api-docs 拉取（springdoc 自动生成，不手写），
    产物在 docs/api/openapi/<服务>.json，索引在 docs/api/接口文档索引.md。

用法：
    # 全部服务已启动时
    python3 scripts/export_openapi.py

    # 分批导出（只拉已启动的服务，最后再跑一次生成索引）
    python3 scripts/export_openapi.py --allow-partial --only auth,qa
    python3 scripts/export_openapi.py --index-only

    # 自定义主机/端口
    python3 scripts/export_openapi.py --host 127.0.0.1 --port-offset 0

退出码：0 成功；1 有服务拉取失败（--allow-partial 时为 0）。
依赖：仅标准库。
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

# 与 AGENTS.md / 开发规范 §3.1 的端口口径一致
SERVICES = [
    ("auth-service", 8100),
    ("qa-service", 8101),
    ("checkin-service", 8102),
    ("navigation-service", 8103),
    ("aid-service", 8104),
    ("life-service", 8105),
    ("content-service", 8106),
    ("admin-service", 8107),
]

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "docs", "api", "openapi")
INDEX_FILE = os.path.join(ROOT, "docs", "api", "接口文档索引.md")

METHODS = ["get", "post", "put", "delete", "patch", "head", "options"]


def fetch(host, port, timeout=10):
    url = "http://%s:%d/v3/api-docs" % (host, port)
    with urllib.request.urlopen(url, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def write_json(service, document):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, service + ".json")
    with open(path, "w", encoding="utf-8", newline="\n") as file:
        json.dump(document, file, ensure_ascii=False, indent=2)
        file.write("\n")
    return path


def rows_of(document):
    rows = []
    for path, operations in sorted((document.get("paths") or {}).items()):
        for method in METHODS:
            operation = operations.get(method)
            if not operation:
                continue
            tags = "、".join(operation.get("tags") or [])
            summary = operation.get("summary") or operation.get("description") or ""
            rows.append((method.upper(), path, summary, tags))
    return rows


def build_index():
    lines = [
        "# 接口文档索引",
        "",
        "> 由 `scripts/export_openapi.py` 从各服务的 `/v3/api-docs` 自动导出，**不要手改**。",
        "> 新增/修改接口后：启动该服务 → 重新执行导出脚本 → 提交 JSON 与本索引。",
        "",
        "## 查看方式",
        "",
        "| 服务 | Swagger UI（直连） | OpenAPI JSON |",
        "|---|---|---|",
    ]
    for service, port in SERVICES:
        json_name = "openapi/%s.json" % service
        exists = os.path.exists(os.path.join(OUT_DIR, service + ".json"))
        json_link = "[%s](%s)" % (json_name, json_name) if exists else "（未导出）"
        lines.append("| `%s` | http://localhost:%d/swagger-ui.html | %s |" % (service, port, json_link))

    lines += [
        "",
        "> Swagger UI 不在网关白名单内（开发规范 §4.2），**只能直连各服务端口**；",
        "> 经网关 `8080` 访问会返回 401。生产环境应收敛 `springdoc.*.enabled=false`。",
        "",
    ]

    for service, port in SERVICES:
        path = os.path.join(OUT_DIR, service + ".json")
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as file:
            document = json.load(file)
        info = document.get("info") or {}
        lines += [
            "## %s" % service,
            "",
            "%s（version %s）" % (info.get("title", service), info.get("version", "?")),
            "",
            "| 方法 | 路径 | 说明 | 分组 |",
            "|---|---|---|---|",
        ]
        for method, api_path, summary, tags in rows_of(document):
            lines.append("| %s | `%s` | %s | %s |" % (method, api_path, summary, tags or "—"))
        lines.append("")

    os.makedirs(os.path.dirname(INDEX_FILE), exist_ok=True)
    with open(INDEX_FILE, "w", encoding="utf-8", newline="\n") as file:
        file.write("\n".join(lines).rstrip() + "\n")
    return INDEX_FILE


def main():
    parser = argparse.ArgumentParser(description="导出 OpenAPI 并生成接口索引")
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--only", default="", help="逗号分隔的服务名，只导出这些")
    parser.add_argument("--index-only", action="store_true", help="只用已有 JSON 重建索引")
    parser.add_argument("--allow-partial", action="store_true", help="有服务不可达时只告警")
    args = parser.parse_args()

    only = {item.strip() for item in args.only.split(",") if item.strip()}
    failed = []
    if not args.index_only:
        for service, port in SERVICES:
            if only and service not in only:
                continue
            try:
                document = fetch(args.host, port)
                path = write_json(service, document)
                print("OK   %-20s %d paths -> %s" % (service, len(document.get("paths") or {}), path))
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
                failed.append((service, port, str(error)))
                print("FAIL %-20s %s" % (service, error))

    index = build_index()
    print("索引 -> %s" % index)
    if failed and not args.allow_partial:
        print("有 %d 个服务导出失败；确认服务已启动（或加 --allow-partial）" % len(failed))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
