import {
  defineComponent,
  ref,
  reactive,
  computed,
  watch,
  h,
  resolveComponent,
  onMounted,
  nextTick
} from 'vue'
import type { PropType } from 'vue'
import { Modal, message } from 'ant-design-vue'
import EmptyComponent from '@q-web-plugin/empty'
import { useI18n } from 'vue-i18n'
import type { FormSchema } from '../../q-form/src/types'
import type {
  ToolbarButton,
  PaginationConfig,
  QueryApiFunction,
  TableColumn
} from './types'
import {
  generateFormModel,
  resolveComponent as resolveSchemaComponent
} from '../../q-form/src/utils'

export default defineComponent({
  name: 'QBigTable',
  inheritAttrs: false,

  props: {
    searchConfig: { type: Array as PropType<FormSchema[]>, default: () => [] },
    searchParams: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({})
    },
    toolbarConfig: {
      type: Array as PropType<ToolbarButton[]>,
      default: () => []
    },
    columns: { type: Array as PropType<TableColumn[]>, default: () => [] },
    queryApi: { type: Function as PropType<QueryApiFunction>, default: null },
    data: { type: Array as PropType<any[]>, default: null },
    paginationConfig: {
      type: Object as PropType<PaginationConfig>,
      default: () => ({ pageSize: 30, pageSizes: [30, 50, 100, 200] })
    },
    gridConfig: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({})
    },
    border: { type: Boolean, default: true },
    stripe: { type: Boolean, default: true },
    height: {
      type: [String, Number] as PropType<string | number>,
      default: '100%'
    },
    selectable: { type: Boolean, default: false },
    onBatchDelete: {
      type: Function as PropType<(ids: string[]) => Promise<void>>,
      default: null
    },
    onRowClick: {
      type: Function as PropType<(row: any) => void>,
      default: null
    },
    rowId: { type: String, default: 'id' },
    showSearchActions: { type: Boolean, default: false },
    autoLoad: { type: Boolean, default: true },
    searchLayout: { type: String as PropType<'stack' | 'inline'>, default: 'stack' }
  },

  emits: ['search', 'checkbox-change', 'row-click'],

  setup(props, { emit, expose, slots }) {
    const { t } = useI18n()
    const tableRef = ref<any>(null)
    const selectedRows = ref<any[]>([])
    const batchDeleting = ref(false)

    const pageNumber = ref(1)
    const pageSize = ref(props.paginationConfig?.pageSize ?? 30)
    const total = ref(0)

    const searchForm = reactive(
      generateFormModel(props.searchConfig, { ...props.searchParams })
    )

    let debounceTimer: ReturnType<typeof setTimeout> | null = null
    watch(
      () => ({ ...searchForm }),
      () => {
        if (props.showSearchActions) return
        if (debounceTimer) clearTimeout(debounceTimer)
        debounceTimer = setTimeout(() => {
          pageNumber.value = 1
          tableRef.value?.commitProxy('query')
        }, 300)
      },
      { deep: true }
    )

    if (props.autoLoad && !props.showSearchActions && props.queryApi != null) {
      onMounted(() => {
        nextTick(() => {
          tableRef.value?.commitProxy('query')
        })
      })
    }

    function handleSearch() {
      pageNumber.value = 1
      tableRef.value?.commitProxy('query')
    }

    function resetSearch() {
      const defaults = generateFormModel(props.searchConfig, {
        ...props.searchParams
      })
      Object.keys(searchForm).forEach((k) => {
        ;(searchForm as any)[k] = defaults[k] ?? null
      })
      pageNumber.value = 1
      tableRef.value?.commitProxy('query')
    }

    const proxyConfig = computed(() => {
      if (!props.queryApi) return undefined
      return {
        seq: true,
        sort: true,
        filter: true,
        ajax: {
          query: async ({ sort, filters }: any) => {
            const res = await props.queryApi!(
              {
                page: {
                  pageNumber: pageNumber.value,
                  pageSize: pageSize.value
                },
                sort,
                filters
              },
              { ...searchForm }
            )
            total.value = res?.page?.total ?? 0
            return res?.result ?? []
          }
        }
      }
    })

    const normalizedColumns = computed(() => {
      const cols = [...props.columns]
      if (props.selectable) {
        cols.unshift({ type: 'checkbox', width: 50, align: 'center' })
      }
      return cols
    })

    const gridProps = computed(() => ({
      border: props.border,
      stripe: props.stripe,
      height: props.height,
      columns: normalizedColumns.value,
      ...(props.data !== null ? { data: props.data } : {}),
      ...(proxyConfig.value ? { proxyConfig: proxyConfig.value } : {}),
      sortConfig: { trigger: 'cell', remote: !!props.queryApi },
      filterConfig: { remote: !!props.queryApi },
      showOverflow: 'title',
      rowConfig: { keyField: props.rowId, height: 40 },
      onCheckboxChange: (e: any) => {
        selectedRows.value = tableRef.value?.getCheckboxRecords() ?? []
        emit('checkbox-change', e)
      },
      onCheckboxAll: (e: any) => {
        selectedRows.value = tableRef.value?.getCheckboxRecords() ?? []
        emit('checkbox-change', e)
      },
      onCellClick: (e: any) => {
        if (e.row && e.column?.type !== 'checkbox') {
          props.onRowClick?.(e.row)
          emit('row-click', { row: e.row })
        }
      },
      ...props.gridConfig
    }))

    function refresh() {
      tableRef.value?.commitProxy('query')
    }

    function reload() {
      pageNumber.value = 1
      tableRef.value?.commitProxy('query')
    }

    function getCheckboxRecords(): any[] {
      return tableRef.value?.getCheckboxRecords() ?? []
    }

    function clearCheckboxRow() {
      return tableRef.value?.clearCheckboxRow()
    }

    function getTableRef() {
      return tableRef.value
    }

    expose({
      refresh,
      reload,
      getCheckboxRecords,
      clearCheckboxRow,
      getTableRef,
      resetSearch
    })

    function handleBatchDelete() {
      const rows = selectedRows.value
      if (!rows.length) {
        message.warning(t('table.selectFirst'))
        return
      }
      Modal.confirm({
        title: t('table.batchDelete'),
        content: t('table.batchDeleteConfirm', { count: rows.length }),
        okText: t('common.confirm'),
        cancelText: t('common.cancel'),
        okType: 'danger',
        async onOk() {
          batchDeleting.value = true
          try {
            const ids = rows.map((r) => r[props.rowId])
            await props.onBatchDelete!(ids)
            message.success(t('table.batchDeleteSuccess'))
            selectedRows.value = []
            tableRef.value?.clearCheckboxRow()
            refresh()
          } catch {
            message.error(t('table.batchDeleteFailed'))
          } finally {
            batchDeleting.value = false
          }
        }
      })
    }

    function renderSearchFields() {
      if (!props.searchConfig?.length) return null

      const AForm = resolveComponent('AForm')
      const AFormItem = resolveComponent('AFormItem')
      const AButton = resolveComponent('AButton')
      const ASpace = resolveComponent('ASpace')

      const fields = props.searchConfig.map((schema, index) => {
        const { field, label, component, componentProps = {} } = schema
        if (!field || !component) return null

        const compName = resolveSchemaComponent(component as string | object)
        if (!compName) return null

        const Comp =
          typeof compName === 'string' ? resolveComponent(compName) : compName

        const resolvedProps =
          typeof componentProps === 'function'
            ? componentProps({ model: searchForm, field })
            : { ...componentProps }
        const { slotsRender, ...restProps } = resolvedProps as Record<
          string,
          any
        >

        return h(
          AFormItem,
          { key: field || index, label, class: 'q-big-table__form-item' },
          () =>
            h(Comp as any, {
              ...restProps,
              value: (searchForm as any)[field],
              'onUpdate:value': (val: any) => {
                ;(searchForm as any)[field] = val
              }
            })
        )
      })

      if (props.showSearchActions) {
        fields.push(
          h(
            AFormItem,
            { key: '__search_actions__', class: 'q-big-table__form-item' },
            () =>
              h(ASpace, { size: 8 }, () => [
                h(AButton, { type: 'primary', onClick: handleSearch }, () =>
                  t('common.search')
                ),
                h(AButton, { onClick: resetSearch }, () => t('common.reset'))
              ])
          )
        )
      }

      return h(AForm, { layout: 'inline' }, () => fields)
    }

    return () => {
      const VxeGrid = resolveComponent('VxeGrid')
      const AButton = resolveComponent('AButton')
      const ASpace = resolveComponent('ASpace')
      const APagination = resolveComponent('APagination')

      const toolbarButtons = props.toolbarConfig.map((btn, i) => {
        const iconNode = btn.iconRender ? btn.iconRender() : btn.icon ? h('span', { class: btn.icon, style: 'margin-right:4px' }) : null
        return h(
          AButton,
          {
            key: i,
            type: btn.type || 'default',
            danger: btn.danger,
            disabled:
              typeof btn.disabled === 'function'
                ? btn.disabled()
                : btn.disabled,
            onClick: btn.onClick,
            icon: iconNode,
            class: iconNode ? 'inline-flex items-center' : undefined
          },
          () => btn.label
        )
      })

      if (props.selectable && props.onBatchDelete != null) {
        toolbarButtons.push(
          h(
            AButton,
            {
              key: '__batch_delete__',
              type: 'primary',
              danger: true,
              disabled: selectedRows.value.length === 0,
              loading: batchDeleting.value,
              onClick: handleBatchDelete
            },
            () =>
              `${t('table.batchDelete')}${selectedRows.value.length ? ` (${selectedRows.value.length})` : ''}`
          )
        )
      }

      const toolbarNodes = toolbarButtons.length
        ? h(
            'div',
            {
              class: 'q-big-table__toolbar-actions',
              style: props.searchLayout === 'inline' ? '' : 'padding-top: 16px;'
            },
            [h(ASpace, {}, () => toolbarButtons)]
          )
        : null

      const hasSearch = props.searchConfig?.length > 0
      const isInline = props.searchLayout === 'inline'
      const searchBar =
        hasSearch || toolbarNodes
          ? isInline
            ? h('div', { class: 'q-big-table__search', style: 'display:flex; justify-content:space-between; align-items:center; margin-bottom:14px;' }, [
                h('div', { class: 'q-big-table__search-form' }, [hasSearch ? renderSearchFields() : null]),
                toolbarNodes,
              ])
            : h('div', { class: 'q-big-table__search', style: 'margin-bottom: 14px;' }, [
                hasSearch ? h('div', { class: 'q-big-table__search-form' }, [renderSearchFields()]) : null,
                toolbarNodes,
              ])
          : null

      const table = h(
        VxeGrid,
        { ref: tableRef, ...gridProps.value },
        {
          empty: () => h(EmptyComponent, { type: 'noData' }),
          ...slots,
        }
      )

      const pageSizeOptions = (
        props.paginationConfig?.pageSizes ?? [30, 50, 100, 200]
      ).map(String)
      const pagination = props.queryApi != null
        ? h(
            'div',
            {
              class: 'q-big-table__pagination',
              style: 'padding-top: 16px; padding-bottom: 16px;'
            },
            [
              h(APagination, {
                current: pageNumber.value,
                pageSize: pageSize.value,
                total: total.value,
                showSizeChanger: true,
                showQuickJumper: true,
                pageSizeOptions,
                showTotal: (t: number) => `共 ${t} 条`,
                onChange: (page: number, size: number) => {
                  pageNumber.value = page
                  pageSize.value = size
                  tableRef.value?.commitProxy('query')
                },
                onShowSizeChange: (_current: number, size: number) => {
                  pageNumber.value = 1
                  pageSize.value = size
                  tableRef.value?.commitProxy('query')
                }
              })
            ]
          )
        : null

      return h(
        'div',
        {
          class: 'q-big-table',
          style:
            'display: flex; flex-direction: column; height: 100%; overflow: hidden;'
        },
        [
          searchBar,
          h(
            'div',
            {
              class: 'q-big-table__body',
              style: 'flex: 1; overflow: hidden; min-height: 0;'
            },
            [table]
          ),
          pagination
        ]
      )
    }
  }
})
