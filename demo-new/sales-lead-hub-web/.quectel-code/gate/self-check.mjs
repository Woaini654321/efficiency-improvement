#!/usr/bin/env node
// ============================================================
// vue-coding self-check — predev quality gate
// 在 pnpm dev 前自动运行，检查 AI 生成代码的规范合规性
// FAIL → exit 1 → dev server 不启动 → 强制修复
//
// Node.js 实现，无 bash 依赖，Win/Mac/Linux 通用
// ============================================================

import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { join, relative, resolve, dirname, basename, sep } from 'node:path'
import { resolveAppSrc, resolveLocaleDirs, describeLookup, stateReadPath } from './paths.mjs'

const ROOT = process.cwd()
// app 目录名不是固定的 web-app（pnpm-workspace 声明的是 apps/*，实际见过 web-app / admin），
// 写死会导致在 apps/admin 类项目上找不到 pages → 被误判成「新项目」→ 全部静默 PASS。
const APP = resolveAppSrc(ROOT)
const SRC_DIR = APP ? APP.srcDir : null
const PAGES_DIR = SRC_DIR ? join(SRC_DIR, 'pages') : null

const STRICT_MODE = process.argv.includes('--strict')
const SCOPE_FILE = (() => {
  const arg = process.argv.find(a => a.startsWith('--scope='))
  return arg ? arg.replace('--scope=', '') : null
})()
const LEGACY_MODULES = (() => {
  if (STRICT_MODE) return []
  try {
    const state = JSON.parse(readFileSync(stateReadPath(ROOT, 'step-state.json'), 'utf-8'))
    return state.legacyModules || []
  } catch (_) { return [] }
})()

let failCount = 0
let warnCount = 0

function pass(msg) {
  console.log(`  PASS: ${msg}`)
}

function fail(msg) {
  console.log(`  FAIL: ${msg}`)
  failCount++
}

function warn(msg) {
  console.log(`  WARN: ${msg}`)
  warnCount++
}

// Walk dir recursively, return all files matching filterFn
function walkDir(dir, filterFn) {
  const results = []
  try {
    const entries = readdirSync(dir)
    for (const entry of entries) {
      const full = join(dir, entry)
      try {
        const st = statSync(full)
        if (st.isDirectory() && entry !== 'node_modules') {
          results.push(...walkDir(full, filterFn))
        } else if (st.isFile() && filterFn(full)) {
          results.push(full)
        }
      } catch (_) { /* perm error, skip */ }
    }
  } catch (_) { /* dir not found, skip */ }
  return results
}

// Read file content, return '' on error
function readFile(path) {
  try { return readFileSync(path, 'utf-8') } catch (_) { return '' }
}

// Grep a regex across files, returns { file, lineNum, line }[]
function grepFiles(files, regex) {
  const matches = []
  for (const f of files) {
    const content = readFile(f)
    const lines = content.split('\n')
    for (let i = 0; i < lines.length; i++) {
      if (regex.test(lines[i])) {
        matches.push({ file: relative(ROOT, f), lineNum: i + 1, line: lines[i].trim() })
      }
    }
  }
  return matches
}

// ─── Gate: 定位 app ───────────────────────────────────────
// ⛔ 必须区分两件事，混为一谈会让门禁被一个目录名绕过：
//   ① 定位不到 app  → 无法验证 → FAIL（曾经这里判 PASS，导致 apps/admin 的项目
//      11 个页面一个没查就报「新项目，跳过」，predev 放行、vite 照常起）
//   ② 定位到 app 但无 pages/ → 确实是新项目（create-project 刚跑完）→ PASS 合理

if (!APP) {
  console.log('')
  console.log('====== vue-coding self-check ======')
  console.log('⛔ FAIL: 定位不到 app 源码目录 —— 无法验证，不等于验证通过')
  console.log(describeLookup(ROOT))
  console.log('  期望结构: <root>/apps/<any>/src/  或  <root>/src/')
  console.log('  若在项目根之外执行，请 cd 到含 package.json / pnpm-workspace.yaml 的目录')
  console.log('')
  console.log('====== self-check FAIL (app not located) ======')
  process.exit(1)
}

let statPages
try { statPages = statSync(PAGES_DIR) } catch (_) { statPages = null }

if (!statPages || !statPages.isDirectory()) {
  console.log('')
  console.log('====== vue-coding self-check ======')
  console.log(`PASS: ${relative(ROOT, PAGES_DIR)} 不存在（app=${APP.appName || 'flat'} 已定位，尚无页面 → 新项目，跳过页面检查）`)
  console.log('====== self-check PASS (no pages) ======')
  process.exit(0)
}

// ─── Scope 模式：只检查单个文件 ────────────────────────────

let vueFiles
let tsFiles

if (SCOPE_FILE) {
  const scopePath = resolve(SCOPE_FILE)
  if (SCOPE_FILE.endsWith('.vue')) {
    vueFiles = existsSync(scopePath) ? [scopePath] : []
    tsFiles = []
  } else if (SCOPE_FILE.endsWith('.ts') || SCOPE_FILE.endsWith('.tsx')) {
    vueFiles = []
    tsFiles = existsSync(scopePath) ? [scopePath] : []
  } else {
    // 目录：检查该目录下所有文件
    vueFiles = walkDir(scopePath, f => f.endsWith('.vue'))
    tsFiles = walkDir(scopePath, f => f.endsWith('.ts') || f.endsWith('.tsx'))
  }
} else {
  vueFiles = walkDir(PAGES_DIR, f => {
    if (!f.endsWith('.vue')) return false
    if (LEGACY_MODULES.length > 0) {
      for (const mod of LEGACY_MODULES) {
        if (f.includes(join('pages', mod))) return false
      }
    }
    return true
  })
  tsFiles = walkDir(SRC_DIR, f => f.endsWith('.ts') || f.endsWith('.tsx'))
}

