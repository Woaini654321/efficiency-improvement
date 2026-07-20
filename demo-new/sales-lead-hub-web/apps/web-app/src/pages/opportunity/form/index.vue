<template>
  <div class="opp-form">
    <!-- Z1 顶部 sticky 标题栏 -->
    <div class="form-header">
      <a-button type="text" @click="handleBack">
        <template #icon><ArrowLeftOutlined /></template>
        {{ t('common.back') }}
      </a-button>
      <h3 class="header-title">{{ headerTitle }}</h3>
      <span v-if="isEdit" class="header-id">ID: {{ editId }}</span>
      <span v-else-if="isCopy" class="header-id">{{ t('opportunity.copyFromLabel') }} {{ copyId }}</span>
      <div class="flex-1" />
      <div class="autosave">
        <span class="as-dot" :style="{ background: autoSaveMeta.color }" />
        <span>{{ autoSaveMeta.text }}</span>
      </div>
    </div>

    <!-- copyFrom 提示 -->
    <div v-if="isCopy" class="tip-banner copy">
      <InfoCircleOutlined />
      <span>{{ t('opportunity.copyTip') }}</span>
    </div>

    <div class="form-body">
      <!-- Z2 基础信息 -->
      <div class="form-card">
        <div class="form-card-title"><span class="dot" />{{ t('opportunity.baseInfo') }}</div>

        <!-- 代发布提示 banner -->
        <div v-if="isProxyPublish" class="tip-banner proxy">
          <UserSwitchOutlined />
          <span>{{ t('opportunity.proxyBanner', { name: publisherLabel }) }}</span>
        </div>

        <!-- 发布者（可选 PM 代发布） -->
        <div class="field">
          <label class="field-label"><UserOutlined /> {{ t('opportunity.publisherField') }}</label>
          <div class="field-inline">
            <a-select
              show-search
              size="large"
              class="flex-1"
              :value="publisherId"
              :options="publisherOptions"
              :filter-option="filterPublisher"
              @update:value="(v: any) => (publisherId = v)"
            />
            <a-tag v-if="isProxyPublish" color="orange" class="proxy-tag">
              <UserSwitchOutlined /> {{ t('opportunity.proxyPublish') }}
            </a-tag>
          </div>
        </div>

        <!-- 标题 -->
        <div class="field">
          <label class="field-label required">{{ t('opportunity.title') }}</label>
          <a-input
            v-model:value="formModel.title"
            size="large"
            :maxlength="100"
            show-count
            :placeholder="t('opportunity.titlePlaceholder')"
          />
        </div>

        <div class="field-row">
          <!-- 类型 -->
          <div class="field flex-1">
            <label class="field-label required">{{ t('opportunity.type') }}</label>
            <a-select
              size="large"
              class="w-full"
              :value="formModel.type"
              :options="typeOptions"
              :placeholder="t('common.selectPlaceholder')"
              @update:value="(v: any) => (formModel.type = v)"
            />
          </div>
          <!-- 分类标签级联多选 -->
          <div class="field flex-2">
            <label class="field-label required">{{ t('opportunity.category') }}</label>
            <a-cascader
              v-model:value="formModel.categoryIds"
              :options="categoryTree"
              multiple
              :max-tag-count="3"
              size="large"
              class="w-full"
              :placeholder="t('opportunity.categoryCascaderPlaceholder')"
            />
            <div class="field-extra">{{ t('opportunity.categoryCountTip') }}</div>
          </div>
        </div>

        <div class="field-row">
          <!-- 行业场景 -->
          <div class="field flex-1">
            <label class="field-label">{{ t('opportunity.industry') }}</label>
            <a-input
              v-model:value="formModel.industry"
              :maxlength="50"
              show-count
              :placeholder="t('opportunity.industryPlaceholder')"
            />
          </div>
          <!-- 关键词标签 -->
          <div class="field flex-2">
            <label class="field-label">{{ t('opportunity.keywords') }}</label>
            <a-select
              mode="tags"
              class="w-full"
              :value="formModel.keywords"
              :placeholder="t('opportunity.keywordsPlaceholder')"
              @update:value="(v: any) => (formModel.keywords = v)"
            />
          </div>
        </div>

        <!-- 摘要 -->
        <div class="field">
          <label class="field-label">{{ t('opportunity.summary') }}</label>
          <a-textarea
            v-model:value="formModel.summary"
            :rows="3"
            :maxlength="200"
            show-count
            :placeholder="t('opportunity.summaryPlaceholder')"
          />
        </div>
      </div>

      <!-- Z3 方案详情（富文本） -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="dot" />{{ t('opportunity.content') }}
          <a-tag color="red" class="required-tag">{{ t('opportunity.requiredOnPublish') }}</a-tag>
        </div>
        <RichEditor v-model:modelValue="formModel.content" :placeholder="t('opportunity.contentPlaceholder')" />
      </div>

      <!-- Z4 附件上传 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="dot" />{{ t('opportunity.attachments') }}
          <span class="attach-summary">
            {{ t('opportunity.attachSummary', { count: fileList.length, size: totalSizeMB }) }}
          </span>
        </div>
        <QUpload
          type="dragger"
          manual
          multiple
          :max-count="20"
          :max-total-size="200"
          :max-file-size="50"
          :value="fileList"
          @update:value="onFileChange"
        >
          <template #tips>
            <span class="upload-tip">{{ t('opportunity.uploadTip') }}</span>
          </template>
        </QUpload>
      </div>
    </div>

    <!-- Z5 底部固定操作栏 -->
    <div class="bottom-bar">
      <a-button size="large" class="round-btn" :loading="saving" @click="handleSaveDraft">
        <template #icon><SaveOutlined /></template>
        {{ t('common.saveDraft') }}
      </a-button>
      <a-button type="primary" size="large" class="round-btn" :loading="publishing" @click="handlePublishClick">
        <template #icon><SendOutlined /></template>
        {{ isProxyPublish ? t('opportunity.proxyPublish') : (isCopy ? t('opportunity.publishAsNew') : t('opportunity.publish')) }}
      </a-button>
    </div>

    <!-- 发布确认弹窗 -->
    <a-modal
      :open="publishConfirmOpen"
      :title="isProxyPublish ? t('opportunity.confirmProxyPublish') : t('opportunity.confirmPublish')"
      :ok-text="isProxyPublish ? t('opportunity.confirmProxyOk') : t('opportunity.confirmPublishOk')"
      :cancel-text="t('common.cancel')"
      :confirm-loading="publishing"
      centered
      @ok="handlePublishConfirm"
      @cancel="publishConfirmOpen = false"
    >
      <p class="confirm-tip">{{ t('opportunity.publishConfirmTip') }}</p>

      <div class="notify-opt">
        <a-checkbox v-model:checked="notify.syncEmailSubscribers">
          {{ t('opportunity.notifySyncEmail') }}
          <span class="opt-hint">{{ t('opportunity.notifySyncEmailHint') }}</span>
        </a-checkbox>
      </div>
      <div class="notify-opt">
        <a-checkbox v-model:checked="notify.sendEmail">
          {{ t('opportunity.notifySendEmail') }}
          <span class="opt-hint">{{ t('opportunity.notifySendEmailHint') }}</span>
        </a-checkbox>
      </div>
      <div class="notify-opt">
        <a-checkbox v-model:checked="notify.notifyRelated">
          {{ t('opportunity.notifyRelated') }}
          <span class="opt-hint">{{ t('opportunity.notifyRelatedHint') }}</span>
        </a-checkbox>
      </div>
      <div v-if="isEdit" class="notify-opt">
        <a-checkbox v-model:checked="notify.notifyUpdated">
          {{ t('opportunity.notifyUpdated') }}
        </a-checkbox>
      </div>

      <!-- 邮件通知人 -->
      <div v-if="notify.syncEmailSubscribers || notify.sendEmail" class="email-recipients">
        <div class="field-label">{{ t('opportunity.emailRecipients') }}</div>
        <a-select
          mode="tags"
          class="w-full"
          :value="emailRecipients"
          :options="emailOptions"
          :token-separators="[',', ';', ' ']"
          :filter-option="filterEmail"
          :placeholder="t('opportunity.emailRecipientsPlaceholder')"
          @update:value="(v: any) => (emailRecipients = v)"
        />
      </div>

      <!-- 代发布提示 -->
      <div v-if="isProxyPublish" class="tip-banner proxy mt-3">
        <UserSwitchOutlined />
        <span>{{ t('opportunity.proxyModalTip', { publisher: publisherLabel, agent: currentUser.name }) }}</span>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined, InfoCircleOutlined, UserOutlined, UserSwitchOutlined,
  SaveOutlined, SendOutlined
} from '@ant-design/icons-vue'
import RichEditor from '@/components/rich-editor/index.vue'
import QUpload from '@/components/q-upload/index.vue'
import { getOpportunityDetail, createOpportunity, updateOpportunity } from '@/apis/opportunity/opportunityApi'
import { fetchCategoryTree, type CategoryTreeNode } from '@/apis/category/categoryTree'
import { getEmployeePage } from '@/apis/employee/employeeApi'
import type { OpportunityCreateParams } from '@/apis/opportunity/types'

