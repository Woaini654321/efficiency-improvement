<template>
  <div class="post-page">
    <!-- sticky 顶栏 -->
    <div class="header-bar">
      <a class="back-btn" @click="router.back()">
        <ArrowLeftOutlined /> {{ t('discussion.backToList') }}
      </a>
      <span class="header-divider"></span>
      <h3>{{ t('discussion.postTitle') }}</h3>
    </div>

    <!-- 滚动内容区 -->
    <div class="form-scroll">
      <div class="form-container">
        <!-- 基本信息卡 -->
        <div class="form-card">
          <div class="card-title"><span class="dot"></span>{{ t('discussion.baseInfo') }}</div>
          <div class="form-field">
            <label>{{ t('discussion.title') }}<span class="required">*</span></label>
            <a-input
              v-model:value="formModel.title"
              :placeholder="t('discussion.titlePlaceholder')"
              :maxlength="100"
              show-count
              size="large"
            />
            <span class="hint">{{ t('discussion.titleHint') }}</span>
          </div>
          <div class="form-field">
            <label>{{ t('discussion.topicLabel') }}<span class="required">*</span></label>
            <div>
              <a-segmented v-model:value="formModel.topic" :options="topicOptions" />
            </div>
          </div>
        </div>

        <!-- 讨论内容卡 -->
        <div class="form-card">
          <div class="card-title">
            <span class="dot"></span>{{ t('discussion.content') }}
            <span class="card-sub">{{ t('discussion.contentRichHint') }}</span>
          </div>
          <RichEditor v-model:modelValue="formModel.content" :placeholder="t('discussion.contentPlaceholder')" />
        </div>

        <!-- 预览切换 -->
        <div class="preview-toggle">
          <a-button type="dashed" @click="showPreview = !showPreview">
            <template #icon><EyeOutlined /></template>
            {{ showPreview ? t('discussion.previewHide') : t('discussion.previewShow') }}
          </a-button>
          <div v-if="showPreview" class="preview-card">
            <div class="preview-label">{{ t('discussion.previewLabel') }}</div>
            <div class="preview-inner">
              <a-tag :color="topicColor[formModel.topic] || 'default'" class="border-none rounded-[6px]">
                {{ t('discussion.topic.' + formModel.topic) }}
              </a-tag>
              <div class="preview-title">{{ formModel.title || t('discussion.previewNoTitle') }}</div>
              <div class="preview-meta">
                {{ t('discussion.previewAuthor') }} · {{ today }} · 0 {{ t('discussion.replies') }}
              </div>
              <div class="preview-divider"></div>
              <div v-if="plainContent" class="preview-body" v-html="formModel.content"></div>
              <div v-else class="preview-empty">{{ t('discussion.previewNoContent') }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部固定操作栏 -->
    <div class="bottom-bar">
      <a-button size="large" @click="router.back()">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
        <template #icon><SendOutlined /></template>
        {{ t('discussion.publishPost') }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, EyeOutlined, SendOutlined } from '@ant-design/icons-vue'
import RichEditor from '@/components/rich-editor/index.vue'
import { createDiscussion } from '@/apis/discussion/discussionApi'

defineOptions({ name: 'DiscussionPost' })
definePage({
  name: 'DiscussionPost',
  meta: {
    layout: false,
    menu: false,
    title: 'discussion.post'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const formModel = reactive<{ title: string; topic: string; content: string }>({
  title: '',
  topic: 'business',
  content: ''
})

const showPreview = ref(false)
const submitting = ref(false)
const today = new Date().toISOString().slice(0, 10)

const topicColor: Record<string, string> = {
  business: 'blue',
  solution: 'orange',
  experience: 'green',
  industry: 'purple',
  complaint: 'magenta'
}

const topicOptions = computed(() => [
  { label: t('discussion.topic.business'), value: 'business' },
  { label: t('discussion.topic.solution'), value: 'solution' },
  { label: t('discussion.topic.experience'), value: 'experience' },
  { label: t('discussion.topic.industry'), value: 'industry' },
  { label: t('discussion.topic.complaint'), value: 'complaint' }
])

// 富文本纯文本判空（去标签）
const plainContent = computed(() => formModel.content.replace(/<[^>]*>/g, '').trim())

async function handleSubmit() {
  if (!formModel.title.trim()) {
    message.warning(t('discussion.titlePlaceholder'))
    return
  }
  if (!plainContent.value) {
    message.warning(t('discussion.contentPlaceholder'))
    return
  }
  submitting.value = true
  try {
    await createDiscussion({ ...formModel })
    message.success(t('discussion.publishSuccess'))
    router.push({ path: '/discussion' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.post-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: hsl(var(--card-bg));
}
.header-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid hsl(var(--line));
  position: sticky;
  top: 0;
  z-index: 10;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: hsl(var(--secondary-text));
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
}
.back-btn:hover {
  background: hsl(var(--card-bg));
  color: hsl(var(--text));
}
.header-divider {
  width: 1px;
  height: 20px;
  background: hsl(var(--line));
}
.header-bar h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--text));
}
.form-scroll {
  flex: 1;
  overflow-y: auto;
}
.form-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 24px 40px;
}
.form-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  padding: 26px 30px;
  margin-bottom: 20px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid hsl(var(--line));
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-title .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: hsl(var(--primary));
}
.card-sub {
  font-size: 12px;
  font-weight: 400;
  color: hsl(var(--secondary-text));
  margin-left: 4px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 18px;
}
.form-field:last-child {
  margin-bottom: 0;
}
.form-field label {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
}
.form-field .required {
  color: hsl(var(--error));
  margin-left: 2px;
}
.form-field .hint {
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
.preview-toggle {
  text-align: center;
}
.preview-card {
  background: hsl(var(--primary) / 0.05);
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 12px;
  padding: 16px 20px;
  margin-top: 16px;
  text-align: left;
}
.preview-label {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-bottom: 8px;
}
.preview-inner {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  padding: 16px 20px;
}
.preview-title {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
  margin: 12px 0 8px;
}
.preview-meta {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-bottom: 12px;
}
.preview-divider {
  height: 1px;
  background: hsl(var(--line));
  margin: 12px 0;
}
.preview-body {
  font-size: 14px;
  line-height: 1.8;
  color: hsl(var(--text));
}
.preview-empty {
  color: hsl(var(--secondary-text));
  font-size: 14px;
}
.bottom-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 14px 24px;
  background: #fff;
  border-top: 1px solid hsl(var(--line));
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.04);
}
</style>
