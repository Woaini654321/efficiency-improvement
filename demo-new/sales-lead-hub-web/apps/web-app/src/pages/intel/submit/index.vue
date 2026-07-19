<template>
  <div class="submit-page">
    <!-- 顶部栏 -->
    <div class="page-header-bar">
      <span class="back-btn" @click="goBack"><ArrowLeftOutlined /> {{ t('intel.backToCenter') }}</span>
      <span class="divider" />
      <h3>{{ t('intel.submit') }}</h3>
    </div>

    <div class="form-container">
      <a-alert type="info" show-icon class="mb-4" :message="t('intel.submitTip')" />

      <!-- 基本信息卡片 -->
      <div class="form-card">
        <div class="form-card-title"><span class="dot" /> {{ t('intel.groupBasic') }}</div>
        <div class="form-grid">
          <div class="form-field">
            <label>{{ t('intel.brand') }} <span class="req">*</span></label>
            <a-select :value="form.brand" show-search :placeholder="t('common.selectPlaceholder')"
              class="w-full" @update:value="(v: any) => (form.brand = v || '')">
              <a-select-option v-for="b in brandOptions" :key="b" :value="b">{{ b }}</a-select-option>
            </a-select>
          </div>
          <div class="form-field">
            <label>{{ t('intel.product') }}</label>
            <a-input v-model:value="form.product" :placeholder="t('intel.productPlaceholder')" :maxlength="50" />
          </div>
          <div class="form-field">
            <label>{{ t('intel.intelType') }}</label>
            <a-select :value="form.intelType" allow-clear :placeholder="t('common.selectPlaceholder')"
              class="w-full" @update:value="(v: any) => (form.intelType = v || '')">
              <a-select-option v-for="o in intelTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
            </a-select>
          </div>
          <div class="form-field">
            <label>{{ t('intel.source') }} <span class="req">*</span></label>
            <a-input v-model:value="form.source" :placeholder="t('intel.sourcePlaceholder')" :maxlength="100" />
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('intel.title') }} <span class="req">*</span></label>
          <a-input v-model:value="form.title" :placeholder="t('intel.titlePlaceholder')" :maxlength="100" show-count />
        </div>
      </div>

      <!-- 情报内容卡片 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="dot" /> {{ t('intel.groupContent') }}
          <span class="tip">{{ t('intel.contentRichTip') }}</span>
        </div>
        <RichEditor v-model:model-value="form.content" :placeholder="t('intel.contentEditorPlaceholder')" />
      </div>

      <!-- 预览 -->
      <div class="text-center mb-6">
        <a-button type="dashed" @click="showPreview = !showPreview">
          <template #icon><EyeOutlined /></template>
          {{ showPreview ? t('intel.previewCollapse') : t('intel.previewToggle') }}
        </a-button>
        <div v-if="showPreview" class="preview-card">
          <div class="preview-label">{{ t('intel.previewLabel') }}</div>
          <div class="preview-inner">
            <a-tag v-if="form.brand" color="purple">{{ form.brand }}</a-tag>
            <a-tag v-if="form.product">{{ form.product }}</a-tag>
            <a-tag v-if="form.intelType" :color="intelTypeColor[form.intelType] || 'default'">
              {{ t('dict.intelType.' + form.intelType) }}
            </a-tag>
            <div class="preview-title">{{ form.title || t('intel.previewNoTitle') }}</div>
            <div class="preview-meta">
              {{ t('intel.source') }}: {{ form.source || t('intel.previewNoSource') }} ·
              {{ t('intel.submitter') }}: {{ t('intel.currentUser') }} · {{ today }}
            </div>
            <div class="preview-divider" />
            <div v-if="form.content" class="preview-body" v-html="form.content" />
            <div v-else class="preview-empty">{{ t('intel.previewNoContent') }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部固定操作栏 -->
    <div class="bottom-bar">
      <a-button size="large" @click="goBack">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
        {{ t('intel.submitAction') }}
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
import { ArrowLeftOutlined, EyeOutlined } from '@ant-design/icons-vue'
import RichEditor from '@/components/rich-editor/index.vue'
import { submitCompetitorIntel } from '@/apis/intel/intelApi'

defineOptions({ name: 'IntelSubmit' })
definePage({
  name: 'IntelSubmit',
  meta: {
    layout: false,
    menu: false,
    title: 'intel.submit'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const form = reactive<{
  brand: string
  product: string
  intelType: string
  source: string
  title: string
  content: string
}>({
  brand: '',
  product: '',
  intelType: '',
  source: '',
  title: '',
  content: ''
})

const showPreview = ref(false)
const submitting = ref(false)
const today = new Date().toISOString().slice(0, 10)

const brandOptions = ['Sierra Wireless', 'Telit', '广和通', '移远', 'u-blox', 'Thales']
const intelTypeColor: Record<string, string> = {
  new_product: 'green',
  price_change: 'orange',
  customer_case: 'blue',
  other: 'default'
}
const intelTypeOptions = computed(() => [
  { label: t('dict.intelType.new_product'), value: 'new_product' },
  { label: t('dict.intelType.price_change'), value: 'price_change' },
  { label: t('dict.intelType.customer_case'), value: 'customer_case' },
  { label: t('dict.intelType.other'), value: 'other' }
])

function goBack() {
  router.push({ path: '/intel' })
}

async function handleSubmit() {
  if (!form.brand) return message.warning(t('intel.warnBrand'))
  if (!form.title.trim()) return message.warning(t('intel.warnTitle'))
  if (!form.source.trim()) return message.warning(t('intel.warnSource'))
  if (!form.content.trim()) return message.warning(t('intel.warnContent'))
  submitting.value = true
  try {
    await submitCompetitorIntel({
      brand: form.brand,
      source: form.source,
      title: form.title,
      content: form.content,
      ...(form.product ? { product: form.product } : {}),
      ...(form.intelType ? { intelType: form.intelType } : {})
    })
    message.success(t('intel.submitSuccess'))
    goBack()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.submit-page {
  min-height: 100%;
  background: hsl(var(--card-bg));
  padding-bottom: 80px;
}
.page-header-bar {
  background: #fff;
  padding: 14px 24px;
  border-bottom: 1px solid hsl(var(--line));
  display: flex;
  align-items: center;
  gap: 12px;
  position: sticky;
  top: 0;
  z-index: 10;
}
.page-header-bar .back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: hsl(var(--secondary-text));
  cursor: pointer;
  transition: color 0.2s;
}
.page-header-bar .back-btn:hover {
  color: hsl(var(--primary));
}
.page-header-bar .divider {
  width: 1px;
  height: 20px;
  background: hsl(var(--line));
}
.page-header-bar h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--text));
}
.form-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px;
}
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 20px;
  border: 1px solid hsl(var(--line));
}
.form-card-title {
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
.form-card-title .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: hsl(var(--primary));
}
.form-card-title .tip {
  font-size: 12px;
  font-weight: 400;
  color: hsl(var(--secondary-text));
  margin-left: 4px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 4px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}
.form-field label {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
}
.form-field .req {
  color: hsl(var(--error));
}
.preview-card {
  background: hsl(var(--primary) / 0.05);
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid hsl(var(--primary) / 0.2);
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
  border-radius: 10px;
  padding: 16px 20px;
  border: 1px solid hsl(var(--line));
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
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid hsl(var(--line));
  padding: 12px 24px;
  display: flex;
  justify-content: center;
  gap: 16px;
  z-index: 20;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.04);
}
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
