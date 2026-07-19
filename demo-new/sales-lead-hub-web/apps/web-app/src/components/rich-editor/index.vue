<template>
  <div class="rich-editor" :class="{ focused }">
    <div class="re-toolbar">
      <button
        v-for="tool in tools"
        :key="tool.cmd"
        type="button"
        class="re-btn"
        :title="tool.title"
        @mousedown.prevent="exec(tool.cmd, tool.value)"
      >
        <component :is="tool.icon" />
      </button>
    </div>
    <div
      ref="editorRef"
      class="re-content"
      contenteditable="true"
      :data-placeholder="placeholder"
      @input="onInput"
      @focus="focused = true"
      @blur="focused = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import {
  BoldOutlined, ItalicOutlined, UnderlineOutlined,
  UnorderedListOutlined, OrderedListOutlined, FontSizeOutlined
} from '@ant-design/icons-vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
}>(), {
  modelValue: '',
  placeholder: ''
})

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const editorRef = ref<HTMLDivElement | null>(null)
const focused = ref(false)

const tools = [
  { cmd: 'bold', title: 'Bold', icon: BoldOutlined, value: undefined as string | undefined },
  { cmd: 'italic', title: 'Italic', icon: ItalicOutlined, value: undefined as string | undefined },
  { cmd: 'underline', title: 'Underline', icon: UnderlineOutlined, value: undefined as string | undefined },
  { cmd: 'formatBlock', title: 'Heading', icon: FontSizeOutlined, value: 'h3' as string | undefined },
  { cmd: 'insertUnorderedList', title: 'Bullet list', icon: UnorderedListOutlined, value: undefined as string | undefined },
  { cmd: 'insertOrderedList', title: 'Ordered list', icon: OrderedListOutlined, value: undefined as string | undefined }
]

function exec(cmd: string, value?: string) {
  document.execCommand(cmd, false, value)
  onInput()
}

function onInput() {
  emit('update:modelValue', editorRef.value?.innerHTML ?? '')
}

function sync(html: string) {
  if (editorRef.value && editorRef.value.innerHTML !== html) {
    editorRef.value.innerHTML = html
  }
}

onMounted(() => sync(props.modelValue))
watch(() => props.modelValue, (v) => sync(v))
</script>

<style scoped>
.rich-editor {
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.rich-editor.focused {
  border-color: hsl(var(--primary));
}
.re-toolbar {
  display: flex;
  gap: 2px;
  padding: 6px 8px;
  border-bottom: 1px solid hsl(var(--line));
  background: hsl(var(--card-bg));
}
.re-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: hsl(var(--text));
  transition: background 0.2s;
}
.re-btn:hover {
  background: hsl(var(--line));
}
.re-content {
  min-height: 180px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.7;
  color: hsl(var(--text));
  outline: none;
  overflow-y: auto;
}
.re-content:empty::before {
  content: attr(data-placeholder);
  color: hsl(var(--secondary-text));
}
:deep(h3) {
  font-size: 16px;
  font-weight: 700;
  margin: 8px 0;
}
:deep(ul), :deep(ol) {
  padding-left: 22px;
  margin: 6px 0;
}
</style>
