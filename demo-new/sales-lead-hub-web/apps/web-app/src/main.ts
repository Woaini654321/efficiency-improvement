// core-provider 提供核心库上层插件的相关配置
// 必须在 @q-web-plugin/request、@q-web-plugin/store、@q-web-plugin/router、App 模块 import 之前
import './core-provider'

import 'virtual:uno.css'
import '@q-mono-x/styles/web.css'
import '@q-mono-x/styles/menu-icons/index.css'
import { createApp } from 'vue'
import App from './App.vue'

// 初始化 Request 实例（request）以及通用请求 CommonApi（ 这里显式引入下，在 @q-web-plugin/store 中也会引入 ）
import '@q-web-plugin/request'
import { store } from '@q-web-plugin/store'
import { router } from '@q-web-plugin/router'

import { i18n } from '@q-web-plugin/i18n'
import QUI from '@q-ui/web'
import '@q-ui/web/dist/style.css'
import Antd from 'ant-design-vue'
import Layouts from '@q-web-plugin/layouts'
import Empty from '@q-web-plugin/empty'
import { eventMessages } from '@q-web-plugin/event-messages'
import { vxe } from '@q-web-plugin/vxe'
import { track } from '@q-web-plugin/track'
import AntdvPro from '@q-web-plugin/antdv-pro'
import { zh_CN, en_US } from './locale'
import 'virtual:ai-request-guard/report-sink'

i18n.global.setLocaleMessage('zh-CN', zh_CN)
i18n.global.setLocaleMessage('en-US', en_US)

createApp(App)
  // 集成状态管理（内置核心 store 和其他通用 store，SSO、飞书、快特等平台自动接入）
  .use(store)
  // 集成约定式路由（文件路径即路由路径）
  .use(router)
  // 集成国际化
  .use(i18n)
  // 集成 Ant Design Vue 基础组件（QBigTable/QForm/QUpload 等依赖 a-* 组件）；必须在 QUI 之前，让 QUI 覆盖同名组件
  .use(Antd)
  // 集成 @q-ui/web 组件库（给 Layouts、Empty 等提供基础组件能力）
  .use(QUI)
  // 集成默认布局组件（内部结合 QLayout、QMenu、Design Token，基本满足 80% 以上项目需求）
  .use(Layouts)
  // 集成空状态组件（内部结合 FrontEndAppConfStore，自动适配对内和对外两种风格）
  .use(Empty)
  // 集成当前应用与 @q-cli/libs 的事件通信中间层（接收并处理 @q-cli/libs 派发的事件）
  .use(eventMessages)
  // 接触 vxe（内置统一风格的 vxe 配置）
  .use(vxe)
  // 集成埋点插件（q-track）
  .use(track)
  // 集成 Ant Design Vue 增强组件
  .use(AntdvPro)
  .mount('#app')
