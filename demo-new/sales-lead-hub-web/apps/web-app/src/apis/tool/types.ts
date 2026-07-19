// 工具助手 mock 数据类型（纯展示层：报价计算器真实计算，AI 类结果为 mock 占位）

export interface QuoteProduct {
  value: string
  label: string
  price: number
}

export interface CompareRow {
  key: string
  feature: string
  quectel: string
  sierra: string
  telit: string
  fibocom: string
}

export interface TemplateItem {
  name: string
  desc: string
  size: string
  color: string
  bg: string
}

export interface EmailItem {
  subject: string
  preview: string
  usage: string
  color: string
}

export interface SceneItem {
  key: string
  label: string
  desc: string
  score: number
  solutions: string[]
  reason: string
}

export interface OptionItem {
  value: string
  label: string
}

export interface ScriptResult {
  title: string
  opening: string
  value: string
  objection: string
  next: string
}

export interface ToolMock {
  quoteProducts: QuoteProduct[]
  compareRows: CompareRow[]
  compareStrengths: string[]
  compareWeaknesses: string[]
  compareStrategy: string
  templates: TemplateItem[]
  emails: EmailItem[]
  scenes: SceneItem[]
  recommendKeywords: Record<string, string[]>
  scriptRoles: OptionItem[]
  scriptScenes: OptionItem[]
  products: OptionItem[]
  scripts: Record<string, ScriptResult>
  docTypes: OptionItem[]
  writerDocs: Record<string, string>
}
