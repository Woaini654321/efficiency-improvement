import type { UserPagesConfig } from '@uni-helper/vite-plugin-uni-pages'

declare module '@uni-helper/vite-plugin-uni-pages' {
  interface PageMeta {
    /** 页面标题（中英文key） */
    title?: string
    /** 白名单：当为 true，无需登录即可访问且不受资源权限管控 */
    whitelisted?: boolean
    /** 资源权限 Code：不配置代表不受资源权限管控，只要登录即可访问 */
    authCode?: string
    /** 是否是错误页 */
    errorPage?: boolean
    /** 是否是 tabbar 页面 */
    tabbarPage?: boolean
    /** 导航栏设置 */
    navbar?: {
      back?: boolean
      title?: boolean
      close?: boolean
    }
    [key: string]: any
  }

  interface PageMetaDatum {
    path: string
    /** 使用 type: "home" 属性设置首页，其他页面不需要设置，默认为 page */
    type?: 'home' | 'page'
    /** 页面布局 */
    layout?: 'default' | 'noTabbar' | false
    /** 页面元信息（类似 vue-router 的元信息），你可以自定义任意字段 */
    meta?: PageMeta
    [p: string]: any
  }

  declare function defineUniPages(config: UserPagesConfig): UserPagesConfig
}
