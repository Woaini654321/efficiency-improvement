<template>
  <div class="file-list" :class="{ simple }">
    <section
      v-for="(file, index) in fileList"
      :key="file.uid"
      class="file-item flex items-center justify-between"
    >
      <section class="flex items-center flex-1 overflow-hidden pr-4">
        <img class="w-6 h-6 mr-[6px] flex-shrink-0" :src="getFileIconPath(file.name)" alt="">
        <span class="flex-1 truncate" :title="file.name">{{ file.name }}</span>
        <span v-if="!simple" class="px-2 ml-[6px] rounded-full text-xs text-primary bg-primary/10">{{ formatFileSize(file.size || file.fileSize || 0) }}</span>
      </section>
      <section v-if="file.status !== 'uploading'" class="flex-shrink-0 file-action flex items-center">
        <template v-if="isImg(file.name)">
          <section class="action-item" @click="handlePreview(file)">
            <EyeOutlined />
          </section>
        </template>
        <template v-else-if="showDownload">
          <section class="action-item" @click="handleDownload(file)">
            <DownloadOutlined />
          </section>
        </template>
        <section v-if="allowDelete" class="action-item" @click="handleRemove(index)">
          <DeleteOutlined />
        </section>
      </section>
      <LoadingOutlined v-if="file.status === 'uploading'" />
    </section>
    <a-image
      :width="200"
      :style="{ display: 'none' }"
      :preview="{
        visible: imgPreviewVisible,
        onVisibleChange: setImgPreviewVisible,
      }"
      :src="previewImgUrl"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { DownloadOutlined, EyeOutlined, DeleteOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import * as QTools from '@q-tools/core'
import defaultIcon from './assets/images/file/default.svg'
import pdfIcon from './assets/images/file/pdf.svg'
import wordIcon from './assets/images/file/word.svg'
import videoIcon from './assets/images/file/video.svg'
import excelIcon from './assets/images/file/excel.svg'
import pptIcon from './assets/images/file/ppt.svg'
import msgIcon from './assets/images/file/msg.svg'
import txtIcon from './assets/images/file/txt.svg'
import mppIcon from './assets/images/file/mpp.svg'
import imgIcon from './assets/images/file/img.svg'
import zipIcon from './assets/images/file/zip.svg'
import xlsIcon from './assets/images/file/xls.svg'

interface FileItem {
  uid: string
  name: string
  url?: string
  size?: number
  fileSize?: number
  status?: string
  id?: string
}

const props = withDefaults(defineProps<{
  fileList?: FileItem[]
  simple?: boolean
  allowDelete?: boolean
  showDownload?: boolean
}>(), {
  fileList: () => [],
  simple: false,
  allowDelete: true,
  showDownload: true,
})

const emit = defineEmits<{
  remove: [file: FileItem]
  preview: [file: FileItem]
  download: [file: FileItem]
}>()

const FILE_ICON_MAP: Record<string, string> = {
  pdf: pdfIcon,
  doc: wordIcon, docx: wordIcon,
  xls: xlsIcon, xlsx: xlsIcon,
  ppt: pptIcon, pptx: pptIcon,
  txt: txtIcon,
  msg: msgIcon,
  mpp: mppIcon,
  zip: zipIcon, rar: zipIcon, '7z': zipIcon,
  jpg: imgIcon, jpeg: imgIcon, png: imgIcon, gif: imgIcon, svg: imgIcon, bmp: imgIcon, webp: imgIcon,
  mp4: videoIcon, avi: videoIcon, mov: videoIcon, mkv: videoIcon,
  csv: excelIcon, xlsm: excelIcon,
}

function getFileSuffix(fileName = '') {
  return fileName.split('.').pop()?.toLowerCase() || ''
}

function formatFileSize(sizeStr: number | string) {
  const size = Number(sizeStr)
  if (size < 1024) return size.toFixed(0) + ' bytes'
  if (size < 1024 * 1024) return (size / 1024).toFixed(0) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}

function isImg(fileName: string) {
  const ext = getFileSuffix(fileName)
  return ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'bmp'].includes(ext)
}

function getFileIconPath(fileName: string) {
  const ext = getFileSuffix(fileName)
  return FILE_ICON_MAP[ext] || defaultIcon
}

function handleRemove(index: number) {
  const item = props.fileList?.[index]
  if (item) emit('remove', item)
}

const previewImgUrl = ref('')
const imgPreviewVisible = ref(false)

function setImgPreviewVisible(visible: boolean) {
  imgPreviewVisible.value = visible
}

function handlePreview(file: FileItem) {
  if (isImg(file.name) && file.url) {
    previewImgUrl.value = file.url
    setImgPreviewVisible(true)
  }
  emit('preview', file)
}

function handleDownload(file: FileItem) {
  if (file.url) {
    const nameArr = (file.name || '').split('.')
    const fileName = nameArr.slice(0, nameArr.length - 1).join('.')
    QTools.downloadByUrl(file.url, fileName)
  }
  emit('download', file)
}
</script>

<style scoped>
.file-list {
  display: flex;
  flex-direction: column;

  &.simple {
    .file-item { padding: 5px 0; }
    .action-item { width: 16px; height: 16px; margin-left: 8px; }
  }
}

.file-item {
  padding: 8px 5px;

  &:not(:last-of-type) {
    border-bottom: 1px solid #f0f0f0;
  }

  &:hover {
    background: #fafafa;

    .file-action {
      display: flex;
    }
  }
}

.file-action {
  display: none;
}

.action-item {
  margin-left: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  cursor: pointer;
  color: #999;

  &:hover { color: #2563eb; }
}
</style>
