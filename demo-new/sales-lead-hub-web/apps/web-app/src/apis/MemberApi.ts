// 后端 member/* 已实现（product_line_member），页面 PC-26 未建，本文件待页面落地时对齐真实契约。
/**
 * 仅 Demo 展示 Request 实例方法如何使用
 *
 * 成员模块相关请求方法，这里演示了 CRUD 请求操作
 */
import { request } from '@q-web-plugin/request'
import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

/**
 * 获取成员分页列表
 */
// 获取成员分页参数类型，这里基于 PaginationParams 基础的分页类型，扩展两个检索条件字段：key、type
type GetMemberPageParams = PaginationParams<{
  key?: string // 关键字
  type?: string // 类型
}>
// 成员分页单条数据类型
export type GetMemberPageRecord = {
  id: string
  legalName: string
  age: number | null
  level: number
  status: string
}
// 获取成员分页返回类型，这里基于 PaginationResult 基础的分页结果，完善了 records 字段的类型
export type GetMemberPageResult = PaginationResult<GetMemberPageRecord>
export const getMemberPage = async (params: GetMemberPageParams): Promise<GetMemberPageResult> => {
  return request.POST<GetMemberPageResult>(
    {
      url: 'member/page',
      urlType: 'default_url' // urlType 默认为 default_url，这里可以不用传
    },
    params
  )
}

/**
 * 获取成员详情
 */
// 获取成员详情返回类型
type GetMemberDetailResult = {
  id: string
  legalName: string
  age: number | null
  level: number
  status: string
}
export const getMemberDetail = (id: string) => {
  return request.GETURL<GetMemberDetailResult>(
    {
      url: 'member'
    },
    id
  )
}

/**
 * 创建成员
 */
// 创建成员参数类型
type CreateMemberParams = {
  legalName: string
  age?: number
}
// 创建成员返回类型（这里沿用上面的成员详情类型）
type CreateMemberResult = GetMemberDetailResult
export const createMember = (params: CreateMemberParams) => {
  return request.POST<CreateMemberResult>(
    {
      url: 'member/add'
    },
    params
  )
}

/**
 * 更新成员
 */
// 更新成员参数类型
type UpdateMemberParams = {
  id: string
  legalName: string
  age?: number
}
// 更新成员返回类型
type UpdateMemberResult = GetMemberDetailResult
export const updateMember = (params: UpdateMemberParams) => {
  return request.PUT<UpdateMemberResult>(
    {
      url: 'member/update'
    },
    params
  )
}

/**
 * 删除成员
 */
export const deleteMember = (id: string) => {
  return request.DELETE<boolean>(
    {
      url: 'member/update'
    },
    id
  )
}
