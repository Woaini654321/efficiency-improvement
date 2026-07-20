// AIRequestGuard 全局配置
//
// Guard 始终保持 'real' 模式。「后端未就位」这件事统一在 request 层处理：各 api 用
// `mockRequest('<模块键>', <DTO切片>, () => request.POST/GET(...))` 包裹，mock 时 request() 直接返回
// 本地 DTO，adapter 正常转换一次即得正确 ViewModel（见 _shared/mock-switch.ts）。
//
// ⚠️ 不要在此改回 `mode:'mock'`：库的 mock 分支会对已注册的 viewSchema（本项目为
// ViewModel）再跑一次 adapter（snake 读 camel），导致数据全部落默认值、图表/列表全空。
// 联调阶段（模块后端就绪）：把该模块从 _shared/mock-switch.ts 的 MOCK_MODULES 名单删掉即可。
import AIRequestGuard from '@ai-request-guard/core'

AIRequestGuard.configure({ mode: 'real' })
