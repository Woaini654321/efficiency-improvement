<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col overflow-hidden">
    <!-- 工具栏：字典类型 Segmented + 新增 -->
    <div class="flex items-center justify-between mb-4">
      <a-segmented v-model:value="dictType" :options="dictTypeOptions" @change="loadList" />
      <a-button type="primary" @click="openCreate()">{{ t('category.addRoot') }}</a-button>
    </div>

    <div class="flex-1 flex gap-4 overflow-hidden">
      <!-- 左：分类树 -->
      <div class="w-[320px] border border-[hsl(var(--line))] rounded p-3 overflow-auto">
        <a-tree
          v-if="treeData.length"
          :tree-data="treeData"
          :field-names="{ title: 'name', key: 'id', children: 'children' }"
          :selected-keys="selectedKeys"
          default-expand-all
          @select="onSelect"
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

      <!-- 右：详情 -->
      <div class="flex-1 border border-[hsl(var(--line))] rounded p-4 overflow-auto">
        <template v-if="current">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-[16px] font-semibold">{{ current.name }}</h3>
            <div class="flex gap-2">
              <a-button size="small" @click="openEdit()">{{ t('common.edit') }}</a-button>
              <a-button size="small" @click="openCreateChild()">{{ t('category.addChild') }}</a-button>
              <a-button size="small" @click="toggleActive()">
                {{ current.isActive ? t('common.disabled') : t('common.enabled') }}
              </a-button>
              <a-popconfirm
                :title="deletable(current) ? t('category.deleteConfirm') : t('category.deleteBlocked')"
                :disabled="!deletable(current)"
                placement="topRight"
                @confirm="removeCurrent()"
              >
                <a-button size="small" danger :disabled="!deletable(current)">{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </div>
          </div>

          <a-descriptions bordered size="small" :column="1">
            <a-descriptions-item :label="t('category.name')">{{ current.name ?? '--' }}</a-descriptions-item>
            <a-descriptions-item :label="t('category.nameEn')">{{ current.nameEn ?? '--' }}</a-descriptions-item>
            <a-descriptions-item :label="t('category.parent')">{{ parentName(current) }}</a-descriptions-item>
            <a-descriptions-item :label="t('category.sortOrder')">{{ current.sortOrder ?? '--' }}</a-descriptions-item>
            <a-descriptions-item :label="t('common.status')">
              <a-tag :color="current.isActive ? 'green' : 'red'">
                {{ current.isActive ? t('common.enabled') : t('common.disabled') }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="t('category.contentCount')">{{ current.contentCount ?? 0 }}</a-descriptions-item>
          </a-descriptions>
        </template>
        <Empty v-else type="noData" :description="t('category.selectTip')" />
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

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import Empty from '@q-web-plugin/empty'
import { QForm } from '@/components/q-form'
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

const dictType = ref('solution_category')
const dictTypeOptions = computed(() => [
  { label: t('category.dictType.solutionCategory'), value: 'solution_category' },
  { label: t('category.dictType.solutionType'), value: 'solution_type' },
  { label: t('category.dictType.requirementCategory'), value: 'requirement_category' },
  { label: t('category.dictType.announceType'), value: 'announce_type' }
])

const listData = ref<CategoryItem[]>([])
const selectedKeys = ref<string[]>([])
const current = ref<CategoryItem | null>(null)

// 扁平列表 → 树
function buildTree(list: CategoryItem[]): TreeNode[] {
  const map = new Map<string, TreeNode>()
  list.forEach(item => map.set(item.id, { ...item, children: [] }))
  const roots: TreeNode[] = []
  map.forEach(node => {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  })
  const sortRec = (nodes: TreeNode[]) => {
    nodes.sort((a, b) => a.sortOrder - b.sortOrder)
    nodes.forEach(n => sortRec(n.children))
  }
  sortRec(roots)
  return roots
}

const treeData = computed(() => buildTree(listData.value))

// TreeSelect 的父级选项（含「无（顶级）」由 allowClear 承担）
const parentTreeData = computed(() => buildTree(listData.value))

function parentName(item: CategoryItem): string {
  if (!item.parentId) return t('category.rootLevel')
  return listData.value.find(c => c.id === item.parentId)?.name ?? '--'
}

// 有子分类或关联内容禁止删除
function hasChildren(id: string): boolean {
  return listData.value.some(c => c.parentId === id)
}
function deletable(item: CategoryItem): boolean {
  return !hasChildren(item.id) && (item.contentCount ?? 0) === 0
}

function onSelect(keys: (string | number)[]) {
  const key = keys[0] != null ? String(keys[0]) : ''
  selectedKeys.value = key ? [key] : []
  current.value = listData.value.find(c => c.id === key) ?? null
}

async function loadList() {
  const res = await getCategoryList({ dictType: dictType.value, pageNumber: 1, pageSize: 999 })
  listData.value = res.records
  // 保持选中项同步
  if (current.value) {
    current.value = listData.value.find(c => c.id === current.value!.id) ?? null
    selectedKeys.value = current.value ? [current.value.id] : []
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
  {
    field: 'parentId',
    label: t('category.parent'),
    component: 'TreeSelect',
    componentProps: {
      placeholder: t('category.parentPlaceholder'),
      allowClear: true,
      style: 'width:100%',
      treeData: parentTreeData.value,
      fieldNames: { label: 'name', value: 'id', children: 'children' },
      treeDefaultExpandAll: true
    }
  },
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
function openEdit() {
  if (!current.value) return
  modalMode.value = 'edit'
  editingId.value = current.value.id
  resetModel({
    name: current.value.name,
    nameEn: current.value.nameEn,
    parentId: current.value.parentId || undefined,
    sortOrder: current.value.sortOrder,
    isActive: current.value.isActive
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
    await updateCategory({ id: editingId.value, ...payload })
  } else {
    await createCategory(payload)
  }
  message.success(t('common.success'))
  modalOpen.value = false
  await loadList()
}

async function toggleActive() {
  if (!current.value) return
  await changeCategoryActive({ id: current.value.id, isActive: !current.value.isActive })
  message.success(t('common.success'))
  await loadList()
}

async function removeCurrent() {
  if (!current.value) return
  await deleteCategory(current.value.id)
  message.success(t('common.success'))
  current.value = null
  selectedKeys.value = []
  await loadList()
}

onMounted(loadList)
</script>
