<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-bold">{{ t('notification.preferenceTitle') }}</h2>
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <a-alert type="info" show-icon class="mb-4" :message="t('notification.preferenceTip')" />

    <a-spin :spinning="loading">
      <div class="matrix border border-[hsl(var(--line))] rounded overflow-hidden">
        <!-- 表头 -->
        <div class="matrix-row matrix-head bg-[hsl(var(--card-bg))]">
          <div class="matrix-cell matrix-label font-semibold">{{ t('notification.notifyType') }}</div>
          <div v-for="ch in channels" :key="ch" class="matrix-cell font-semibold">
            <span>{{ t('dict.channel.' + ch) }}</span>
            <a-tag v-if="ch === 'in_app'" class="ml-1">{{ t('notification.locked') }}</a-tag>
          </div>
        </div>
        <!-- 数据行 -->
        <div v-for="type in notifyTypes" :key="type" class="matrix-row">
          <div class="matrix-cell matrix-label">{{ t('dict.notifyType.' + type) }}</div>
          <div v-for="ch in channels" :key="ch" class="matrix-cell">
            <a-switch
              v-model:checked="matrix[type][ch]"
              :disabled="ch === 'in_app'"
            />
          </div>
        </div>
      </div>

      <div class="flex flex-wrap gap-2 mt-4">
        <a-button @click="enableAll">{{ t('notification.enableAll') }}</a-button>
        <a-button @click="disableAll">{{ t('notification.disableAll') }}</a-button>
        <a-button @click="restoreDefault">{{ t('notification.restoreDefault') }}</a-button>
      </div>
    </a-spin>

    <div class="flex justify-end gap-2 pt-4 border-t border-[hsl(var(--line))] mt-4">
      <a-button @click="router.back()">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { saveNotificationPreference } from '@/apis/notification/notificationApi'

defineOptions({ name: 'NotificationPreference' })
definePage({
  name: 'NotificationPreference',
  meta: {
    layout: false,
    menu: false,
    title: 'notification.preference'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const notifyTypes = ['publish', 'response', 'adopt', 'mention', 'subscribe', 'system'] as const
const channels = ['in_app', 'feishu', 'email'] as const

type NotifyType = (typeof notifyTypes)[number]
type Channel = (typeof channels)[number]
type MatrixType = Record<NotifyType, Record<Channel, boolean>>

// 默认矩阵：站内信全部锁定开启；飞书/邮箱按通知类型给出合理默认
function buildDefault(): MatrixType {
  const feishuDefault: Record<NotifyType, boolean> = {
    publish: true, response: true, adopt: true, mention: true, subscribe: false, system: false
  }
  const emailDefault: Record<NotifyType, boolean> = {
    publish: false, response: true, adopt: true, mention: false, subscribe: false, system: false
  }
  const m = {} as MatrixType
  for (const type of notifyTypes) {
    m[type] = { in_app: true, feishu: feishuDefault[type], email: emailDefault[type] }
  }
  return m
}

const loading = ref(false)
const saving = ref(false)
const matrix = reactive<MatrixType>(buildDefault())

function assign(next: MatrixType) {
  for (const type of notifyTypes) {
    for (const ch of channels) {
      matrix[type][ch] = next[type][ch]
    }
  }
}

function enableAll() {
  for (const type of notifyTypes) {
    for (const ch of channels) matrix[type][ch] = true
  }
}
function disableAll() {
  for (const type of notifyTypes) {
    for (const ch of channels) matrix[type][ch] = ch === 'in_app' // 站内信保持开启
  }
}
function restoreDefault() {
  assign(buildDefault())
}

async function handleSave() {
  saving.value = true
  try {
    await saveNotificationPreference({ matrix: JSON.parse(JSON.stringify(matrix)) })
    message.success(t('notification.saveSuccess'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.matrix-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  border-bottom: 1px solid hsl(var(--line));
}
.matrix-row:last-child {
  border-bottom: none;
}
.matrix-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
  border-left: 1px solid hsl(var(--line));
}
.matrix-cell:first-child {
  border-left: none;
}
.matrix-label {
  justify-content: flex-start;
}
</style>
