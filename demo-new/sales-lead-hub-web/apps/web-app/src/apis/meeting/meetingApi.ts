import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getMeetingListAdapter } from './meetingAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/meeting.json'
import type {
  MeetingTaskPageParams,
  MeetingTaskPageResult,
  MeetingTaskSaveParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询会议任务列表 */
export const getMeetingList = async (
  params: MeetingTaskPageParams
): Promise<MeetingTaskPageResult> => {
  return (await AIRequestGuard({
    adapter: getMeetingListAdapter,
    request: mockRequest(
      { records: mockData.records, total: mockData.total },
      () => request.POST<MeetingTaskPageResult>({ url: 'meeting/page' }, params)
    )
  })) as MeetingTaskPageResult
}

// ============ 增删改（直接 request）============

/** 新建/编辑会议任务 */
export const saveMeetingTask = async (params: MeetingTaskSaveParams): Promise<void> => {
  const url = params.id ? 'meeting/update' : 'meeting/create'
  await request.POST({ url }, params)
}

/** 催办会议任务 */
export const urgeMeetingTask = async (id: string, remark: string): Promise<void> => {
  await request.POST({ url: 'meeting/urge' }, { id, remark })
}

/** 作废会议任务 */
export const cancelMeetingTask = async (id: string, reason: string): Promise<void> => {
  await request.POST({ url: 'meeting/cancel' }, { id, reason })
}
