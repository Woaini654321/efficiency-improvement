import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getBatchMetaAdapter } from './batchAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/batch.json'
import type { BatchMeta, BatchPublishParams } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询批量发布向导元数据（会议 / 执行人）*/
export const getBatchMeta = async (): Promise<BatchMeta> => {
  return (await AIRequestGuard({
    adapter: getBatchMetaAdapter,
    request: mockRequest(
      mockData,
      () => request.GET<BatchMeta>({ url: 'operation/batch/meta' })
    )
  })) as BatchMeta
}

// ============ 写操作（直接 request）============

/** 批量发布任务 */
export const publishBatchTasks = async (params: BatchPublishParams): Promise<void> => {
  await request.POST({ url: 'operation/batch/publish' }, params)
}
