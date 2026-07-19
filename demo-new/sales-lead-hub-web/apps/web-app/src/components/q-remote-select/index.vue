<template>
  <a-select
    v-bind="$attrs"
    :mode="(mode === 'multiple' ? 'multiple' : undefined) as any"
    :options="mergedOptions"
    :value="innerValue as any"
    allow-clear
    :auto-clear-search-value="true"
    show-arrow
    show-search
    :placeholder="(($attrs.placeholder as string) || '请搜索')"
    :get-popup-container="(getPopupContainer as any)"
    label-in-value
    :filter-option="false"
    @search="handleSearch"
    @popup-scroll="handleScroll"
    @change="handleChange"
    @dropdown-visible-change="handleVisibleChange"
  >
    <template #dropdownRender="{ menuNode: menu }">
      <section :class="loading ? 'min-h-[200px]' : ''" v-loading="loading">
        <v-nodes v-if="page === 1 ? !loading : true" :vnodes="menu" />
      </section>
    </template>
  </a-select>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, onMounted, defineComponent } from 'vue'
import { debounce } from 'lodash-es'

const VNodes = defineComponent({
  props: { vnodes: { type: Object, required: true } },
  render() { return this.vnodes },
})

interface OptionItem {
  label?: string
  value?: string
  [key: string]: any
}

interface PaginatedResponse {
  records: Record<string, any>[]
  total: number
}

const props = withDefaults(defineProps<{
  modelValue?: any[] | any
  mode?: 'multiple' | 'default'
  selectedOptions?: OptionItem[]
  pageApi: (params: Record<string, any>) => Promise<PaginatedResponse>
  pageParams?: Record<string, any>
  fieldNames?: { label?: string; value?: string }
  dataHandler?: (data: Record<string, any>[]) => Record<string, any>[]
  searchKey?: string
  pageSize?: number
  getPopupContainer?: (trigger: HTMLElement) => HTMLElement
}>(), {
  mode: 'default',
  selectedOptions: () => [],
  pageParams: () => ({}),
  fieldNames: () => ({ label: 'label', value: 'id' }),
  dataHandler: (data: Record<string, any>[]) => data,
  searchKey: 'key',
  pageSize: 50,
})

const emit = defineEmits<{
  'update:modelValue': [value: any]
  change: [value: any]
}>()

const options = ref<Record<string, any>[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const searchKeyword = ref('')

const { label: labelKey = 'label', value: valueKey = 'id' } = props.fieldNames

// 已选选项缓存：用户操作时自动填充；外部通过 selectedOptions 喂入初始值（解决刷新后无缓存问题）
const selectionCache = reactive<Map<string, Record<string, any>>>(new Map())

watch(
  () => props.selectedOptions,
  (list) => {
    (list ?? []).forEach((item) => {
      if (item[valueKey] !== undefined) {
        selectionCache.set(String(item[valueKey]), { ...item, label: item[labelKey], value: item[valueKey] })
      }
    })
  },
  { immediate: true, deep: true },
)

function cacheSelected(selected: any) {
  const items = Array.isArray(selected) ? selected : [selected]
  items.forEach((item: any) => {
    if (item && item[valueKey] !== undefined) {
      selectionCache.set(String(item[valueKey]), { ...item, label: item[labelKey], value: item[valueKey] })
    }
  })
}

function clearCache(valuesToKeep: string[]) {
  const keep = new Set(valuesToKeep.map(String))
  selectionCache.forEach((_, key) => {
    if (!keep.has(key)) selectionCache.delete(key)
  })
}

// 缓存 + 当前选项合并，未在当前页的已选项也能正常渲染
const mergedOptions = computed(() => {
  const existingValues = new Set(options.value.map((o) => String(o.value)))
  const cacheEntries: Record<string, any>[] = []
  selectionCache.forEach((item, key) => {
    if (!existingValues.has(key)) {
      cacheEntries.push(item)
    }
  })
  return [...options.value, ...cacheEntries]
})

const innerValue = ref<any>(props.mode === 'multiple' ? [] : undefined)

watch(
  () => props.modelValue,
  (val) => {
    if (val === undefined || val === null) {
      innerValue.value = props.mode === 'multiple' ? [] : undefined
      return
    }
    if (props.mode === 'multiple') {
      const arr = Array.isArray(val) ? val : [val]
      innerValue.value = arr.map((item: any) => {
        // already { label, value } from a-select
        if (item && typeof item === 'object' && item.value !== undefined) {
          cacheSelected(item)
          return item
        }
        // { userId, displayName } from selectedOptions — map to label/value
        if (item && typeof item === 'object' && item[valueKey] !== undefined) {
          const mapped = { ...item, label: item[labelKey], value: item[valueKey] }
          cacheSelected(mapped)
          return mapped
        }
        // raw value — restore from cache or fallback
        const key = String(item)
        const cached = selectionCache.get(key)
        if (cached) return cached
        return { value: item, label: item }
      })
    } else {
      if (val && typeof val === 'object' && val.value !== undefined) {
        cacheSelected(val)
        innerValue.value = val
      } else if (val && typeof val === 'object' && val[valueKey] !== undefined) {
        const mapped = { ...val, label: val[labelKey], value: val[valueKey] }
        cacheSelected(mapped)
        innerValue.value = mapped
      } else if (val) {
        const key = String(val)
        const cached = selectionCache.get(key)
        innerValue.value = cached || { value: val, label: val }
      } else {
        innerValue.value = undefined
      }
    }
  },
  { immediate: true },
)

const loadOptions = async (reset = false) => {
  if (loading.value || !props.pageApi) return
  if (reset) options.value = []
  loading.value = true
  try {
    const currentPage = reset ? 1 : page.value
    const params = {
      ...props.pageParams,
      [props.searchKey]: searchKeyword.value,
      pageNumber: currentPage,
      pageSize: props.pageSize,
    }
    const response = await props.pageApi(params)
    const { records, total: totalItems } = response

    const result = props.dataHandler(records).map((e: Record<string, any>) => ({
      ...e,
      label: e[labelKey],
      value: e[valueKey],
    }))
    if (reset) {
      options.value = result
      page.value = 2
    } else {
      options.value = [...options.value, ...result]
      page.value += 1
    }
    total.value = totalItems
  } catch (error) {
    console.error('Failed to load options:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = debounce((value: string) => {
  searchKeyword.value = value
  loadOptions(true)
}, 300)

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target
  if (scrollTop + clientHeight >= scrollHeight - 10 && options.value.length < total.value) {
    loadOptions()
  }
}

const handleChange = (value: any) => {
  cacheSelected(value)
  if (props.mode === 'multiple') {
    const selectedValues = (Array.isArray(value) ? value : []).map((v: any) => String(v.value))
    clearCache(selectedValues)
  }
  innerValue.value = value
  emit('update:modelValue', value)
  emit('change', value)
}

const handleVisibleChange = () => {
  if (options.value.length === 0) loadOptions(true)
}

onMounted(() => {
  loadOptions(true)
})
</script>
