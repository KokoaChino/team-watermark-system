<template>
  <div class="log-filter-bar">
    <div class="control-row">
      <template v-if="tags.length > 0">
        <el-select
          :model-value="activeTag"
          clearable
          placeholder="全部事件"
          style="width: 220px"
          @update:model-value="handleTagChange"
        >
          <el-option label="全部" value="" />
          <el-option v-for="tag in tags" :key="tag.value" :label="tag.label" :value="tag.value" />
        </el-select>
      </template>
      <slot name="before-operator" />
      <LogUserKeywordAutocomplete
        v-if="operatorFetchOptions"
        :model-value="operatorKeyword"
        :placeholder="operatorPlaceholder"
        :fetch-options="operatorFetchOptions"
        @update:model-value="handleOperatorChange"
      />
      <el-input
        v-else
        :model-value="operatorKeyword"
        :placeholder="operatorPlaceholder"
        clearable
        style="width: 220px"
        @update:model-value="handleOperatorChange"
        @keyup.enter="emit('search')"
      />
      <slot name="after-operator" />
      <el-date-picker
        :model-value="timeRange"
        type="datetimerange"
        value-format="YYYY-MM-DDTHH:mm:ss"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        style="width: 320px"
        @update:model-value="handleTimeRangeChange"
      />
      <div class="action-row">
        <el-button type="primary" :loading="loading" @click="emit('search')">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import LogUserKeywordAutocomplete from '@/components/logs/LogUserKeywordAutocomplete.vue'
import type { LogTagOption } from '@/utils/logs'

withDefaults(defineProps<{
  activeTag?: string
  operatorKeyword?: string
  timeRange?: string[]
  tags?: LogTagOption[]
  loading?: boolean
  operatorPlaceholder?: string
  operatorFetchOptions?: (keyword: string) => Promise<string[]>
}>(), {
  activeTag: '',
  operatorKeyword: '',
  timeRange: () => [],
  tags: () => [],
  loading: false,
  operatorPlaceholder: '按操作人搜索'
})

const emit = defineEmits<{
  (event: 'update:activeTag', value: string): void
  (event: 'update:operatorKeyword', value: string): void
  (event: 'update:timeRange', value: string[]): void
  (event: 'search'): void
  (event: 'reset'): void
}>()

function handleTagChange(value: string | number | boolean) {
  emit('update:activeTag', value == null ? '' : String(value))
}

function handleOperatorChange(value: string | number) {
  emit('update:operatorKeyword', String(value))
}

function handleTimeRangeChange(value?: string[]) {
  emit('update:timeRange', value || [])
}

function handleReset() {
  emit('update:activeTag', '')
  emit('update:operatorKeyword', '')
  emit('update:timeRange', [])
  emit('reset')
}
</script>

<style scoped lang="scss">
.log-filter-bar {
  margin-bottom: 20px;
  overflow-x: auto;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
  min-width: max-content;
}

.filter-label {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 18px;
}
</style>
