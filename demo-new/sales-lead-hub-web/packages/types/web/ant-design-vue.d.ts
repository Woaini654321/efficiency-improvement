import 'ant-design-vue'
import type { ItemType as AMenuItemType } from 'ant-design-vue'

declare module 'ant-design-vue' {
  type ItemType = AMenuItemType & {
    key: string
    order: number
    path: string[]
    children?: ItemType[]
  }
}
