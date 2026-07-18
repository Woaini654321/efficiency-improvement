<script setup lang="ts">
import type { RouteMeta, LocationQueryValue } from 'vue-router'
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useFrontEndAppConfStore, useTokenStore } from '@q-web-plugin/store'
import { useAppTitle } from '@q-web-plugin/hooks'
import { errorPageRoutePathArr } from '@q-web-plugin/router/helpers'

definePage({
  meta: {
    layout: false,
    whitelisted: true,
    title: 'login'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { getLoginPublicKey } = useFrontEndAppConfStore()
const { login } = useTokenStore()

const onLoginSuccess = () => {
  message.success(t('loginSuccess'))
  if (errorPageRoutePathArr.includes(route.query.oauth_callback as string)) {
    router.replace('/')
  } else {
    router.replace((route.query.oauth_callback as LocationQueryValue | undefined) || '/')
  }
}

const { title, introduction } = useAppTitle()

const loginPageProps = reactive({
  appTitle: '',
  appIntroduction: introduction.value,
  rememberMode: 'password',
  rememberLsKey: import.meta.env.VITE_APP_KEY + '_REMEMBER_LOGIN_FORM',
  rememberModeSecretKey: import.meta.env.VITE_SECRET,
  loginPublicKey: '',
  loginApi: login,
  onLoginSuccess
})

const spinning = ref(true)
getLoginPublicKey().then(key => {
  loginPageProps.loginPublicKey = key
  spinning.value = false
})

watch(
  () => title.value,
  val => {
    loginPageProps.appTitle = val
  },
  {
    immediate: true
  }
)
</script>

<template>
  <a-spin :spinning="spinning">
    <q-login-page v-bind="loginPageProps" />
  </a-spin>
</template>

<style scoped></style>
