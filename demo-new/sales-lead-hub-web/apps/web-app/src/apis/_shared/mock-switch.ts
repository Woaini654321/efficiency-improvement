/**
 * 数据来源开关（模块粒度）。
 *
 * 为什么不是全局单布尔：20 个模块的后端是分批接通的。全局 false 会让所有
 * 未接通模块同时打向不存在的接口、满屏 404，淹没当前正在联调模块的信号
 * （requirement 竖切时就被迫绕开全局开关单独直连）。改为白名单后，接通
 * 一个模块就从 MOCK_MODULES 里删一行，互不干扰。
 *
 * 维护规则：模块后端联调通过后，删掉名单里对应的一行即可，该模块 api 文件
 * 的 mockRequest 调用处不用改（module 键保留，方便回退时加回名单）。
 * module 键 = apis/ 下目录名；唯一例外 notification/announcementApi.ts 用
 * 'announce'——它打的是 announcement/* 端点，须随 announce 后端一起切真。
 */
// 2026-07-19 全模块接通，白名单清空：后端 68+ 端点已全部实现并过真库集成测试，
// 所有模块的 mockRequest 调用一律走真实请求。机制与注释刻意保留——
// 回退调试某模块时，把它的 module 键加回下面的数组即可（api 文件的 mockRequest 调用处不用动）。
export const MOCK_MODULES = new Set<string>([])

/** 该模块当前是否仍走本地 mock */
export function isMocked(module: string): boolean {
  return MOCK_MODULES.has(module)
}

/**
 * 包裹一个真实请求：该模块仍在 mock 名单中则直接返回本地 DTO 切片，否则执行真实请求。
 *
 * 为什么不用 AIRequestGuard 的全局 `mode:'mock'`：其 mock 分支会对已注册的
 * viewSchema（本项目注册的是 ViewModel，camelCase）再跑一次 adapter（adapter 按
 * snake_case 读 camelCase 对象），导致字段全部落默认值、图表/列表全空。改为在
 * request 层注入 mock，则 adapter 只转换一次，数据正确；且这正是后端就绪后的
 * 正确形态——届时把模块从名单删掉即可切回真实接口。
 *
 * @param module 模块键（见 MOCK_MODULES 注释）
 * @param mock 该接口对应的原始 DTO 切片（与其 adapter 的 viewSchema 入参一致）
 * @param real 真实请求 thunk，如 `() => request.POST({ url }, params)`
 */
export function mockRequest(
  module: string,
  mock: unknown,
  real: () => Promise<unknown>
): () => Promise<unknown> {
  return isMocked(module) ? () => Promise.resolve(mock) : real
}
