<template>
  <div class="q-upload-wrapper w-full">
    <component
      :is="type === 'dragger' ? 'a-upload-dragger' : 'a-upload'"
      ref="uploadRef"
      :accept="accept"
      :action="manual ? undefined : uploadApi"
      :headers="manual ? undefined : headers"
      :custom-request="manual ? manualRequest : undefined"
      :list-type="listType"
      :before-upload="beforeUpload"
      :multiple="multiple"
      :show-upload-list="listType === 'picture-card' ? { showRemoveIcon: true, showPreviewIcon: true } : false"
      :max-count="maxCount"
      :file-list="fileList"
      :disabled="props.disabled"
      v-bind="$attrs"
      @change="handleUploadChange"
      @preview="pictureCardPreview"
      @remove="handleRemove"
    >
      <template v-if="$slots.default">
        <span
          class="q-upload-trigger"
          role="button"
          tabindex="0"
          :aria-disabled="props.disabled || isMaxReached"
          @click.stop.prevent="triggerPick"
          @keydown.enter.stop.prevent="triggerPick"
          @keydown.space.stop.prevent="triggerPick"
        >
          <slot />
        </span>
      </template>
      <template v-else>
        <template v-if="listType === 'picture-card'">
          <div class="picture-card-trigger flex flex-col justify-center items-center text-center">
            <PlusOutlined />
            <div class="mt-2">{{ t('upload.upload') }}</div>
          </div>
        </template>
        <template v-else-if="type === 'dragger'">
          <div class="flex flex-col items-center text-center py-8 px-8">
            <UploadOutlined class="text-2xl text-gray-400 mb-2" />
            <slot name="tips">
              <span class="text-gray-400 text-sm">
                {{ t('upload.acceptAllFileTypeTips', { maxSize: maxTotalSize || maxFileSize, maxCount: maxCount }) }}
              </span>
            </slot>
          </div>
        </template>
        <template v-else>
          <a-button :disabled="props.disabled || isMaxReached" class="inline-flex! items-center!">
            <template #icon><UploadOutlined /></template>
            {{ t('upload.clickToUpload') }}
          </a-button>
          <span class="text-gray-400 text-xs" v-if="showUploadTips || hasTipsSlot">
            <slot name="tips">
              <span class="ml-5">
                {{ t('upload.acceptAllFileTypeTips', { maxSize: maxTotalSize || maxFileSize, maxCount: maxCount }) }}
              </span>
            </slot>
          </span>
        </template>
      </template>
    </component>
    <section v-if="fileList.length && showFileList && listType !== 'picture-card'" class="mt-2 w-full">
      <QFileList :file-list="fileList" :show-download="!props.manual" @remove="handleRemove" />
    </section>
    <a-image
      v-if="allowPreview"
      :width="200"
      :style="{ display: 'none' }"
      :preview="{ visible: imgPreviewVisible, onVisibleChange: setImgPreviewVisible }"
      :src="previewImgUrl"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, useSlots } from 'vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useTokenStore } from '@q-web-plugin/store'
import { useI18n } from 'vue-i18n'
import QFileList from '../q-file-list/index.vue'

interface FileItem {
  uid: string
  name: string
  url?: string
  size?: number
  status?: string
  id?: string
  attachId?: string
  originalName?: string
  link?: string
  fileSize?: number
}

const { t } = useI18n()
const slots = useSlots()
const hasTipsSlot = computed(() => !!slots.tips)

const props = withDefaults(defineProps<{
  type?: string
  listType?: string
  uploadApi?: string
  manual?: boolean
  allowPreview?: boolean
  allowCover?: boolean
  disabled?: boolean
  draggable?: boolean
  showFileList?: boolean
  multiple?: boolean
  value?: FileItem[]
  maxCount?: number
  maxFileSize?: number
  maxTotalSize?: number
  accept?: string
  showUploadTips?: boolean
}>(), {
  listType: 'default',
  uploadApi: '',
  manual: false,
  allowPreview: true,
  allowCover: false,
  disabled: false,
  draggable: true,
  showFileList: true,
  multiple: true,
  value: () => [],
  maxCount: 1,
  maxFileSize: 20,
  accept: '',
  showUploadTips: true,
})

const emit = defineEmits<{
  'update:value': [value: FileItem[]]
  success: [file: FileItem]
  error: [error: any]
  preview: [file: FileItem]
  fileSelected: [file: File]
}>()

const headers = computed(() => ({
  Authorization: useTokenStore().getToken(),
}))

const uploadApi = computed(() => {
  return props.uploadApi || `/api/qos/oss/upload`
})

const uploadRef = ref<any>(null)
const fileList = ref<FileItem[]>([...props.value])

const previewImgUrl = ref('')
const imgPreviewVisible = ref(false)

function setImgPreviewVisible(val: boolean) {
  imgPreviewVisible.value = val
}

function getFileSuffix(fileName = '') {
  return fileName.split('.').pop()?.toLowerCase() || ''
}

function isImg(fileName = '') {
  const ext = getFileSuffix(fileName)
  return ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'bmp'].includes(ext)
}

const isMaxReached = computed(() => {
  if (props.allowCover) return false
  if (!props.maxCount) return false
  return fileList.value.length >= props.maxCount
})

watch(
  () => props.value,
  (val) => {
    if (val === fileList.value) return
    fileList.value = [...val].map((e: FileItem) => {
      const uid = e.attachId || e.id || e.uid
      return {
        ...e,
        name: e.originalName || e.name,
        id: uid,
        uid,
        url: e.link || e.url,
        size: e.fileSize || e.size,
      }
    }) as FileItem[]
  },
)

