import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import {
  getCompetitorListAdapter,
  getCompetitorDetailAdapter,
  getIndustryListAdapter,
  getIndustryDetailAdapter
} from './intelAdapter'
import type {
  CompetitorIntelPageParams,
  CompetitorIntelPageResult,
  CompetitorIntelItem,
  IndustryIntelPageParams,
  IndustryIntelPageResult,
  IndustryIntelItem,
  CompetitorIntelSubmitParams
} from './types'

// ============ 竞品情报 查询（AIRequestGuard 包裹）============
export const getCompetitorList = async (
  params: CompetitorIntelPageParams
): Promise<CompetitorIntelPageResult> => {
  return (await AIRequestGuard({
    adapter: getCompetitorListAdapter,
    request: () => request.POST<CompetitorIntelPageResult>({ url: 'intel/competitor/page' }, params)
  })) as CompetitorIntelPageResult
}

export const getCompetitorDetail = async (id: string): Promise<CompetitorIntelItem> => {
  return (await AIRequestGuard({
    adapter: getCompetitorDetailAdapter,
    request: () => request.GET<CompetitorIntelItem>({ url: 'intel/competitor/detail' }, { id })
  })) as CompetitorIntelItem
}

// ============ 行业情报 查询（AIRequestGuard 包裹）============
export const getIndustryList = async (
  params: IndustryIntelPageParams
): Promise<IndustryIntelPageResult> => {
  return (await AIRequestGuard({
    adapter: getIndustryListAdapter,
    request: () => request.POST<IndustryIntelPageResult>({ url: 'intel/industry/page' }, params)
  })) as IndustryIntelPageResult
}

export const getIndustryDetail = async (id: string): Promise<IndustryIntelItem> => {
  return (await AIRequestGuard({
    adapter: getIndustryDetailAdapter,
    request: () => request.GET<IndustryIntelItem>({ url: 'intel/industry/detail' }, { id })
  })) as IndustryIntelItem
}

// ============ 提交竞品情报（直接 request）============
export const submitCompetitorIntel = async (params: CompetitorIntelSubmitParams): Promise<void> => {
  await request.POST({ url: 'intel/competitor/submit' }, params)
}
