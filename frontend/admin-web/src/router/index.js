import { createRouter, createWebHistory } from 'vue-router'

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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局守卫：无 token 访问业务页跳登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('yingxin_token')
  if (!to.meta.public && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
