/** kebab-case → camelCase：如 'sidebar-sub' → 'sidebarSub' */
export const kebabToCamel = (s: string) => s.replace(/-([a-z])/g, (_, ch) => ch.toUpperCase())

/** camelCase → kebab-case：如 'sidebarSub' → 'sidebar-sub' */
export const camelToKebab = (s: string) =>
  s.replace(/([a-z])([A-Z])/g, (_, a, b) => `${a}-${b.toLowerCase()}`)

/** 生成一个键的候选形态：原样、kebab→camel、camel→kebab（尽量提高匹配成功率） */
export const candidates = (key: string) => [key, kebabToCamel(key), camelToKebab(key)]

/** 去掉透明度后缀：'token/80' → 'token' */
export const stripOpacity = (raw: string) => raw.replace(/\/\d{1,3}$/, '')

/** 连续移除尾部 DEFAULT（先去透明度），把 DEFAULT 视为父级键 */
export const stripTrailingDefault = (raw: string) => {
  let s = stripOpacity(raw)
  // 连续移除末尾 DEFAULT：如 'sidebar.DEFAULT.DEFAULT' → 'sidebar'
  while (/(?:\.|-)DEFAULT$/.test(s)) s = s.replace(/(?:\.|-)DEFAULT$/, '')
  return s
}

/**
 * 应用别名映射：将 key 中的某些片段替换为另一路径。
 * 例如传入 { 'sidebar-sub': 'sidebar.sub' }，即可将 'sidebar-sub' 视为 'sidebar.sub'。
 * @param raw 原始路径字符串（可能包含别名）
 * @param aliases 别名映射表（key: 被替换片段；value: 替换后的片段）
 */
export const applyAliases = (raw: string, aliases: Record<string, string> = {}) => {
  let s = raw
  for (const [from, to] of Object.entries(aliases)) {
    s = s.replace(new RegExp(from, 'g'), to)
  }
  return s
}

/**
 * 解析路径并在 `root` 对象下取值（返回一个颜色字符串或 null）。
 * - 支持 '.' / '-' 分隔；kebab/camel 互通
 * - 尾部多个 DEFAULT 自动去除并视为父级
 * - 命中对象时优先取该对象的 DEFAULT（preferObjectDefault=true）
 * - 未命中时可回退至最近祖先的 DEFAULT（fallbackUpwardDefault=true）
 *
 * @param root 解析的根对象（如 theme.colors.*.bg / .text / .fg）
 * @param rawKey 原始路径（如 'sidebar-sub'、'palette.brand-500'、'x.y.z.DEFAULT'）
 * @param options 解析选项：
 *  - aliases：别名映射表
 *  - preferObjectDefault：如果最终命中的是对象，是否优先返回对象的 DEFAULT（默认 true）
 *  - fallbackUpwardDefault：中途未命中时是否回退到最近祖先的 DEFAULT（默认 false），避免误伤 Wind4 或者其他预设中的规则
 * @returns string | null
 */
export const resolveToken = (
  root: any,
  rawKey: string,
  options?: {
    aliases?: Record<string, string>
    preferObjectDefault?: boolean
    fallbackUpwardDefault?: boolean
  }
): string | null => {
  const { aliases = {}, preferObjectDefault = true, fallbackUpwardDefault = false } = options || {}

  // 统一预处理：应用别名 → 去掉尾部 DEFAULT
  const normalized = stripTrailingDefault(applyAliases(rawKey, aliases))
  const parts = normalized.split(/[.-]/).filter(Boolean)

  let node: any = root
  let lastDefaultHolder: any = undefined // 记录最近一个“拥有 DEFAULT 的祖先”

  for (const part of parts) {
    if (node == null) break

    // 记录拥有 DEFAULT 的祖先节点，便于后续回退
    if (typeof node === 'object' && 'DEFAULT' in node) {
      lastDefaultHolder = node
    }

    // 在当前 node 下尝试匹配 part 的所有候选形态（原/kebab→camel/camel→kebab）
    const hit = candidates(part).find(k => node && typeof node === 'object' && k in node)
    if (hit) {
      node = node[hit]
      continue
    }

    // 当前层未命中：选择“向上回退到最近祖先的 DEFAULT”
    if (
      fallbackUpwardDefault &&
      lastDefaultHolder &&
      typeof lastDefaultHolder.DEFAULT === 'string'
    ) {
      return lastDefaultHolder.DEFAULT as string
    }

    // 不回退就失败
    node = undefined
    break
  }

  if (node == null) return null

  // 如果最终是对象：按 preferObjectDefault 决定是否降级到该对象的 DEFAULT
  if (typeof node === 'object') {
    if (preferObjectDefault && 'DEFAULT' in node && typeof node.DEFAULT === 'string')
      return node.DEFAULT as string
    return null // 返回对象通常不是你要的颜色字符串；按需改为自定义行为
  }

  // 最终命中的是字符串（颜色值）
  return node as string
}

/**
 * 将颜色应用透明度（通用实现）。
 * 使用 CSS Color Mix（oklab）进行混合，现代浏览器表现较好。
 * 如果你的色值都是 hsl()/rgb()/hex，也可以改为分别生成 hsla()/rgba() 或对 hex 做 alpha 转换。
 *
 * @param color 基础颜色字符串（如 'hsl(var(--card-bg))'）
 * @param alpha 透明度（0–100）；null 表示不加透明度
 * @returns string
 */
export const withAlpha = (color: string, alpha: number | null) =>
  alpha == null ? color : `color-mix(in oklab, ${color} ${alpha}%, transparent)`
