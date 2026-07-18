export interface HSL {
  h: number // 0-360
  s: number // 0-100
  l: number // 0-100
}

/**
 * 将 HEX 颜色转换为 HSL
 * 支持 #RGB、#RRGGBB、#RRGGBBAA
 */
export function hexToHsl(hex: string): HSL {
  let clean = hex.replace('#', '').trim()

  // 处理 #RGB 简写
  if (clean.length === 3) {
    clean = clean
      .split('')
      .map(ch => ch + ch)
      .join('')
  }

  // 忽略透明度 (#RRGGBBAA)
  if (clean.length === 8) {
    clean = clean.substring(0, 6)
  }

  if (clean.length !== 6) {
    throw new Error(`Invalid HEX color: ${hex}`)
  }

  const r = parseInt(clean.substring(0, 2), 16) / 255
  const g = parseInt(clean.substring(2, 4), 16) / 255
  const b = parseInt(clean.substring(4, 6), 16) / 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const delta = max - min

  let h = 0
  const l = (max + min) / 2

  // saturation
  let s = 0
  if (delta !== 0) {
    // hue
    if (max === r) {
      h = ((g - b) / delta) % 6
    } else if (max === g) {
      h = (b - r) / delta + 2
    } else {
      h = (r - g) / delta + 4
    }
    h = Math.round(h * 60)
    if (h < 0) h += 360

    // saturation
    s = delta / (1 - Math.abs(2 * l - 1))
  }

  return {
    h,
    s: +(s * 100).toFixed(1),
    l: +(l * 100).toFixed(1)
  }
}

export function removeHslWrapper(val: string) {
  return val.replace(/^hsl\(|\)|,/g, '')
}
