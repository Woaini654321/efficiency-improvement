<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col overflow-hidden">
    <!-- 工具栏：字典类型 Segmented + 新增 -->
    <div class="flex items-center justify-between mb-4">
      <a-segmented v-model:value="dictType" :options="dictTypeOptions" @change="onDictTypeChange" />
      <a-button type="primary" @click="openCreate()">
        <template #icon><PlusOutlined /></template>
        {{ isTree ? t('category.addRoot') : t('category.addDictItem') }}
      </a-button>
    </div>

    <!-- 扁平字典（方案类型 / 公告类型）：整宽表格 -->
    <div v-if="!isTree" class="flex-1 min-h-0">
      <QBigTable :data="flatRows" :columns="flatColumns" height="100%" />
    </div>

    <!-- 树字典（方案分类 / 需求分类）：左树右详情 + 子级列表 -->
    <div v-else class="flex-1 flex gap-4 overflow-hidden">
      <!-- 左：分类树（可拖拽排序） -->
      <div class="w-[320px] border border-[hsl(var(--line))] rounded p-3 overflow-auto">
        <a-tree
          v-if="treeData.length"
          :tree-data="(treeData as any)"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          :selected-keys="selectedKeys"
          draggable
          block-node
          default-expand-all
          @select="onSelect"
          @drop="onDrop"
        >
          <template #title="node">
            <span class="inline-flex items-center gap-1">
              <span>{{ node.name }}</span>
              <span class="text-[hsl(var(--secondary-text))]">({{ node.contentCount }})</span>
              <a-tag v-if="!node.isActive" color="red">{{ t('common.disabled') }}</a-tag>
            </span>
          </template>
        </a-tree>
        <Empty v-else type="noData" />
      </div>

      <!-- 右：详情 + 子级列表 -->
      <div class="flex-1 overflow-auto">
        <template v-if="current">
          <div class="border border-[hsl(var(--line))] rounded p-4">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-[16px] font-semibold">{{ current.name }}</h3>
              <div class="flex gap-2">
                <a-button size="small" @click="openEdit()">{{ t('common.edit') }}</a-button>
                <a-button v-if="selectedDepth < 3" size="small" @click="openCreateChild()">{{ t('category.addChild') }}</a-button>
                <a-button size="small" @click="toggleActive()">
                  {{ current.isActive ? t('common.disabled') : t('common.enabled') }}
                </a-button>
                <a-popconfirm
                  :title="deletable(current) ? t('category.deleteConfirm') : t('category.deleteBlocked')"
                  :disabled="!deletable(current)"
                  placement="topRight"
                  @confirm="removeItem(current)"
                >
                  <a-button size="small" danger :disabled="!deletable(current)">{{ t('common.delete') }}</a-button>
                </a-popconfirm>
              </div>
            </div>

            <a-descriptions bordered size="small" :column="1">
              <a-descriptions-item :label="t('category.name')">{{ current.name || '--' }}</a-descriptions-item>
              <a-descriptions-item :label="t('category.nameEn')">{{ current.nameEn || '--' }}</a-descriptions-item>
              <a-descriptions-item :label="t('category.parent')">{{ parentName(current) }}</a-descriptions-item>
              <a-descriptions-item :label="t('category.sortOrder')">{{ current.sortOrder ?? '--' }}</a-descriptions-item>
              <a-descriptions-item :label="t('common.status')">
                <a-tag :color="current.isActive ? 'green' : 'red'">
                  {{ current.isActive ? t('common.enabled') : t('common.disabled') }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="t('category.contentCount')">{{ current.contentCount ?? 0 }}</a-descriptions-item>
            </a-descriptions>
          </div>

          <!-- 子级列表 Card -->
          <div v-if="currentChildren.length" class="border border-[hsl(var(--line))] rounded p-4 mt-4">
            <h4 class="text-[14px] font-semibold mb-3">{{ t('category.childList') }}</h4>
            <QBigTable :data="currentChildren" :columns="childColumns" height="260" />
          </div>
        </template>
        <div v-else class="h-full flex items-center justify-center border border-[hsl(var(--line))] rounded">
          <Empty type="noData" :description="t('category.selectTip')" />
        </div>
      </div>
    </div>

    <!-- 新增 / 编辑 Modal -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalMode === 'edit' ? t('category.editTitle') : t('category.createTitle')"
      :width="480"
      @ok="handleSave"
    >
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import { QForm } from '@/components/q-form'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn } from '@/components/q-big-table'
import {
  getCategoryList,
  createCategory,
  updateCategory,
  changeCategoryActive,
  deleteCategory
} from '@/apis/category/categoryApi'
import type { CategoryItem } from '@/apis/category/types'

