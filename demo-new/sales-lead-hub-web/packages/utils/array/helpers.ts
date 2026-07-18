/**
 * 完全展开一个任意层级嵌套的数组（递归实现）
 *
 * @param arr - 输入的可能包含多层嵌套的数组
 * @returns 展开后的一维数组
 *
 * 示例:
 * flattenDeep(['header', ['sider', ['content', 'footer']]])
 * => ['header', 'sider', 'content', 'footer']
 */
export function flattenDeep<T>(arr: any[]): T[] {
  const result: T[] = []
  for (const item of arr) {
    if (Array.isArray(item)) {
      result.push(...flattenDeep<T>(item))
    } else {
      result.push(item as T)
    }
  }
  return result
}
