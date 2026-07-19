<template>
  <div class="mini-line">
    <svg viewBox="0 0 100 40" preserveAspectRatio="none" class="line-svg">
      <defs>
        <linearGradient :id="gid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="color" stop-opacity="0.25" />
          <stop offset="100%" :stop-color="color" stop-opacity="0" />
        </linearGradient>
      </defs>
      <polygon v-if="areaPoints" :points="areaPoints" :fill="`url(#${gid})`" />
      <polyline v-if="linePoints" :points="linePoints" fill="none" :stroke="color" stroke-width="1.5" vector-effect="non-scaling-stroke" />
    </svg>
    <div v-if="labels && labels.length" class="line-labels">
      <span v-for="(l, i) in labels" :key="i">{{ l }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  points: number[]
  labels?: string[]
  color?: string
  gid?: string
}>(), {
  color: '#1890ff',
  gid: 'mlg'
})

const coords = computed(() => {
  const pts = props.points
  const n = pts.length
  if (n === 0) return [] as { x: number; y: number }[]
  const max = Math.max(...pts)
  const min = Math.min(...pts)
  const span = max - min || 1
  return pts.map((p, i) => ({
    x: n === 1 ? 50 : (i / (n - 1)) * 100,
    y: 38 - ((p - min) / span) * 34
  }))
})

const linePoints = computed(() =>
  coords.value.length ? coords.value.map((c) => `${c.x},${c.y}`).join(' ') : ''
)
const areaPoints = computed(() => {
  const c = coords.value
  if (!c.length) return ''
  const first = c[0]!
  const last = c[c.length - 1]!
  return `${first.x},40 ` + c.map((p) => `${p.x},${p.y}`).join(' ') + ` ${last.x},40`
})
</script>

<style scoped>
.mini-line {
  width: 100%;
}
.line-svg {
  width: 100%;
  height: 120px;
  display: block;
}
.line-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
</style>
