import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true, title: '注册' },
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

/**
 * 会话守卫：凭证是 HttpOnly Cookie，前端拿不到，只能问后端 /me。
 * - public 路由（/login、/register）直接放行 —— 跳转目标是 public 时不会再回到守卫，不会死循环；
 * - 其余路由：未确认过会话状态就先 fetchMe()，失败跳登录并带上 redirect。
 */
router.beforeEach(async (to) => {
  if (to.meta.public) return true

  const userStore = useUserStore()
  if (!userStore.checked) {
    const ok = await userStore.fetchMe()
    if (!ok) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  // checked 为 true 但没有用户 = 确认过且未登录（如刚退出登录）
  if (!userStore.username) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
