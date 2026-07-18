export function getNavigatorLang(): string {
  if (import.meta.env.VITE_PLATFORM === 'uni') {
    return uni.getLocale() === 'zh-Hans' ? 'zh-CN' : 'en-US'
  }
  const lowerCaseNavigatorLanguage = navigator.language.toLowerCase()
  const isCn =
    lowerCaseNavigatorLanguage.indexOf('cn') > -1 || lowerCaseNavigatorLanguage.indexOf('zh') > -1
  return isCn ? 'zh-CN' : 'en-US'
}

export const ua = navigator.userAgent
export const lowerUA = ua.toLowerCase()

// iPhone
export const isIPhone = /iphone/.test(lowerUA)
// iPad（含 iPadOS 13+ 伪装 Mac 的情况）
export const isIPad =
  /ipad/.test(lowerUA) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
// Apple Watch
export const isIWatch = /watch|watchos/.test(lowerUA)

/**
 * 操作系统维度
 */
// 安卓
export const isAndroid = /android/.test(lowerUA)
// 鸿蒙（HarmonyOS / Harmony / HMOS / HMSCore）
export const isHarmonyOS =
  /harmonyos|hmos|harmony/.test(lowerUA) ||
  (/huawei|honor/.test(lowerUA) && /hmscore/.test(lowerUA))
// iOS
export const isIOS = isIPhone || isIPad || isIWatch
// macOS（排除 iPadOS 伪装 Mac）
export const isMac = /macintosh|mac os x/.test(lowerUA) && !isIPad
// Windows
export const isWindows = /windows nt/.test(lowerUA)

/**
 * 屏幕尺寸维度
 */
// 移动设备（Phone / Mobile）
export const isMobile = /iphone|android.*mobile|windows phone|mobile/.test(lowerUA)
// 平板设备（Pad类）
export const isTablet = isIPad || (/android/.test(lowerUA) && !/mobile/.test(lowerUA))
// PC设备
export const isPC = isWindows || isMac