defineOptions({ name: 'OpportunityForm' })
definePage({
  name: 'OpportunityForm',
  meta: {
    layout: false,
    menu: false,
    title: 'opportunity.form'
  } satisfies RouteMeta
})

interface UploadFile { uid: string; name: string; size?: number; status?: string }
interface PmUser { id: string; name: string; dept: string }

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const editId = route.query.id as string | undefined
const copyId = route.query.copyFrom as string | undefined
const isEdit = computed(() => !!editId)
const isCopy = computed(() => !!copyId)

const headerTitle = computed(() =>
  isEdit.value ? t('opportunity.editTitle') : isCopy.value ? t('opportunity.copyAsNew') : t('opportunity.add')
)

// ======== 代发布（PM 人员选择器切远程 employee/page）========
const currentUser = { id: 'U-SELF', name: 'San.Zhang 张三', dept: '销售部' }
// PM 列表来自 employee/page（id = 后端真实用户 id，resolvePublisher 按 id 查本地档案）。
// 代发布要求操作人是产品经理/管理员，销售账号选别人会被后端 403（fail-closed，符合预期）。
// onMounted 加载，失败兜底空数组（选择器只剩「本人」，不阻塞发布）。
const pmList = ref<PmUser[]>([])
const publisherId = ref(currentUser.id)
const publisherOptions = computed(() => [
  { label: `${currentUser.name}（${t('opportunity.self')}）`, value: currentUser.id },
  ...pmList.value.map((p) => ({ label: `${p.name}（${p.dept}）`, value: p.id }))
])
const isProxyPublish = computed(() => publisherId.value !== currentUser.id)
const publisherLabel = computed(() => {
  const pm = pmList.value.find((p) => p.id === publisherId.value)
  return pm ? `${pm.name}（${pm.dept}）` : currentUser.name
})
async function loadPmList() {
  const emp = await getEmployeePage({ pageNumber: 1, pageSize: 500 }).catch(() => null)
  pmList.value = (emp?.records ?? []).map((e) => ({
    id: e.id,
    name: e.name,
    dept: e.departmentName
  }))
}
function filterPublisher(input: string, option: any): boolean {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
}

