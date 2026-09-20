# -*- coding: utf-8 -*-
"""容器化演示程序：同一份文件在宿主机与容器里运行，结果不同。"""
import os
import platform
import socket
import sys

print("===== demo.py =====")
print("主机名   :", socket.gethostname())
print("进程 PID :", os.getpid())
print("Python   :", sys.version.split()[0])
print("系统     :", platform.system(), platform.release())
print("工作目录 :", os.getcwd())
print("环境变量 :", os.environ.get("APP_ENV", "未设置"))
