<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="mb-4">
      <h1 class="text-[20px] font-bold mb-1">{{ t('tool.title') }}</h1>
      <p class="text-[hsl(var(--secondary-text))] m-0">{{ t('tool.subtitle') }}</p>
    </div>

    <a-row :gutter="16">
      <a-col v-for="item in tools" :key="item.key" :xs="24" :sm="12" :md="8" :xl="6">
        <a-card hoverable size="small" class="mb-4 tool-card">
          <div class="flex items-start gap-3">
            <span class="tool-emoji">{{ item.emoji }}</span>
            <div class="min-w-0 flex-1">
              <div class="font-semibold mb-1">{{ item.title }}</div>
              <div class="text-[12px] text-[hsl(var(--secondary-text))] tool-desc">{{ item.desc }}</div>
            </div>
          </div>
          <div class="text-right mt-3">
            <a-button type="primary" size="small" @click="openTool(item)">{{ t('tool.enter') }}</a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-modal v-model:open="modalOpen" :title="activeTool?.title" :footer="null">
      <a-result status="info" :title="t('tool.wip')" :sub-title="t('tool.wipDesc')" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'ToolAssistant' })
definePage({
  name: 'ToolAssistant',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:shop-linear' },
    title: 'tool'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()

interface ToolItem {
  key: string
  emoji: string
  title: string
  desc: string
}

const tools = computed<ToolItem[]>(() => [
  { key: 'quote', emoji: '🧮', title: t('tool.items.quote'), desc: t('tool.items.quoteDesc') },
  { key: 'compare', emoji: '⚖️', title: t('tool.items.compare'), desc: t('tool.items.compareDesc') },
  { key: 'template', emoji: '📚', title: t('tool.items.template'), desc: t('tool.items.templateDesc') },
  { key: 'email', emoji: '✉️', title: t('tool.items.email'), desc: t('tool.items.emailDesc') },
  { key: 'recommend', emoji: '🎯', title: t('tool.items.recommend'), desc: t('tool.items.recommendDesc') },
  { key: 'script', emoji: '🗣️', title: t('tool.items.script'), desc: t('tool.items.scriptDesc') },
  { key: 'writing', emoji: '✍️', title: t('tool.items.writing'), desc: t('tool.items.writingDesc') }
])

const modalOpen = ref(false)
const activeTool = ref<ToolItem | null>(null)

function openTool(item: ToolItem) {
  activeTool.value = item
  modalOpen.value = true
}
</script>

<style scoped>
.tool-card {
  height: 100%;
}
.tool-emoji {
  font-size: 30px;
  line-height: 1.1;
  flex-shrink: 0;
}
.tool-desc {
  line-height: 1.6;
}
</style>
