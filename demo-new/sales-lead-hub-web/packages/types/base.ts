/**
 * 基础（通用）类型
 * 通过 import type 显式去引入
 */
export type Primitive = string | number | boolean | null | undefined
export type JsonValue = Primitive | { [k: string]: JsonValue } | JsonValue[]

/**
 * Query 参数：原始值或原始值数组
 */
export type QueryParams = Record<string, Primitive | Primitive[]>

/**
 * 请求体：FormData 或 JSON 对象
 */
export type RequestBody = FormData | Record<string, JsonValue>

/**
 * 平台类型
 */
export type Platform = 'web' | 'h5' | 'uni-app' | 'electron'

/**
 * 分页接口返回
 */
export type PaginationResult<T = unknown> = {
  records: T[]
  total: number
}

/**
 * 分页接口请求参数
 */
export type PaginationParams<T = unknown> = {
  orderBy?: string
  orderDirection?: string
  pageNumber: number
  pageSize: number
} & T
