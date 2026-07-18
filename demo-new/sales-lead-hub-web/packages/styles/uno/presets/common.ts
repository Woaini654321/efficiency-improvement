import type { Theme, Colors } from '../types'
import { definePreset } from 'unocss'
import { commonTheme } from '../theme/common'
import { WIND4_BLOCKLIST_NAMESPACE, resolveToken, withAlpha, genAutocompleteConf } from '../utils'

interface CommonTheme extends Theme {
  colors: Colors
}

export const presetCommon = definePreset<CommonTheme>(() => {
  return {
    name: 'common-preset',

    // 约定 WIND4_BLOCKLIST_NAMESPACE 下的 color 不参与 Wind4 原子类生成，避免一些奇怪的原子类生成
    blocklist: [new RegExp(`/-${WIND4_BLOCKLIST_NAMESPACE}-/`)],
    shortcuts: [],

    rules: [
      /**
       * ======================================
       * 背景色：`bg`（裸类 & 模糊路径）
       * ======================================
       * 匹配：
       *  - `bg`                    → 等价取 `bg.DEFAULT`
       *  - `bg-card`               → 取 `colors[WIND4_BLOCKLIST_NAMESPACE].bg.card`
       *  - `bg-sidebar`            → 取 `colors[WIND4_BLOCKLIST_NAMESPACE].bg.sidebar.DEFAULT`（对象默认降级）
       *  - `bg-sidebar-sub`        → 别名映射成 `sidebar.sub`
       *  - `bg-dialog-overlay`     → 任意键，只要在 `bg.*` 下能解析到即可
       *  - `bg-a.b.c`              → 点分路径
       *  - `bg-popover/80`         → 80% 不透明
       *
       * 使用示例：
       *  <div class="bg">默认背景</div>
       *  <div class="bg-card">卡片背景</div>
       *  <div class="bg-sidebar-sub">侧边子区背景</div>
       *  <div class="bg-dialog-overlay/80">弹层遮罩 80%</div>
       */
      [
        /^bg(?:-([A-Za-z0-9_.-]+))?(?:\/(\d{1,3}))?$/,
        ([, keyOrUndefined, a], { theme }) => {
          const alpha = a ? Math.max(0, Math.min(100, Number(a))) : null
          if (keyOrUndefined === 'DEFAULT') return
          const path = keyOrUndefined ?? 'DEFAULT' // 裸 bg → DEFAULT
          const wind4BlocklistColors = (theme as Theme).colors[WIND4_BLOCKLIST_NAMESPACE] as Colors
          if (!wind4BlocklistColors?.bg) return
          const color = resolveToken(wind4BlocklistColors.bg, path, {
            aliases: { 'sidebar-sub': 'sidebar.sub' }, // 你现有的别名
            preferObjectDefault: true, // 对象优先 DEFAULT
            fallbackUpwardDefault: false // 未命中时不回退祖先 DEFAULT，避免误伤 Wind4 或者其他预设中的规则
          })
          if (!color) return
          return { 'background-color': withAlpha(color, alpha) }
        },
        genAutocompleteConf(commonTheme, WIND4_BLOCKLIST_NAMESPACE, 'bg', false)
      ],

      /**
       * ======================================
       * 文本色：`text`（裸类 & 模糊路径）
       * ======================================
       * 匹配：
       *  - `text`                   → 等价取 `text.DEFAULT`
       *  - `text-secondary`         → 取 `colors[WIND4_BLOCKLIST_NAMESPACE].text.secondary`
       *  - `text-auxiliary`         → 取 `colors[WIND4_BLOCKLIST_NAMESPACE].text.auxiliary`
       *  - `text-x.y.z`             → 点分路径
       *  - `text-black/70`          → 70% 不透明
       *
       * 使用示例：
       *  <p class="text">默认文本色</p>
       *  <p class="text-secondary">次要文本色</p>
       *  <p class="text-black/70">黑色文本 70%</p>
       */
      [
        /^text(?:-([A-Za-z0-9_.-]+))?(?:\/(\d{1,3}))?$/,
        ([, keyOrUndefined, a], { theme }) => {
          const alpha = a ? Math.max(0, Math.min(100, Number(a))) : null
          if (keyOrUndefined === 'DEFAULT') return
          const path = keyOrUndefined ?? 'DEFAULT' // 裸 text → DEFAULT
          const wind4BlocklistColors = (theme as Theme).colors[WIND4_BLOCKLIST_NAMESPACE] as Colors
          if (!wind4BlocklistColors?.text) return
          const color = resolveToken(wind4BlocklistColors.text, path, {
            preferObjectDefault: true,
            fallbackUpwardDefault: false
          })
          if (!color) return
          return { color: withAlpha(color, alpha) }
        },
        genAutocompleteConf(commonTheme, WIND4_BLOCKLIST_NAMESPACE, 'text', false)
      ],
      /**
       * 阴影
       */
      [
        'box-shadow',
        {
          'box-shadow': 'var(--shadow)'
        },
        {
          autocomplete: ['box-shadow'],
          layer: 'utilities'
        }
      ]
    ],
    theme: commonTheme
  }
})
