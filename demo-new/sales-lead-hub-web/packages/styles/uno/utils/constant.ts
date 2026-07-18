/**
 * 约定不参与 Wind4 原子类生成的命名空间
 * 为了避免我们自定义的预设（如 common-preset）中定义的主题被 Wind4 生成一些奇怪的原子类，约定定义在如 colors.wind4-blocklist 中的颜色将不参与 wind4 原子类的生成
 * 如在 common-preset 中配置了 blocklist: [new RegExp(`/-${WIND4_BLOCKLIST_NAMESPACE}-/`)]，如果你有类似的需要，可以参考 common-preset
 */
export const WIND4_BLOCKLIST_NAMESPACE = 'wind4-blocklist'
