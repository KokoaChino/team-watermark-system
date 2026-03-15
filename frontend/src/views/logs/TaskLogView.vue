<template>
  <div class="log-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h2 class="page-card-title">任务记录日志</h2>
          </div>
        </div>
      </template>
      <LogFilterBar
        v-model:operatorKeyword="filters.operatorKeyword"
        v-model:timeRange="filters.timeRange"
        :loading="loading"
        operator-placeholder="按提交人名称搜索"
        :operator-fetch-options="fetchOperatorOptions"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template #before-operator>
          <el-input
            v-model="filters.taskNo"
            clearable
            placeholder="按任务编号搜索"
            style="width: 190px"
          />
          <el-input
            v-model="filters.templateName"
            clearable
            placeholder="按模板名称搜索"
            style="width: 200px"
          />
        </template>
      </LogFilterBar>
      <LogDataTable
        :data="logs"
        :loading="loading"
        :page="pagination.page"
        :size="pagination.size"
        :total="pagination.total"
        empty-text="暂无任务记录日志"
        @update:page="handlePageChange"
        @update:size="handleSizeChange"
        @column-resize="handleColumnResize"
      >
        <el-table-column prop="taskNo" label="任务编号" column-key="taskNo" :width="getColumnWidth('taskNo', 280)" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" column-key="templateName" :width="getColumnWidth('templateName', 280)" show-overflow-tooltip />
        <el-table-column label="提交人" column-key="createdByUsername" :width="getColumnWidth('createdByUsername', 180)">
          <template #default="{ row }">
            <LogUserName :username="row.createdByUsername" :status="row.userStatus" :status-desc="row.userStatusDesc" />
          </template>
        </el-table-column>
        <el-table-column label="图片统计" column-key="imageStats" :width="getColumnWidth('imageStats', 230)">
          <template #default="{ row }">总数 {{ row.totalCount }} / 成功 {{ row.successCount }} / 失败 {{ row.failedCount }}</template>
        </el-table-column>
        <el-table-column label="点数结算" column-key="pointSettlement" :width="getColumnWidth('pointSettlement', 240)">
          <template #default="{ row }">预扣 {{ row.deductedPoints }} / 消耗 {{ row.consumedPoints }} / 返还 {{ row.refundedPoints }}</template>
        </el-table-column>
        <el-table-column label="提交时间" column-key="createdAt" :width="getColumnWidth('createdAt', 180)">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="完成时间" column-key="finishedAt" :width="getColumnWidth('finishedAt', 180)">
          <template #default="{ row }">{{ formatDateTime(row.finishedAt) }}</template>
        </el-table-column>
        <el-table-column label="详情" column-key="detail" :width="getColumnWidth('detail', 90)" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </LogDataTable>
    </el-card>
    <LogJsonDrawer v-model="drawerVisible" title="任务记录详情" :sections="drawerSections" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import LogDataTable from '@/components/logs/LogDataTable.vue'
import LogFilterBar from '@/components/logs/LogFilterBar.vue'
import LogJsonDrawer from '@/components/logs/LogJsonDrawer.vue'
import LogUserName from '@/components/logs/LogUserName.vue'
import { queryTaskLogs, queryTaskLogUsernames } from '@/api/logs'
import type { TaskLogQueryDTO, TaskLogVO } from '@/types'
import {
  buildTimeRangePayload,
  debounce,
  emptyToUndefined,
  formatDateTime,
  loadPersistedColumnWidths,
  persistColumnWidths,
  resolvePersistedColumnKey,
  type PersistedColumnMeta
} from '@/utils/logs'

interface DrawerSection {
  label: string
  value?: unknown
  kind?: 'text' | 'json' | 'date'
}

const TABLE_STORAGE_KEY = 'task-log-table'
const loading = ref(false)
const logs = ref<TaskLogVO[]>([])
const drawerVisible = ref(false)
const drawerSections = ref<DrawerSection[]>([])
const columnWidths = ref<Record<string, number>>(loadPersistedColumnWidths(TABLE_STORAGE_KEY))
const autoSearchPaused = ref(false)
const filters = reactive({
  operatorKeyword: '',
  templateName: '',
  taskNo: '',
  timeRange: [] as string[]
})
const pagination = reactive({ page: 1, size: 20, total: 0 })

