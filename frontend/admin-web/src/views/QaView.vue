<template>
  <el-container style="height: 100%">
    <el-aside width="200px">
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">概览</el-menu-item>
        <el-menu-item index="/qa">问答库管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="title">问答会话 · 增删查</span>
        <span class="chain">Vue → Vite /api 代理 → qa-service:8101 → MyBatis-Plus → CloudBase MySQL</span>
      </el-header>

      <el-main>
        <div class="toolbar">
          <el-button type="primary" @click="openCreate">新建会话</el-button>
          <el-input
            v-model="userIdInput"
            placeholder="按 userId 过滤（可空）"
            clearable
            style="width: 220px"
            @keyup.enter="search"
          />
          <el-button @click="search">查询</el-button>
          <el-button text @click="fetchList">刷新</el-button>
          <span class="grow" />
          <span class="hint">共 {{ total }} 条</span>
        </div>

        <el-table v-loading="loading" :data="rows" border empty-text="还没有会话，点「新建会话」写一条">
          <el-table-column prop="id" label="id（雪花）" width="210">
            <template #default="{ row }">
              <span class="mono">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="userId" label="userId" width="130">
            <template #default="{ row }">
              <span class="mono">{{ row.userId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="channel" width="140">
            <template #default="{ row }">
              <el-tag size="small" :type="row.channel === 1 ? 'success' : 'warning'">
                {{ row.channel === 1 ? '学生端网页' : '管理后台' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="start_time" width="180" />
          <el-table-column prop="messageCount" label="message_count" width="160" />
          <el-table-column prop="satisfied" label="satisfied" width="110">
            <template #default="{ row }">
              <span v-if="row.satisfied === null" class="hint">NULL</span>
              <span v-else>{{ row.satisfied }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="create_time" width="180" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 14px"
          @current-change="fetchList"
          @size-change="search"
        />

        <el-divider content-position="left">最近一次响应（后端 Result 包体）</el-divider>
        <pre class="resp">{{ lastResp || '（还没有请求）' }}</pre>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="dialog.visible" title="新建会话" width="420px">
    <el-form label-width="90px">
      <el-form-item label="userId">
        <el-input v-model="form.userId" placeholder="纯数字，如 1001" style="width: 100%" />
      </el-form-item>
      <el-form-item label="channel">
        <el-select v-model="form.channel" style="width: 100%">
          <el-option :value="1" label="1 · 学生端网页" />
          <el-option :value="2" label="2 · 管理后台" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="dialog.submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createSession, deleteSession, listSessions } from '@/api/qa'

const route = useRoute()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const userIdInput = ref('')
const lastResp = ref('')

const query = reactive({ page: 1, size: 10 })
const dialog = reactive({ visible: false, submitting: false })
const form = reactive({ userId: '1001', channel: 1 })

/** 统一处理后端 Result：code !== 0 视为业务失败 */
function accept(res) {
  lastResp.value = JSON.stringify(res, null, 2)
  if (res.code !== 0) {
    ElMessage.error(res.message || '请求失败')
    return null
  }
  return res.data
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listSessions({
      page: query.page,
      size: query.size,
      userId: userIdInput.value ? Number(userIdInput.value) : undefined,
    })
    const data = accept(res)
    if (!data) return
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchList()
}

function openCreate() {
  form.userId = '1001'
  form.channel = 1
  dialog.visible = true
}

async function submit() {
  const userId = String(form.userId).trim()
  if (!/^\d+$/.test(userId) || Number(userId) < 1) {
    ElMessage.warning('userId 必须是不小于 1 的整数')
    return
  }
  dialog.submitting = true
  try {
    const data = accept(await createSession({ userId: Number(userId), channel: form.channel }))
    if (!data) return
    ElMessage.success(`已写入 qa_session，id=${data.id}`)
    dialog.visible = false
    search()
  } finally {
    dialog.submitting = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `逻辑删除：把 deleted 写成本行 id（不是 1）。id = ${row.id}`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  const res = await deleteSession(row.id)
  if (accept(res)) {
    ElMessage.success('已删除（deleted = 本行 id）')
    fetchList()
  }
}

onMounted(fetchList)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #e4e7ed;
}
.title { font-weight: 600; }
.chain {
  font-family: Consolas, "Cascadia Code", monospace;
  font-size: 12px;
  color: #909399;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.grow { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.mono {
  font-family: Consolas, "Cascadia Code", monospace;
  font-size: 12px;
}
.resp {
  margin: 0;
  padding: 12px 14px;
  background: #0f172a;
  color: #cbd5e1;
  border-radius: 8px;
  font-family: Consolas, "Cascadia Code", monospace;
  font-size: 12px;
  max-height: 260px;
  overflow: auto;
}
</style>
