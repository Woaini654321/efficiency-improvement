import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getTaskListAdapter } from './taskAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/task.json'
import type { TaskPageParams, TaskPageResult, TaskTransferParams } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询我的任务列表 */
export const getTaskList = async (params: TaskPageParams): Promise<TaskPageResult> => {
  return (await AIRequestGuard({
    adapter: getTaskListAdapter,
    request: mockRequest('task',
      { records: mockData.records, total: mockData.total },
      () => request.POST<TaskPageResult>({ url: 'task/page' }, params)
    )
  })) as TaskPageResult
}

// ============ 操作（直接 request）============

/** 开始处理任务 */
export const startTask = async (id: string): Promise<void> => {
  await request.POST({ url: 'task/start' }, { id })
}

/** 标记任务完成 */
export const completeTask = async (id: string, remark: string): Promise<void> => {
  await request.POST({ url: 'task/complete' }, { id, remark })
}

/** 转交任务 */
export const transferTask = async (params: TaskTransferParams): Promise<void> => {
  await request.POST({ url: 'task/transfer' }, params)
}
