import type { PersistenceOptions } from 'pinia-plugin-persistedstate'

declare module 'pinia' {
  // 适用于 options 风格的 defineStore
  export interface DefineStoreOptionsBase<S, Store> {
    persist?: boolean | PersistenceOptions
  }

  // 如果你使用 setup 风格的 defineStore('id', () => {}, { ...options })
  // persist 需要放在第三个参数的 options 中，通常不需要额外改这里
}
