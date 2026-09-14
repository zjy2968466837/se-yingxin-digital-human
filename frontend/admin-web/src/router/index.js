import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: '概览' },
  },
  {
    path: '/qa',
    name: 'qa',
    component: () => import('@/views/QaView.vue'),
    meta: { title: '问答库管理' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 简单登录守卫：未登录跳登录页（真实鉴权待接入 auth-service）
router.beforeEach((to) => {
  const token = localStorage.getItem('yingxin_token')
  if (!to.meta.public && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
