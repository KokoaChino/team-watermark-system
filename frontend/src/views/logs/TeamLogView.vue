<template>
  <div class="log-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h2 class="page-card-title">团队变更日志</h2>
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
        <template #after-operator>
          <LogUserKeywordAutocomplete
            v-model="filters.affectedKeyword"
            placeholder="按影响成员名称搜索"
            :fetch-options="fetchAffectedOptions"
          />
          <el-input
            v-model="filters.inviteCode"
            clearable
            placeholder="按邀请码搜索"
            style="width: 180px"
          />
        </template>
      </LogFilterBar>
      <LogDataTable
        :data="logs"
        :loading="loading"
        :page="pagination.page"
        :size="pagination.size"
        :total="pagination.total"
        empty-text="暂无团队变更日志"
        @update:page="handlePageChange"
        @update:size="handleSizeChange"
        @column-resize="handleColumnResize"
      >
        <el-table-column label="事件类型" column-key="eventType" :width="getColumnWidth('eventType', 140)">
          <template #default="{ row }">
            <el-tag :type="getEventTagType(row.eventType)">{{ row.eventTypeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" column-key="operatorUsername" :width="getColumnWidth('operatorUsername', 180)">
          <template #default="{ row }">
            <LogUserName
              :username="row.operatorUsername"
              :status="row.operatorUserStatus"
              :status-desc="row.operatorUserStatusDesc"
            />
          </template>
        </el-table-column>
        <el-table-column label="影响成员" column-key="affectedUsername" :width="getColumnWidth('affectedUsername', 180)">
          <template #default="{ row }">
            <LogUserName
              :username="row.affectedUsername"
              :status="row.affectedUserStatus"
              :status-desc="row.affectedUserStatusDesc"
            />
          </template>
        </el-table-column>
        <el-table-column prop="inviteCode" label="邀请码" column-key="inviteCode" :width="getColumnWidth('inviteCode', 150)" />
        <el-table-column label="摘要" column-key="summary" :width="getColumnWidth('summary', 340)">
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
    <LogJsonDrawer v-model="drawerVisible" title="团队变更详情" :sections="drawerSections" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import LogDataTable from '@/components/logs/LogDataTable.vue'
import LogFilterBar from '@/components/logs/LogFilterBar.vue'
import LogJsonDrawer from '@/components/logs/LogJsonDrawer.vue'
import LogUserKeywordAutocomplete from '@/components/logs/LogUserKeywordAutocomplete.vue'
import LogUserName from '@/components/logs/LogUserName.vue'
import { queryTeamEventLogs, queryTeamLogUsernames } from '@/api/logs'
import type { TeamEventLogQueryDTO, TeamEventLogVO } from '@/types'
import {
  buildTimeRangePayload,
  debounce,
  emptyToUndefined,
  formatDateTime,
  loadPersistedColumnWidths,
  parseJsonObject,
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

const TABLE_STORAGE_KEY = 'team-log-table'
const inviteCodeEvents = new Set(['invite_code_create', 'invite_code_deactivate'])
const memberEvents = new Set(['member_join', 'member_leave', 'member_kick'])
const eventTags: LogTagOption[] = [
  { label: '邀请码创建', value: 'invite_code_create' },
  { label: '邀请码停用', value: 'invite_code_deactivate' },
  { label: '成员加入', value: 'member_join' },
  { label: '成员退出', value: 'member_leave' },
  { label: '成员移出', value: 'member_kick' },
  { label: '队长转移', value: 'leader_transfer' },
  { label: '团队更名', value: 'team_rename' }
]

const loading = ref(false)
const logs = ref<TeamEventLogVO[]>([])
const drawerVisible = ref(false)
const drawerSections = ref<DrawerSection[]>([])
const columnWidths = ref<Record<string, number>>(loadPersistedColumnWidths(TABLE_STORAGE_KEY))
const autoSearchPaused = ref(false)
const filters = reactive({
  eventType: '',
  operatorKeyword: '',
  affectedKeyword: '',
  inviteCode: '',
  timeRange: [] as string[]
})
const pagination = reactive({ page: 1, size: 20, total: 0 })

const scheduleFilterSearch = debounce(() => {
  pagination.page = 1
  void fetchLogs()
}, 300)

function buildQuery(): TeamEventLogQueryDTO {
  return {
    page: pagination.page,
    size: pagination.size,
    eventType: emptyToUndefined(filters.eventType),
    operatorKeyword: emptyToUndefined(filters.operatorKeyword),
    affectedKeyword: emptyToUndefined(filters.affectedKeyword),
    inviteCode: emptyToUndefined(filters.inviteCode),
    ...buildTimeRangePayload(filters.timeRange)
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await queryTeamEventLogs(buildQuery())
    logs.value = res.data.list
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchOperatorOptions(keyword: string) {
  try {
    const res = await queryTeamLogUsernames('operator', emptyToUndefined(keyword))
    return res.data
  } catch {
    return []
  }
}

async function fetchAffectedOptions(keyword: string) {
  try {
    const res = await queryTeamLogUsernames('affected', emptyToUndefined(keyword))
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
  filters.affectedKeyword = ''
  filters.inviteCode = ''
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

function getEventTagType(eventType: string) {
  if (inviteCodeEvents.has(eventType)) {
    return 'warning'
  }
  if (memberEvents.has(eventType)) {
    return 'success'
  }
  return 'info'
}

function buildSummary(row: TeamEventLogVO) {
  const details = parseJsonObject(row.details)
  const beforeData = parseJsonObject(row.beforeData)
  const afterData = parseJsonObject(row.afterData)
  const targetTeamName = typeof details?.targetTeamName === 'string' ? details.targetTeamName : ''
  const beforeTeamName = typeof beforeData?.teamName === 'string' ? beforeData.teamName : '--'
  const afterTeamName = typeof afterData?.teamName === 'string' ? afterData.teamName : '--'

  switch (row.eventType) {
    case 'invite_code_create':
      return `创建邀请码：${row.inviteCode || '--'}`
    case 'invite_code_deactivate':
      return `停用邀请码：${row.inviteCode || '--'}`
    case 'member_join':
      return `${row.affectedUsername || '成员'}加入团队${targetTeamName ? `（${targetTeamName}）` : ''}`
    case 'member_leave':
      return `${row.affectedUsername || '成员'}主动退出团队`
    case 'member_kick':
      return `${row.affectedUsername || '成员'}被移出团队`
    case 'leader_transfer':
      return `队长身份已转移给 ${row.affectedUsername || '--'}`
    case 'team_rename':
      return `团队名称：${beforeTeamName} → ${afterTeamName}`
    default:
      return row.eventTypeDesc || row.eventType
  }
}

function openDetail(row: TeamEventLogVO) {
  drawerSections.value = [
    { label: '事件类型', value: row.eventTypeDesc },
    { label: '操作人', value: formatUserText(row.operatorUsername, row.operatorUserStatus, row.operatorUserStatusDesc) },
    { label: '影响成员', value: formatUserText(row.affectedUsername, row.affectedUserStatus, row.affectedUserStatusDesc) },
    { label: '邀请码', value: row.inviteCode },
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
    filters.operatorKeyword,
    filters.affectedKeyword,
    filters.inviteCode,
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
