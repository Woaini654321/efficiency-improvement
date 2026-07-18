// ============================================================
// vue-coding paths — 项目结构解析（单一源）
//
// 为什么需要：脚手架的 pnpm-workspace.yaml 声明的是 `apps/*`，**app 目录名是变的**：
//   cadre-system-web → apps/web-app      quectel-cli-admin → apps/admin
// 而 CLI 各处写死了 `apps/web-app`（self-check 4 处 / gate-check 21 处 / step-executor 4 处
// / render-template 3 处 / verify 3 处），联调层则写死了根级 `src/`（differ / code-reader
// / schema-matcher / context-detector）。
//
// 后果不是报错，是**静默放行**：self-check 在 apps/admin 的项目上找不到 apps/web-app/src/pages，
// 于是判定「新项目，跳过页面检查」→ PASS → 11 个真实页面一个没查。门禁被一个目录名绕过了。
//
// 铁律：**找不到 app 目录 = FAIL，不是 PASS。**「无法验证」永远不等于「验证通过」。
// 本模块只负责定位，不负责判定；调用方拿到 null 必须 FAIL。
//
// 纯 node、零依赖、跨平台。.mjs 供 CLI 直接 import；TS 层经 tsx 亦可 import。
// ============================================================

import { existsSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

function isDir(p) {
  try { return statSync(p).isDirectory() } catch { return false }
}

/**
 * 定位项目的 app 源码目录。
 *
 * @param {string} root 项目根（含 package.json / pnpm-workspace.yaml 的那层）
 * @returns {{ srcDir: string, appDir: string, appName: string, layout: 'monorepo'|'flat' } | null}
 *          null = 定位失败，调用方必须 FAIL
 */
export function resolveAppSrc(root) {
  // ① monorepo：扫 apps/*，不假设 app 叫什么
  const appsDir = join(root, 'apps')
  if (isDir(appsDir)) {
    const found = []
    for (const name of readdirSync(appsDir)) {
      const srcDir = join(appsDir, name, 'src')
      if (isDir(srcDir)) found.push({ srcDir, appDir: join(appsDir, name), appName: name, layout: 'monorepo' })
    }
    if (found.length === 1) return found[0]
    if (found.length > 1) {
      // 多 app（web + admin + h5…）：优先含 pages/ 的那个，再优先 web-app，最后字典序。
      // 保证同一项目每次解析结果一致 —— 不确定性会让门禁时好时坏，比选错更难查。
      const withPages = found.filter(f => isDir(join(f.srcDir, 'pages')))
      const pool = withPages.length ? withPages : found
      return pool.find(f => f.appName === 'web-app') || pool.sort((a, b) => a.appName.localeCompare(b.appName))[0]
    }
  }

  // ② 单体项目：根级 src/
  const flatSrc = join(root, 'src')
  if (isDir(flatSrc)) return { srcDir: flatSrc, appDir: root, appName: '', layout: 'flat' }

  // ③ 定位失败 —— 调用方必须 FAIL
  return null
}

/** app 源码目录，找不到返回 null。 */
export function resolveSrcDir(root) {
  const r = resolveAppSrc(root)
  return r ? r.srcDir : null
}

/** src/apis 目录，找不到 app 或该目录不存在时返回 null。 */
export function resolveApisDir(root) {
  const src = resolveSrcDir(root)
  if (!src) return null
  const apis = join(src, 'apis')
  return isDir(apis) ? apis : null
}

/** src/pages 目录，找不到 app 或该目录不存在时返回 null。 */
export function resolvePagesDir(root) {
  const src = resolveSrcDir(root)
  if (!src) return null
  const pages = join(src, 'pages')
  return isDir(pages) ? pages : null
}

/**
 * locale 目录。脚手架里单复数都出现过（真实项目是单数 locale），全部候选。
 * @returns {string[]} 存在的 locale 目录列表（可能为空）
 */
export function resolveLocaleDirs(root) {
  const src = resolveSrcDir(root)
  const cands = []
  if (src) cands.push(join(src, 'locale'), join(src, 'locales'))
  cands.push(join(root, 'packages', 'locale'))
  return cands.filter(isDir)
}

// ─── Skill 运行时状态与缓存 ───────────────────────────────
//
// entities.json / step-state.json / env.lock / gate-tokens.json / artifacts/ / swagger-cache.json
// 全是 **skill 自己产、自己消费** 的产物 —— 没有一样是 harness 读写的。
//
// 目录名的演进：`.claude/`（harness 命名，在 Codex/Hermes 下造 .claude 目录是错的）
// → `.vue-coding/`（skill 命名，对了但各技能各造一个）→ 现统一到 `.quectel-code/`。
// 套件命名的理由：code-master / vue-coding / java-coding 是一套东西，共用一个命名空间，
// 且不暗示任何 agent。目录归属由 code-master 在 workspace 标准里定义。
//
// 分两层，因为语义不同 —— 混在一起就没法用一条 .gitignore 表达意图：
//   state/  状态：删了丢工作，可提交进用户仓库（团队共享进度）
//   cache/  缓存：可删可重建，应被 .gitignore
// 那条 .gitignore 由 reference-sync 的 ensureGitignore() 写在 .quectel-code/.gitignore
// —— 自包含，不碰用户根目录的 .gitignore。⛔ 划分依据是「能不能重建」而不是目录名：
// gate-tokens.json / artifacts/ / swagger-cache.json 虽然在 state/ 下，但也是可重建的瞬时
// 产物，同样被忽略。改这个划分时记得同步 ensureGitignore()。
//
// 关于「为什么这些依据写在代码注释而不是 SKILL.md」：SKILL.md 只给 AI 读，人不读。
// 能改变 AI 行为的话才留在那儿（如「不要写进 .claude/」）；「为什么当初这么定」属于
// 维护者知识，放这里 —— 塞进 SKILL.md 等于让每次运行都为历史考据付 token。
//
// 兼容：**读**时新旧都认（存量项目的 `.claude/` `.vue-coding/` 不能炸），**写**只写新路径。
// 因此存量项目会在下一次写入时自然迁移，无需手工搬。

const STATE_DIR = join('.quectel-code', 'state')
const CACHE_DIR = join('.quectel-code', 'cache')
const LEGACY_STATE_DIRS = ['.vue-coding', '.claude']

// ⛔ legacy 回落只对下列「状态产物」生效，**不含 reference 缓存**。
// 原因：历史上 reference 缓存也曾放在 .claude/ 下，那些目录至今还在（实测 4 个），
// 且装的是**迁移前的旧文档**（含已被修掉的 layout:'custom'、GET({params})、colProps 等错规则）。
// 一旦回落过去，等于把刚修好的规范换回错的 —— 比找不到还糟。
//
// `.vue-coding/reference-cache` 的数据虽然是对的，但同样不回落：reference-sync 幂等且秒退，
// 重拉一次的代价远低于「两个 legacy 目录、两种数据质量」所需的分级逻辑。
// 规范只认 .quectel-code/cache/reference，缺了就让 reference-sync 重新拉。
const LEGACY_FALLBACK_ALLOWLIST = new Set([
  'entities.json',
  'step-state.json',
  'env.lock',
  'gate-tokens.json',
  'swagger-cache.json',
  'artifacts',
])

/** 状态文件的写入路径 —— 永远是新目录。 */
export function stateWritePath(root, ...parts) {
  return join(root, STATE_DIR, ...parts)
}

/**
 * 状态文件的读取路径：新目录优先，仅白名单内的产物按序回落到 legacy 目录。
 * 全都没有时返回新路径（调用方按「不存在」处理即可）。
 */
export function stateReadPath(root, ...parts) {
  const next = join(root, STATE_DIR, ...parts)
  if (existsSync(next)) return next
  if (LEGACY_FALLBACK_ALLOWLIST.has(parts[0])) {
    for (const dir of LEGACY_STATE_DIRS) {
      const legacy = join(root, dir, ...parts)
      if (existsSync(legacy)) return legacy
    }
  }
  return next
}

/** 该状态文件当前是否还躺在某个 legacy 目录（用于提示迁移，不用于判定失败）。 */
export function isLegacyState(root, ...parts) {
  if (!LEGACY_FALLBACK_ALLOWLIST.has(parts[0])) return false
  if (existsSync(join(root, STATE_DIR, ...parts))) return false
  return LEGACY_STATE_DIRS.some(dir => existsSync(join(root, dir, ...parts)))
}

/**
 * reference 缓存路径 —— **目录的单一源**。
 *
 * 此前 reference-sync 读 manifest 的 cache.directory、gate-check 自己 join 一遍，
 * 同一个目录两个源。manifest 只该管「有哪些文档」，不该管「放在哪」。
 */
export function cachePath(root, ...parts) {
  return join(root, CACHE_DIR, 'reference', ...parts)
}

/**
 * 供门禁打印的定位诊断 —— 定位失败时必须让调用方能说清「在哪儿找过、没找到什么」，
 * 否则 AI 只能看到一句 FAIL，无从修复。
 */
export function describeLookup(root) {
  const appsDir = join(root, 'apps')
  const lines = [`  项目根: ${root}`]
  if (isDir(appsDir)) {
    const entries = readdirSync(appsDir).filter(n => isDir(join(appsDir, n)))
    lines.push(`  apps/ 存在，子目录: ${entries.length ? entries.join(', ') : '(空)'}`)
    for (const n of entries) {
      lines.push(`    apps/${n}/src ${existsSync(join(appsDir, n, 'src')) ? '存在' : '不存在'}`)
    }
  } else {
    lines.push('  apps/ 不存在')
  }
  lines.push(`  src/ ${isDir(join(root, 'src')) ? '存在' : '不存在'}`)
  return lines.join('\n')
}
