<template>
  <div class="log-table-section">
    <el-table
      :data="data"
      :row-key="rowKey"
      :empty-text="emptyText"
      v-loading="loading"
      border
      @header-dragend="handleHeaderDragEnd"
    >
      <slot />
    </el-table>
    <div class="pagination-row">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  data: unknown[]
  loading?: boolean
  page: number
  size: number
  total: number
  rowKey?: string
  emptyText?: string
}>(), {
  loading: false,
  rowKey: 'id',
  emptyText: '暂无数据'
})

const emit = defineEmits<{
  (event: 'update:page', value: number): void
  (event: 'update:size', value: number): void
  (event: 'column-resize', payload: {
    width: number
    oldWidth: number
    column: { columnKey?: string; property?: string; label?: string }
  }): void
}>()

function handleSizeChange(value: number) {
  emit('update:size', value)
}

function handleCurrentChange(value: number) {
  emit('update:page', value)
}

function handleHeaderDragEnd(newWidth: number, oldWidth: number, column: { columnKey?: string; property?: string; label?: string }) {
  emit('column-resize', { width: newWidth, oldWidth, column })
}
</script>

<style scoped lang="scss">
.log-table-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
}
</style>
