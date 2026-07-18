<script setup lang="ts">
import { useTheme, useDocumentFavicon, useAppTitle } from '@q-web-plugin/hooks'
import { initStoreData } from '@q-web-plugin/store'
import { syncLatestLocaleFromStore } from '@q-web-plugin/i18n'
import { routesMap } from '@q-web-plugin/router/helpers'

import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'

import { useI18n } from 'vue-i18n'

initStoreData({ routesMap })
const { theme } = useTheme()
syncLatestLocaleFromStore()
useDocumentFavicon()
useAppTitle(true)

const i18n = useI18n()
</script>

<template>
  <a-config-provider
    :theme="theme"
    :locale="i18n.locale.value === 'zh-CN' ? zhCN : enUS"
    :input="{ autocomplete: 'off' }">
    <RouterView />
  </a-config-provider>
</template>

<style scoped></style>
