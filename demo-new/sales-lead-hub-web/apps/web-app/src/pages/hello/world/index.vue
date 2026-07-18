<script setup lang="ts">
import type { RouteMeta } from 'vue-router'

// definePage.name 与 defineOptions.name 必须完全一致（routes.md 铁律2）
defineOptions({ name: 'HelloWorld' })

definePage({
  name: 'HelloWorld',
  meta: {
    // 二级页面：子级 layout 只能 false（routes.md layout 铁律）
    layout: false,
    // 进侧边栏菜单
    menu: true,
    // 纯 locale key（框架内部自动加前缀并翻译），勿包裹翻译函数、勿自加前缀
    title: 'hello.world.DEFAULT'
    // authCode: '', // upm 配置资源权限后再填
    // keepAlive: true, // 如需缓存再放开
  } satisfies RouteMeta
})
</script>

<template>
  <div class="wrapper h-full p-[16px] bg-white rounded">
    <h1 class="text-[24px] font-bold">Hello World</h1>
    <p class="mt-[8px]">A level-2 route demo page (/hello/world).</p>
  </div>
</template>

<style scoped></style>
