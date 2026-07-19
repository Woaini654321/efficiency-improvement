<template>
  <div class="mini-pie">
    <div class="pie-circle" :style="{ background: gradient }">
      <div class="pie-hole">
        <div class="pie-total">{{ total }}</div>
        <div v-if="totalLabel" class="pie-total-label">{{ totalLabel }}</div>
      </div>
    </div>
    <div class="pie-legend">
      <div v-for="(s, i) in segments" :key="i" class="legend-item">
        <span class="legend-dot" :style="{ background: s.color }" />
        <span class="legend-label">{{ s.label }}</span>
        <span class="legend-value">{{ s.value }} ({{ pct(s.value) }}%)</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  segments: { label: string; value: number; color: string }[]
  totalLabel?: string
}>(), {
  totalLabel: ''
})

const total = computed(() => props.segments.reduce((s, x) => s + x.value, 0))

function pct(v: number): string {
  return total.value ? ((v / total.value) * 100).toFixed(0) : '0'
}

const gradient = computed(() => {
  const t = total.value || 1
  let acc = 0
  const stops = props.segments.map((s) => {
    const start = (acc / t) * 360
    acc += s.value
    const end = (acc / t) * 360
    return `${s.color} ${start}deg ${end}deg`
  })
  return `conic-gradient(${stops.join(', ')})`
})
</script>

<style scoped>
.mini-pie {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}
.pie-circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.pie-hole {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.pie-total {
  font-size: 24px;
  font-weight: 700;
  color: hsl(var(--text));
}
.pie-total-label {
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.pie-legend {
  flex: 1;
  min-width: 160px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}
.legend-label {
  color: hsl(var(--text));
}
.legend-value {
  margin-left: auto;
  color: hsl(var(--secondary-text));
}
</style>
