// src/adapters/userAdapter.ts
import AIRequestGuard from '@ai-request-guard/core'

export const getMemberPageAdapter = (raw: unknown) => {
  return raw
}

AIRequestGuard.register({
  // viewSchema: () => ({ id: 0, userName: '', mobile: '', deptName: '', age: 0 }),
  adapter: getMemberPageAdapter
})
