import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/request'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 运行期会话过期（401）：统一跳登录页并带回来路。
// 登录/注册页自己处理 401（口令错误等），此时不跳转，否则会打断用户输入。
setUnauthorizedHandler(() => {
  const current = router.currentRoute.value
  if (current.name === 'login' || current.name === 'register') return
  router.replace({ name: 'login', query: { redirect: current.fullPath } })
})

app.mount('#app')
