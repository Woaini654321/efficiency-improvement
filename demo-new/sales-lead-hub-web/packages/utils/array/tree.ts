/**
 * 将树形数据遍历并收集到 Map 中。
 *
 * @param tree - 根节点数组
 * @param keySelector - 生成 Map 的 key（必须唯一）
 * @param valueSelector - 生成 Map 的 value（默认返回节点自身）
 * @param options - 可选配置：childrenKey、strategy（'DFS'|'BFS'）
 * @returns Map<K, V>
 */
export function treeToMap<T extends Record<string, any>, K extends string, V = T>(
  tree: T[],
  keySelector: (node: T) => K,
  valueSelector: (node: T) => V = node => node as unknown as V,
  options?: {
    childrenKey?: keyof T // 默认为 'children'
    strategy?: 'DFS' | 'BFS' // 默认为 'DFS'
    setMap?: (node: T) => void
  }
): Map<K, V> {
  const childrenKey = (options?.childrenKey ?? 'children') as keyof T
  const strategy = options?.strategy ?? 'DFS'

  const map = new Map<K, V>()
  if (!Array.isArray(tree) || tree.length === 0) return map

  // 内部统一遍历实现，避免重复逻辑
  if (strategy === 'BFS') {
    const queue: T[] = [...tree]
    while (queue.length) {
      const node = queue.shift() as T
      const key = keySelector(node)
      if (!map.has(key)) {
        if (options?.setMap) options.setMap(node)
        map.set(key, valueSelector(node))
      }
      const children = node[childrenKey] as unknown as T[] | undefined
      if (Array.isArray(children) && children.length) {
        queue.push(...children)
      }
    }
  } else {
    // DFS 迭代版，避免递归深度问题
    const stack: T[] = [...tree].reverse()
    while (stack.length) {
      const node = stack.pop() as T
      const key = keySelector(node)
      if (!map.has(key)) {
        if (options?.setMap) options.setMap(node)
        map.set(key, valueSelector(node))
      }
      const children = node[childrenKey] as unknown as T[] | undefined
      if (Array.isArray(children) && children.length) {
        // 由于栈是后进先出，为保持从左到右遍历顺序，这里反转
        stack.push(...children.slice().reverse())
      }
    }
  }

  return map
}