defineOptions({ name: 'OperationCategory' })
definePage({
  name: 'OperationCategory',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.category.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

interface TreeNode extends CategoryItem {
  children: TreeNode[]
}

const { t } = useI18n()
const qFormRef = ref()

// 树类型（左树右详情）：方案分类 / 需求分类；其余为扁平字典（整宽表格）
const TREE_TYPES = ['solution_category', 'requirement_category']
const dictType = ref('solution_category')
const isTree = computed(() => TREE_TYPES.includes(dictType.value))
const dictTypeOptions = computed(() => [
  { label: t('category.dictType.solutionCategory'), value: 'solution_category' },
  { label: t('category.dictType.solutionType'), value: 'solution_type' },
  { label: t('category.dictType.requirementCategory'), value: 'requirement_category' },
  { label: t('category.dictType.announceType'), value: 'announce_type' }
])

const listData = ref<CategoryItem[]>([])
const selectedKeys = ref<string[]>([])
const current = ref<CategoryItem | null>(null)

// ============ 扁平列表 → 树 ============
function buildTree(list: CategoryItem[]): TreeNode[] {
  const map = new Map<string, TreeNode>()
  list.forEach((item) => map.set(item.id, { ...item, children: [] }))
  const roots: TreeNode[] = []
  map.forEach((node) => {
    const parent = node.parentId ? map.get(node.parentId) : undefined
    if (parent) parent.children.push(node)
    else roots.push(node)
  })
  const sortRec = (nodes: TreeNode[]) => {
    nodes.sort((a, b) => a.sortOrder - b.sortOrder)
    nodes.forEach((n) => sortRec(n.children))
  }
  sortRec(roots)
  return roots
}

const treeData = computed(() => buildTree(listData.value))
const parentTreeData = computed(() => buildTree(listData.value))

// 扁平字典：仅取顶级项，按排序号展示
const flatRows = computed(() =>
  listData.value.filter((i) => !i.parentId).slice().sort((a, b) => a.sortOrder - b.sortOrder)
)

function renderActive(row: CategoryItem) {
  return (
    <a-badge
      status={row.isActive ? 'success' : 'error'}
      text={row.isActive ? t('common.enabled') : t('common.disabled')}
    />
  )
}
function renderFlatAction(row: CategoryItem) {
  return (
    <span class="flex gap-1">
      <a-button type="link" size="small" onClick={() => openEdit(row)}>{t('common.edit')}</a-button>
      <a-button
        type="link"
        size="small"
        style={{ color: row.isActive ? 'hsl(var(--error))' : 'hsl(var(--primary))' }}
        onClick={() => toggleActive(row)}
      >
        {row.isActive ? t('common.disabled') : t('common.enabled')}
      </a-button>
      <a-popconfirm
        title={deletable(row) ? t('category.deleteConfirm') : t('category.deleteBlocked')}
        disabled={!deletable(row)}
        placement="topRight"
        onConfirm={() => removeItem(row)}
      >
        <a-button type="link" size="small" danger disabled={!deletable(row)}>{t('common.delete')}</a-button>
      </a-popconfirm>
    </span>
  )
}

const flatColumns = computed<TableColumn[]>(() => [
  { field: 'name', title: t('category.name'), width: 220 },
  { field: 'sortOrder', title: t('category.sortOrder'), width: 90, align: 'center' },
  { field: 'isActive', title: t('common.status'), width: 110, align: 'center', slots: { default: ({ row }: any) => renderActive(row) } },
  { field: 'contentCount', title: t('category.contentCount'), width: 120, align: 'center' },
  { field: 'action', title: t('common.action'), width: 200, slots: { default: ({ row }: any) => renderFlatAction(row) } }
])
const childColumns = computed<TableColumn[]>(() => [
  { field: 'name', title: t('category.name') },
  { field: 'sortOrder', title: t('category.sortOrder'), width: 90, align: 'center' },
  { field: 'isActive', title: t('common.status'), width: 110, align: 'center', slots: { default: ({ row }: any) => renderActive(row) } },
  { field: 'contentCount', title: t('category.contentCount'), width: 120, align: 'center' }
])

// 当前节点的子级
const currentChildren = computed(() => {
  if (!current.value) return []
  return listData.value
    .filter((c) => c.parentId === current.value!.id)
    .slice()
    .sort((a, b) => a.sortOrder - b.sortOrder)
})

// 当前节点在树中的层级（1 起）
function depthOf(id: string): number {
  const chain = (cid: string, d: number): number => {
    const node = listData.value.find((c) => c.id === cid)
    if (!node || !node.parentId) return d
    return chain(node.parentId, d + 1)
  }
  return chain(id, 1)
}
const selectedDepth = computed(() => (current.value ? depthOf(current.value.id) : 0))

function parentName(item: CategoryItem): string {
  if (!item.parentId) return t('category.rootLevel')
  return listData.value.find((c) => c.id === item.parentId)?.name ?? '--'
}

// 后代 id（用于级联停用）
function descendantIds(id: string): string[] {
  const out: string[] = []
  const walk = (pid: string) => {
    listData.value.forEach((c) => {
      if (c.parentId === pid) {
        out.push(c.id)
        walk(c.id)
      }
    })
  }
  walk(id)
  return out
}
function hasChildren(id: string): boolean {
  return listData.value.some((c) => c.parentId === id)
}
function deletable(item: CategoryItem): boolean {
  return !hasChildren(item.id) && (item.contentCount ?? 0) === 0
}

function onSelect(keys: (string | number)[]) {
  const key = keys[0] != null ? String(keys[0]) : ''
  selectedKeys.value = key ? [key] : []
  current.value = listData.value.find((c) => c.id === key) ?? null
}

function onDictTypeChange() {
  current.value = null
  selectedKeys.value = []
}

// ============ 拖拽排序（本地重排 parentId + sortOrder）============
function onDrop(info: any) {
  const dragKey = String(info.dragNode.key)
  const dropKey = String(info.node.key)
  const dragged = listData.value.find((i) => i.id === dragKey)
  const dropNode = listData.value.find((i) => i.id === dropKey)
  if (!dragged || !dropNode) return
  const dropPos: string[] = info.node.pos.split('-')
  const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1])

  const newParentId = info.dropToGap ? dropNode.parentId : dropKey
  // C-5：仅允许同级排序，不允许跨父级改变层级。
  // 放入他节点成为其子级（!dropToGap），或落到不同父级的相邻位（parentId 不一致）→ 阻止本次拖拽。
  if (!info.dropToGap || String(dragged.parentId || '') !== String(newParentId || '')) {
    message.warning(t('category.dragSameLevelOnly'))
    return
  }
  dragged.parentId = newParentId
  const siblings = listData.value.filter((i) => i.parentId === newParentId && i.id !== dragKey)
  if (!info.dropToGap) {
    siblings.unshift(dragged)
  } else {
    const idx = siblings.findIndex((i) => i.id === dropKey)
    if (dropPosition === -1) siblings.splice(idx, 0, dragged)
    else siblings.splice(idx + 1, 0, dragged)
  }
  siblings.forEach((s, i) => (s.sortOrder = i + 1))
  listData.value = [...listData.value]
  message.success(t('category.sortUpdated'))
}

