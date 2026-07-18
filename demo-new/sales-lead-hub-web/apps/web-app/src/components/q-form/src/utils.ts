import { unref, type Ref } from 'vue'
import type { FormSchema } from './types'

export function isFunction(val: unknown): val is Function {
  return typeof val === 'function'
}

export function isObject(val: unknown): val is Record<string, any> {
  return val !== null && typeof val === 'object'
}

export function deepMerge(src: Record<string, any> = {}, target: Record<string, any> = {}): Record<string, any> {
  for (const key in target) {
    src[key] = isObject(src[key]) ? deepMerge(src[key], target[key]) : target[key]
  }
  return src
}

export function generateFormModel(schemas: FormSchema[] | Ref<FormSchema[]>, extraValues: Record<string, any> = {}): Record<string, any> {
  const model: Record<string, any> = {}
  const items = unref(schemas)

  items.forEach(item => {
    const { field, component, componentProps = {}, initValue } = item
    if (!field) return
    if (Object.prototype.hasOwnProperty.call(model, field)) return

    let defaultValue: any

    if (initValue !== undefined) {
      defaultValue = initValue
    } else {
      const props = isFunction(componentProps) ? {} : componentProps
      switch (component) {
        case 'Input':
        case 'InputPassword':
        case 'Textarea':
          defaultValue = ''
          break
        case 'InputNumber':
        case 'Rate':
        case 'Slider':
          defaultValue = undefined
          break
        case 'Switch':
        case 'Checkbox':
          defaultValue = false
          break
        case 'Select':
        case 'TreeSelect':
          defaultValue = props.mode === 'multiple' || props.mode === 'tags' ? [] : undefined
          break
        case 'CheckboxGroup':
          defaultValue = []
          break
        case 'RadioGroup':
          defaultValue = undefined
          break
        case 'RangePicker':
          defaultValue = []
          break
        case 'DatePicker':
        case 'TimePicker':
          defaultValue = undefined
          break
        default:
          defaultValue = undefined
      }
    }

    model[field] = defaultValue
  })

  return { ...model, ...extraValues }
}

export function resolveComponent(component: string | object | Function | null | undefined): string | object | Function | null {
  if (!component) return null
  if (typeof component !== 'string') return component

  if (component.startsWith('Q') || component.startsWith('Custom')) {
    return component
  }

  const kebab = component.replace(/([A-Z])/g, (match, letter, offset) =>
    offset === 0 ? letter.toLowerCase() : `-${letter.toLowerCase()}`
  )
  return `a-${kebab}`
}
