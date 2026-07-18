/** Vue SFC 模块声明 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
declare module 'virtual:ai-request-guard/report-sink' {
  export function flushReport(): void
}
