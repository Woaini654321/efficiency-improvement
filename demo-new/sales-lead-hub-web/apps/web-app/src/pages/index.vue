<script setup lang="ts">
import type { NavigationGuardNext, RouteLocationNormalizedGeneric, RouteMeta } from 'vue-router'
import { EventMessages } from '@q-cli/libs'
import { routerInterceptor } from '@q-web-plugin/router/helpers'

definePage({
  /**
   * 路由守卫，解决当前路由重定向问题，这里给出三种方案：
   * 1）当前实现的方式，重定向至第一个可达的路由，一般用于管理后台；
   * 2）手动指定一个不受权限控制的路由，比如 /home，一般用于 C 端；
   * 3）不重定向，当前页面有自己实现 UI；
   */
  beforeEnter: async (
    _: RouteLocationNormalizedGeneric,
    __: RouteLocationNormalizedGeneric,
    next: NavigationGuardNext
  ) => {
    /**
     * 采用上述方案一：重定向至第一个可达的路由
     */
    // 取第一个有效的路由跳转，除了白名单以及 getFirstValidRoute 中传入的 excludeNames 以外的路由
    try {
      const firstValidRoute = await routerInterceptor.getFirstValidRoute(['/'])
      if (firstValidRoute) {
        next(firstValidRoute)
      } else {
        EventMessages.emitRedirectErrorPage('404')
        next(false)
      }
    } catch (e) {
      if (e instanceof Error && e.message) {
        if (e.message === 'NO_LOGIN') {
          EventMessages.emitLogout()
        }
        if (e.message === 'NO_PERMISSION') {
          EventMessages.emitRedirectErrorPage('401') // 没有权限，你也可以重定向到一个不受权限控制的页面
        }
      }
      next(false)
    }
  },
  meta: {
    layout: false,
    whitelisted: true,
    title: ''
  } satisfies RouteMeta
})
</script>

<template>
  <div class="h-full p-[16px] bg-white rounded">loading</div>
</template>

<style scoped></style>
