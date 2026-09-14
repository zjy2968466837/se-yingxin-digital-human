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
        <span>迎新数字人管理后台</span>
        <el-button link type="primary" @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <el-card>
          <h3>服务连通性自检</h3>
          <el-button type="primary" :loading="loading" @click="check">调 admin-service /ping</el-button>
          <pre v-if="result">{{ result }}</pre>
        </el-card>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pingAdmin } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const result = ref('')

async function check() {
  loading.value = true
  try {
    result.value = JSON.stringify(await pingAdmin(), null, 2)
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
}
pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}
</style>
