import { WIND4_BLOCKLIST_NAMESPACE } from '../utils'
export const commonTheme = {
  colors: {
    /**
     * 这里和预设配置约定下：WIND4_BLOCKLIST_NAMESPACE 下的颜色不参与 Wind4 原子类生成，避免像 text-bg-main 等奇怪的原子类的生成和被使用
     */
    [WIND4_BLOCKLIST_NAMESPACE]: {
      bg: {
        DEFAULT: 'hsl(var(--bg))',
        main: 'hsl(var(--main-bg))',
        card: 'hsl(var(--card-bg))',
        popover: 'hsl(var(--popover-bg))',
        muted: 'hsl(var(--muted-bg))',
        sidebar: {
          DEFAULT: 'hsl(var(--sidebar-bg))',
          sub: 'hsl(var(--sub-sidebar-bg))'
        },
        header: 'hsl(var(--header-bg))',
        footer: 'hsl(var(--footer-bg))'
      },

      text: {
        DEFAULT: 'hsl(var(--text))',
        title: 'hsl(var(--fg))',
        secondary: 'hsl(var(--secondary-text))',
        auxiliary: 'hsl(var(--auxiliary-text))'
      }
    }
  }
}
