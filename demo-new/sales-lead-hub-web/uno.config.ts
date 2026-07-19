import { wind4Theme, presetCommon } from '@q-mono-x/styles/uno'
import { presetIcon } from '@q-ui/icons'
import {
  defineConfig,
  presetWind4,
  presetAttributify,
  transformerDirectives,
  transformerVariantGroup,
  transformerAttributifyJsx
} from 'unocss'

export default defineConfig({
  safelist: ['animate-spin', 'rotate-90'], // 如果需要动态使用 Wind4 类名，需要加入到 safelist，否则相关动态类名可能无法编译进去
  presets: [
    presetAttributify({
      prefix: 'uno-'
    }),
    presetWind4({
      // 启用 Wind4 的重置与主题变量生成（默认 on-demand）
      preflights: { reset: true, theme: 'on-demand' }
    }),
    presetIcon({
      // 如果需要动态使用 图标 类名，需要加入到 safelist，否则相关动态 图标 类名可能无法编译进去
      safelist: [
        // ==== 销售商机互助平台 模块菜单图标 ====
        'q-icon:bulb-linear',
        'q-icon:document-linear',
        'q-icon:notice-linear',
        'q-icon:bookmark-linear',
        'q-icon:settings-linear',
        'q-icon:discuss-linear',
        'q-icon:like-linear',
        'q-icon:search-linear',
        'q-icon:shop-linear',
        'q-icon:calendar-linear',
        'q-icon:point-list-linear',
        // ==== 框架 demo 原有 ====
        'q-icon:home-linear',
        'q-icon:smile-linear',
        'q-icon:code-base-linear',
        'q-icon:link-linear',
        'q-icon:like-linear',
        'q-icon:ring-linear',
        'q-icon:pack-up2-linear',
        'q-icon:drop-down-linear',
        'q-icon:filter-face',
        'q-icon:edit2-linear',
        'q-icon:help-face',
        'q-icon:loading2-linear',
        'q-icon:page-right-linear',
        'q-icon:go-ahead-linear',
        'q-icon:complete-square-face',
        'q-icon:square-linear',
        'q-icon:sub-square-face',
        'q-icon:square-face',
        'q-icon:locate-more-linear',
        'q-icon:sort-linear',
        'q-icon:prohibit-linear',
        'q-icon:close-linear',
        'q-icon:load-linear',
        'q-icon:upload-linear',
        'q-icon:download-linear',
        'q-icon:full-screen2-linear',
        'q-icon:shrink2-linear',
        'q-icon:fixed-linear',
        'q-icon:fixed-face'
      ]
    }),
    presetCommon()
  ],
  theme: {
    ...wind4Theme
  },
  transformers: [transformerDirectives(), transformerVariantGroup(), transformerAttributifyJsx()]
})
