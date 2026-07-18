/**
 * 生成色阶
 */
export const genColorStages = (name: string) => {
  const stages: Record<string | number, string> = {}
  // 生成 50 ~ 950 的色阶
  for (let i = 50; i <= 950; i += 50) {
    if (i === 500) {
      stages[i] = `hsl(var(--${name}))`
    } else {
      const alpha = Math.min(1, i / 1000)
      stages[i] = `hsl(var(--${name}) / ${alpha})`
    }
  }

  return {
    ...stages,
    active: `hsl(var(--${name}) / 0.70)`,
    hover: `hsl(var(--${name}) / 0.70)`
  }
}
