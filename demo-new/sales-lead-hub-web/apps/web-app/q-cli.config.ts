/**
 * 本地联调代理（依据 vue-coding integration.md STEP 00）。
 *
 * 作用：把业务接口 /api/sales-lead-hub/* 打到本地后端 :8081，
 * 其余（平台 upm-config / SSO 等）仍走 existingProxy 的默认目标（dev 网关）。
 *
 * ⛔ 约束：
 *  - 自定义规则必须在前，...existingProxy 在后（http-proxy 按 key 顺序先到先得）
 *  - 用 mutation（不 return），return 对象会触发 mergeConfig 导致 proxy 合并不可靠
 *  - proxy key 以 /api/sales-lead-hub 开头，与 VITE_API_DEFAULT_SERVICE_BASE_URL 第一段一致
 *  - rewrite 去掉 /api 前缀，与后端 context-path=/sales-lead-hub 对齐
 */
export default function (finalConfig: any) {
  const existingProxy = finalConfig.server?.proxy || {}
  finalConfig.server = finalConfig.server || {}
  finalConfig.server.proxy = {
    '/api/sales-lead-hub': {
      target: 'http://localhost:8081',
      changeOrigin: true,
      rewrite: (path: string) => path.replace(/^\/api\/sales-lead-hub/, '/sales-lead-hub'),
    },
    ...existingProxy,
  }
}