console.log('')
console.log('====== vue-coding self-check ======')
if (SCOPE_FILE) {
  console.log(`  [scope] ${SCOPE_FILE}`)
}
if (LEGACY_MODULES.length > 0) {
  console.log(`  ℹ️  Legacy 模块免检 (${LEGACY_MODULES.length}): ${LEGACY_MODULES.join(', ')}`)
  console.log('  (使用 --strict 全量检查)')
}

// ─── 1. 组件禁用 ──────────────────────────────────────────

console.log('')
console.log('--- 组件禁用 ---')

const aTableMatches = grepFiles(vueFiles, /<a-table/)
aTableMatches.length
  ? (aTableMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('使用了 <a-table>，应改为 <QBigTable>'))
  : pass('无 <a-table>')

const aFormMatches = grepFiles(vueFiles, /<a-form/)
aFormMatches.length
  ? (aFormMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('使用了 <a-form>，应改为 <QForm schemas>'))
  : pass('无 <a-form>')

const uploadMatches = grepFiles(vueFiles, /<(a-upload|el-upload)/)
uploadMatches.length
  ? (uploadMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('使用了 <a-upload>/<el-upload>，应改为 <QUpload>'))
  : pass('上传组件正确')

// ─── 2. 列格式 ────────────────────────────────────────────

console.log('')
console.log('--- 列格式 ---')

const colMatches = grepFiles(vueFiles, /dataIndex|customRender/)
colMatches.length
  ? (colMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('使用了 antdv 列格式 (dataIndex/customRender)，QBigTable 底层是 vxe-table，应用 field/slots'))
  : pass('vxe-table 列格式正确')

// ─── 3. 样式 ──────────────────────────────────────────────

console.log('')
console.log('--- 样式 ---')

const inlineStyleMatches = []
// 行内 style 仅在含硬编码颜色/像素尺寸时判 FAIL;
// color: hsl(var(--token)) / border: 1px solid hsl(var(--line)) 等"主题 token 引用"是合法 pattern
for (const f of vueFiles) {
  const content = readFile(f)
  const lines = content.split('\n')
  for (let i = 0; i < lines.length; i++) {
    if (!/style="[^"]*"/.test(lines[i]) || /:style|v-bind/.test(lines[i]) || /<style/.test(lines[i])) continue
    const m = lines[i].match(/style="([^"]*)"/)
    if (!m) continue
    const styleContent = m[1]
    // 去掉所有 hsl(var(...)) / var(...) token 引用
    const stripped = styleContent
      .replace(/hsl\(\s*var\([^)]+\)\s*(?:\/\s*[\d.]+)?\s*\)/g, '')
      .replace(/var\(\s*--[^)]+\s*\)/g, '')
    // 剩下若有 hex 颜色 / rgb() / 命名颜色 → FAIL
    if (/#[0-9a-fA-F]{3,8}|rgba?\([^)]*\)|\b(red|blue|green|black|white|gray|grey)\b/.test(stripped)) {
      inlineStyleMatches.push({ file: relative(ROOT, f), lineNum: i + 1, line: lines[i].trim() })
    }
  }
}
inlineStyleMatches.length
  ? (inlineStyleMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('存在行内 style="..."含硬编码颜色,应改为 hsl(var(--token)) 或 UnoCSS class'))
  : pass('无硬编码行内样式')

const cssVarMatches = grepFiles(vueFiles, /var\(--(accent|border|surface|muted|danger)\)/)
cssVarMatches.length
  ? (cssVarMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('使用了不存在的原型 CSS 变量，应改用 hsl(var(--xxx))'))
  : pass('CSS Token 正确')

// ─── 4. 国际化 ────────────────────────────────────────────

console.log('')
console.log('--- 国际化 ---')