// ======== 分类树（远程，SSOT = DB category 表） ========
const categoryTree = ref<CategoryTreeNode[]>([])

const typeOptions = computed(() => [
  { label: t('dict.oppType.product_info'), value: 'product_info' },
  { label: t('dict.oppType.solution'), value: 'solution' },
  { label: t('dict.oppType.success_case'), value: 'success_case' }
])

const emailOptions = [
  { label: '李明 Ming.Li — ming.li@company.com', value: 'ming.li@company.com' },
  { label: '周涛 Tao.Zhou — tao.zhou@company.com', value: 'tao.zhou@company.com' },
  { label: '赵伟 David.Zhao — david.zhao@company.com', value: 'david.zhao@company.com' },
  { label: '张红 Hong.Zhang — hong.zhang@company.com', value: 'hong.zhang@company.com' },
  { label: '王芳 Fang.Wang — fang.wang@company.com', value: 'fang.wang@company.com' }
]
function filterEmail(input: string, option: any): boolean {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
}

// ======== 表单模型 ========
const formModel = reactive<{
  title: string
  type: string
  categoryIds: string[][]
  industry: string
  keywords: string[]
  summary: string
  content: string
  /** 编辑回填的乐观锁版本，随 update 提交；新建/复制恒为 0 */
  version: number
}>({
  title: '',
  type: '',
  categoryIds: [],
  industry: '',
  keywords: [],
  summary: '',
  content: '',
  version: 0
})

