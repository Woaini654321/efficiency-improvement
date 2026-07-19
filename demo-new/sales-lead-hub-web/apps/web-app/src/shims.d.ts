// 环境补充声明：为若干「随框架源码引入但未提供类型声明」的模块与全局补声明，
// 消除 vue-tsc 对 node_modules 源码 / workspace 源码的类型噪音（不影响运行时）。

// @q-ui/types：被 @q-web-plugin/{antdv-pro,empty,layouts} 源码 import，但该内部包未随依赖安装。
// 按其实际被引用的类型工具补声明（泛型签名与用法对齐），消除「找不到模块 / 命名空间当类型」噪音。
declare module '@q-ui/types' {
  import type { Plugin } from 'vue'
  export type SFCWithInstall<T> = T & Plugin
  export type ExtendAntdComponentPropsOf<T = any, U = any, V = any> = Record<string, any>
  const _default: any
  export default _default
}

// nprogress：@q-web-plugin/event-messages 引用，缺 @types/nprogress
declare module 'nprogress'

// @q-tools/core：q-file-list 组件引用的压缩产物，无声明文件
declare module '@q-tools/core'

// uni：packages/utils 兼容 uni-app 平台时使用的全局对象，Web 端不存在
declare const uni: any
