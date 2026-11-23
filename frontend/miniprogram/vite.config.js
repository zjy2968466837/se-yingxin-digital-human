import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// 开发端口固定 5174（5173 留给管理后台），/api 代理到网关，
// 这样学生端与管理后台一样是"同源请求"，不依赖网关 CORS 白名单。
export default defineConfig(({ mode }) => {
  // .env 变量不会自动进入 process.env，用 loadEnv 才能让 VITE_API_TARGET 生效
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [uni()],
    server: {
      port: 5174,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
