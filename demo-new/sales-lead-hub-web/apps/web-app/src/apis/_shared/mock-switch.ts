/**
 * 后端未就位期间的数据来源开关（前端预览 / 建表参考用）。
 *
 * 用法：在各 api 的 `AIRequestGuard({ adapter, request })` 中，用
 *   `request: mockRequest(<该接口的原始 DTO 切片>, () => request.POST/GET(...))`
 * 包裹真实请求。`MOCK_ENABLED === true` 时 `request()` 直接返回 DTO 切片，
 * 由 adapter 正常转换为 ViewModel；`=== false` 时走真实 HTTP。
 *
 * 为什么不直接用 AIRequestGuard 的全局 `mode:'mock'`：其 mock 分支会对已注册的
 * viewSchema（本项目注册的是 ViewModel，camelCase）再跑一次 adapter（adapter 按
 * snake_case 读 camelCase 对象），导致字段全部落默认值、图表/列表全空。改为在
 * request 层注入 mock，则 adapter 只转换一次，数据正确；且这正是后端就绪后的正确
 * 形态——届时把 `MOCK_ENABLED` 置为 false 即可全量切回真实接口。
 */
export const MOCK_ENABLED = true

/**
 * 包裹一个真实请求：mock 开启时直接返回本地 DTO 切片，否则执行真实请求。
 * @param mock 该接口对应的原始 DTO 切片（与其 adapter 的 viewSchema 入参一致）
 * @param real 真实请求 thunk，如 `() => request.POST({ url }, params)`
 */
export function mockRequest(mock: unknown, real: () => Promise<unknown>): () => Promise<unknown> {
  return MOCK_ENABLED ? () => Promise.resolve(mock) : real
}
