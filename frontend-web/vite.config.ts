import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 后端 Spring Boot 默认 8080，统一 /api 前缀
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // element-plus 全量引入约 1.1MB（vendor 块可长效缓存），放宽默认 500KB 警告阈值
    chunkSizeWarningLimit: 1200,
    rolldownOptions: {
      output: {
        // 大体积三方库独立分包，利于浏览器长效缓存
        advancedChunks: {
          groups: [
            { name: 'echarts', test: /node_modules[\\/]+(echarts|zrender)/ },
            { name: 'element-plus', test: /node_modules[\\/]+(element-plus|@element-plus)/ },
            { name: 'vue-vendor', test: /node_modules[\\/]+(@vue|vue|vue-router|pinia)[\\/]/ },
          ],
        },
      },
    },
  },
})
