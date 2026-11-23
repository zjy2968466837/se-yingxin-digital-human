import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // .env 里的变量不会自动进入 process.env，必须用 loadEnv 读，否则 VITE_API_TARGET 永远走兜底值
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      // Element Plus 按需自动导入（官方推荐）：组件与 ElMessage 等 API 连同样式按需引入
      AutoImport({ imports: ['vue', 'vue-router'], resolvers: [ElementPlusResolver()], dts: false }),
      Components({ resolvers: [ElementPlusResolver()], dts: false }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      // 开发期把 /api 代理到网关，避免跨域；网关再按 /api/<服务> 转发到各微服务
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      // 依赖单独成块：业务代码改动不会让 vendor 缓存失效
      // 注意 Vite 8 用 Rolldown，manualChunks 只支持函数形式（对象形式会报 "Expected Function"）
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
            if (id.includes('element-plus')) return 'element'
            if (id.includes('@vue') || id.includes('vue-router') || id.includes('pinia')) return 'vue'
            return
          },
        },
      },
    },
  }
})
