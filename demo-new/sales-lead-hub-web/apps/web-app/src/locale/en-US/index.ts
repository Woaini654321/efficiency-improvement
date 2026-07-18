import { en_US as en_US_of_common } from '@q-mono-x/locale'
import { en_US as en_US_of_qui } from '@q-ui/locale'
import { en_US as en_US_of_error_pages } from '@q-web-plugin/error-pages/src/locale'
import { en_US as en_US_of_antdv_pro } from '@q-web-plugin/antdv-pro'
export const en_US = {
  ...en_US_of_common, // 通用的英文包
  ...en_US_of_qui, // @q-ui 组件库英文包
  ...en_US_of_antdv_pro, // antdv 增强组件库英文包
  page: {
    ...en_US_of_error_pages.page, // 错误页英文包
    'login': 'Log In',
    'home': 'Home',
    'hello': {
      'DEFAULT': 'Hello',
      'world': {
        'DEFAULT': 'Hello World'
      }
    },
    'demos': {
      'DEFAULT': 'Demos',
      'antdv-pro': {
        'DEFAULT': 'antdv-pro',
        'pro-button': 'ProButton',
        'pro-icon-button': 'ProIconButton',
        'pro-input': 'ProInput'
      },
      'vxe': 'vxe-table',
      'empty': 'Empty'
    },
    'fold-1': 'Fold-1',
    'fold-1-1': 'Fold-1-1',
    'fold-1-2': 'Fold-1-2',
    'fold-1-2-1': 'Fold-1-2-1',
    'external-link': 'External Link',
    'with-badge': 'With Badge'
  }
}
