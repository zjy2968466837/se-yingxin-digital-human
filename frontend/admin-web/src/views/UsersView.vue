<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab" @tab-change="loadCurrent">
      <el-tab-pane label="待审批" name="pending">
        <el-table v-loading="loading" :data="pendingList" stripe>
          <el-table-column prop="account" label="账号" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="status" label="账号状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" label="最近登录" width="180" />
          <el-table-column label="所属用户组" min-width="180">
            <template #default="{ row }">
              <el-tag v-for="g in row.groups" :key="g.code" size="small" class="group-tag">
                {{ g.name }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="handleApprove(row, true)">通过</el-button>
              <el-button type="danger" size="small" @click="handleApprove(row, false)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="全部用户" name="all">
        <el-table v-loading="loading" :data="userList" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="account" label="账号" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="approveStatus" label="审批状态" width="100">
            <template #default="{ row }">
              <el-tag :type="approveTagType(row.approveStatus)" size="small">
                {{ approveText(row.approveStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="账号状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" label="最近登录" width="180" />
          <el-table-column label="所属用户组" min-width="160">
            <template #default="{ row }">
              <el-tag v-for="g in row.groups" :key="g.code" size="small" class="group-tag">
                {{ g.name }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="openAssign(row)">分配角色</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <!-- 分配角色对话框 -->
  <el-dialog v-model="assignVisible" title="分配角色" width="420px">
    <el-checkbox-group v-model="selectedGroups">
      <el-checkbox v-for="g in groups" :key="g.code" :value="g.code" class="assign-check">
        {{ g.name }}（{{ g.code }}）
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="assignVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveAssign">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
// 用户与审批：待审批 tab + 全部用户 tab，支持通过/驳回/分配角色
import { onMounted, ref } from 'vue'
import {
  approveUser,
  assignUserGroups,
  listGroups,
  listPendingUsers,
  listUsers,
} from '@/api/rbac'

const activeTab = ref('pending')
const loading = ref(false)
const saving = ref(false)
const pendingList = ref([])
const userList = ref([])
const groups = ref([])

const assignVisible = ref(false)
const selectedGroups = ref([])
let assignUserId = null

function approveText(s) {
  return { 0: '待审批', 1: '已通过', 2: '已驳回' }[s] ?? '未知'
}

function approveTagType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] ?? 'info'
}

async function loadCurrent() {
  loading.value = true
  try {
    const res = activeTab.value === 'pending' ? await listPendingUsers() : await listUsers()
    if (activeTab.value === 'pending') {
      pendingList.value = res.data
    } else {
      userList.value = res.data
    }
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadGroups() {
  try {
    const res = await listGroups()
    groups.value = res.data
  } catch {
    // 忽略，分配角色时再提示
  }
}

async function handleApprove(row, approve) {
  try {
    await ElMessageBox.confirm(
      `确认${approve ? '通过' : '驳回'}用户「${row.realName || row.account}」的注册申请？`,
      '审批确认',
      { type: 'warning' },
    )
  } catch {
    return // 用户取消
  }
  try {
    await approveUser(row.id, approve)
    ElMessage.success(approve ? '已通过' : '已驳回')
    loadCurrent()
  } catch {
    // 错误提示由拦截器统一处理
  }
}

function openAssign(row) {
  assignUserId = row.id
  selectedGroups.value = (row.groups || []).map((g) => g.code)
  assignVisible.value = true
}

async function saveAssign() {
  saving.value = true
  try {
    await assignUserGroups(assignUserId, selectedGroups.value)
    ElMessage.success('分配成功')
    assignVisible.value = false
    loadCurrent()
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCurrent()
  loadGroups()
})
</script>

<style scoped>
.group-tag {
  margin-right: 6px;
}
.assign-check {
  display: block;
  margin: 0 0 12px;
}
</style>
