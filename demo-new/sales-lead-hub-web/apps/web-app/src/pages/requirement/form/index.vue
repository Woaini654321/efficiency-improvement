<template>
  <div class="req-form">
    <!-- 顶部标题栏 -->
    <div class="form-header">
      <a-button type="text" @click="handleBack"><template #icon><ArrowLeftOutlined /></template>{{ t('common.back') }}</a-button>
      <h3>{{ isEdit ? t('requirement.editTitle') : t('requirement.add') }}</h3>
    </div>

    <div class="form-body">
      <a-alert type="info" show-icon class="mb-4" :message="t('requirement.roleTip')" />

      <!-- 基础信息 -->
      <div class="card">
        <div class="card-title">{{ t('requirement.baseInfo') }}</div>

        <div class="field">
          <label class="req">{{ t('requirement.title') }}</label>
          <a-input :value="model.title" :maxlength="100" show-count :placeholder="t('requirement.titlePlaceholder')"
            @update:value="(v: any) => { model.title = v ?? ''; dirty = true }" />
        </div>

        <div class="field">
          <label class="req">{{ t('requirement.urgency') }}</label>
          <a-select :value="model.urgency" class="w-[220px]" @update:value="(v: any) => { model.urgency = v; dirty = true }">
            <a-select-option v-for="o in urgencyOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
          </a-select>
          <div v-if="model.urgency === 'critical'" class="critical-tip"><AlertOutlined /> {{ t('requirement.criticalInline') }}</div>
        </div>

        <div class="field">
          <label>{{ t('requirement.industry') }}</label>
          <a-input :value="model.industry" :maxlength="50" show-count :placeholder="t('requirement.industryPlaceholder')"
            @update:value="(v: any) => { model.industry = v ?? ''; dirty = true }" />
        </div>

        <div class="field">
          <label>{{ t('requirement.keywords') }}</label>
          <a-select mode="tags" :value="model.keywords" class="w-full" :placeholder="t('requirement.keywordsPlaceholder')"
            @update:value="(v: any) => { model.keywords = v ?? []; dirty = true }" />
        </div>

        <div class="field">
          <label class="req">{{ t('requirement.category') }}</label>
          <a-cascader v-model:value="model.categoryIds" :options="categoryTree" multiple
            max-tag-count="responsive" class="w-full" :placeholder="t('requirement.categoryPlaceholder')"
            @change="dirty = true" />
          <div class="field-extra">{{ t('requirement.categoryExtra') }}</div>
        </div>
      </div>

      <!-- 可见范围 -->
      <div class="card">
        <div class="card-title flex items-center gap-2"><EyeOutlined /> {{ t('requirement.visibility') }}</div>
        <a-radio-group v-model:value="model.visibilityType" class="mb-3" @change="dirty = true">
          <a-radio-button value="all">{{ t('requirement.visibilityType.all') }}</a-radio-button>
          <a-radio-button value="dept">{{ t('requirement.visibleByDept') }}</a-radio-button>
          <a-radio-button value="personnel">{{ t('requirement.visibleByPerson') }}</a-radio-button>
        </a-radio-group>

        <a-tree-select v-if="model.visibilityType === 'dept'" v-model:value="model.visibleDepts" :tree-data="deptTree"
          tree-checkable :show-checked-strategy="SHOW_PARENT" class="w-full" max-tag-count="responsive"
          allow-clear :placeholder="t('requirement.deptPlaceholder')" @change="dirty = true" />

        <a-select v-else-if="model.visibilityType === 'personnel'" mode="multiple" :value="model.visiblePersonnel"
          :options="personnelList" class="w-full" max-tag-count="responsive" allow-clear
          :filter-option="filterOption" :placeholder="t('requirement.personPlaceholder')"
          @update:value="(v: any) => { model.visiblePersonnel = v ?? []; dirty = true }" />

        <div v-else class="scope-all"><CheckCircleOutlined /> {{ t('requirement.visibilityAllHint') }}</div>
      </div>

      <!-- 邀请产品线回答 -->
      <div class="card">
        <div class="card-title flex items-center gap-2"><UsergroupAddOutlined /> {{ t('requirement.inviteProductLine') }}</div>
        <a-cascader v-model:value="model.invitedProductLines" :options="productLineTree" multiple
          max-tag-count="responsive" class="w-full" allow-clear :placeholder="t('requirement.invitePlaceholder')"
          @change="dirty = true" />
        <div v-if="model.invitedProductLines.length" class="invite-hint on">
          <InfoCircleOutlined /> {{ t('requirement.inviteSelected', { n: model.invitedProductLines.length }) }}
        </div>
        <div v-else class="invite-hint"><InfoCircleOutlined /> {{ t('requirement.inviteAuto') }}</div>
      </div>

      <!-- 需求详情 -->
      <div class="card">
        <div class="card-title req">{{ t('requirement.solutionDetail') }}</div>
        <RichEditor v-model:modelValue="model.description" :placeholder="t('requirement.descriptionPlaceholder')" />
      </div>

      <!-- 附件上传 -->
      <div class="card">
        <div class="card-title flex items-center justify-between">
          <span>{{ t('requirement.attachments') }}</span>
          <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ model.files.length }}/10</span>
        </div>
        <QUpload type="dragger" manual multiple :max-count="10" :max-file-size="50"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,.rar,.jpg,.jpeg,.png,.gif"
          @update:value="(v: any) => { model.files = v; dirty = true }" />
      </div>

      <!-- 相似需求检测 -->
      <div class="card">
        <div class="card-title flex items-center gap-2"><ProfileOutlined /> {{ t('requirement.similarTitle') }}</div>

        <div v-if="(model.title || '').length < 2" class="similar-empty">
          <SearchOutlined class="block text-[20px] mb-2" />{{ t('requirement.similarIdle') }}
        </div>
        <div v-else-if="detecting" class="similar-empty">
          <LoadingOutlined class="block text-[20px] mb-2" />{{ t('requirement.similarDetecting') }}
        </div>
        <div v-else-if="similarResults.length" class="similar-list">
          <div class="similar-lead"><InfoCircleOutlined /> {{ t('requirement.similarLead') }}</div>
          <div v-for="r in similarResults" :key="r.id" class="similar-item">
            <div class="min-w-0 flex-1">
              <div class="similar-name">{{ r.title }}</div>
              <div class="similar-meta">{{ (r.categoryNames.join(' / ') || '--') }} · {{ t('dict.reqStatus.' + r.status) }}</div>
            </div>
            <div class="flex items-center gap-3 flex-shrink-0">
              <span class="similar-score" :style="{ color: scoreColor(r.score) }">{{ r.score }}%</span>
              <a-button type="link" size="small" @click="goDetail(r.id)">{{ t('common.view') }}</a-button>
            </div>
          </div>
        </div>
        <div v-else class="similar-ok"><CheckCircleOutlined /> {{ t('requirement.similarNone') }}</div>
      </div>
    </div>

    <!-- 底部固定栏 -->
    <div class="form-footer">
      <a-button size="large" @click="handleBack">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" size="large" @click="handlePublish">
        <template #icon><SendOutlined /></template>{{ isEdit ? t('requirement.saveEdit') : t('requirement.publish') }}
      </a-button>
    </div>

    <!-- 发布确认弹窗 -->
    <a-modal :open="confirmOpen" :title="t('requirement.publishConfirmTitle')" :ok-text="highSimilar ? t('requirement.stillPublish') : t('requirement.confirmPublish')"
      :cancel-text="t('common.cancel')" @ok="doPublish" @cancel="confirmOpen = false">
      <p>{{ t('requirement.publishConfirmDesc') }}</p>
      <div class="confirm-box">
        <div class="confirm-row"><EyeOutlined /> <b>{{ t('requirement.visibility') }}：</b>{{ visibilityLabel }}</div>
        <div v-if="model.invitedProductLines.length" class="confirm-row invite">
          <UsergroupAddOutlined /> <b>{{ t('requirement.invitedProductLines') }}：</b>{{ invitedLeafLabels }}
        </div>
        <div v-else class="confirm-row muted"><InfoCircleOutlined /> {{ t('requirement.inviteAuto') }}</div>
      </div>
      <p v-if="highSimilar" class="warn-similar"><AlertOutlined /> {{ t('requirement.highSimilarWarn', { n: topScore }) }}</p>
      <p v-if="model.urgency === 'critical'" class="warn-critical"><AlertOutlined /> {{ t('requirement.criticalWarn') }}</p>
      <a-checkbox v-model:checked="notifyEmail">{{ t('requirement.notifyEmailCheck') }}</a-checkbox>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal, TreeSelect } from 'ant-design-vue'
