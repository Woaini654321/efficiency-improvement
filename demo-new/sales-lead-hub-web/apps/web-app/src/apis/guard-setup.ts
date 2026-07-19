// AIRequestGuard 全局配置
// 开发阶段开启 mock 模式：所有查询类请求（经 AIRequestGuard 包裹）不发起 HTTP，
// 直接返回各 adapter 注册的 viewSchema（ViewModel 格式的 mock 数据）。
// 联调阶段：删除各模块 mocks/*.json 或将 mode 切为 'real'。
import AIRequestGuard from '@ai-request-guard/core'

if (import.meta.env.DEV) {
  AIRequestGuard.configure({ mode: 'mock' })
}
