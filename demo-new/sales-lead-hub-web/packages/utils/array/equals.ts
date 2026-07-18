/**
 * 计算两个数组（视为集合）的对称差集：
 * 返回只在 left 或只在 right 中出现的元素，忽略顺序与重复次数。
 *
 * 注意：
 * - 仅适用于原始值元素（string | number | symbol），依赖 Set 的引用相等与哈希。
 * - 如果需要考虑重复次数（多重集差异），请改用计数 Map 的实现。
 *
 * 时间复杂度：O(n + m) —— 构建两个 Set 并线性遍历
 * 空间复杂度：O(n + m) —— 两个 Set 与输出数组
 *
 * @param left  左侧数组（只读），元素类型为字符串/数字/符号
 * @param right 右侧数组（只读），元素类型同 left
 * @returns     对称差集数组（不保证顺序；不保留重复次数）
 */
export function symmetricDifference<T extends readonly (string | number | symbol)[]>(
  left: T,
  right: T
) {
  const lset = new Set(left)
  const rset = new Set(right)
  return [
    ...Array.from(lset).filter(x => !rset.has(x)),
    ...Array.from(rset).filter(x => !lset.has(x))
  ]
}

/**
 * 判断两个数组在“集合语义”下是否相同：
 * 1) 先比较长度，不等直接返回 false（短路）
 * 2) 再用对称差集判断：若对称差为空，则集合相同
 *
 * 注意：
 * - 忽略顺序与重复次数；只要元素集合相同，就返回 true。
 * - 仅适用于 string | number | symbol 等原始值元素。
 *
 * @param left  左侧只读数组
 * @param right 右侧只读数组
 * @returns     集合语义下是否相同
 */
export function isSameSet<T extends readonly (string | number | symbol)[]>(
  left: T,
  right: T
): boolean {
  // 长度不同，必不相同（集合语义下是一个很快的必要条件）
  if (left.length !== right.length) return false

  // 使用你已有的 symmetricDifference 来判断是否存在差异
  const diff = symmetricDifference(left, right)
  return diff.length === 0
}
