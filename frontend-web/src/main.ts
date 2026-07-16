import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/index.css'

const app = createApp(App)

// pinia 先于 router：路由守卫中会使用 user store
app.use(createPinia())
app.use(router)
// 开发阶段全量引入 Element Plus，后续可切 unplugin-vue-components 按需优化
app.use(ElementPlus, { locale: zhCn })

// 全局注册图标：菜单/StatCard 等按名称字符串引用（<component :is="iconName">）
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.mount('#app')