import {
  ArrowLeftOutlined, AlertOutlined, EyeOutlined, CheckCircleOutlined, UsergroupAddOutlined,
  InfoCircleOutlined, ProfileOutlined, SearchOutlined, LoadingOutlined, SendOutlined
} from '@ant-design/icons-vue'
import RichEditor from '@/components/rich-editor/index.vue'
import QUpload from '@/components/q-upload/index.vue'
import { getRequirementList, getRequirementDetail, createRequirement, updateRequirement } from '@/apis/requirement/requirementApi'
import type { RequirementItem } from '@/apis/requirement/types'
import options from '@/apis/requirement/mocks/requirementOptions.json'

defineOptions({ name: 'RequirementForm' })
definePage({
  name: 'RequirementForm',
  meta: {
    layout: false,
    menu: false,
    title: 'requirement.form'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const SHOW_PARENT = TreeSelect.SHOW_PARENT

const editId = route.query.id as string | undefined
const isEdit = computed(() => !!editId)

interface FormModel {
  title: string
  urgency: string
  industry: string
  keywords: string[]
  categoryIds: any[]
  visibilityType: string
  visibleDepts: string[]
  visiblePersonnel: string[]
  invitedProductLines: any[]
  description: string
  files: any[]
}
const model = reactive<FormModel>({
  title: '',
  urgency: 'normal',
  industry: '',
  keywords: [],
  categoryIds: [],
  visibilityType: 'all',
  visibleDepts: [],
  visiblePersonnel: [],
  invitedProductLines: [],
  description: '',
  files: []
})
const dirty = ref(false)

const urgencyOptions = computed(() => [
  { label: t('dict.urgency.normal'), value: 'normal' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.critical'), value: 'critical' }
])

// ===== 选项 mock（UI 目录，集中在 requirementOptions.json） =====
const categoryTree = options.categoryTree
const deptTree = options.deptTree
const personnelList = options.personnelList
const productLineTree = options.productLineTree

function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ===== 相似需求检测（引用需求列表作为池，SSOT） =====
const existingReqs = ref<RequirementItem[]>([])
const similarResults = ref<{ id: string; title: string; status: string; categoryNames: string[]; score: number }[]>([])
const detecting = ref(false)
let detectTimer: ReturnType<typeof setTimeout> | null = null

const STOP_WORDS = options.stopWords
function extractKeywords(text: string): string[] {
  if (!text) return []
  const tokens = text.split(/[\s,，、。.；;：:！!？?()（）【】[\]《》<>/\\|—-]+/)
  const seen: Record<string, boolean> = {}
  const out: string[] = []
  tokens.forEach((raw) => {
    const w = raw.toLowerCase().trim()
    if (w.length >= 2 && !STOP_WORDS.includes(w) && !seen[w]) { seen[w] = true; out.push(w) }
  })
  return out
}
function similarity(inputTitle: string, req: RequirementItem): number {
  const a = extractKeywords(inputTitle)
  const b = extractKeywords(req.title)
  if (!a.length) return 0
  let hit = 0
  a.forEach((k) => { if (b.includes(k)) hit++ })
  let score = (hit / a.length) * 100
  const cats = model.categoryIds.map((path) => path[path.length - 1] || '').join(' ')
  extractKeywords(cats).forEach((ct) => { if (req.categoryNames.some((c) => c.toLowerCase().includes(ct))) score = Math.min(score + 15, 100) })
  return Math.round(score)
}
function runDetect() {
  const title = model.title || ''
  if (title.length < 2) { similarResults.value = []; detecting.value = false; return }
  detecting.value = true
  if (detectTimer) clearTimeout(detectTimer)
  detectTimer = setTimeout(() => {
    similarResults.value = existingReqs.value
      .filter((r) => r.id !== editId)
      .map((r) => ({ id: r.id, title: r.title, status: r.status, categoryNames: r.categoryNames, score: similarity(title, r) }))
      .filter((r) => r.score >= 30)
      .sort((a, b) => b.score - a.score)
    detecting.value = false
  }, 600)
}
function onTitleForDetect() { runDetect() }

const topScore = computed(() => (similarResults.value[0]?.score ?? 0))
const highSimilar = computed(() => topScore.value >= 60)

function scoreColor(s: number): string {
  return s >= 70 ? '#ff4d4f' : s >= 50 ? '#faad14' : '#52c41a'
}
function goDetail(id: string) {
  router.push({ path: '/requirement/detail', query: { id } })
}

// ===== 发布确认 =====
const confirmOpen = ref(false)
const notifyEmail = ref(true)
const visibilityLabel = computed(() =>
  model.visibilityType === 'all' ? t('requirement.visibilityType.all')
    : model.visibilityType === 'dept' ? t('requirement.visibleByDept') : t('requirement.visibleByPerson'))
const invitedLeafLabels = computed(() => model.invitedProductLines.map((p) => leafLabel(productLineTree, p)).join('、'))

function leafLabel(tree: any[], path: string[]): string {
  let nodes = tree
  let label = ''
  for (const v of path) {
    const found = nodes.find((n) => n.value === v)
    if (!found) break
    label = found.label
    nodes = found.children || []
  }
  return label
}

function validate(): boolean {
  if (!model.title.trim()) { message.warning(t('requirement.titlePlaceholder')); return false }
  if (!model.categoryIds.length) { message.warning(t('requirement.categoryPlaceholder')); return false }
  const descText = model.description.replace(/<[^>]+>/g, '').trim()
  if (!descText) { message.warning(t('requirement.descriptionPlaceholder')); return false }
  if (model.visibilityType === 'dept' && !model.visibleDepts.length) { message.warning(t('requirement.deptPlaceholder')); return false }
  if (model.visibilityType === 'personnel' && !model.visiblePersonnel.length) { message.warning(t('requirement.personPlaceholder')); return false }
  return true
}
function handlePublish() {
  if (!validate()) return
  confirmOpen.value = true
}
async function doPublish() {
  const categoryIds = model.categoryIds.map((p) => p[p.length - 1] || '').filter(Boolean)
  const payload = {
    title: model.title,
    urgency: model.urgency,
    industry: model.industry,
    keywords: model.keywords,
    categoryIds,
    visibilityType: model.visibilityType,
    description: model.description
  }
  if (isEdit.value) await updateRequirement({ id: editId as string, ...payload })
  else await createRequirement(payload)
  dirty.value = false
  confirmOpen.value = false
  message.success(isEdit.value ? t('requirement.saveOk') : t('requirement.publishSuccess'))
  router.push('/requirement')
}

// ===== 离开确认 =====
function handleBack() {
  if (dirty.value) {
    Modal.confirm({
      title: t('requirement.leaveTitle'),
      content: t('requirement.leaveContent'),
      okText: t('requirement.leaveOk'),
      cancelText: t('requirement.leaveCancel'),
      onOk: () => { dirty.value = false; router.push('/requirement') }
    })
  } else {
    router.push('/requirement')
  }
}
onBeforeRouteLeave((_to, _from, next) => {
  if (!dirty.value) { next(); return }
  Modal.confirm({
    title: t('requirement.leaveTitle'),
    content: t('requirement.leaveContent'),
    okText: t('requirement.leaveOk'),
    cancelText: t('requirement.leaveCancel'),
    onOk: () => { dirty.value = false; next() },
    onCancel: () => next(false)
  })
})

async function loadPool() {
  const res = await getRequirementList({ pageNumber: 1, pageSize: 999 })
  existingReqs.value = res.records
}
async function populate(rid: string) {
  const d = await getRequirementDetail(rid)
  model.title = d.title
  model.urgency = d.urgency
  model.industry = d.industry
  model.description = d.description
  model.visibilityType = d.visibilityType || 'all'
  dirty.value = false
}

onMounted(async () => {
  await loadPool()
  if (editId) await populate(editId)
})
onUnmounted(() => { if (detectTimer) clearTimeout(detectTimer) })

// 标题/分类变更即触发相似检测
watch(() => model.title, onTitleForDetect)
watch(() => model.categoryIds, onTitleForDetect, { deep: true })
</script>

<style scoped>
.req-form {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: hsl(var(--card-bg));
}
.form-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid hsl(var(--line));
}
.form-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--text));
}
.form-body {
  flex: 1;
  overflow: auto;
  max-width: 820px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 20px 90px;
}
.card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 14px;
}
.field {
  margin-bottom: 16px;
}
.field:last-child {
  margin-bottom: 0;
}
.field label,
.card-title.req {
  display: block;
  font-size: 13px;
  color: hsl(var(--text));
  margin-bottom: 6px;
  font-weight: 500;
}
.field label.req::before,
.card-title.req::before {
  content: '*';
  color: hsl(var(--error));
  margin-right: 4px;
}
.field-extra {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-top: 6px;
}
.critical-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--error));
  font-size: 13px;
  margin-top: 8px;
}
.scope-all {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: hsl(var(--primary) / 0.06);
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 8px;
  font-size: 13px;
  color: hsl(var(--primary));
}
.invite-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  background: hsl(var(--card-bg));
  border: 1px dashed hsl(var(--line));
  color: hsl(var(--secondary-text));
}
.invite-hint.on {
  background: hsl(var(--primary) / 0.06);
  border: 1px solid hsl(var(--primary) / 0.25);
  color: hsl(var(--primary));
}
.similar-empty {
  background: hsl(var(--card-bg));
  border: 1px dashed hsl(var(--line));
  border-radius: 8px;
  padding: 26px 24px;
  text-align: center;
  color: hsl(var(--secondary-text));
  font-size: 14px;
}
.similar-ok {
  display: flex;
  align-items: center;
  gap: 8px;
  background: hsl(var(--primary) / 0.06);
  border: 1px solid hsl(var(--primary) / 0.25);
  border-radius: 8px;
  padding: 16px;
  font-size: 14px;
  color: #52c41a;
}
.similar-lead {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin-bottom: 12px;
}
.similar-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid hsl(var(--line));
}
.similar-item:last-child {
  border-bottom: none;
}
.similar-name {
  font-weight: 500;
  font-size: 14px;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.similar-meta {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
.similar-score {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.form-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid hsl(var(--line));
}
.confirm-box {
  margin: 10px 0;
  padding: 10px 14px;
  background: hsl(var(--card-bg));
  border-radius: 8px;
  font-size: 13px;
}
.confirm-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  color: hsl(var(--text));
}
.confirm-row.invite {
  color: hsl(var(--primary));
}
.confirm-row.muted {
  color: hsl(var(--secondary-text));
}
.warn-similar {
  color: #faad14;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 8px 0 4px;
}
.warn-critical {
  color: hsl(var(--error));
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0 8px;
}
</style>
