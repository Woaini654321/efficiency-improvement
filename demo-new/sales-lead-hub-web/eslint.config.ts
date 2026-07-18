import oxlint from 'eslint-plugin-oxlint'
import vue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'

export default [
  // ① Vue 模板与 SFC 支持（只让 ESLint 管 <template> / 需要 vue 相关规则）
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: '@typescript-eslint/parser',
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true
        }
      }
    },
    plugins: { vue },
    rules: {
      'vue/no-unused-components': 'error',
      'vue/no-unused-vars': 'error',
      'vue/valid-define-props': 'error',
      'vue/multi-word-component-names': 'off',
      'vue/valid-template-root': 'off',
      'vue/require-default-prop': 'off',
      'vue/one-component-per-file': 'off',
      'vue/attribute-hyphenation': 'off',
      'vue/attributes-order': 'off',
      'vue/v-on-event-hyphenation': 'off',
      'vue/require-prop-types': 'off',
      'vue/no-multiple-template-root': 'off'
    }
  },

  // ② 让 eslint-plugin-oxlint 在最后一个配置里“关掉”被 Oxlint 覆盖的 ESLint 规则
  // 官方建议：oxlint 的 flat 配置项要放在数组的最后。[2](https://github.com/oxc-project/eslint-plugin-oxlint)
  ...oxlint.configs['flat/recommended']

  // 如果你已经有 .oxlintrc.json，也可以直接让插件读取来生成“禁用表”
  // oxlint.buildFromOxlintConfigFile('./.oxlintrc.json'),
]
