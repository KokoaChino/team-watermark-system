<template>
  <div class="log-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h2>点数流水日志</h2>
            <p>查看团队点数的充值、预扣、返还记录，以及对应的业务来源</p>
          </div>
        </div>
      </template>
      <LogFilterBar
        v-model:activeTag="filters.changeType"
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
          <el-select v-model="filters.sourceType" clearable placeholder="业务来源" style="width: 160px">
            <el-option label="支付订单" value="payment" />
            <el-option label="批量任务" value="batch_task" />
          </el-select>
          <el-input
            v-model="filters.sourceId"
            clearable
            placeholder="按来源标识搜索"
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
        empty-text="暂无点数流水日志"
        @update:page="handlePageChange"
        @update:size="handleSizeChange"
        @column-resize="handleColumnResize"
      >
        <el-table-column label="变动类型" column-key="changeType" :width="getColumnWidth('changeType', 120)">
          <template #default="{ row }">
            <el-tag :type="row.changeType === 'recharge' ? 'success' : row.changeType === 'deduct' ? 'danger' : 'info'">
              {{ row.changeTypeDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动点数" column-key="points" :width="getColumnWidth('points', 120)">
          <template #default="{ row }">
            <span :class="getPointValueClass(row)">{{ formatPointValue(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额变化" column-key="balanceChange" :width="getColumnWidth('balanceChange', 180)">
          <template #default="{ row }">{{ row.balanceBefore }} → {{ row.balanceAfter }}</template>
        </el-table-column>
        <el-table-column prop="sourceTypeDesc" label="业务来源" column-key="sourceTypeDesc" :width="getColumnWidth('sourceTypeDesc', 120)" />
        <el-table-column prop="sourceId" label="来源标识" column-key="sourceId" :width="getColumnWidth('sourceId', 300)" show-overflow-tooltip />
        <el-table-column label="操作人" column-key="operatorUsername" :width="getColumnWidth('operatorUsername', 180)">
          <template #default="{ row }">
            <LogUserName
              :username="row.operatorUsername"
              :status="row.operatorUserStatus"
              :status-desc="row.operatorUserStatusDesc"
            />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="变动说明" column-key="description" :width="getColumnWidth('description', 280)" show-overflow-tooltip />
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
    <LogJsonDrawer v-model="drawerVisible" title="点数流水详情" :sections="drawerSections" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import LogDataTable from '@/components/logs/LogDataTable.vue'
import LogFilterBar from '@/components/logs/LogFilterBar.vue'
import LogJsonDrawer from '@/components/logs/LogJsonDrawer.vue'
import LogUserName from '@/components/logs/LogUserName.vue'
import { queryPointChangeLogs, queryPointLogUsernames } from '@/api/logs'
import type { PointChangeLogQueryDTO, PointChangeLogVO } from '@/types'
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

const TABLE_STORAGE_KEY = 'point-log-table'
const eventTags: LogTagOption[] = [
  { label: '充值', value: 'recharge' },
  { label: '预扣', value: 'deduct' },
  { label: '返还', value: 'refund' }
]

const loading = ref(false)
const logs = ref<PointChangeLogVO[]>([])
const drawerVisible = ref(false)
const drawerSections = ref<DrawerSection[]>([])
const columnWidths = ref<Record<string, number>>(loadPersistedColumnWidths(TABLE_STORAGE_KEY))
const autoSearchPaused = ref(false)
const filters = reactive({
  changeType: '',
  sourceType: '',
  sourceId: '',
  operatorKeyword: '',
  timeRange: [] as string[]
})
const pagination = reactive({ page: 1, size: 20, total: 0 })

const scheduleFilterSearch = debounce(() => {
  pagination.page = 1
  void fetchLogs()
}, 300)

function buildQuery(): PointChangeLogQueryDTO {
  return {
    page: pagination.page,
    size: pagination.size,
    changeType: emptyToUndefined(filters.changeType),
    sourceType: emptyToUndefined(filters.sourceType),
    sourceId: emptyToUndefined(filters.sourceId),
    operatorKeyword: emptyToUndefined(filters.operatorKeyword),
    ...buildTimeRangePayload(filters.timeRange)
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await queryPointChangeLogs(buildQuery())
    logs.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchOperatorOptions(keyword: string) {
  try {
    const res = await queryPointLogUsernames(emptyToUndefined(keyword))
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
  filters.sourceType = ''
  filters.sourceId = ''
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

function formatPointValue(row: PointChangeLogVO) {
  if (row.points === 0) {
    return '0'
  }

  const prefix = row.changeType === 'deduct' ? '-' : '+'
  return `${prefix}${row.points}`
}

function getPointValueClass(row: PointChangeLogVO) {
  return {
    'point-value': true,
    [row.changeType]: true,
    'is-zero': row.points === 0
  }
}

function openDetail(row: PointChangeLogVO) {
  drawerSections.value = [
    { label: '变动类型', value: row.changeTypeDesc },
    { label: '变动点数', value: formatPointValue(row) },
    { label: '余额变化', value: `${row.balanceBefore} → ${row.balanceAfter}` },
    { label: '业务来源', value: row.sourceTypeDesc },
    { label: '来源标识', value: row.sourceId },
    { label: '操作人', value: formatUserText(row.operatorUsername, row.operatorUserStatus, row.operatorUserStatusDesc) },
    { label: '变动说明', value: row.description },
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
    filters.changeType,
    filters.sourceType,
    filters.sourceId,
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
  .page-header h2 {
    margin: 0;
    font-size: 22px;
    color: var(--color-text-primary);
  }

  .page-header p {
    margin: 8px 0 0;
    color: var(--color-text-secondary);
  }
}

.point-value {
  font-weight: 600;
  color: #606266;

  &.recharge:not(.is-zero) {
    color: #67c23a;
  }

  &.refund:not(.is-zero) {
    color: #409eff;
  }

  &.deduct:not(.is-zero) {
    color: #d9363e;
  }

  &.is-zero {
    color: #606266;
  }
}
</style>
