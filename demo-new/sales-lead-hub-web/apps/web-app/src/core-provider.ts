/**
 * ==================================================
 * 基于 CoreProvider 容器给核心库（@q-cli/libs）上层插件（@q-web-plugin/request、@q-web-plugin/store、@q-web-plugin/router）的提供相关配置
 * ==================================================
 */
import { CoreProvider } from '@q-web-plugin/core-provider'

/**
 * 设置系统默认配置
 */
CoreProvider.defaultSystemInfo = {
  layout: ['header', ['sider', 'content']], // 默认布局
  followSystemTheme: false, // 跟随系统主题
  theme: 'default', // 默认主题
  // 以下是扩展字段，systemStore 初始数据中未定义

  deepHeader: false, // 深色顶栏
  deepSidebar: false, // 深色侧边栏
  deepSubSidebar: false, // 深色二级侧栏
  deepFooter: false, // 深色底栏
  menuPosition: 'sidebar', // 菜单展示位置：sidebar、header、mixed
  menuMode: 'inline', // 侧边菜单类型：vertical、inline
  doubleColMenu: false, // 侧边双列菜单
  expandMenu: true, // 默认展开侧边菜单
  fixedMenu: false, // 默认固定侧边菜单
  sidebarAutoCollapseThreshold: 1200, // 侧边栏自动收起的屏幕尺寸阈值
  openDrawerMenuThreshold: 576, // 启用抽屉菜单的屏幕尺寸阈值
  sidebarWidth: 200, // 侧边栏宽度
  sidebarCollapsedWidth: 80, // 侧边栏收缩宽度
  sidebarMenuBarWidth: 60, // 侧边双列菜单模式下，左侧一级菜单的宽度
  ellipsisMenuLabel: true, // 菜单文本省略展示，为 true 超过一行显示省略，Tooltip 展示完整，可以配置 number，指定超过 n 行后省略
  showTopTabs: true, // 显示标签页
  enableKeepAlive: true // 开启页面保活模式
  // 更多扩展字段...
}
