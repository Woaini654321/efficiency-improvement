/**
 * 将对象扁平为连字符路径：
 * { sidebar: { sub: ... }, card: ... } → ['sidebar-sub', 'card', ...]
 * 会跳过 DEFAULT 键
 */
export const flattenThemeKeys = (obj: any, parent = ''): string[] => {
  if (!obj || typeof obj !== 'object') return []
  const out: string[] = []
  for (const k of Object.keys(obj)) {
    if (k === 'DEFAULT') continue
    const path = parent ? `${parent}-${k}` : k
    out.push(path)
    out.push(...flattenThemeKeys(obj[k], path))
  }
  return out
}

/** 正则分组安全转义（防止键名里有特殊字符破坏模板） */
export const escapeForRegexGroup = (s: string): string => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * 从 theme 提取某组的键并生成枚举模板字符串：
 * 返回类似 'text-(card|popover|navbar|sidebar|sidebar-sub|muted)'
 */
export const makeEnumAutocompleteFromTheme = (
  theme: any,
  namespace: string,
  group: string,
  options?: {
    prefix?: string // 输出模板的前缀，默认与 group 相同
    sort?: boolean // 是否排序，默认 true
    unique?: boolean // 是否去重，默认 true
    transformKey?: (k: string) => string // 键映射（如有额外别名处理）
  }
): string => {
  const { prefix = group, sort = true, unique = true, transformKey } = options || {}

  const groupObj = theme?.colors?.[namespace]?.[group]
  if (!groupObj) {
    // 组不存在时，返回一个仅含裸类的模板以避免报错
    return `${prefix}`
  }

  // 展平 + 变换 + 去重 + 排序 + 转义
  let keys = flattenThemeKeys(groupObj).map(k => (transformKey ? transformKey(k) : k))
  if (unique) keys = Array.from(new Set(keys))
  if (sort) keys = keys.sort((a, b) => a.localeCompare(b))
  const safe = keys.map(escapeForRegexGroup)

  return `${prefix}-(${safe.join('|')})`
}

/**
 * 组合生成：普通枚举模板 + 透明度位的模板
 * 例如返回：
 *  ['text-(card|popover|...|muted)', 'text-(card|popover|...|muted)/<percent>']
 */
export const makeEnumWithAlphaTemplatesFromTheme = (
  theme: any,
  namespace: string,
  group: string,
  alphaPlaceholder: '<percent>' | '<num>' | false = '<percent>',
  options?: Parameters<typeof makeEnumAutocompleteFromTheme>[3]
): string[] => {
  const base = makeEnumAutocompleteFromTheme(theme, namespace, group, options)
  const arr = [base]
  if (alphaPlaceholder) arr.push(`${base}/${alphaPlaceholder}`)
  return arr
}

export const genAutocompleteConf = (
  theme: any,
  namespace: string,
  group: string,
  alphaPlaceholder: '<percent>' | '<num>' | false = '<percent>',
  options?: Parameters<typeof makeEnumAutocompleteFromTheme>[3]
): {
  autocomplete: string[]
  layer: string
} => {
  return {
    autocomplete: makeEnumWithAlphaTemplatesFromTheme(
      theme, // 主题对象
      namespace, // 命名空间
      group, // 组：text
      alphaPlaceholder, // 透明度占位（VS Code/JetBrains 插件识别）
      options
    ),
    layer: 'utilities'
  }
}
