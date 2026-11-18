<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">迎新数字人 · 管理后台</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/users">用户与审批</el-menu-item>
        <el-menu-item index="/roles">角色与权限</el-menu-item>
        <el-menu-item index="/qa">问答库</el-menu-item>
        <el-menu-item index="/checkin">报到管理</el-menu-item>
        <el-menu-item index="/audit">审计日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ route.meta.title || '' }}</span>
        <div class="user-area">
          <span class="user-name">{{ userStore.realName || userStore.username }}</span>
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
// 后台整体布局：左侧菜单 + 顶栏（用户名/退出登录）
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout as logoutApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await logoutApi()
  } catch {
    // 接口失败也继续本地登出
  }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}
.menu {
  border-right: none;
  flex: 1;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  color: #606266;
  font-size: 14px;
}
.main {
  background: #f0f2f5;
}
</style>
