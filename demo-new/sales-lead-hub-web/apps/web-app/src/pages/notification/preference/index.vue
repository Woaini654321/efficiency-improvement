<template>
  <div class="pref-page">
    <div class="pref-header">
      <SettingOutlined class="header-icon" />
      <span class="page-title">{{ t('notification.preferenceTitle') }}</span>
      <span class="page-sub">{{ t('preference.subtitle') }}</span>
    </div>

    <div class="pref-tip">
      <BulbOutlined class="tip-icon" />
      <span>{{ t('notification.preferenceTip') }}</span>
    </div>

    <a-spin :spinning="loading">
      <div class="matrix-card">
        <div class="matrix-card-header">
          <NotificationOutlined />
          {{ t('preference.matrixTitle') }}
        </div>
        <div class="matrix">
          <!-- 表头 -->
          <div class="matrix-row matrix-head">
            <div class="matrix-cell matrix-label">{{ t('notification.notifyType') }}</div>
            <div v-for="ch in channels" :key="ch.key" class="matrix-cell">
              <div class="channel-head">
                <component :is="ch.icon" class="channel-icon" />
                <span>{{ t('dict.channel.' + ch.key) }}</span>
                <a-tag v-if="ch.locked" class="mt-1">{{ t('notification.locked') }}</a-tag>
              </div>
            </div>
          </div>
          <!-- 数据行 -->
          <div v-for="type in notifyTypes" :key="type" class="matrix-row">
            <div class="matrix-cell matrix-label">
              <div class="type-cell">
                <div class="type-icon" :class="'ic-' + type">
                  <component :is="typeIcon[type]" />
                </div>
                <div>
                  <div class="type-label">{{ t('dict.notifyType.' + type) }}</div>
                  <div class="type-desc">{{ t('preference.typeDesc.' + type) }}</div>
                </div>
              </div>
            </div>
            <div v-for="ch in channels" :key="ch.key" class="matrix-cell">
              <a-tooltip v-if="ch.locked" :title="t('preference.inAppLockedTip')">
                <a-switch v-model:checked="matrix[type][ch.key]" size="small" disabled />
              </a-tooltip>
              <a-switch v-else v-model:checked="matrix[type][ch.key]" size="small" />
            </div>
          </div>
        </div>
      </div>

      <div class="quick-ops">
        <a-button size="small" type="primary" ghost @click="enableAll">
          <template #icon><CheckOutlined /></template>
          {{ t('notification.enableAll') }}
        </a-button>
        <a-button size="small" danger ghost @click="disableAll">
          <template #icon><CloseOutlined /></template>
          {{ t('notification.disableAll') }}
        </a-button>
        <a-button size="small" @click="restoreDefault">
          <template #icon><ReloadOutlined /></template>
          {{ t('notification.restoreDefault') }}
        </a-button>
      </div>
    </a-spin>

    <div class="pref-footer">
      <a-button @click="router.back()">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" :disabled="!hasChanges" @click="handleSave">
        <template #icon><SaveOutlined /></template>
        {{ t('common.save') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  SettingOutlined, BulbOutlined, NotificationOutlined, CheckOutlined, CloseOutlined,
  ReloadOutlined, SaveOutlined, BellOutlined, MessageOutlined, MailOutlined,
  FileTextOutlined, TrophyOutlined, InfoCircleOutlined, CommentOutlined, SoundOutlined
} from '@ant-design/icons-vue'
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
type NotifyType = (typeof notifyTypes)[number]
type Channel = 'in_app' | 'feishu' | 'email'
type MatrixType = Record<NotifyType, Record<Channel, boolean>>

const channels: { key: Channel; icon: Component; locked: boolean }[] = [
  { key: 'in_app', icon: BellOutlined, locked: true },
  { key: 'feishu', icon: MessageOutlined, locked: false },
  { key: 'email', icon: MailOutlined, locked: false }
]

const typeIcon: Record<NotifyType, Component> = {
  publish: FileTextOutlined,
  response: MessageOutlined,
  adopt: TrophyOutlined,
  mention: CommentOutlined,
  subscribe: SoundOutlined,
  system: InfoCircleOutlined
}

// 默认矩阵：站内信全部锁定开启；飞书默认仅方案发布/采纳；邮箱默认仅系统通知（对齐原型）
function buildDefault(): MatrixType {
  const feishuOn: Record<NotifyType, boolean> = {
    publish: true, response: false, adopt: true, mention: false, subscribe: false, system: false
  }
  const emailOn: Record<NotifyType, boolean> = {
    publish: false, response: false, adopt: false, mention: false, subscribe: false, system: true
  }
  const m = {} as MatrixType
  for (const type of notifyTypes) {
    m[type] = { in_app: true, feishu: feishuOn[type], email: emailOn[type] }
  }
  return m
}

const loading = ref(false)
const saving = ref(false)
const matrix = reactive<MatrixType>(buildDefault())
const baseline = ref(JSON.stringify(matrix))

const hasChanges = computed(() => JSON.stringify(matrix) !== baseline.value)

function assign(next: MatrixType) {
  for (const type of notifyTypes) {
    for (const ch of channels) matrix[type][ch.key] = next[type][ch.key]
  }
}
function enableAll() {
  for (const type of notifyTypes) {
    for (const ch of channels) matrix[type][ch.key] = true
  }
}
function disableAll() {
  for (const type of notifyTypes) {
    for (const ch of channels) matrix[type][ch.key] = ch.key === 'in_app'
  }
}
function restoreDefault() {
  assign(buildDefault())
  message.info(t('preference.restoredTip'))
}

async function handleSave() {
  saving.value = true
  try {
    await saveNotificationPreference({ matrix: JSON.parse(JSON.stringify(matrix)) })
    baseline.value = JSON.stringify(matrix)
    message.success(t('notification.saveSuccess'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.pref-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 16px;
}
.pref-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.header-icon {
  font-size: 22px;
  color: hsl(var(--primary));
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: hsl(var(--text));
}
.page-sub {
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.pref-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 12px;
  color: #8c6d00;
  margin-bottom: 20px;
}
.tip-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}
.matrix-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
}
.matrix-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid hsl(var(--line));
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--text));
}
.matrix-card-header :deep(svg) {
  color: #fa8c16;
}
.matrix-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  border-bottom: 1px solid hsl(var(--line) / 0.6);
}
.matrix-row:last-child {
  border-bottom: none;
}
.matrix-head {
  background: hsl(var(--card-bg));
}
.matrix-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px 16px;
}
.matrix-label {
  justify-content: flex-start;
}
.channel-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
  font-weight: 600;
}
.channel-icon {
  font-size: 18px;
}
.type-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.type-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.ic-publish {
  background: #e6f7ff;
  color: #1890ff;
}
.ic-response {
  background: #f6ffed;
  color: #52c41a;
}
.ic-adopt {
  background: #fff7e6;
  color: #fa8c16;
}
.ic-mention {
  background: #f9f0ff;
  color: #722ed1;
}
.ic-subscribe {
  background: #e6fffb;
  color: #13c2c2;
}
.ic-system {
  background: #f5f5f5;
  color: #8c8c8c;
}
.type-label {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
}
.type-desc {
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
.quick-ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pref-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid hsl(var(--line));
}
</style>
