import type { PresetUnoTheme } from 'unocss'

export interface Colors {
  [key: string]:
    | (Colors & {
        DEFAULT?: string
      })
    | string
}

export interface Theme extends PresetUnoTheme {
  colors: Colors
}
