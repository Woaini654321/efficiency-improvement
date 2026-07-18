/**
 * 取英文名首字母组成缩写；若没有英文名，则取中文名拼音首字母。
 * - 若运行环境不支持 'zh-Hans-u-co-pinyin'（缺少 ICU），中文路径退化为：取输入的第一个字符。
 *
 * @param input 姓名字符串（可中英文混排）
 * @param options.limit 返回字母个数上限（默认 2）
 * @param options.upper 是否转为大写（默认 true）
 * @returns 缩写字符串
 */
export function getNameAbbr(
  input: string,
  options: { limit?: number; upper?: boolean } = {}
): string {
  const { limit = 2, upper = true } = options
  if (!input) return ''

  // 1) 优先英文名（Latin）首字母
  const latinInitials = getLatinInitials(input)
  if (latinInitials.length > 0) {
    const out = latinInitials.slice(0, limit).join('')
    return upper ? out.toUpperCase() : out
  }

  // 2) 中文：尝试使用 ICU 拼音排序；失败则退化：取首个字符
  const chineseInitials = getChineseInitialsWithFallback(input, limit)
  const out = chineseInitials.slice(0, limit).join('')
  return upper ? out.toUpperCase() : out
}

/** 提取英文（Latin）单词的首字母 */
function getLatinInitials(s: string): string[] {
  const words = s
    .trim()
    .split(/[\s\-_./\\]+/g)
    .filter(Boolean)

  const initials: string[] = []
  for (const w of words) {
    // 找到单词中的第一个英文字母
    const m = w.match(/[A-Za-z]/)
    if (m) initials.push(m[0])
  }
  return initials
}

/**
 * 中文首字母（ICU 正常：用拼音排序边界；ICU 不可用：退化为首字符）
 */
function getChineseInitialsWithFallback(s: string, limit: number): string[] {
  const firstVisibleChar = getFirstVisibleChar(s)
  const collator = getPinyinCollatorOrNull()

  if (!collator) {
    // 无 ICU：直接返回首字符（如果没有可见字符则空串）
    return firstVisibleChar ? [firstVisibleChar] : []
  }

  // 有 ICU：按拼音边界推断 A-Z
  const boundaries = '阿芭擦搭蛾发噶哈击喀垃妈拿哦啪期然撒塌挖昔压匝'.split('')
  const A2Z = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

  const initials: string[] = []
  for (const ch of s) {
    if (!isCJK(ch)) continue
    const idx = findInitialIndexByBoundary(collator, ch, boundaries)
    if (idx !== -1) initials.push(A2Z[idx] as string)
    if (initials.length >= limit) break // 达到上限即可停止
  }

  // 如果中文里没有可映射的字符，仍然退化到首字符
  if (initials.length === 0 && firstVisibleChar) {
    return [firstVisibleChar]
  }
  return initials
}

function getPinyinCollatorOrNull(): Intl.Collator | null {
  try {
    const c = new Intl.Collator('zh-Hans-u-co-pinyin', { sensitivity: 'base' })
    // 做一次轻量自检：'阿' 应该排在 '芭' 之前
    if (c.compare('阿', '芭') < 0) return c
    return null
  } catch {
    return null
  }
}

function isCJK(ch: string): boolean {
  const code = ch.codePointAt(0)!
  return (
    (code >= 0x4e00 && code <= 0x9fff) || // 基本中日韩
    (code >= 0x3400 && code <= 0x4dbf) // 扩展 A
  )
}

function findInitialIndexByBoundary(
  collator: Intl.Collator,
  ch: string,
  boundaries: string[]
): number {
  if (!collator || collator.compare(ch, boundaries[0] as string) < 0) return 0
  for (let i = 0; i < boundaries.length - 1; i++) {
    const l = boundaries[i] as string,
      r = boundaries[i + 1] as string
    if (collator.compare(ch, l) >= 0 && collator.compare(ch, r) < 0) return i
  }
  return boundaries.length - 1 // >= '匝' 归 Z
}

/** 获取第一个“可见字符”（非空白、非控制字符） */
function getFirstVisibleChar(s: string): string {
  for (const ch of s.trim()) {
    // 过滤空白与常见分隔符；如果你希望保留任何字符，直接 return ch
    if (!/[\s]/.test(ch)) {
      return ch
    }
  }
  return ''
}