function getTotalSize(files: FileItem[]) {
  return files.reduce((total, f) => total + (f.size || 0), 0)
}

function beforeUpload(file: File) {
  if (props.accept) {
    const acceptList = props.accept.split(',').map((s) => s.trim())
    const isAccepted = acceptList.some((type) => {
      if (type === '*/*') return true
      if (type.endsWith('/*')) return file.type.startsWith(type.split('/')[0] + '/')
      const ext = getFileSuffix(file.name)
      return type.slice(1) === ext
    })
    if (!isAccepted) {
      message.error(`${file.name} ${t('upload.fileTypeNotAccepted')}`)
      return false
    }
  }

  if (props.maxFileSize && file.size / 1024 / 1024 > props.maxFileSize) {
    message.error(`${file.name} ${t('upload.fileSizeExceeded', { size: props.maxFileSize })}`)
    return false
  }

  const newTotalSize = getTotalSize(fileList.value.concat(file as any))
  if (props.maxTotalSize && newTotalSize / 1024 / 1024 > props.maxTotalSize) {
    message.error(t('upload.totalSizeExceeded', { size: props.maxTotalSize }))
    return false
  }

  return true
}

const selectedFiles = ref<File[]>([])

function manualRequest(option: { file: any; onSuccess: (body: any) => void; onError: (err: Error) => void }) {
  const { file, onSuccess, onError } = option
  const nativeFile = (file as any).originFileObj ?? file
  if (!nativeFile || !(nativeFile instanceof File)) {
    onError(new Error('Invalid file'))
    return
  }
  selectedFiles.value.push(nativeFile)
  emit('fileSelected', nativeFile)
  // 手动模式不真实上传，创建本地 blob URL 用于下载/预览
  const blobUrl = URL.createObjectURL(nativeFile)
  onSuccess({ name: nativeFile.name, url: blobUrl })
}

function handleUploadChange({ fileList: innerFileList }: any) {
  if (props.manual) {
    fileList.value = innerFileList.map((f: any) => {
      const nativeFile = (f.originFileObj ?? f) as File | undefined
      return {
        uid: f.uid,
        status: 'done',
        name: f.name,
        size: f.size,
        url: nativeFile instanceof File ? URL.createObjectURL(nativeFile) : (f.response?.url ?? f.url),
      }
    })
  } else {
    fileList.value = innerFileList
      .filter((f: any) => ['uploading', 'done'].includes(f.status))
      .map((f: any) => {
        const obj: FileItem = {
          uid: f.uid,
          status: f.status,
          name: f.name,
          url: f.response?.data?.url ?? f.response?.url ?? f.url,
          size: f.size,
          attachId: f.response?.data?.attachId ?? f.response?.attachId,
        }
        if (f.status === 'done') {
          emit('success', obj)
        }
        return obj
      })
  }
  emit('update:value', fileList.value)
}

function handleRemove(file: FileItem) {
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
  selectedFiles.value = selectedFiles.value.filter((f) => f.name !== file.name && file.size !== f.size)
  emit('update:value', fileList.value)
}

function getFiles(): File[] {
  return [...selectedFiles.value]
}

function clearFiles() {
  selectedFiles.value = []
  fileList.value = []
  emit('update:value', [])
}

function pictureCardPreview(file: FileItem) {
  if (!props.allowPreview) return
  const name = file?.name || ''
  const url = file?.url || (file as any).thumbUrl

  const isImageFile = (() => {
    if (name) return isImg(name)
    if (!url) return false
    try {
      const u = new URL(url, window.location.origin)
      const lastSegment = (u.pathname || '').split('/').pop() || ''
      return isImg(lastSegment)
    } catch {
      const pure = (String(url).split('#')[0] ?? '').split('?')[0] ?? ''
      const lastSegment = pure.split('/').pop() || ''
      return isImg(lastSegment)
    }
  })()

  if (isImageFile && url) {
    previewImgUrl.value = url
    setImgPreviewVisible(true)
    emit('preview', file)
    return
  }
  if (url) {
    window.open(url, '_blank')
    emit('preview', file)
  }
}

function triggerPick() {
  if (props.disabled || isMaxReached.value) return
  const uploadVm = uploadRef.value
  const rootEl = uploadVm?.$el || uploadVm
  let inputEl: HTMLInputElement | null = null
  inputEl = rootEl?.querySelector?.('input[type="file"]') || null
  if (!inputEl) {
    const wrapper = rootEl?.querySelector?.('.ant-upload, .ant-upload-select, .ant-upload-wrapper')
    inputEl = wrapper?.querySelector?.('input[type="file"]') || null
  }
  if (!inputEl) {
    const qRoot = rootEl?.closest?.('.q-upload-wrapper')
    inputEl = qRoot?.querySelector?.('input[type="file"]') || null
  }
  if (!inputEl) {
    console.warn('[q-upload] file input not found')
    return
  }
  inputEl.focus?.()
  inputEl.click()
}

defineExpose({ submit: () => uploadRef.value?.submit(), getFiles, clearFiles })
</script>

<style scoped>
.q-upload-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.q-upload-trigger {
  display: inline-flex;
}

.q-upload-trigger[aria-disabled='true'] {
  cursor: not-allowed;
}
</style>
<style>
.q-upload-wrapper {
  display: inline-block;
  vertical-align: top;
}

.q-upload-wrapper > .ant-upload,
.q-upload-wrapper > .ant-upload-drag {
  width: 100% !important;
  display: block;
}
</style>
