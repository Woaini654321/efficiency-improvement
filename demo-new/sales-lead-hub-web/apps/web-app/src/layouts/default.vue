<script lang="tsx" setup>
import type { NavbarProps } from '@q-ui/web'
import { useBadgeValueStore } from '@q-web-plugin/store'
import { message } from 'ant-design-vue'

// 默认布局的装饰器
const decorator = {
  navbarAttrs: (attrs: NavbarProps) => {
    const { itemAttrs } = attrs
    if (itemAttrs) {
      itemAttrs.notice = {
        ...itemAttrs.notice,
        onClick: () => {
          // 这里可以自定义顶栏 Notice 的点击事件，比如跳转到通知列表页，或者弹出弹框
          message.info('点击了通知按钮')
        }
        // 你也可以通过 noticeVNode 自定义顶栏 Notice 的渲染内容，比如你需要通过气泡来显示通知列表
        // noticeVNode: <div>111</div>
      }
    }
    // 当你需要自定义顶栏，你可以在这里修改顶栏中的任何属性、事件、渲染内容等
    return attrs
  }
}

// 初始化徽标值（默认布局主要用作中后台场景，通常会有一些徽标需要显示，所以统一在这里初始化）
const initBadgeValue = () => {
  const badgeValueStore = useBadgeValueStore()
  badgeValueStore.badgeValue['messagesCount'] = 99
  badgeValueStore.badgeValue['withBadgeCount'] = 99
}

initBadgeValue()
</script>
<template>
  <DefaultLayoutProvider :decorator="decorator">
    <!--  使用 @q-web-plugin/layouts 中的 DefaultLayout 布局组件 -->
    <DefaultLayout />
  </DefaultLayoutProvider>
</template>
