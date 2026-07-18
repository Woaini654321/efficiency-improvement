import { zh_CN as zh_CN_of_common } from '@q-mono-x/locale'
import { zh_CN as zh_CN_of_qui } from '@q-ui/locale'
import { zh_CN as zh_CN_of_error_pages } from '@q-web-plugin/error-pages/src/locale'
import { zh_CN as zh_CN_of_antdv_pro } from '@q-web-plugin/antdv-pro'

export const zh_CN = {
  ...zh_CN_of_common, // 通用的中文包
  ...zh_CN_of_qui, // @q-ui 组件库中文包
  ...zh_CN_of_antdv_pro, // antdv 增强组件库中文包
  page: {
    ...zh_CN_of_error_pages.page, // 错误页中文包
    'login': '登录',
    'home': '首页',
    'demos': {
      'DEFAULT': '示例',
      'antdv-pro': {
        'DEFAULT': 'antdv-pro',
        'pro-button': 'ProButton',
        'pro-icon-button': 'ProIconButton',
        'pro-input': 'ProInput'
      },
      'vxe': {
        DETAIL: 'vxe-table',
        detail: '详情页'
      },
      'empty': '空状态'
    },
    'fold-1': '目录-1',
    'fold-1-1': '目录-1-1',
    'fold-1-2': '目录-1-2',
    'fold-1-2-1': '目录-1-2-1',
    'external-link': '外部链接',
    'with-badge': '带徽标'
  },
  demos: {
    vxe: {
      legalName: '中文名',
      enName: '英文名',
      enCnName: '中英文名',
      staffNo: '工号'
    }
  }
}
