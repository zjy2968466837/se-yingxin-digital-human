import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/components/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '数据看板' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/UsersView.vue'),
        meta: { title: '用户与审批' },
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/RolesView.vue'),
        meta: { title: '角色与权限' },
      },
      {
        path: 'qa',
        name: 'qa',
        component: () => import('@/views/QaView.vue'),
        meta: { title: '问答库' },
      },
      {
        path: 'checkin',
        name: 'checkin',
        component: () => import('@/views/CheckinView.vue'),
        meta: { title: '报到管理' },
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/AuditView.vue'),
        meta: { title: '审计日志' },
      },
    ],
  },
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
  // 兜底：未匹配路径回看板（未登录会被守卫带到登录页），避免布局里 router-view 空白
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 会话守卫：凭证是 HttpOnly Cookie（YX_SESSION），前端拿不到，只能问后端 /me。
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
