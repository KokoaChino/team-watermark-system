<template>
  <el-drawer :model-value="modelValue" :title="title" :size="size" destroy-on-close @update:model-value="handleClose">
    <div class="drawer-body">
      <section v-for="section in sections" :key="section.label" class="drawer-section">
        <div class="section-label">{{ section.label }}</div>
        <pre v-if="section.kind === 'json'" class="section-json">{{ formatJson(section.value) }}</pre>
        <div v-else class="section-text">{{ formatSectionValue(section) }}</div>
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { formatDateTime, formatJson } from '@/utils/logs'

interface DrawerSection {
  label: string
  value?: unknown
  kind?: 'text' | 'json' | 'date'
}
withDefaults(defineProps<{
    modelValue: boolean
    title: string
    sections: DrawerSection[]
    size?: string
}>(), {
    size: '720px'
});
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

function handleClose(value: boolean) {
  emit('update:modelValue', value)
}

function formatSectionValue(section: DrawerSection) {
  if (section.kind === 'date') {
    return formatDateTime(section.value as string | undefined)
  }

  if (section.value === undefined || section.value === null || section.value === '') {
    return '--'
  }

  return String(section.value)
}
</script>

<style scoped lang="scss">
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-section {
  border: 1px solid #e7edf5;
  border-radius: 12px;
  padding: 14px 16px;
  background: #fbfdff;
}

.section-label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.section-text {
  line-height: 1.7;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.section-json {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>