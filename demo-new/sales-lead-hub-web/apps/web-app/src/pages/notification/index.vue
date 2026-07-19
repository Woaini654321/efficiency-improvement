<template>
  <div class="h-full p-[16px] bg-white rounded">
    <QBigTable
      ref="tableRef"
      :search-config="searchConfig"
      :toolbar-config="toolbarConfig"
      :columns="columns"
      :query-api="queryApi"
      height="100%"
    />
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, ToolbarButton, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getNotificationList, markNotificationRead } from '@/apis/notification/notificationApi'

defineOptions({ name: 'NotificationList' })
definePage({
  name: 'NotificationList',
  meta: {
    layout: false,
    menu: true,
    title: 'notification.list'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const tableRef = ref<QBigTableExpose | null>(null)

// a-tag 颜色遵循 BUILD-PLAN §4 铁律：仅 blue/red/green/orange/default
const typeColor: Record<string, string> = {
  publish: 'blue',
  response: 'green',
  adopt: 'orange',
  system: 'default',
  mention: 'blue',
  subscribe: 'green',
  force_confirm: 'red'
}

const typeOptions = computed(() => [
  { label: t('dict.notifyType.publish'), value: 'publish' },
  { label: t('dict.notifyType.response'), value: 'response' },
  { label: t('dict.notifyType.adopt'), value: 'adopt' },
  { label: t('dict.notifyType.mention'), value: 'mention' },
  { label: t('dict.notifyType.subscribe'), value: 'subscribe' },
  { label: t('dict.notifyType.system'), value: 'system' },
  { label: t('dict.notifyType.force_confirm'), value: 'force_confirm' }
])
const readOptions = computed(() => [
  { label: t('common.all'), value: '' },
  { label: t('notification.unread'), value: 'false' },
  { label: t('notification.read'), value: 'true' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('notification.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'type',
    label: t('notification.type'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:150px', options: typeOptions.value }
  },
  {
    field: 'isRead',
    label: t('notification.readStatus'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: readOptions.value }
  }
]

const toolbarConfig: ToolbarButton[] = [
  { label: t('notification.gotoPreference'), onClick: () => { router.push({ path: '/notification/preference' }) } },
  { label: t('notification.gotoAnnouncement'), onClick: () => { router.push({ path: '/notification/announcement' }) } }
]

const columns: TableColumn[] = [
  {
    field: 'title',
    title: t('notification.title'),
    minWidth: 320,
    slots: {
      default: ({ row }: any) => (
        <span class="flex items-center gap-1">
          {!row.isRead ? <span class="inline-block w-[8px] h-[8px] rounded-full bg-[hsl(var(--error))]"></span> : null}
          <a class="text-[hsl(var(--primary))]" onClick={() => handleView(row)}>{row.title}</a>
        </span>
      )
    }
  },
  {
    field: 'type',
    title: t('notification.type'),
    width: 120,
    slots: { default: ({ row }: any) => <a-tag color={typeColor[row.type] ?? 'default'}>{t('dict.notifyType.' + row.type)}</a-tag> }
  },
  { field: 'triggerUserName', title: t('notification.triggerUser'), width: 110 },
  {
    field: 'channel',
    title: t('notification.channel'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag>{t('dict.channel.' + row.channel)}</a-tag> }
  },
  {
    field: 'isForceConfirm',
    title: t('notification.forceConfirm'),
    width: 110,
    slots: { default: ({ row }: any) => (row.isForceConfirm ? <a-tag color="red">{t('notification.forceConfirm')}</a-tag> : <span>--</span>) }
  },
  {
    field: 'isRead',
    title: t('notification.readStatus'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={row.isRead ? 'default' : 'green'}>{row.isRead ? t('notification.read') : t('notification.unread')}</a-tag> }
  },
  { field: 'createdAt', title: t('common.createdAt'), width: 160, slots: { default: ({ row }: any) => <span>{row.createdAt ?? '--'}</span> } },
  {
    title: t('common.action'),
    width: 160,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1">
          {!row.isRead ? <a-button type="link" onClick={() => handleMarkRead(row.id)}>{t('notification.markRead')}</a-button> : null}
          <a-button type="link" onClick={() => handleView(row)}>{t('common.view')}</a-button>
        </span>
      )
    }
  }
]

function handleView(row: any) {
  if (row.targetType === 'announcement') {
    router.push({ path: '/notification/announcement', query: { id: row.targetId } })
  } else {
    message.info(row.title)
  }
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getNotificationList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}

async function handleMarkRead(id: string) {
  await markNotificationRead(id)
  message.success(t('common.success'))
  tableRef.value?.refresh()
}
</script>
