<template>
  <el-container style="height: 100%">
    <el-aside width="200px">
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">概览</el-menu-item>
        <el-menu-item index="/qa">问答库管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">问答库管理</el-header>
      <el-main>
        <el-empty description="骨架占位：问答库增删改查接入 qa-service 后实现" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}
</style>
