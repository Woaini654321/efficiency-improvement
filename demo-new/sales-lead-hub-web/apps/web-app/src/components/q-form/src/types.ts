import type { Component, VNode } from 'vue'

export type FormLayout = 'horizontal' | 'vertical' | 'inline'
export type FormMode = 'edit' | 'view'

export interface ColProps {
  span?: number
  offset?: number
  push?: number
  pull?: number
  xs?: number | object
  sm?: number | object
  md?: number | object
  lg?: number | object
  xl?: number | object
  xxl?: number | object
}

export interface FormSchema {
  field: string
  label?: string
  component?: string | Component
  required?: boolean
  rules?: any[]
  dynamicRules?: (ctx: { model: Record<string, any>; field: string }) => any[]
  componentProps?: Record<string, any> | ((ctx: { model: Record<string, any>; field: string }) => Record<string, any>)
  colProps?: ColProps
  show?: boolean | ((ctx: { model: Record<string, any>; field: string }) => boolean)
  initValue?: any
  extra?: string
  labelCol?: Record<string, any>
  wrapperCol?: Record<string, any>
  noStyle?: boolean
  trim?: boolean
  valuePropName?: string
  updateEventName?: string
  render?: (model: Record<string, any>, setFormModel: SetFormModel, schema: FormSchema) => VNode
  slot?: string
  on?: Record<string, (model: Record<string, any>, ...args: any[]) => void>
  viewRender?: Component
  labelRender?: Component
  labelPlaceholder?: boolean
  noUpdateTrigger?: boolean
  class?: string
}

export type SetFormModel = (field: string, value: any) => void

export interface QFormProps {
  model?: Record<string, any>
  schemas?: FormSchema[]
  layout?: FormLayout
  labelWidth?: string | number
  colon?: boolean
  rowProps?: Record<string, any>
  disabled?: boolean
  mode?: FormMode
  resetKeepRender?: boolean
}

export interface QFormExpose {
  getModel: () => Record<string, any>
  validate: () => Promise<any>
  validateFields: (nameList?: string[]) => Promise<any>
  resetFields: () => void
  clearValidate: (name?: string | string[]) => void
  submit: () => Promise<void>
  setFormModel: SetFormModel
}
