<template>
  <div class="log-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h2 class="page-card-title">水印资源日志</h2>
          </div>
        </div>
      </template>
      <LogFilterBar
        v-model:activeTag="filters.eventType"
        v-model:operatorKeyword="filters.operatorKeyword"
        v-model:timeRange="filters.timeRange"
        :tags="eventTags"
        :loading="loading"
        operator-placeholder="按操作人名称搜索"
        :operator-fetch-options="fetchOperatorOptions"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template #before-operator>
          <el-select v-model="filters.resourceScope" clearable placeholder="资源范围" style="width: 160px">
            <el-option label="模板" value="template" />
            <el-option label="字体" value="font" />
          </el-select>
          <el-input
            v-model="filters.resourceName"
            clearable
            placeholder="按资源名称搜索"
            style="width: 220px"
          />
        </template>
      </LogFilterBar>
      <LogDataTable
        :data="logs"
        :loading="loading"
        :page="pagination.page"
        :size="pagination.size"
        :total="pagination.total"
        empty-text="暂无水印资源日志"
        @update:page="handlePageChange"
        @update:size="handleSizeChange"
        @column-resize="handleColumnResize"
      >
        <el-table-column label="事件类型" column-key="eventType" :width="getColumnWidth('eventType', 150)">
          <template #default="{ row }">
            <el-tag :type="row.resourceScope === 'font' ? 'success' : 'primary'">{{ row.eventTypeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceScopeDesc" label="资源范围" column-key="resourceScopeDesc" :width="getColumnWidth('resourceScopeDesc', 120)" />
        <el-table-column prop="resourceName" label="资源名称" column-key="resourceName" :width="getColumnWidth('resourceName', 220)" show-overflow-tooltip />
        <el-table-column label="操作人" column-key="operatorUsername" :width="getColumnWidth('operatorUsername', 180)">
          <template #default="{ row }">
            <LogUserName
              :username="row.operatorUsername"
              :status="row.operatorUserStatus"
              :status-desc="row.operatorUserStatusDesc"
            />
          </template>
        </el-table-column>
        <el-table-column label="摘要" column-key="summary" :width="getColumnWidth('summary', 300)">
          <template #default="{ row }">{{ buildSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="记录时间" column-key="createdAt" :width="getColumnWidth('createdAt', 180)">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="详情" column-key="detail" :width="getColumnWidth('detail', 90)" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </LogDataTable>
    </el-card>
    <LogJsonDrawer v-model="drawerVisible" title="水印资源详情" :sections="drawerSections" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import LogDataTable from '@/components/logs/LogDataTable.vue'
import LogFilterBar from '@/components/logs/LogFilterBar.vue'
import LogJsonDrawer from '@/components/logs/LogJsonDrawer.vue'
import LogUserName from '@/components/logs/LogUserName.vue'
import { queryWatermarkLogUsernames, queryWatermarkResourceLogs } from '@/api/logs'
import type { WatermarkResourceLogQueryDTO, WatermarkResourceLogVO } from '@/types'
import {
  buildTimeRangePayload,
  debounce,
  emptyToUndefined,
  formatDateTime,
  loadPersistedColumnWidths,
  persistColumnWidths,
  resolvePersistedColumnKey,
  type LogTagOption,
  type PersistedColumnMeta
} from '@/utils/logs'

interface DrawerSection {
  label: string
  value?: unknown
  kind?: 'text' | 'json' | 'date'
}

const TABLE_STORAGE_KEY = 'watermark-log-table'
const eventTags: LogTagOption[] = [
  { label: '模板创建', value: 'template_create' },
  { label: '模板修改', value: 'template_update' },
  { label: '模板删除', value: 'template_delete' },
  { label: '字体上传', value: 'font_upload' },
  { label: '字体删除', value: 'font_delete' }
]

const loading = ref(false)
const logs = ref<WatermarkResourceLogVO[]>([])
const drawerVisible = ref(false)
const drawerSections = ref<DrawerSection[]>([])
const columnWidths = ref<Record<string, number>>(loadPersistedColumnWidths(TABLE_STORAGE_KEY))
const autoSearchPaused = ref(false)
const filters = reactive({
  eventType: '',
  resourceScope: '',
  resourceName: '',
  operatorKeyword: '',
  timeRange: [] as string[]
})
const pagination = reactive({ page: 1, size: 20, total: 0 })

const scheduleFilterSearch = debounce(() => {
  pagination.page = 1
  void fetchLogs()
}, 300)

function buildQuery(): WatermarkResourceLogQueryDTO {
  return {
    page: pagination.page,
    size: pagination.size,
    eventType: emptyToUndefined(filters.eventType),
    resourceScope: emptyToUndefined(filters.resourceScope),
    resourceName: emptyToUndefined(filters.resourceName),
    operatorKeyword: emptyToUndefined(filters.operatorKeyword),
    ...buildTimeRangePayload(filters.timeRange)
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await queryWatermarkResourceLogs(buildQuery())
    logs.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchOperatorOptions(keyword: string) {
  try {
    const res = await queryWatermarkLogUsernames(emptyToUndefined(keyword))
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
  filters.resourceScope = ''
  filters.resourceName = ''
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

function buildSummary(row: WatermarkResourceLogVO) {
  return `${row.eventTypeDesc || row.eventType}：${row.resourceName || '--'}`
}

function openDetail(row: WatermarkResourceLogVO) {
  drawerSections.value = [
    { label: '事件类型', value: row.eventTypeDesc },
    { label: '资源范围', value: row.resourceScopeDesc },
    { label: '资源名称', value: row.resourceName },
    { label: '操作人', value: formatUserText(row.operatorUsername, row.operatorUserStatus, row.operatorUserStatusDesc) },
    { label: '摘要', value: buildSummary(row) },
    { label: '记录时间', value: row.createdAt, kind: 'date' },
    { label: 'IP', value: row.ipAddress }
  ]
  drawerVisible.value = true
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
    filters.eventType,
    filters.resourceScope,
    filters.resourceName,
    filters.operatorKeyword,
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
