import js from '@eslint/js'
import globals from 'globals'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'

export default [
  { ignores: ['dist/**', 'node_modules/**'] },
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    languageOptions: {
      ecmaVersion: 2023,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        // Element Plus 由 unplugin-auto-import 按需注入，声明为全局供 lint 识别
        ElMessage: 'readonly',
        ElMessageBox: 'readonly',
        ElLoading: 'readonly',
      },
    },
    rules: {
      // 页面就是单词名（DashboardView 等已足够表达，不强求多词）
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', caughtErrors: 'none' }],
    },
  },
  // 必须放最后：关闭与 Prettier 冲突的格式规则
  prettier,
  // 构建配置文件跑在 Node 里（process.cwd 等）
  {
    files: ['*.config.js', 'vite.config.js'],
    languageOptions: { globals: { ...globals.node } },
  },
]
