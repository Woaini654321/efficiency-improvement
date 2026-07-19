<template>
  <div class="social-stats">
    <span v-if="views !== undefined" class="stat-item" :title="'views'">
      <EyeOutlined /> {{ fmt(views) }}
    </span>
    <span
      v-if="likes !== undefined"
      class="stat-item"
      :class="{ active: liked, clickable: interactive }"
      @click="interactive && emit('like')"
    >
      <LikeFilled v-if="liked" /><LikeOutlined v-else /> {{ fmt(likes) }}
    </span>
    <span
      v-if="collects !== undefined"
      class="stat-item"
      :class="{ 'active-star': collected, clickable: interactive }"
      @click="interactive && emit('collect')"
    >
      <StarFilled v-if="collected" /><StarOutlined v-else /> {{ fmt(collects) }}
    </span>
    <span
      v-if="comments !== undefined"
      class="stat-item"
      :class="{ clickable: interactive }"
      @click="interactive && emit('comment')"
    >
      <MessageOutlined /> {{ fmt(comments) }}
    </span>
    <span
      v-if="showShare"
      class="stat-item clickable"
      @click="emit('share')"
    >
      <ShareAltOutlined /> {{ shareLabel }}
    </span>
  </div>
</template>

<script setup lang="ts">
import {
  EyeOutlined, LikeOutlined, LikeFilled, StarOutlined, StarFilled,
  MessageOutlined, ShareAltOutlined
} from '@ant-design/icons-vue'

withDefaults(defineProps<{
  views?: number
  likes?: number
  collects?: number
  comments?: number
  liked?: boolean
  collected?: boolean
  interactive?: boolean
  showShare?: boolean
  shareLabel?: string
}>(), {
  interactive: false,
  liked: false,
  collected: false,
  showShare: false,
  shareLabel: ''
})

const emit = defineEmits<{
  like: []
  collect: []
  comment: []
  share: []
}>()

function fmt(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>

<style scoped>
.social-stats {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  transition: color 0.2s;
}
.stat-item.clickable {
  cursor: pointer;
}
.stat-item.clickable:hover {
  color: hsl(var(--primary));
}
.stat-item.active {
  color: hsl(var(--primary));
}
.stat-item.active-star {
  color: #faad14;
}
</style>
