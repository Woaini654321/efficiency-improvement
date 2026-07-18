import 'vue-router'

declare module 'vue-router' {
  interface RouteMetaMenu {
    type?: 'group'
    label?: any
    title?: string
    icon?: any
    disabled?: boolean
    external?: {
      link: string
      blank?: boolean
    }
    badge?: {
      fieldName: string
    }
  }

  interface RouteMeta {
    layout?: 'default' | false
    whitelisted?: boolean
    authCode?: string
    hidden?: boolean
    order?: number
    title?: string
    menu?: RouteMetaMenu | boolean
    keepAlive?: true
    errorPage?: boolean
    tab?: boolean
    needRouteParams?: boolean
  }
}
