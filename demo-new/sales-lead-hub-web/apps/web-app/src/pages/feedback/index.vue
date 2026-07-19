<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- 粉色 banner -->
    <div class="banner rounded-[8px] px-[24px] py-[22px] mb-4 flex items-center justify-between">
      <div>
        <h1 class="text-[20px] font-bold text-white mb-1">{{ t('feedback.title') }}</h1>
        <p class="text-white/85 m-0">{{ t('feedback.subtitle') }}</p>
      </div>
      <a-button size="large" class="post-btn" @click="openModal">{{ t('feedback.postBtn') }}</a-button>
    </div>

    <!-- Tab 热门/最新 -->
    <div class="flex items-center justify-between mb-3">
      <a-segmented v-model:value="tab" :options="tabOptions" />
      <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('feedback.total', { n: list.length }) }}</span>
    </div>

    <!-- 2 列吐槽卡网格 -->
    <a-row :gutter="16">
      <a-col v-for="item in sortedList" :key="item.id" :xs="24" :md="12">
        <a-card size="small" class="mb-4 fb-card">
          <div class="flex items-start gap-3">
            <span class="fb-avatar" :style="{ background: `linear-gradient(135deg, ${item.color}, #ff85c0)` }">
              {{ item.emoji }}
            </span>
            <div class="min-w-0 flex-1">
              <div class="font-semibold mb-1">{{ item.title }}</div>
              <div class="text-[hsl(var(--text))] mb-2 fb-content">{{ item.content }}</div>
              <div class="flex items-center justify-between">
                <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ item.anonName }} · {{ item.createdAt }}</span>
                <a-button
                  type="text"
                  size="small"
                  class="like-btn"
                  :class="{ liked: liked.has(item.id) }"
                  @click="toggleLike(item.id)"
                >
                  ♥ {{ item.likeCount + (liked.has(item.id) ? 1 : 0) }}
                </a-button>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <p class="text-center text-[12px] text-[hsl(var(--secondary-text))] mt-2">{{ t('feedback.disclaimer') }}</p>

    <!-- 我要吐槽 Modal -->
    <a-modal v-model:open="modalOpen" :title="t('feedback.postBtn')" :confirm-loading="submitting" @ok="handleSubmit">
      <a-alert type="warning" show-icon class="mb-3" :message="t('feedback.anonTip')" />
      <div>
        <div class="mb-4">
          <div class="mb-[6px]">{{ t('feedback.formTitle') }}</div>
          <a-input v-model:value="formModel.title" :placeholder="t('feedback.formTitlePlaceholder')" :maxlength="50" show-count />
        </div>
        <div class="mb-4">
          <div class="mb-[6px]">{{ t('feedback.formContent') }}</div>
          <a-textarea
            v-model:value="formModel.content"
            :placeholder="t('feedback.formContentPlaceholder')"
            :rows="4"
            :maxlength="300"
            show-count
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getFeedbackList, createFeedback, likeFeedback } from '@/apis/feedback/feedbackApi'
import type { FeedbackItem } from '@/apis/feedback/types'

defineOptions({ name: 'FeedbackWall' })
definePage({
  name: 'FeedbackWall',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:like-linear' },
    title: 'feedback'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()

const list = ref<FeedbackItem[]>([])
const liked = ref<Set<string>>(new Set())

const tab = ref<'hot' | 'latest'>('hot')
const tabOptions = computed(() => [
  { label: t('feedback.tabHot'), value: 'hot' },
  { label: t('feedback.tabLatest'), value: 'latest' }
])

const sortedList = computed(() => {
  const arr = [...list.value]
  if (tab.value === 'hot') {
    arr.sort((a, b) => b.likeCount + (liked.value.has(b.id) ? 1 : 0) - (a.likeCount + (liked.value.has(a.id) ? 1 : 0)))
  } else {
    arr.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  }
  return arr
})

function toggleLike(id: string) {
  const next = new Set(liked.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
    // 乐观点赞，后端接入后由该接口回写计数
    likeFeedback(id)
  }
  liked.value = next
}

// ==== Modal ====
const modalOpen = ref(false)
const submitting = ref(false)
const formModel = reactive<{ title: string; content: string }>({ title: '', content: '' })

function openModal() {
  formModel.title = ''
  formModel.content = ''
  modalOpen.value = true
}

async function handleSubmit() {
  if (!formModel.title.trim() || !formModel.content.trim()) {
    message.warning(t('feedback.formRequired'))
    return
  }
  submitting.value = true
  try {
    await createFeedback({ title: formModel.title, content: formModel.content })
    // 本地插入，展示效果（后端接入后改为重新拉取列表）
    list.value.unshift({
      id: `local-${Date.now()}`,
      title: formModel.title,
      content: formModel.content,
      anonName: t('feedback.anonSelf'),
      likeCount: 0,
      createdAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
      emoji: '💬',
      color: '#eb2f96'
    })
    message.success(t('feedback.postSuccess'))
    modalOpen.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  list.value = await getFeedbackList()
})
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, #eb2f96 0%, #ff85c0 100%);
}
.post-btn {
  color: #eb2f96;
  font-weight: 600;
}

.fb-card {
  border-left: 3px solid #ffadd2;
}
.fb-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}
.fb-content {
  line-height: 1.7;
}

.like-btn {
  color: hsl(var(--secondary-text));
}
.like-btn.liked {
  color: #eb2f96;
  font-weight: 600;
}
</style>
