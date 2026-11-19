<template>
  <el-card shadow="never">
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="ts" label="时间" width="180" />
      <el-table-column prop="operator" label="操作人" width="140" />
      <el-table-column prop="action" label="操作" width="160" />
      <el-table-column prop="target" label="对象" width="180" />
      <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="load"
    />
  </el-card>
</template>

<script setup>
// 审计日志分页（时间倒序由后端保证）
import { onMounted, ref } from 'vue'
import { listAudit } from '@/api/rbac'

const size = 10
const page = ref(1)
const total = ref(0)
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await listAudit({ page: page.value, size })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