async function loadList() {
  const res = await getCategoryList({ dictType: dictType.value, pageNumber: 1, pageSize: 999 })
  listData.value = res.records
  if (current.value) {
    current.value = listData.value.find((c) => c.id === current.value!.id) ?? null
    selectedKeys.value = current.value ? [current.value.id] : []
  }
}

// ============ 停用 / 启用（树类型停用父级级联子级）============
function toggleActive(item: CategoryItem | null = current.value) {
  if (!item) return
  const willDisable = item.isActive
  if (isTree.value && willDisable && hasChildren(item.id)) {
    Modal.confirm({
      title: t('category.disableCascadeTitle'),
      content: t('category.disableCascadeContent', { name: item.name }),
      okType: 'danger',
      onOk: () => doToggle(item, false)
    })
  } else {
    doToggle(item, !item.isActive)
  }
}
function setActive(id: string, v: boolean) {
  const c = listData.value.find((x) => x.id === id)
  if (c) c.isActive = v
}
function doToggle(item: CategoryItem, active: boolean) {
  setActive(item.id, active)
  if (!active) descendantIds(item.id).forEach((id) => setActive(id, false))
  listData.value = [...listData.value]
  changeCategoryActive({ id: item.id, isActive: active }).catch(() => {})
  message.success(t('common.success'))
}