const chineseMatches = []
// 只对"严重暴露"的硬编码中文判 FAIL：
//   1. <a-button>xxx</a-button> / <a-tag>xxx</a-tag> / <a-breadcrumb-item>xxx</a-breadcrumb-item> 等可见 UI 文本节点
//   2. Modal.xxx({ title: '中', content: '中' })  / message.xxx('中')
//   3. :placeholder="'中'"  / :label="'中'"
// 跳过：注释 / t() 调用 / locale 文件 / JSX 内部 slots （后续 prototype→t() 重构）
const CHINESE_VISIBLE_CONTEXT_RE = /(<a-button[^>]*>[\s\S]*?[一-鿿]|<a-tag[^>]*>[一-鿿]|<a-breadcrumb-item[^>]*>[一-鿿]|Modal\.\w+\(\s*\{[^}]*title\s*:\s*['"][一-鿿]|Modal\.\w+\(\s*\{[^}]*content\s*:\s*['"][一-鿿]|message\.\w+\(\s*['"][一-鿿]|placeholder\s*=\s*['"][一-鿿]|label\s*=\s*['"][一-鿿])/
for (const f of vueFiles) {
  const content = readFile(f)
  const lines = content.split('\n')
  for (let i = 0; i < lines.length; i++) {
    if (/[一-鿿]/.test(lines[i])) {
      const line = lines[i].trim()
      if (line.includes('locale') || line.includes('locales') || /t\s*\(/.test(line) || line.startsWith('//') || line.startsWith('*')) continue
      // 仅在这一行命中"可见 UI 上下文"才记 FAIL
      if (CHINESE_VISIBLE_CONTEXT_RE.test(lines[i])) {
        chineseMatches.push({ file: relative(ROOT, f), lineNum: i + 1, line })
      }
    }
  }
}
chineseMatches.length
  ? (chineseMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail("页面存在硬编码中文，应改为 t('module.key')"))
  : pass('无硬编码中文')

const dollarTMatches = grepFiles(vueFiles, /\$t\(/)
dollarTMatches.some(m => /title/i.test(m.line))
  ? (dollarTMatches.filter(m => /title/i.test(m.line)).forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('definePage title 含 $t()，应直接写字符串 key（框架内部自动调用 t()）'))
  : pass('title 正确')

// ─── 5. 布局 ──────────────────────────────────────────────

console.log('')
console.log('--- 布局 ---')

const wrapperCount = grepFiles(vueFiles, /class="h-full p-\[16px] bg-white rounded"/).length
const pageVueFiles = vueFiles.filter(f => !f.includes(join('pages', 'login')))
if (wrapperCount === 0 && pageVueFiles.length > 0) {
  fail('wrapper class 缺失，每个页面根 div 必须 class="h-full p-[16px] bg-white rounded"')
} else {
  pass('wrapper class 存在')
}

// ─── 6. 组件前缀 ──────────────────────────────────────────

console.log('')
console.log('--- 组件前缀 ---')

// QBigTable 必须从 @/components/q-big-table 导入
for (const f of vueFiles) {
  const content = readFile(f)
  if (/\bQBigTable\b/.test(content)) {
    if (!content.includes("@/components/q-big-table")) {
      fail(`${relative(ROOT, f)}: QBigTable 未从 @/components/q-big-table 导入`)
    }
  }
}
// QForm 必须从 @/components/q-form 导入
for (const f of vueFiles) {
  const content = readFile(f)
  if (/\bQForm\b/.test(content)) {
    if (!content.includes("@/components/q-form")) {
      fail(`${relative(ROOT, f)}: QForm 未从 @/components/q-form 导入`)
    }
  }
}
// 禁止无 Q 前缀的组件导入
for (const comp of ['Upload', 'FileList', 'RemoteSelect']) {
  const re = new RegExp(`import\\s+${comp}\\b`)
  const matches = grepFiles(vueFiles, re).filter(m => !m.line.includes(`Q${comp}`))
  if (matches.length) {
    matches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`))
    fail(`组件 ${comp} 缺少 Q 前缀，应 import Q${comp}`)
  }
}

// ─── 7. 组件属性 ──────────────────────────────────────────

console.log('')
console.log('--- 组件属性 ---')

const toolbarMatches = grepFiles(vueFiles, /toolbarConfig:\s*\{/)
toolbarMatches.length
  ? (toolbarMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('toolbarConfig 是对象，应为 ToolbarButton[] 数组'))
  : pass('toolbarConfig 正确')

const heightAutoMatches = grepFiles(vueFiles, /height="auto"/)
heightAutoMatches.length
  ? (heightAutoMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('QBigTable 使用了 height="auto"'))
  : pass('height 正确')

const tagColorMatches = grepFiles(vueFiles, /a-tag.*color="#/)
tagColorMatches.length
  ? (tagColorMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('a-tag 使用了 hex 颜色，应使用 Antd 内置色'))
  : pass('a-tag 颜色正确')

const aSpinMatches = grepFiles(vueFiles, /:loading=/).filter(m => m.line.includes('a-spin'))
aSpinMatches.length
  ? (aSpinMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('a-spin 用了 :loading，应改为 :spinning (antdv 4.x)'))
  : pass('a-spin 属性正确')

const vModelValueMatches = grepFiles(vueFiles, /v-model:value=/).filter(m => /a-input-number|a-select/.test(m.line))
vModelValueMatches.length
  ? (vModelValueMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('antdv 4.x 应使用 :value + @update:value 替代 v-model:value'))
  : pass('v-model 兼容')

// ─── 7b. FormSchema 字段名 + QRemoteSelect/QUpload 用法（P0 三源防漂移）───

console.log('')
console.log('--- FormSchema / QRemoteSelect / QUpload 门禁 ---')

// 7b-1. schema 项用了 props: 而非 componentProps:（QForm 真实字段名）
// 匹配 schema-style 对象内 props: 紧跟 component/component:'Input'/'Select'/'Switch' 等控件标志
const schemaPropsMatches = []
for (const f of vueFiles) {
  const content = readFile(f)
  const lines = content.split('\n')
  for (let i = 0; i < lines.length; i++) {
    // FormSchema 上下文标志：行内有 field/component + props: 字面量（非 componentProps）
    if (/\bprops:\s*\{/.test(lines[i]) && /component\s*:|field\s*:/.test(lines[i])) {
      schemaPropsMatches.push({ file: relative(ROOT, f), lineNum: i + 1, line: lines[i].trim() })
    }
    // 多行情况：component: 'Input', 后续几行内出现 props: {
    if (/component\s*:\s*['"][A-Za-z]+['"]/.test(lines[i]) || /component\s*:\s*Q[A-Z]/.test(lines[i])) {
      for (let j = i + 1; j < Math.min(i + 6, lines.length); j++) {
        if (/^\s*props:\s*\{/.test(lines[j])) {
          schemaPropsMatches.push({ file: relative(ROOT, f), lineNum: j + 1, line: lines[j].trim() })
          break
        }
        if (/^\s*componentProps:\s*\{/.test(lines[j])) break  // 已写对，跳过
      }
    }
  }
}
schemaPropsMatches.length
  ? (schemaPropsMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('FormSchema 项用了 props:（应为 componentProps:，QForm 真实字段名）'))
  : pass('FormSchema 字段名正确')

// 7b-2. Switch schema 项把 valuePropName/updateEventName 塞进 componentProps（应放 schema 顶层）
const switchInComponentPropsMatches = []
for (const f of vueFiles) {
  const content = readFile(f)
  // 粗糙块扫：component: 'Switch' 后续 componentProps 块内出现 valuePropName/updateEventName
  const re = /component\s*:\s*['"]Switch['"][\s\S]{0,400}?componentProps\s*:\s*\{[^}]*\b(valuePropName|updateEventName)\b/
  if (re.test(content)) {
    const lines = content.split('\n')
    const idx = lines.findIndex(l => /component\s*:\s*['"]Switch['"]/.test(l))
    switchInComponentPropsMatches.push({ file: relative(ROOT, f), lineNum: idx + 1, line: lines[idx].trim() })
  }
}
switchInComponentPropsMatches.length
  ? (switchInComponentPropsMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('Switch schema 把 valuePropName/updateEventName 塞进 componentProps（应放 schema 顶层）'))
  : pass('Switch 配置位置正确')

// 7b-3. QRemoteSelect 用 component: 形式而非 render（v-model 不匹配导致不回填）
const qremoteComponentMatches = grepFiles(vueFiles, /component\s*:\s*QRemoteSelect\b/)
qremoteComponentMatches.length
  ? (qremoteComponentMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('QRemoteSelect 用 component: 形式（v-model 是 modelValue，必须用 render: (model, setFormModel) => h(QRemoteSelect, {...})，见 constraints/qform-props.json custom_component_usage）'))
  : pass('QRemoteSelect 嵌入方式正确')

// 7b-4. QUpload schema 用了不存在的 mode prop
const quploadModeMatches = []
for (const f of vueFiles) {
  const content = readFile(f)
  const re = /component\s*:\s*QUpload[\s\S]{0,300}?\bprops\s*:\s*\{[^}]*\bmode\b/
  const re2 = /component\s*:\s*QUpload[\s\S]{0,300}?componentProps\s*:\s*\{[^}]*\bmode\b/
  if (re.test(content) || re2.test(content)) {
    const lines = content.split('\n')
    const idx = lines.findIndex(l => /component\s*:\s*QUpload/.test(l))
    quploadModeMatches.push({ file: relative(ROOT, f), lineNum: idx + 1, line: lines[idx].trim() })
  }
}
quploadModeMatches.length
  ? (quploadModeMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('QUpload schema 用了 mode prop（不存在，应改 manual: boolean，见 constraints/qupload-props.json）'))
  : pass('QUpload props 正确')

// ─── 8. 调试残留 ──────────────────────────────────────────

console.log('')
console.log('--- 调试残留 ---')

const debuggerMatches = [...grepFiles(vueFiles, /debugger/), ...grepFiles(tsFiles, /debugger/)]
debuggerMatches.length
  ? (debuggerMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('debugger 残留'))
  : pass('无 debugger')

const consoleLogMatches = [...grepFiles(vueFiles, /console\.log/), ...grepFiles(tsFiles, /console\.log/)]
consoleLogMatches.length
  ? (consoleLogMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), warn('console.log 残留（建议清理）'))
  : pass('无 console.log')

// ─── 8b. 禁止 fetch / axios / XMLHttpRequest / uni.request（adapter-api.md:68/93 三源铁律）───

console.log('')
console.log('--- 网络请求方式门禁 ---')

// 禁止在 src 内直接发请求：必须用 @q-web-plugin/request 的 request 实例（查询类经 AIRequestGuard 包裹）
// 命中即 FAIL：fetch( / import 'axios' / require('axios') / new XMLHttpRequest( / uni.request(
// 跳过 node_modules（tsFiles 已排除）与 md/json
const NET_FORBID_RE = /\bfetch\s*\(|\bimport\s+['"]axios['"]|\brequire\s*\(\s*['"]axios['"]\s*\)|new\s+XMLHttpRequest\s*\(|\buni\.request\s*\(/
const netForbiddenMatches = [...grepFiles(vueFiles, NET_FORBID_RE), ...grepFiles(tsFiles, NET_FORBID_RE)]
netForbiddenMatches.length
  ? (netForbiddenMatches.forEach(m => console.log(`  ${m.file}:${m.lineNum}  ${m.line}`)), fail('禁止直接用 fetch/axios/XMLHttpRequest/uni.request 发请求，统一用 import { request } from \'@q-web-plugin/request\'（查询类包 AIRequestGuard，见 adapter-api.md:68/93）'))
  : pass('网络请求走 request 实例')

// ─── 8c. 根 env VITE_API_DEFAULT_SERVICE_BASE_URL 不允许空（env-rules.md:21 / prototype-to-code.md:153）

console.log('')
console.log('--- 服务上下文配置 ---')

// STEP 04.6 应向用户确认服务端上下文路径后写入根 env/.env 的 VITE_API_DEFAULT_SERVICE_BASE_URL。
// 空值 = 模板默认未配置 = 联调必然 404，必须在 self-check 拦下，不能让 AI 跳过问询一路跑到 pnpm dev。
const rootEnvPath = join(ROOT, 'env', '.env')
const emptyServiceBaseMatches = []
if (existsSync(rootEnvPath)) {
  const m = readFile(rootEnvPath).match(/^VITE_API_DEFAULT_SERVICE_BASE_URL\s*=\s*['"]?([^'"\n]*)['"]?\s*$/m)
  if (m && m[1].trim() === '') {
    emptyServiceBaseMatches.push({ file: relative(ROOT, rootEnvPath), line: 'VITE_API_DEFAULT_SERVICE_BASE_URL = ' + (m[1] || "''") })
  }
}
emptyServiceBaseMatches.length
  ? (emptyServiceBaseMatches.forEach(m => console.log(`  ${m.file}  ${m.line}`)), fail('根 env/.env 的 VITE_API_DEFAULT_SERVICE_BASE_URL 为空，联调会全部 404（STEP 04.6 必须向用户确认服务端上下文路径后写入，如 /crm/，见 env-rules.md:21 / prototype-to-code.md:153）'))
  : pass('VITE_API_DEFAULT_SERVICE_BASE_URL 已配置')

// ─── 9. definePage 规范 ───────────────────────────────────

console.log('')
console.log('--- definePage 规范 ---')

for (const f of vueFiles) {
  const content = readFile(f)
  const m = content.match(/definePage\(\s*\{/)
  if (!m) continue

  // 9a. satisfies RouteMeta 检查（⛔ FAIL）
  if (!content.includes('satisfies RouteMeta')) {
    fail(`${relative(ROOT, f)}: definePage 缺少 satisfies RouteMeta，需 import type { RouteMeta } from 'vue-router' 并在闭合 ) 前加 satisfies RouteMeta`)
  }

  // 9b. meta 包裹检查
  const after = content.slice(m.index)
  const metaIdx = after.indexOf('meta:')
  const layoutIdx = after.indexOf('layout:')
  if (layoutIdx >= 0 && (metaIdx < 0 || layoutIdx < metaIdx)) {
    fail(`${relative(ROOT, f)}: definePage 中 layout/title/menu 等必须在 meta: {} 内包裹`)
  }

  // 9c. title 禁止 page. 前缀（routes.md 形态 A/B/C 共同铁律）
  //     框架运行时 t('page.' + meta.title)；写 'page.xxx' → t('page.page.xxx') → undefined
  const titleM = after.match(/title\s*:\s*['"]([^'"]+)['"]/)
  if (titleM && /^page\./.test(titleM[1])) {
    fail(`${relative(ROOT, f)}: definePage title='${titleM[1]}' 含 page. 前缀，框架会再拼一次，应去掉前缀写成 '${titleM[1].replace(/^page\./, '')}'（见 routes.md definePage 5 条铁律 #3）`)
  }

  // 9d. 子级 layout 必须为 false（routes.md 铁律一句话）
  //     判定此 .vue 是否为"子级"：
  //       - 父目录有 dir.meta.yaml → 子级
  //       - .vue 在 pages/{X}/{Y}/... 多层子目录下且 pages/{X}/ 有 dir.meta.yaml → 子级
  //       - .vue 直接在 pages/ 下（如 home.vue）或 pages/{X}/index.vue 且 pages/{X}/ 无 dir.meta.yaml
  //         → 顶级独立页，layout 可 'default'
  //     最简判定：从 .vue 父目录沿 pages 路径向上找，若任何中间目录有 dir.meta.yaml → 子级
  //     detail.vue 一律子级（详情不进菜单）
  const parentDir = dirname(f)
  const isUnderAnyMeta = (() => {
    let d = parentDir
    for (let i = 0; i < 8; i++) {
      if (existsSync(join(d, 'dir.meta.yaml'))) return true
      const parent = dirname(d)
      if (parent === d || !parent.startsWith(PAGES_DIR)) break
      d = parent
    }
    return false
  })()
  const isDetail = basename(f) === 'detail.vue' || (basename(f) === 'index.vue' && basename(parentDir) === 'detail')
  const layoutM = after.match(/layout\s*:\s*(?:'([^']+)'|"([^"]+)"|(\w+))/)
  const layoutVal = layoutM ? (layoutM[1] || layoutM[2] || layoutM[3]) : null
  if ((isUnderAnyMeta || isDetail) && layoutVal && layoutVal !== 'false' && layoutVal !== false) {
    fail(`${relative(ROOT, f)}: 本页是子级（父级链含 dir.meta.yaml${isDetail ? ' 或本页是 detail.vue' : ''}），layout 必须为 false（当前: ${layoutVal}）。仅顶级分组 dir.meta.yaml 用 layout: default，见 routes.md 铁律`)
  }

  // 9e. definePage.meta.menu 对象形式仅允许"顶级独立页"（无任何父级 dir.meta.yaml）
  //     - 父级链含 dir.meta.yaml 时 menu 对象 = FAIL（菜单图标属于 dir.meta.yaml menu.icon，不进 definePage）
  //     - 顶级独立页（如 home.vue / pages/{X}/index.vue 且祖先无 dir.meta.yaml）menu 可对象
  //     - home.vue 是 routes.md:288 明文例外：menu: { icon: 'q-icon:home-linear' }
  const menuObjM = after.match(/meta\s*:\s*\{[\s\S]*?\bmenu\s*:\s*\{/)
  if (menuObjM && isUnderAnyMeta) {
    fail(`${relative(ROOT, f)}: 父级链含 dir.meta.yaml，本页是子级，definePage meta.menu 不能是对象形式（menu: { icon: ... }）。菜单图标属于顶级分组 dir.meta.yaml 的 menu.icon，子级 definePage 只写 menu: true 或 menu: false，见 routes.md §1/§4`)
  }

  // 9g. 形态 A 配置核对（pages/{X}/index.vue 祖先无 dir.meta.yaml = 顶级独立页）
  //     routes.md 形态 A 明文合法态：layout: 'default' + menu 任意（true/false/{icon}）
  //     - layout 非 'default' → FAIL（形态 A 只有 default 一种合法 layout；走嵌套该移到 pages/{parent}/{X}/index.vue 并加父级 dir.meta.yaml）
  //     - menu 值不查（true 进菜单 / false 独立单页不进 / { icon } 进菜单带图标，三种都合法）
  //     home.vue 等顶级单文件页不触发此规则（basename != index.vue）
  const isIndexLeaf = basename(f) === 'index.vue'
  const parentOfPagesChild = relative(PAGES_DIR, parentDir)
  const isOneLevelUnderPages = parentOfPagesChild && !parentOfPagesChild.includes(sep) && /^[a-z][\w-]*$/.test(parentOfPagesChild)
  if (isIndexLeaf && isOneLevelUnderPages && !isUnderAnyMeta) {
    // 框架内置顶级独立页（login 等）豁免：与 9f 同组
    const rel = relative(ROOT, f).replace(/\\/g, '/')
    if (!/\/(login|error-pages)\//.test(rel)) {
      if (layoutVal && layoutVal !== 'default' && layoutVal !== "'default'") {
        fail(`${relative(ROOT, f)}: 形态 A 顶级独立页（pages/${parentOfPagesChild}/index.vue，祖先无 dir.meta.yaml）layout 必须为 'default'（当前: ${layoutVal}）。若本页是某分组下的子级 → 移到 pages/{parent}/${parentOfPagesChild}/index.vue 并在 pages/{parent}/ 建 dir.meta.yaml（layout: default + menu.icon），本页改 layout: false。见 routes.md 形态 A/B`)
      }
    }
  }

  // 9h. 父级有 dir.meta.yaml 但本页是平级散落 .vue 叶子（非 index.vue 非 detail.vue）
  //     → 必须显式 menu: false，否则容易意外进菜单或与兄弟 index.vue 冲突
  //     合法散落叶子：components 同级抽出的辅助 .vue 应放 components/，不放 pages/
  if (isUnderAnyMeta && !isIndexLeaf && !isDetail) {
    const menuFalseM = after.match(/meta\s*:\s*\{[\s\S]*?\bmenu\s*:\s*false/)
    if (!menuFalseM) {
      fail(`${relative(ROOT, f)}: 父级链含 dir.meta.yaml，本页是散落 .vue 叶子（非 index.vue/detail.vue），必须显式 meta.menu: false 防止意外进菜单或与兄弟 index.vue 冲突。若本页是路由可达的散落叶页（如 pages/X/aux.vue）→ 加 menu: false；若是辅助片段/局部组件 → 移到 pages/X/components/ 下，见 routes.md 布局规则`)
    }
  }

  // 9i. 禁平级 detail.vue（routes.md 实证：详情统一用 detail/index.vue 目录形态）
  //     框架 demo pages/demos/vxe/detail/index.vue 即此形态；平级 detail.vue 与
  //     "模块内 .vue 平级放置"的列表兄弟混排易冲突，统一目录形态
  if (isDetail && basename(parentDir) !== 'detail') {
    fail(`${relative(ROOT, f)}: 详情页必须用 detail/index.vue 目录形态（如 pages/X/detail/index.vue），禁止平级 detail.vue。把本文件移到同级 detail/ 目录下并改名 index.vue。见 routes.md 形态 C`)
  }

  // 9j. detail/index.vue 必须配 menu: false + layout: false（不进菜单、子级 layout）
  //     detail 形态固定语义：详情不进菜单 + 子页 layout
  if (isDetail && basename(parentDir) === 'detail') {
    if (layoutVal && layoutVal !== 'false' && layoutVal !== false) {
      fail(`${relative(ROOT, f)}: detail/index.vue layout 必须为 false（当前: ${layoutVal}）。详情是子级页，见 routes.md 形态 C`)
    }
    const detailMenuFalseM = after.match(/meta\s*:\s*\{[\s\S]*?\bmenu\s*:\s*false/)
    if (!detailMenuFalseM) {
      const detailMenuTrueM = after.match(/meta\s*:\s*\{[\s\S]*?\bmenu\s*:\s*true/)
      const detailMenuObjM = after.match(/meta\s*:\s*\{[\s\S]*?\bmenu\s*:\s*\{/)
      if (detailMenuTrueM || detailMenuObjM) {
        fail(`${relative(ROOT, f)}: detail/index.vue 必须设 menu: false，详情页不进菜单（当前: ${detailMenuObjM ? '对象形式' : 'true'}）。见 routes.md 形态 C`)
      }
    }
  }

  // 9f. definePage.name 与 defineOptions.name 必须一致（routes.md 5 条铁律 #2）
  //     一方缺、或字符串不等 → 'Duplicate named routes' 运行时报错
  const dpNameM = content.match(/definePage\(\s*\{[\s\S]*?name\s*:\s*['"]([^'"]+)['"]/)
  const doNameM = content.match(/defineOptions\s*\(\s*\{\s*name\s*:\s*['"]([^'"]+)['"]/)
  if (dpNameM && doNameM && dpNameM[1] !== doNameM[1]) {
    fail(`${relative(ROOT, f)}: definePage.name='${dpNameM[1]}' 与 defineOptions.name='${doNameM[1]}' 不一致，会触发 'Duplicate named routes'，见 routes.md 5 条铁律 #2`)
  } else if (dpNameM && !doNameM) {
    // 框架内置页（login 等）豁免：路径含 /login 或 /error-pages
    const rel = relative(ROOT, f).replace(/\\/g, '/')
    if (!/\/(login|error-pages)\//.test(rel)) {
      fail(`${relative(ROOT, f)}: definePage.name='${dpNameM[1]}' 但缺 defineOptions({ name: ... })，业务页两者必设且一致，见 routes.md 5 条铁律 #2`)
    }
  }
}

const chineseFiles = walkDir(PAGES_DIR, f => /[一-鿿]/.test(f)).map(f => relative(ROOT, f))
chineseFiles.length
  ? (chineseFiles.forEach(f => console.log(`  ${f}`)), fail('文件名含中文'))
  : pass('文件名正确')

// ─── 10. 叶子模块 dir.meta.yaml 多余门禁（P2-1）────────────

console.log('')
console.log('--- 叶子模块 dir.meta.yaml ---')

// 扫 pages 目录判 dir.meta.yaml 是否符合 routes.md：
//   - layout: default  → 顶级分组：合法（含 detail.vue 兄弟或其他 .vue 兄弟都允许）
//   - layout: false    → 嵌套子分组/叶子：合法（routes.md:61-66 三级菜单中间层形态）
//   - 无 dir.meta.yaml → 跳过
// 仅当 dir.meta.yaml 存在但 layout 字段缺失/为空时才 FAIL（不符合任何一类模板）
const leafMetaMatches = []
function scanLeafMeta(dir) {
  try {
    const entries = readdirSync(dir)
    const hasMeta = entries.includes('dir.meta.yaml')
    if (hasMeta) {
      const metaContent = readFile(join(dir, 'dir.meta.yaml'))
      const layoutMatch = metaContent.match(/^layout\s*:\s*(\S+)/m)
      const layoutVal = layoutMatch ? layoutMatch[1].replace(/^['"]|['"]$/g, '') : ''
      // 合法值：default / false（routes.md 只允许这两种）
      if (layoutVal !== 'default' && layoutVal !== 'false') {
        leafMetaMatches.push({ dir: relative(ROOT, dir), layout: layoutVal || '(missing)' })
      }
    }
    // 继续递归子目录
    for (const e of entries) {
      const sub = join(dir, e)
      try { if (statSync(sub).isDirectory() && e !== 'components' && e !== 'node_modules') scanLeafMeta(sub) } catch (_) {}
    }
  } catch (_) { /* perm */ }
}
scanLeafMeta(PAGES_DIR)
leafMetaMatches.length
  ? (leafMetaMatches.forEach(m => console.log(`  ${m.dir}/dir.meta.yaml  layout=${m.layout}`)), fail('dir.meta.yaml layout 字段非法（routes.md 只允许 default / false，见 routes.md:15/61）'))
  : pass('dir.meta.yaml layout 合法')

// ─── 11. i18n page namespace 模块名铁律（P2-2）────────────

console.log('')
console.log('--- i18n page namespace 模块名 ---')

// 加载 entities.json 模块名集合（kebab-case 单一源）
const entitiesPath = stateReadPath(ROOT, 'entities.json')
let validModuleNames = new Set()
if (existsSync(entitiesPath)) {
  try {
    const e = JSON.parse(readFileSync(entitiesPath, 'utf-8'))
    if (e.module) validModuleNames.add(e.module)
    const collect = (arr) => {
      if (!Array.isArray(arr)) return
      arr.forEach(m => {
        if (!m) return
        if (m.module) validModuleNames.add(m.module)   // entities.json schema: modules[].module
        if (m.name) validModuleNames.add(m.name)        // 兼容别名
      })
    }
    collect(e.modules)
    collect(e.pcModules)        // PC 端专属模块（workbench / report-pc 等）
    collect(e.mobileModules)
    collect(e.sharedModules)    // 跨端共享模块（relation-entry / progress-board 等）
  } catch (_) { /* ignore */ }
}

// route-meta 白名单：page.* 顶层 key 既可以是 module 名，也可以是 route-meta 的菜单标题
// 后者不强制对齐 entities.json，按 i18n.md「page.* — 菜单标题」约定放行
const ROUTE_META_KEYS = new Set([
  'login', 'home', 'dashboard', '404', '403', '500',
  // 兜底:entities.json 已声明的模块都视为合法(避免双重报错)
  ...validModuleNames,
])

// 栈匹配抓 { ... } 完整块（从 openBraceIdx 起始的 { 到对应 } 闭合）
function extractBraceBlock(content, openBraceIdx) {
  let depth = 0
  for (let i = openBraceIdx; i < content.length; i++) {
    if (content[i] === '{') depth++
    else if (content[i] === '}') {
      depth--
      if (depth === 0) return content.slice(openBraceIdx, i + 1)
    }
  }
  return null
}

// 在 block 内（不含外层 { }）抓深度 0 的顶层 key + value 形态。
// 返回 [{ key, isObject }]：isObject=true 表示 value 是 { } / [ ]（即真 namespace 嵌套）。
// i18n.md:120/129 区分：
//   - value 是字符串 → page.* 菜单标题，只查 kebab-case
//   - value 是对象   → page.{module}.* 业务 namespace，{module} 必须对齐 entities.json.module
function extractTopLevelKeys(block) {
  const keys = []
  const inner = block.slice(1, -1)
  let i = 0
  while (i < inner.length) {
    const rest = inner.slice(i)
    // 匹配 'xxx': / "xxx": / xxx: （value 后面是 { 或 ' 或 " 或 数字 或 true/false/null）
    const m = rest.match(/^(['"]?)([a-zA-Z0-9_-]+)\1\s*:\s*/)
    if (!m) { i++; continue }
    const key = m[2]
    // 跳过 key + ':' + 空白
    let j = i + m[0].length
    // 跳过 value
    if (inner[j] === '{' || inner[j] === '[') {
      // 嵌套对象/数组：用栈找到闭合
      const block2 = extractBraceBlock(inner, j)
      if (!block2) break
      keys.push({ key, isObject: true })
      j += block2.length
    } else if (inner[j] === "'" || inner[j] === '"') {
      // 字符串：跳到对应闭合引号（含转义）
      const q = inner[j]
      let k = j + 1
      while (k < inner.length) {
        if (inner[k] === '\\') { k += 2; continue }
        if (inner[k] === q) { k++; break }
        k++
      }
      keys.push({ key, isObject: false })
      j = k
    } else {
      // 数字 / true / false / null：跳到逗号或换行
      let k = j
      while (k < inner.length && !/[,\n\r]/.test(inner[k])) k++
      keys.push({ key, isObject: false })
      j = k
    }
    // 跳过尾逗号
    while (j < inner.length && /[,\s]/.test(inner[j])) j++
    i = j
  }
  return keys
}

// 单复数都候选（脚手架里两种都出现过，真实项目是单数 locale）；app 名由 paths.mjs 解析，不写死
const localeDirs = resolveLocaleDirs(ROOT)
const i18nModuleMismatches = []
const i18nCamelCaseMatches = []
for (const locDir of localeDirs) {
  if (!existsSync(locDir)) continue
  const localeFiles = walkDir(locDir, f => f.endsWith('.ts') || f.endsWith('.json'))
  for (const f of localeFiles) {
    const content = readFile(f)
    // 找所有 `page: {` 出现位置（可能多次 export/合并）
    let searchFrom = 0
    while (true) {
      const pageMatch = content.slice(searchFrom).match(/\bpage\s*:\s*\{/)
      if (!pageMatch) break
      const pageIdx = searchFrom + pageMatch.index
      const braceIdx = content.indexOf('{', pageIdx)
      const block = extractBraceBlock(content, braceIdx)
      if (!block) break
      const keys = extractTopLevelKeys(block)
      for (const item of keys) {
        const k = item.key
        if (!/^[a-z]/.test(k)) continue  // 跳过非 namespace 形式
        // kebab-case 铁律：含大写字母直接 FAIL（如 systemConfigCenter 应为 system-config-center）
        if (/[A-Z]/.test(k)) {
          i18nCamelCaseMatches.push({ file: relative(ROOT, f), module: k })
          continue
        }
        // value 是对象才是真 page.{module}.* 业务 namespace，{module} 必须对齐 entities.json.module。
        // value 是字符串/数字 = page.* 菜单标题（如 'plugins-detail': '插件详情'），不强制对齐。
        if (!item.isObject) continue
        if (ROUTE_META_KEYS.has(k)) continue
        if (validModuleNames.size > 0 && !validModuleNames.has(k)) {
          i18nModuleMismatches.push({ file: relative(ROOT, f), module: k })
        }
      }
      searchFrom = braceIdx + block.length
    }
  }
}
i18nCamelCaseMatches.length
  ? (i18nCamelCaseMatches.forEach(m => console.log(`  ${m.file}: page.${m.module}.*`)), fail('i18n page.{module}.* 含大写字母（必须 kebab-case，如 systemConfigCenter 应为 system-config-center）'))
  : pass('i18n page namespace kebab-case 正确')
i18nModuleMismatches.length
  ? (i18nModuleMismatches.forEach(m => console.log(`  ${m.file}: page.${m.module}.*`)), fail(`i18n page.{module}.* 的 {module} 不在 entities.json 模块集合 (${[...validModuleNames].join(', ') || '空'})，请与 entities.json.module 对齐`))
  : pass('i18n page namespace 模块名一致')

// ─── 结果 ─────────────────────────────────────────────────

console.log('')
console.log('====== self-check done ======')
console.log(`  FAIL: ${failCount}  WARN: ${warnCount}`)
console.log('')

if (failCount > 0) {
  console.log('⛔ self-check FAIL — 修复以上 FAIL 项后重新运行 dev')
  process.exit(1)
} else {
  console.log('✅ self-check PASS')
  process.exit(0)
}
