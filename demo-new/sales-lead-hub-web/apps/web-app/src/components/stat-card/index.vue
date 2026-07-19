<template>
  <div
    class="stat-card"
    :class="{ clickable, active }"
    :style="active ? { borderColor: accent, boxShadow: `0 4px 16px ${accent}22` } : {}"
    @click="clickable && emit('click')"
  >
    <div class="stat-icon" :style="{ background: accent + '18', color: accent }">
      <slot name="icon" />
    </div>
    <div class="stat-body">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
    <div v-if="trend !== undefined" class="stat-trend" :class="trend >= 0 ? 'up' : 'down'">
      {{ trend >= 0 ? '+' : '' }}{{ trend }}%
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: string | number
  accent?: string
  active?: boolean
  clickable?: boolean
  trend?: number
}>(), {
  accent: 'hsl(var(--primary))',
  active: false,
  clickable: false
})

const emit = defineEmits<{ click: [] }>()
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.stat-card.clickable {
  cursor: pointer;
}
.stat-card.clickable:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.stat-body {
  flex: 1;
  min-width: 0;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  color: hsl(var(--text));
}
.stat-label {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
.stat-trend {
  font-size: 12px;
  font-weight: 600;
}
.stat-trend.up {
  color: #52c41a;
}
.stat-trend.down {
  color: hsl(var(--error));
}
</style>
