<template>
  <div class="mini-bar">
    <div v-for="(b, i) in bars" :key="i" class="bar-row">
      <span class="bar-label">{{ b.label }}</span>
      <div class="bar-track">
        <div
          class="bar-fill"
          :style="{ width: pct(b.value) + '%', background: b.color || color }"
        />
      </div>
      <span class="bar-value">{{ b.value }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  bars: { label: string; value: number; color?: string }[]
  color?: string
  max?: number
}>(), {
  color: '#1890ff'
})

const maxVal = computed(() => props.max ?? Math.max(1, ...props.bars.map((b) => b.value)))

function pct(v: number): number {
  return Math.round((v / maxVal.value) * 100)
}
</script>

<style scoped>
.mini-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bar-label {
  width: 96px;
  font-size: 13px;
  color: hsl(var(--text));
  text-align: right;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar-track {
  flex: 1;
  height: 18px;
  background: hsl(var(--card-bg));
  border-radius: 9px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 9px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.bar-value {
  width: 52px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--secondary-text));
  flex-shrink: 0;
}
</style>
