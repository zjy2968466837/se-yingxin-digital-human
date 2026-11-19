<template>
  <!-- 上半部分：用户组（角色）管理 -->
  <el-card shadow="never" header="用户组管理" class="section">
    <template #header>
      <div class="card-header">
        <span>用户组管理</span>
        <el-button type="primary" size="small" @click="openCreate">新建角色</el-button>
      </div>
    </template>
    <el-table :data="groups" v-loading="loading" stripe>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column label="内置" width="90">
        <template #default="{ row }">
          <el-tag :type="row.builtin ? 'info' : 'success'" size="small">
            {{ row.builtin ? '内置' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userCount" label="用户数" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            type="danger"
            size="small"
            :disabled="row.builtin"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 下半部分：权限矩阵 -->
  <el-card shadow="never" header="权限矩阵" class="section">
    <el-table :data="matrixRows" v-loading="matrixLoading" border stripe>
      <el-table-column label="模块" width="140" fixed="left">
        <template #default="{ row }">{{ row.moduleName }}</template>
      </el-table-column>
      <el-table-column label="权限点" width="180" fixed="left">
        <template #default="{ row }">{{ row.methodName }}</template>
      </el-table-column>
      <el-table-column
        v-for="g in matrixGroups"
        :key="g.code"
        :label="g.builtin ? `${g.name}（内置）` : g.name"
        align="center"
        min-width="100"
      >
        <template #default="{ row }">
          <el-checkbox
            :model-value="isGranted(g.code, row.methodCode)"
            :disabled="g.builtin"
            @change="(val) => togglePriv(g, row, val)"
          />
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新建角色对话框 -->
  <el-dialog v-model="createVisible" title="新建角色" width="420px">
    <el-form :model="createForm" label-width="70px">
      <el-form-item label="编码">
        <el-input v-model="createForm.code" placeholder="如 freshman" />
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="createForm.name" placeholder="如 2025 级新生" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveCreate">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
// 角色与权限：用户组增删 + 禅道式权限矩阵（勾选即调 priv 接口）
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createGroup,
  deleteGroup,
  getMatrix,
  listGroups,
  setPriv,
} from '@/api/rbac'

const loading = ref(false)
const matrixLoading = ref(false)
const saving = ref(false)
const groups = ref([])

// 矩阵原始数据
const modules = ref([])
const matrixGroups = ref([])
const grants = ref([])
const grantSet = computed(() => new Set(grants.value))

const createVisible = ref(false)
const createForm = reactive({ code: '', name: '' })

// 矩阵行：模块.权限点
const matrixRows = computed(() =>
  modules.value.flatMap((m) =>
    (m.methods || []).map((mt) => ({
      moduleCode: m.code,
      moduleName: m.name,
      methodCode: mt.code,
      methodName: mt.name,
    })),
  ),
)

function isGranted(groupCode, methodCode) {
  return grantSet.value.has(`${groupCode}|${methodCode}`)
}

async function loadGroups() {
  loading.value = true
  try {
    const res = await listGroups()
    groups.value = res.data
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadMatrix() {
  matrixLoading.value = true
  try {
    const res = await getMatrix()
    modules.value = res.data.modules || []
    matrixGroups.value = res.data.groups || []
    grants.value = res.data.grants || []
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    matrixLoading.value = false
  }
}

function openCreate() {
  createForm.code = ''
  createForm.name = ''
  createVisible.value = true
}

async function saveCreate() {
  if (!createForm.code || !createForm.name) {
    ElMessage.warning('请填写角色编码和名称')
    return
  }
  saving.value = true
  try {
    await createGroup({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadGroups()
    loadMatrix()
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除用户组「${row.name}」？`, '删除确认', { type: 'warning' })
  await deleteGroup(row.id)
  ElMessage.success('已删除')
  loadGroups()
  loadMatrix()
}

// 勾选/取消授权
async function togglePriv(group, row, grant) {
  try {
    await setPriv(group.code, row.methodCode, grant)
    const key = `${group.code}|${row.methodCode}`
    if (grant) {
      grants.value = [...grants.value, key]
    } else {
      grants.value = grants.value.filter((k) => k !== key)
    }
    ElMessage.success(grant ? '已授权' : '已取消授权')
  } catch {
    // 失败时不更新本地状态
  }
}

onMounted(() => {
  loadGroups()
  loadMatrix()
})
</script>

<style scoped>
.section {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
