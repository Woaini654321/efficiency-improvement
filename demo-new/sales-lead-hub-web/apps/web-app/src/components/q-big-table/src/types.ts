import type { FormSchema } from '../../q-form/src/types'

export interface ToolbarButton {
  label: string
  type?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
  danger?: boolean
  disabled?: boolean | (() => boolean)
  icon?: string
  onClick?: () => void
  permission?: string
}

export interface PaginationConfig {
  pageSize?: number
  pageSizes?: number[]
}

export interface PageInfo {
  pageNumber: number
  pageSize: number
}

export interface QueryApiParams {
  page: PageInfo
  sort?: any
  filters?: any
}

export interface QueryApiResult {
  result: any[]
  page: { total: number }
}

export type QueryApiFunction = (params: QueryApiParams, searchParams: Record<string, any>) => Promise<QueryApiResult>

export interface TableColumn {
  field?: string
  title?: string
  type?: string
  width?: number
  minWidth?: number
  fixed?: 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  slots?: {
    default?: (params: { row: any; rowIndex: number; column: any }) => any
    header?: (params: { column: any }) => any
  }
  [key: string]: any
}

export interface QBigTableProps {
  searchConfig?: FormSchema[]
  searchParams?: Record<string, any>
  toolbarConfig?: ToolbarButton[]
  columns?: TableColumn[]
  queryApi?: QueryApiFunction | null
  data?: any[] | null
  paginationConfig?: PaginationConfig
  gridConfig?: Record<string, any>
  border?: boolean
  stripe?: boolean
  height?: string | number
  selectable?: boolean
  onBatchDelete?: ((ids: string[]) => Promise<void>) | null
  onRowClick?: ((row: any) => void) | null
  rowId?: string
  showSearchActions?: boolean
}

export interface QBigTableExpose {
  refresh: () => void
  reload: () => void
  getCheckboxRecords: () => any[]
  clearCheckboxRow: () => void
  getTableRef: () => any
  resetSearch: () => void
}
