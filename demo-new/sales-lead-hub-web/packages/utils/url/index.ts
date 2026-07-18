/**
 * 将对象序列化为URL查询字符串，用于替代第三方的 qs 库，节省宝贵的体积
 * 支持基本类型值和数组，不支持嵌套对象
 * @param obj 要序列化的对象
 * @returns 序列化后的查询字符串
 */
export function stringifyQuery(obj: Record<string, any>): string {
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return ''

  return Object.entries(obj)
    .filter(([_, value]) => value !== undefined && value !== null)
    .map(([key, value]) => {
      // 对键进行编码
      const encodedKey = encodeURIComponent(key)

      // 处理数组类型
      if (Array.isArray(value)) {
        return value
          .filter(item => item !== undefined && item !== null)
          .map(item => `${encodedKey}=${encodeURIComponent(item)}`)
          .join('&')
      }

      // 处理基本类型
      return `${encodedKey}=${encodeURIComponent(value)}`
    })
    .join('&')
}

/**
 * 页面参数转对象
 */
export function urlParam2Obj(url?: string) {
  url = url || location.href
  const search = url.split('?')[1]
  if (!search) {
    return {}
  }
  const obj: Record<string, string> = {}
  search.split('&').forEach(v => {
    const index = v.indexOf('=')
    if (index !== -1) {
      const [key, value] = v.split('=')
      if (key && value) {
        obj[key] = decodeURIComponent(value)
      }
    }
  })
  return obj
}

/**
 * 解析 fullPath 得到 path 和 query
 * 比如输入url: /pages/login/index?oauth_callback=%2Fpages%2Fdemo%2Fbase%2Froute-interceptor
 * 输出: {path: /pages/login/index, query: {oauth_callback: /pages/demo/base/route-interceptor}}
 */
export function parseFullPath(fullPath: string) {
  const [path, queryStr] = fullPath.split('?')

  if (!queryStr) {
    return {
      path,
      query: {}
    }
  }
  const query: Record<string, string> = urlParam2Obj(fullPath)
  return { path, query }
}