// ======== 附件 ========
const fileList = ref<UploadFile[]>([])
const totalSizeMB = computed(() =>
  (fileList.value.reduce((sum, f) => sum + (f.size || 0), 0) / 1048576).toFixed(1)
)
function onFileChange(list: UploadFile[]) {
  fileList.value = list
  markDirty()
}

// ======== 脏检测 + 自动保存（60s，展示层）========
const dirty = ref(false)
function markDirty() {
  dirty.value = true
  if (autoSaveStatus.value === 'saved') autoSaveStatus.value = 'idle'
}
const autoSaveStatus = ref<'idle' | 'saving' | 'saved'>('idle')
const lastSaveTime = ref('')
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const autoSaveMeta = computed(() => {
  if (autoSaveStatus.value === 'saving') return { color: '#faad14', text: t('opportunity.autoSaving') }
  if (autoSaveStatus.value === 'saved') return { color: '#52c41a', text: t('opportunity.autoSaved', { time: lastSaveTime.value }) }
  return { color: 'hsl(var(--line))', text: t('opportunity.autoSaveIdle') }
})

function nowTime(): string {
  return new Date().toTimeString().slice(0, 8)
}

// ======== 发布确认 / 通知 ========
const saving = ref(false)
const publishing = ref(false)
const publishConfirmOpen = ref(false)
const notify = reactive({
  syncEmailSubscribers: true,
  sendEmail: false,
  notifyRelated: false,
  notifyUpdated: false
})
const emailRecipients = ref<string[]>([])

function buildParams(status: 'draft' | 'published'): OpportunityCreateParams {
  const categoryIds = formModel.categoryIds
    .map((path) => path[path.length - 1])
    .filter((v): v is string => !!v)
  return {
    // 存草稿/发布共用一套字段，靠 status 区分——后端按状态条件校验
    // （draft 只要标题；published 须分类 1~5 + 正文）
    status,
    title: formModel.title,
    type: formModel.type,
    categoryIds,
    industry: formModel.industry,
    keywords: formModel.keywords,
    summary: formModel.summary,
    content: formModel.content,
    ...(isProxyPublish.value ? { publisherId: publisherId.value } : {})
  }
}

async function handleSaveDraft() {
  if (!formModel.title.trim()) {
    message.warning(t('opportunity.draftNeedTitle'))
    return
  }
  saving.value = true
  try {
    const params = buildParams('draft')
    if (isEdit.value) await updateOpportunity({ id: editId as string, version: formModel.version, ...params })
    else await createOpportunity(params)
    dirty.value = false
    autoSaveStatus.value = 'saved'
    lastSaveTime.value = nowTime()
    message.success(t('opportunity.draftSaved'))
  } finally {
    saving.value = false
  }
}

function handlePublishClick() {
  if (!formModel.title.trim()) {
    message.error(t('opportunity.titlePlaceholder'))
    return
  }
  if (!formModel.type) {
    message.error(t('common.selectPlaceholder'))
    return
  }
  if (formModel.categoryIds.length < 1 || formModel.categoryIds.length > 5) {
    message.error(t('opportunity.categoryCountTip'))
    return
  }
  if (!formModel.content.replace(/<[^>]*>/g, '').trim()) {
    message.error(t('opportunity.contentRequired'))
    return
  }
  publishConfirmOpen.value = true
}

async function handlePublishConfirm() {
  publishing.value = true
  try {
    const params = buildParams('published')
    if (isEdit.value) await updateOpportunity({ id: editId as string, version: formModel.version, ...params })
    else await createOpportunity(params)
    dirty.value = false
    publishConfirmOpen.value = false
    message.success(isProxyPublish.value ? t('opportunity.proxyPublishSuccess', { name: publisherLabel.value }) : t('opportunity.publishSuccess'))
    router.push({ path: '/opportunity' })
  } finally {
    publishing.value = false
  }
}

// ======== 返回 / 离开未保存确认 ========
function handleBack() {
  if (dirty.value) {
    Modal.confirm({
      title: t('opportunity.leaveTitle'),
      content: t('opportunity.leaveContent'),
      okText: t('common.confirm'),
      cancelText: t('common.cancel'),
      onOk: () => router.back()
    })
  } else {
    router.back()
  }
}

onBeforeRouteLeave((_to, _from, next) => {
  if (!dirty.value) {
    next()
    return
  }
  Modal.confirm({
    title: t('opportunity.leaveTitle'),
    content: t('opportunity.leaveContent'),
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onOk: () => next(),
    onCancel: () => next(false)
  })
})

