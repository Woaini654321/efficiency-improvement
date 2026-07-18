import { genColorStages } from '../utils'
/**
 * 覆盖 & 扩展 Wind4 的主题
 */
export const wind4Theme = {
  colors: {
    /**
     * 【覆盖】white、black 采用 --bg 和 --fg 覆盖，blue、green、orange、red 等颜色分别和品牌色（--primary）以及状态色对应
     */
    white: 'hsl(var(--bg))',
    black: 'hsl(var(--fg))',

    blue: {
      DEFAULT: 'hsl(var(--primary))',
      ...genColorStages('primary')
    },

    green: {
      DEFAULT: 'hsl(var(--success))',
      ...genColorStages('success')
    },

    orange: {
      DEFAULT: 'hsl(var(--warning))',
      ...genColorStages('warning')
    },

    red: {
      DEFAULT: 'hsl(var(--error))',
      ...genColorStages('error')
    },

    /**
     * 【扩展】primary、success、warning、error、info 等品牌色和状态色，借由 wind4 的规则自动成如：bg-primary、bg-success-300 等原子类
     */
    primary: {
      DEFAULT: 'hsl(var(--primary))',
      ...genColorStages('primary')
    },
    success: {
      DEFAULT: 'hsl(var(--success))',
      ...genColorStages('success')
    },
    warning: {
      DEFAULT: 'hsl(var(--warning))',
      ...genColorStages('warning')
    },
    error: {
      DEFAULT: 'hsl(var(--error))',
      ...genColorStages('error')
    },
    info: {
      DEFAULT: 'hsl(var(--info))',
      ...genColorStages('info')
    },

    /**
     * 【扩展】线的颜色，常用于边框，分割线
     */
    line: {
      // 稍浅一点，但更常用
      DEFAULT: 'hsl(var(--line))',
      // 主要用于表单类组件的边框
      base: 'hsl(var(--line-base))'
    },

    /**
     * 【扩展】遮罩
     */
    overlay: {
      DEFAULT: 'hsl(var(--overlay))'
    }
  },
  /**
   * 【覆盖】字体
   */
  font: {
    sans: 'var(--font-family)'
  },
  /**
   * 【覆盖】圆角
   * 覆盖前：
   * --radius-sm: 0.25rem;
   * --radius-md: 0.375rem;
   * --radius-lg: 0.5rem;
   * --radius-xl: 0.75rem
   * 覆盖后:
   * --radius: 0.5rem
   */
  radius: {
    sm: 'calc(var(--radius) - 4px)', // ===> 4px
    md: 'calc(var(--radius) - 2px)', // ===> 6px
    lg: 'var(--radius)', // ===> 8px
    xl: 'calc(var(--radius) + 4px)' // ===> 12px
  },
  /**
   * 【覆盖】响应式断点
   * 如下是同 Antd 一样设计，后面备注的是 Wind4 默认的断点
   */
  breakpoints: {
    'xs': 0,
    'sm': '576px', // 40rem ==> 640px
    'md': '768px', // 48rem ==> 768px
    'lg': '992px', // 64rem ==> 1024px
    'xl': '1200px', // 80rem ==> 1280px
    '2xl': '1600px' // 96rem ==> 1536px
  }
}
