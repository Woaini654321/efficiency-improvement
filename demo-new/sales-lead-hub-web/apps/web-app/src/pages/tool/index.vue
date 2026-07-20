<template>
  <div class="tool-page">
    <div class="page-head">
      <h1 class="page-title">{{ t('tool.title') }}</h1>
      <p class="page-sub">{{ t('tool.subtitle') }}</p>
    </div>

    <a-segmented :value="activeTab" :options="tabOptions" class="mb-5"
      @update:value="(v: any) => (activeTab = v)" />

    <!-- ============ 销售工具 ============ -->
    <div v-if="activeTab === 'sales'" class="tools-grid">
      <!-- 报价/折扣计算器 -->
      <div class="tool-card">
        <div class="tool-icon" style="background: hsl(var(--primary) / 0.1); color: hsl(var(--primary))"><CalculatorOutlined /></div>
        <h3 class="tool-title">{{ t('tool.quoteTitle') }}</h3>
        <div class="quote-form">
          <div class="quote-row">
            <span class="quote-label">{{ t('tool.quoteProduct') }}</span>
            <a-select :value="quoteProduct" class="flex-1" @update:value="(v: any) => (quoteProduct = v)">
              <a-select-option v-for="p in mock.quoteProducts" :key="p.value" :value="p.value">{{ p.label }}</a-select-option>
            </a-select>
          </div>
          <div class="quote-row">
            <span class="quote-label">{{ t('tool.quoteQty') }}</span>
            <a-input-number :value="quoteQty" @update:value="(v: any) => (quoteQty = v)" :min="1" :max="1000000" class="flex-1" />
          </div>
          <div class="quote-row">
            <span class="quote-label">{{ t('tool.quoteDiscount') }}</span>
            <a-input-number :value="quoteDiscount" @update:value="(v: any) => (quoteDiscount = v)" :min="0" :max="50" :formatter="discountFormatter" class="flex-1" />
          </div>
        </div>
        <div class="result-box">
          <div class="result-grid">
            <div><span class="muted">{{ t('tool.quoteUnit') }}：</span><strong>{{ unitPrice }} {{ t('tool.yuan') }}</strong></div>
            <div><span class="muted">{{ t('tool.quoteSubtotal') }}：</span><strong>{{ subtotal.toLocaleString() }} {{ t('tool.yuan') }}</strong></div>
            <div><span class="muted">{{ t('tool.quoteDiscountAmt') }}：</span><strong class="result-discount">-{{ discountAmt.toLocaleString() }} {{ t('tool.yuan') }} ({{ quoteDiscount }}%)</strong></div>
            <div><span class="muted">{{ t('tool.quoteMargin') }}：</span><strong class="result-margin">{{ margin.toLocaleString() }} {{ t('tool.yuan') }} (35%)</strong></div>
          </div>
          <div class="result-total">{{ t('tool.quoteTotal') }}：{{ total.toLocaleString() }} {{ t('tool.yuan') }}</div>
        </div>
      </div>

      <!-- 竞品对比表 -->
      <div class="tool-card">
        <div class="tool-icon tool-icon-green"><TableOutlined /></div>
        <h3 class="tool-title">{{ t('tool.compareTitle') }}</h3>
        <p class="tool-desc">{{ t('tool.compareSub') }}</p>
        <div class="compare-table-wrap">
          <table class="compare-table">
            <thead>
              <tr>
                <th>{{ t('tool.compareDim') }}</th>
                <th>{{ t('tool.brandQuectel') }}</th>
                <th>Sierra Wireless</th>
                <th>Telit</th>
                <th>{{ t('tool.brandFibocom') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in mock.compareRows" :key="row.key">
                <td class="compare-dim">{{ row.feature }}</td>
                <td>{{ row.quectel }}</td>
                <td>{{ row.sierra }}</td>
                <td>{{ row.telit }}</td>
                <td>{{ row.fibocom }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="text-right mt-4">
          <a-button type="primary" :loading="compareLoading" @click="runCompareAI">
            <template #icon><ThunderboltOutlined /></template>
            {{ t('tool.aiAnalysis') }}
          </a-button>
        </div>
        <div v-if="showCompareAI" class="ai-result-box">
          <h4 class="ai-result-title"><ThunderboltOutlined /> {{ t('tool.aiReport') }}</h4>
          <div class="swot-grid">
            <div>
              <div class="swot-head swot-strength">{{ t('tool.strengths') }}</div>
              <ul class="swot-list">
                <li v-for="(s, i) in mock.compareStrengths" :key="i">{{ s }}</li>
              </ul>
            </div>
            <div>
              <div class="swot-head swot-weakness">{{ t('tool.weaknesses') }}</div>
              <ul class="swot-list">
                <li v-for="(w, i) in mock.compareWeaknesses" :key="i">{{ w }}</li>
              </ul>
            </div>
          </div>
          <div class="swot-strategy">
            <div class="swot-head" style="color: hsl(var(--primary))">{{ t('tool.strategy') }}</div>
            <p class="m-0">{{ mock.compareStrategy }}</p>
            <div class="flex gap-2 mt-3">
              <a-button size="small" @click="copy(mock.compareStrategy, t('tool.copiedReport'))">{{ t('tool.copyReport') }}</a-button>
              <a-button size="small" type="link" @click="showCompareAI = false">{{ t('tool.collapse') }}</a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 方案模板库 -->
      <div class="tool-card">
        <div class="tool-icon tool-icon-orange"><FileTextOutlined /></div>
        <h3 class="tool-title">{{ t('tool.templateTitle') }}</h3>
        <div class="template-grid">
          <div v-for="tpl in mock.templates" :key="tpl.name" class="template-card"
            @click="message.success(t('tool.mockDownload') + tpl.name)">
            <div class="template-icon" :style="{ background: tpl.bg, color: tpl.color }"><FileOutlined /></div>
            <div class="min-w-0">
              <div class="template-name">{{ tpl.name }}</div>
              <div class="template-desc">{{ tpl.desc }} · {{ tpl.size }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 邮件模板库 -->
      <div class="tool-card">
        <div class="tool-icon tool-icon-purple"><MailOutlined /></div>
        <h3 class="tool-title">{{ t('tool.emailTitle') }}</h3>
        <div class="email-list">
          <div v-for="mailItem in mock.emails" :key="mailItem.subject" class="template-card block"
            @click="copy(mailItem.subject + '\n' + mailItem.preview, t('tool.mockCopyEmail') + mailItem.subject)">
            <div class="email-subject">
              <span class="email-dot" :style="{ background: mailItem.color }" />
              <span class="truncate">{{ mailItem.subject }}</span>
            </div>
            <div class="email-preview">{{ mailItem.preview }}</div>
            <a-tag :color="mailItem.color" :bordered="false" class="mt-1">{{ mailItem.usage }}</a-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ AI 智能工具 ============ -->
    <div v-else class="tools-grid">
      <!-- AI方案推荐 -->
      <div class="tool-card ai-card">
        <div class="tool-icon tool-icon-purple"><BulbOutlined /></div>
        <h3 class="tool-title">{{ t('tool.recommendTitle') }} <span class="ai-badge">AI</span></h3>
        <p class="tool-desc">{{ t('tool.recommendSub') }}</p>
        <div class="chip-hint">{{ t('tool.quickScene') }}</div>
        <div class="chip-row">
          <span v-for="s in mock.scenes" :key="s.key" class="scenario-chip" :class="{ active: recScene === s.key }"
            @click="recScene = s.key; recDesc = s.desc">{{ s.label }}</span>
        </div>
        <a-textarea v-model:value="recDesc" :rows="3" :placeholder="t('tool.reqPlaceholder')" />
        <div class="flex items-center gap-3 mt-3">
          <a-button type="primary" :loading="recLoading" @click="runRecommend">
            <template #icon><BulbOutlined /></template>
            {{ recLoading ? t('tool.analyzing') : t('tool.smartRecommend') }}
          </a-button>
          <a-button v-if="recResult" @click="recResult = null; recDesc = ''; recScene = ''">{{ t('tool.reset') }}</a-button>
        </div>
        <div v-if="recResult" class="ai-result-box">
          <div class="flex items-center gap-2 mb-3">
            <span class="muted">{{ t('tool.matchScore') }}</span>
            <a-progress :percent="recResult.score" size="small" :stroke-color="{ from: '#722ed1', to: '#1890ff' }" class="flex-1 max-w-[160px]" />
          </div>
          <div class="ai-sub-title">{{ t('tool.recommendSolutions') }}</div>
          <div v-for="(sol, i) in recResult.solutions" :key="i" class="sol-item"><FileTextOutlined /> {{ i + 1 }}. {{ sol }}</div>
          <div class="ai-reason"><strong>{{ t('tool.recommendReason') }}</strong>{{ recResult.reason }}</div>
        </div>
      </div>

      <!-- AI话术生成 -->
      <div class="tool-card ai-card">
        <div class="tool-icon tool-icon-pink"><MessageOutlined /></div>
        <h3 class="tool-title">{{ t('tool.scriptTitle') }} <span class="ai-badge">AI</span></h3>
        <p class="tool-desc">{{ t('tool.scriptSub') }}</p>
        <div class="sel-form">
          <div class="sel-row">
            <span class="sel-label">{{ t('tool.role') }}</span>
            <a-select :value="scriptRole" class="flex-1" @update:value="(v: any) => (scriptRole = v)">
              <a-select-option v-for="r in mock.scriptRoles" :key="r.value" :value="r.value">{{ r.label }}</a-select-option>
            </a-select>
          </div>
          <div class="sel-row">
            <span class="sel-label">{{ t('tool.commScene') }}</span>
            <a-select :value="scriptScene" class="flex-1" @update:value="(v: any) => (scriptScene = v)">
              <a-select-option v-for="s in mock.scriptScenes" :key="s.value" :value="s.value">{{ s.label }}</a-select-option>
            </a-select>
          </div>
          <div class="sel-row">
            <span class="sel-label">{{ t('tool.product') }}</span>
            <a-select :value="scriptProduct" class="flex-1" @update:value="(v: any) => (scriptProduct = v)">
              <a-select-option v-for="p in mock.products" :key="p.value" :value="p.value">{{ p.label }}</a-select-option>
            </a-select>
          </div>
        </div>
        <a-button type="primary" block :loading="scriptLoading" class="mt-3" @click="runScript">
          <template #icon><MessageOutlined /></template>
          {{ scriptLoading ? t('tool.generating') : t('tool.genScript') }}
        </a-button>
        <div v-if="scriptResult" class="ai-result-box">
          <div class="ai-result-title mb-3">{{ scriptResult.title }}</div>
          <div class="script-block">
            <div class="script-tag script-tag-pink">{{ t('tool.opening') }}</div>
            <div class="script-text">{{ scriptResult.opening }}</div>
          </div>
          <div class="script-block">
            <div class="script-tag script-tag-blue">{{ t('tool.valuePoints') }}</div>
            <div class="script-text whitespace-pre-line">{{ scriptResult.value }}</div>
          </div>
          <div class="script-block">
            <div class="script-tag script-tag-orange">{{ t('tool.objection') }}</div>
            <div class="script-text">{{ scriptResult.objection }}</div>
          </div>
          <div class="script-block">
            <div class="script-tag script-tag-green">{{ t('tool.nextStep') }}</div>
            <div class="script-text">{{ scriptResult.next }}</div>
          </div>
          <div class="flex gap-2">
            <a-button size="small" @click="copy(scriptFullText, t('tool.copiedAll'))">
              <template #icon><CopyOutlined /></template>{{ t('tool.copyAll') }}
            </a-button>
            <a-button size="small" @click="scriptResult = null">{{ t('tool.regenerate') }}</a-button>
          </div>
        </div>
      </div>

      <!-- AI智能撰写 -->
      <div class="tool-card ai-card">
        <div class="tool-icon tool-icon-cyan"><EditOutlined /></div>
        <h3 class="tool-title">{{ t('tool.writerTitle') }} <span class="ai-badge">AI</span></h3>
        <p class="tool-desc">{{ t('tool.writerSub') }}</p>
        <div class="sel-form">
          <div class="sel-row">
            <span class="sel-label">{{ t('tool.docType') }}</span>
            <a-select :value="writerType" class="flex-1" @update:value="(v: any) => (writerType = v)">
              <a-select-option v-for="d in mock.docTypes" :key="d.value" :value="d.value">{{ d.label }}</a-select-option>
            </a-select>
          </div>
          <div v-if="writerType !== 'proposal'" class="sel-row">
            <span class="sel-label">{{ t('tool.customer') }}</span>
            <a-input v-model:value="writerCustomer" :placeholder="t('tool.customerPlaceholder')" class="flex-1" />
          </div>
          <div class="sel-row">
            <span class="sel-label">{{ t('tool.product') }}</span>
            <a-select :value="writerProduct" class="flex-1" @update:value="(v: any) => (writerProduct = v)">
              <a-select-option v-for="p in mock.products" :key="p.value" :value="p.value">{{ p.label }}</a-select-option>
            </a-select>
          </div>
        </div>
        <a-button type="primary" block :loading="writerLoading" class="mt-3" @click="runWriter">
          <template #icon><EditOutlined /></template>
          {{ writerLoading ? t('tool.writing') : t('tool.genDoc') }}
        </a-button>
        <div v-if="writerResult" class="ai-result-box">
          <div class="writer-output">{{ writerResult }}</div>
          <div class="flex gap-2 mt-3">
            <a-button size="small" @click="copy(writerResult, t('tool.copied'))">
              <template #icon><CopyOutlined /></template>{{ t('tool.copyContent') }}
            </a-button>
            <a-button size="small" @click="writerResult = ''">{{ t('tool.regenerate') }}</a-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  CalculatorOutlined, TableOutlined, FileTextOutlined, FileOutlined, MailOutlined,
  BulbOutlined, MessageOutlined, EditOutlined, ThunderboltOutlined, CopyOutlined
} from '@ant-design/icons-vue'
// tool 是纯前端工具页（话术/邮件/参数计算等本地生成），无对应后端端点，
// 本期刻意保留本地 JSON 作数据源，不纳入 mock→真实切换范围。
import toolMock from '@/apis/tool/mocks/tool.json'
import type { ToolMock, SceneItem, ScriptResult } from '@/apis/tool/types'

defineOptions({ name: 'ToolAssistant' })
definePage({
  name: 'ToolAssistant',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:shop-linear' },
    title: 'tool'
  } satisfies RouteMeta
})

const { t } = useI18n()
const mock = toolMock as unknown as ToolMock

const activeTab = ref<'sales' | 'ai'>('sales')
const tabOptions = computed(() => [
  { label: t('tool.tabSales'), value: 'sales' },
  { label: t('tool.tabAI'), value: 'ai' }
])

// ===== 定时器管理（AI mock 延时） =====
const timers = new Set<ReturnType<typeof setTimeout>>()
function delay(fn: () => void, ms: number) {
  const id = setTimeout(() => {
    timers.delete(id)
    fn()
  }, ms)
  timers.add(id)
}
onUnmounted(() => {
  timers.forEach((id) => clearTimeout(id))
  timers.clear()
})

async function copy(text: string, tip: string) {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // 剪贴板不可用时忽略，仍给出提示
  }
  message.success(tip)
}

// ===== 报价计算器（真实计算） =====
const quoteProduct = ref('RG500Q')
const quoteQty = ref<number>(1000)
const quoteDiscount = ref<number>(15)
function discountFormatter(v?: string | number): string {
  return `${v ?? 0}%`
}
const unitPrice = computed(() => mock.quoteProducts.find((p) => p.value === quoteProduct.value)?.price ?? 0)
const subtotal = computed(() => unitPrice.value * (quoteQty.value || 0))
const discountAmt = computed(() => Math.round(subtotal.value * (quoteDiscount.value || 0) / 100))
const total = computed(() => subtotal.value - discountAmt.value)
const margin = computed(() => Math.round(total.value * 0.35))

// ===== 竞品对比表（静态原生表格，见 template）=====
const compareLoading = ref(false)
const showCompareAI = ref(false)
function runCompareAI() {
  compareLoading.value = true
  delay(() => {
    compareLoading.value = false
    showCompareAI.value = true
  }, 1500)
}

// ===== AI 方案推荐 =====
const recScene = ref('')
const recDesc = ref('')
const recLoading = ref(false)
const recResult = ref<SceneItem | null>(null)
function runRecommend() {
  if (!recDesc.value.trim() && !recScene.value) return message.warning(t('tool.warnReq'))
  recLoading.value = true
  recResult.value = null
  delay(() => {
    recLoading.value = false
    let key = recScene.value || 'factory'
    const d = recDesc.value
    for (const [sceneKey, words] of Object.entries(mock.recommendKeywords)) {
      if (words.some((w) => d.includes(w))) {
        key = sceneKey
        break
      }
    }
    recResult.value = mock.scenes.find((s) => s.key === key) ?? mock.scenes[0] ?? null
  }, 1200)
}

// ===== AI 话术生成 =====
const scriptRole = ref('tech')
const scriptScene = ref('first')
const scriptProduct = ref('RG500Q')
const scriptLoading = ref(false)
const scriptResult = ref<ScriptResult | null>(null)
const scriptFullText = computed(() => {
  const r = scriptResult.value
  if (!r) return ''
  return [r.title, r.opening, r.value, r.objection, r.next].join('\n\n')
})
function runScript() {
  scriptLoading.value = true
  scriptResult.value = null
  delay(() => {
    scriptLoading.value = false
    const key = `${scriptRole.value}-${scriptScene.value}`
    scriptResult.value = mock.scripts[key] ?? mock.scripts['tech-first'] ?? null
  }, 1000)
}

// ===== AI 智能撰写 =====
const writerType = ref('email')
const writerCustomer = ref('')
const writerProduct = ref('RG500Q')
const writerLoading = ref(false)
const writerResult = ref('')
function runWriter() {
  if (!writerCustomer.value.trim() && writerType.value !== 'proposal') return message.warning(t('tool.warnCustomer'))
  writerLoading.value = true
  writerResult.value = ''
  delay(() => {
    writerLoading.value = false
    const tpl = mock.writerDocs[writerType.value] ?? mock.writerDocs['email'] ?? ''
    const label = mock.products.find((p) => p.value === writerProduct.value)?.label ?? writerProduct.value
    writerResult.value = tpl
      .replace(/\{customer\}/g, writerCustomer.value)
      .replace(/\{product\}/g, label.split(' ')[0] ?? writerProduct.value)
  }, 1200)
}
</script>

<style scoped>
.tool-page {
  padding: 16px;
}
.page-head {
  margin-bottom: 18px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: hsl(var(--text));
  margin: 0 0 4px;
}
.page-sub {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin: 0;
}
.tools-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
.tool-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid hsl(var(--line));
  transition: box-shadow 0.25s;
}
.tool-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.tool-card.ai-card {
  border-left: 3px solid hsl(var(--primary));
}
.tool-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  font-size: 24px;
}
/* 工具图标语义底色 */
.tool-icon-green { background: #f6ffed; color: #52c41a; }
.tool-icon-orange { background: #fff7e6; color: #fa8c16; }
.tool-icon-purple { background: #f9f0ff; color: #722ed1; }
.tool-icon-pink { background: #fff0f6; color: #eb2f96; }
.tool-icon-cyan { background: #e6fffb; color: #13c2c2; }
/* 报价结果语义色 */
.result-discount { color: #fa8c16; }
.result-margin { color: #52c41a; }
/* SWOT 语义色 */
.swot-strength { color: #52c41a; }
.swot-weakness { color: #fa8c16; }
/* 竞品对比原生表格 */
.compare-table-wrap {
  width: 100%;
  overflow-x: auto;
}
.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.compare-table th,
.compare-table td {
  border: 1px solid hsl(var(--line));
  padding: 6px 8px;
  text-align: left;
  color: hsl(var(--text));
}
.compare-table th {
  background: hsl(var(--card-bg));
  font-weight: 600;
}
.compare-table .compare-dim {
  font-weight: 600;
  white-space: nowrap;
}
.tool-title {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
  margin: 0 0 6px;
  display: flex;
  align-items: center;
}
.tool-desc {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin: 0 0 16px;
}
.ai-badge {
  display: inline-flex;
  align-items: center;
  background: linear-gradient(135deg, #722ed1, #1890ff);
  color: #fff;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 8px;
  font-weight: 500;
}
/* 报价计算器 */
.quote-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 6px;
}
.quote-row,
.sel-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.quote-label {
  width: 70px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  flex-shrink: 0;
}
.sel-label {
  width: 72px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
  flex-shrink: 0;
}
.sel-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.result-box {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 12px;
  padding: 18px;
  margin-top: 16px;
}
.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  font-size: 14px;
  color: hsl(var(--text));
}
.result-total {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #b7eb8f;
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
}
.muted {
  color: hsl(var(--secondary-text));
}
/* AI 结果框 */
.ai-result-box {
  background: linear-gradient(135deg, rgba(114, 46, 209, 0.06), rgba(24, 144, 255, 0.06));
  border: 1px solid hsl(var(--primary) / 0.25);
  border-radius: 12px;
  padding: 18px;
  margin-top: 16px;
}
.ai-result-title {
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--primary));
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}
.ai-sub-title {
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 6px;
}
.swot-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  font-size: 13px;
  margin-top: 12px;
}
.swot-head {
  font-weight: 600;
  margin-bottom: 6px;
}
.swot-list {
  padding-left: 16px;
  margin: 0;
  color: hsl(var(--secondary-text));
  line-height: 1.8;
}
.swot-strategy {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid hsl(var(--primary) / 0.2);
  font-size: 13px;
  color: hsl(var(--secondary-text));
  line-height: 1.7;
}
.sol-item {
  padding: 8px 12px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  color: hsl(var(--text));
}
.ai-reason {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-top: 8px;
  line-height: 1.7;
}
/* 模板库 */
.template-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.template-card {
  background: hsl(var(--card-bg));
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 12px;
}
.template-card.block {
  display: block;
}
.template-card:hover {
  background: hsl(var(--primary) / 0.06);
  border-color: hsl(var(--primary) / 0.3);
}
.template-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.template-name {
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--text));
}
.template-desc {
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
/* 邮件库 */
.email-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.email-subject {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 4px;
}
.email-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.email-preview {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* AI 推荐场景 chips */
.chip-hint {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-bottom: 6px;
}
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.scenario-chip {
  display: inline-flex;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid hsl(var(--line));
  background: #fff;
  color: hsl(var(--secondary-text));
  transition: all 0.2s;
}
.scenario-chip:hover {
  border-color: hsl(var(--primary));
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.06);
}
.scenario-chip.active {
  background: hsl(var(--primary));
  color: #fff;
  border-color: hsl(var(--primary));
}
/* 话术 */
.script-block {
  margin-bottom: 10px;
}
.script-tag {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 4px;
}
.script-tag-pink { color: #eb2f96; }
.script-tag-blue { color: #1890ff; }
.script-tag-orange { color: #fa8c16; }
.script-tag-green { color: #52c41a; }
.script-text {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  line-height: 1.8;
  background: #fff;
  padding: 10px;
  border-radius: 8px;
}
.writer-output {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  line-height: 1.8;
  background: #fff;
  padding: 14px;
  border-radius: 8px;
  white-space: pre-line;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid hsl(var(--line));
}
@media (max-width: 900px) {
  .tools-grid {
    grid-template-columns: 1fr;
  }
}
</style>
