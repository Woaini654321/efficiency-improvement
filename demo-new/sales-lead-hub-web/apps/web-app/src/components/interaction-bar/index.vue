<template>
  <div class="interaction-bar flex items-center gap-3">
    <button type="button" class="pill" :class="{ active: liked }" @click="emit('like')">
      <LikeOutlined />
      <span>{{ t('common.like') }}</span>
      <span class="count">{{ likeCount }}</span>
    </button>
    <button type="button" class="pill" :class="{ active: collected }" @click="emit('collect')">
      <StarOutlined />
      <span>{{ t('common.collect') }}</span>
      <span class="count">{{ collectCount }}</span>
    </button>
    <button type="button" class="pill" @click="emit('share')">
      <ShareAltOutlined />
      <span>{{ t('common.share') }}</span>
    </button>
    <div class="comment-stat flex items-center gap-1">
      <MessageOutlined />
      <span>{{ t('common.comment') }}</span>
      <span class="count">{{ commentCount }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { LikeOutlined, StarOutlined, ShareAltOutlined, MessageOutlined } from '@ant-design/icons-vue'

defineProps<{
  likeCount: number
  collectCount: number
  commentCount: number
  liked?: boolean
  collected?: boolean
}>()

const emit = defineEmits<{
  like: []
  collect: []
  share: []
}>()

const { t } = useI18n()
</script>

<style scoped>
.interaction-bar {
  padding: 12px 0;
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 999px;
  background: hsl(var(--card-bg));
  color: hsl(var(--text));
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: hsl(var(--primary));
    color: hsl(var(--primary));
  }

  &.active {
    border-color: hsl(var(--primary));
    background: hsl(var(--primary) / 10%);
    color: hsl(var(--primary));
  }
}

.count {
  font-weight: 600;
}

.comment-stat {
  margin-left: auto;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
</style>
