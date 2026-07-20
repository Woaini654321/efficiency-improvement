import { request } from '@q-web-plugin/request'

/** 产品线单层选项（a-cascader/a-select options 契约） */
export interface ProductLineOption {
  value: string
  label: string
}

// 后端 GET productLine/list 出参：平铺 [{ product_line_id, name }]
interface ProductLineDTO {
  product_line_id: string
  name: string
}

/**
 * 拉取产品线列表（发布需求「邀请产品线回答」用）。
 * 后端出参平铺无 BU 层级，映射为单层 options（级联降为单层，最小改动）。
 */
export const fetchProductLineOptions = async (): Promise<ProductLineOption[]> => {
  const raw = await request.GET<ProductLineDTO[]>({ url: 'productLine/list' })
  return ((raw as ProductLineDTO[] | null) ?? []).map((dto) => ({
    value: String(dto.product_line_id),
    label: dto.name ?? ''
  }))
}