function removeItem(item: CategoryItem) {
  listData.value = listData.value.filter((c) => c.id !== item.id)
  deleteCategory(item.id).catch(() => {})
  message.success(t('common.success'))
  if (current.value?.id === item.id) {
    current.value = null
    selectedKeys.value = []
  }
}

// ============ Modal 表单 ============
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref('')

const formModel = reactive<{
  name: string
  nameEn: string
  parentId: string | undefined
  sortOrder: number | undefined
  isActive: boolean
}>({
  name: '',
  nameEn: '',
  parentId: undefined,
  sortOrder: 1,
  isActive: true
})

const schemas = computed(() => [
  {
    field: 'name',
    label: t('category.name'),
    component: 'Input',
    rules: [{ required: true, message: t('category.namePlaceholder') }],
    componentProps: { placeholder: t('category.namePlaceholder'), maxlength: 30, showCount: true }
  },
  {
    field: 'nameEn',
    label: t('category.nameEn'),
    component: 'Input',
    componentProps: { placeholder: t('common.inputPlaceholder'), maxlength: 50 }
  },
  ...(isTree.value
    ? [
        {
          field: 'parentId',
          label: t('category.parent'),
          component: 'TreeSelect',
          componentProps: {
            placeholder: t('category.parentPlaceholder'),
            // C-5：编辑时不允许改上级分类（跨级移动），置为只读展示；仅新增时可选父级
            disabled: modalMode.value === 'edit',
            allowClear: modalMode.value !== 'edit',
            style: 'width:100%',
            treeData: parentTreeData.value,
            fieldNames: { label: 'name', value: 'id', children: 'children' },
            treeDefaultExpandAll: true
          }
        }
      ]
    : []),
  {
    field: 'sortOrder',
    label: t('category.sortOrder'),
    component: 'InputNumber',
    componentProps: { min: 1, max: 999, style: 'width:100%' }
  },
  {
    field: 'isActive',
    label: t('common.status'),
    component: 'Switch',
    valuePropName: 'checked',
    updateEventName: 'update:checked'
  }
])

function resetModel(patch: Partial<typeof formModel>) {
  formModel.name = patch.name ?? ''
  formModel.nameEn = patch.nameEn ?? ''
  formModel.parentId = patch.parentId
  formModel.sortOrder = patch.sortOrder ?? 1
  formModel.isActive = patch.isActive ?? true
}

function openCreate() {
  modalMode.value = 'create'
  editingId.value = ''
  resetModel({})
  modalOpen.value = true
}
function openCreateChild() {
  if (!current.value) return
  modalMode.value = 'create'
  editingId.value = ''
  resetModel({ parentId: current.value.id })
  modalOpen.value = true
}
function openEdit(item: CategoryItem | null = current.value) {
  if (!item) return
  modalMode.value = 'edit'
  editingId.value = item.id
  resetModel({
    name: item.name,
    nameEn: item.nameEn,
    parentId: item.parentId || undefined,
    sortOrder: item.sortOrder,
    isActive: item.isActive
  })
  modalOpen.value = true
}

async function handleSave() {
  await qFormRef.value?.validate()
  const payload = {
    name: formModel.name,
    nameEn: formModel.nameEn,
    parentId: formModel.parentId,
    sortOrder: formModel.sortOrder,
    isActive: formModel.isActive,
    dictType: dictType.value
  }
  if (modalMode.value === 'edit') {
    const c = listData.value.find((x) => x.id === editingId.value)
    if (c) {
      c.name = formModel.name
      c.nameEn = formModel.nameEn || ''
      c.parentId = formModel.parentId || ''
      c.sortOrder = formModel.sortOrder ?? 1
      c.isActive = formModel.isActive
    }
    updateCategory({ id: editingId.value, ...payload }).catch(() => {})
  } else {
    const siblings = listData.value.filter((i) => i.parentId === (formModel.parentId || ''))
    listData.value.push({
      id: 'CAT-' + Date.now(),
      name: formModel.name,
      nameEn: formModel.nameEn || '',
      parentId: formModel.parentId || '',
      sortOrder: formModel.sortOrder ?? siblings.length + 1,
      isActive: formModel.isActive,
      contentCount: 0
    })
    createCategory(payload).catch(() => {})
  }
  listData.value = [...listData.value]
  message.success(t('common.success'))
  modalOpen.value = false
}

onMounted(loadList)
</script>