// ======== 编辑 / 复制 回填 ========
async function loadForPopulate(sourceId: string, keepTitle: boolean) {
  const d = await getOpportunityDetail(sourceId)
  formModel.title = keepTitle ? d.title : `${d.title}（${t('opportunity.copySuffix')}）`
  formModel.type = d.type
  formModel.summary = d.summary
  formModel.content = d.content
  // 编辑带走 version 参与乐观锁；复制是新建，version 归零
  formModel.version = keepTitle ? d.version : 0
}

onMounted(async () => {
  categoryTree.value = await fetchCategoryTree()
  await loadPmList()
  if (editId) await loadForPopulate(editId, true)
  else if (copyId) {
    await loadForPopulate(copyId, false)
    dirty.value = true
  }
  // 自动保存定时器（展示层，60s）
  autoSaveTimer = setInterval(() => {
    if (dirty.value) {
      autoSaveStatus.value = 'saving'
      setTimeout(() => {
        autoSaveStatus.value = 'saved'
        lastSaveTime.value = nowTime()
        dirty.value = false
      }, 800)
    }
  }, 60000)
})

onUnmounted(() => {
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})

// 表单字段变动 → 标脏（用 watch 覆盖 reactive 全字段）
watch(
  () => [formModel.title, formModel.type, formModel.categoryIds, formModel.industry, formModel.keywords, formModel.summary, formModel.content, publisherId.value],
  () => markDirty(),
  { deep: true }
)
</script>

<style scoped>
.opp-form {
  height: 100%;
  overflow: auto;
  background: hsl(var(--card-bg));
  padding-bottom: 88px;
  position: relative;
}

/* sticky 标题栏 */
.form-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 14px 24px;
  border-bottom: 1px solid hsl(var(--line));
}
.header-title {
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--text));
  margin: 0;
}
.header-id {
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.flex-1 {
  flex: 1;
}
.autosave {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.as-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

/* 提示 banner */
.tip-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 13px;
  margin: 16px auto 0;
  max-width: 860px;
}
.tip-banner.copy {
  background: hsl(var(--primary) / 0.08);
  border: 1px solid hsl(var(--primary) / 0.25);
  color: hsl(var(--primary));
}
.tip-banner.proxy {
  background: #fff7e6;
  border: 1px solid #ffd591;
  color: #ad6800;
  margin: 0 0 16px;
  max-width: none;
}
.tip-banner.mt-3 {
  margin-top: 12px;
}

.form-body {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px 24px 0;
}
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 20px;
  border: 1px solid hsl(var(--line));
}
.form-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid hsl(var(--line));
}
.form-card-title .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: hsl(var(--primary));
}
.required-tag {
  margin-left: 4px;
  font-size: 11px;
}
.attach-summary {
  margin-left: auto;
  font-size: 12px;
  font-weight: 400;
  color: hsl(var(--secondary-text));
}

/* 字段布局 */
.field {
  margin-bottom: 16px;
}
.field-row {
  display: flex;
  gap: 16px;
}
.flex-1 {
  flex: 1;
}
.flex-2 {
  flex: 2;
}
.w-full {
  width: 100%;
}
.field-label {
  display: block;
  font-size: 13px;
  color: hsl(var(--text));
  margin-bottom: 6px;
}
.field-label.required::before {
  content: '*';
  color: hsl(var(--error));
  margin-right: 4px;
}
.field-inline {
  display: flex;
  align-items: center;
  gap: 12px;
}
.proxy-tag {
  border-radius: 8px;
  padding: 4px 10px;
}
.field-extra {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-top: 4px;
}
.upload-tip {
  color: hsl(var(--secondary-text));
  font-size: 12px;
}

/* 底部固定操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid hsl(var(--line));
  padding: 14px 24px;
  display: flex;
  justify-content: center;
  gap: 16px;
  z-index: 100;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.04);
}
.round-btn {
  border-radius: 10px;
}

/* 发布确认弹窗 */
.confirm-tip {
  margin-bottom: 16px;
  color: hsl(var(--secondary-text));
}
.notify-opt {
  margin-bottom: 12px;
}
.opt-hint {
  color: hsl(var(--secondary-text));
  font-size: 12px;
  margin-left: 6px;
}
.email-recipients {
  margin-bottom: 12px;
}
</style>