const scheduleFilterSearch = debounce(() => {
  pagination.page = 1
  void fetchLogs()
}, 300)

function buildQuery(): TaskLogQueryDTO {
  return {
    page: pagination.page,
    size: pagination.size,
    operatorKeyword: emptyToUndefined(filters.operatorKeyword),
    templateName: emptyToUndefined(filters.templateName),
    taskNo: emptyToUndefined(filters.taskNo),
    ...buildTimeRangePayload(filters.timeRange)
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await queryTaskLogs(buildQuery())
    logs.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchOperatorOptions(keyword: string) {
  try {
    const res = await queryTaskLogUsernames(emptyToUndefined(keyword))
    return res.data
  } catch {
    return []
  }
}

function handleSearch() {
  scheduleFilterSearch.cancel()
  pagination.page = 1
  void fetchLogs()
}

async function handleReset() {
  scheduleFilterSearch.cancel()
  autoSearchPaused.value = true
  filters.templateName = ''
  filters.taskNo = ''
  pagination.page = 1
  await nextTick()
  autoSearchPaused.value = false
  void fetchLogs()
}

function handlePageChange(page: number) {
  scheduleFilterSearch.cancel()
  pagination.page = page
  void fetchLogs()
}

function handleSizeChange(size: number) {
  scheduleFilterSearch.cancel()
  pagination.size = size
  pagination.page = 1
  void fetchLogs()
}

function getColumnWidth(key: string, fallback: number) {
  return columnWidths.value[key] ?? fallback
}

function handleColumnResize(payload: { width: number; column: PersistedColumnMeta }) {
  const key = resolvePersistedColumnKey(payload.column)
  if (!key) {
    return
  }

  const nextWidths = {
    ...columnWidths.value,
    [key]: Math.round(payload.width)
  }
  columnWidths.value = nextWidths
  persistColumnWidths(TABLE_STORAGE_KEY, nextWidths)
}

function openDetail(row: TaskLogVO) {
  drawerSections.value = [
    { label: '任务编号', value: row.taskNo },
    { label: '模板名称', value: row.templateName },
    { label: '提交人', value: formatUserText(row.createdByUsername, row.userStatus, row.userStatusDesc) },
    { label: '图片统计', value: `总数 ${row.totalCount} / 成功 ${row.successCount} / 失败 ${row.failedCount}` },
    { label: '点数结算', value: `预扣 ${row.deductedPoints} / 消耗 ${row.consumedPoints} / 返还 ${row.refundedPoints}` },
    { label: '提交时间', value: row.createdAt, kind: 'date' },
    { label: '完成时间', value: row.finishedAt, kind: 'date' },
    { label: 'IP', value: resolveTaskIp(row) }
  ]
  drawerVisible.value = true
}

function resolveTaskIp(row: TaskLogVO) {
  return (row as TaskLogVO & { ipAddress?: string }).ipAddress
}

function formatUserText(username?: string, status?: string, statusDesc?: string) {
  if (!username) {
    return '--'
  }
  if (!status || status === 'active' || !statusDesc) {
    return username
  }
  return `${username}（${statusDesc}）`
}

watch(
  () => [
    filters.operatorKeyword,
    filters.templateName,
    filters.taskNo,
    filters.timeRange[0] || '',
    filters.timeRange[1] || ''
  ],
  () => {
    if (autoSearchPaused.value) {
      return
    }
    scheduleFilterSearch()
  }
)

onMounted(() => {
  void fetchLogs()
})

onBeforeUnmount(() => {
  scheduleFilterSearch.cancel()
})
</script>

<style scoped lang="scss">
.log-page {
  .page-header .page-card-title {
    margin: 0;
  }

  .page-header p {
    margin: 8px 0 0;
    color: var(--color-text-secondary);
  }
}
</style>
