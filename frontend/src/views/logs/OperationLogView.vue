<template>
  <div class="log-view">
    <el-card>
      <template #header>
        <span>操作日志</span>
      </template>
      <div class="filter-bar">
        <el-select v-model="filterEventType" placeholder="事件类型" clearable style="width: 150px">
          <el-option label="模板操作" value="TEMPLATE" />
          <el-option label="任务操作" value="TASK" />
          <el-option label="点数变动" value="POINT" />
          <el-option label="团队变更" value="TEAM" />
        </el-select>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="margin-left: 12px"
        />
        <el-button type="primary" style="margin-left: 12px" @click="handleSearch">查询</el-button>
      </div>
      <el-table :data="logs" v-loading="loading" style="width: 100%; margin-top: 16px">
        <el-table-column prop="eventTypeDesc" label="事件类型" width="120" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="details" label="详情" />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="createdAt" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryOperationLogs } from '@/api/operationLog'
import type { OperationLogVO } from '@/types'

const loading = ref(false)
const logs = ref<OperationLogVO[]>([])
const filterEventType = ref('')
const filterDateRange = ref<[Date, Date] | null>(null)
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await queryOperationLogs({
      eventType: filterEventType.value || undefined,
      page: pagination.value.page,
      size: pagination.value.size
    })
    if (res.code === 200) {
      logs.value = res.data?.list || []
      pagination.value.total = res.data?.total || 0
    }
  } catch (error) {
    console.error('获取操作日志失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  fetchLogs()
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped lang="scss">
.log-view {
  .filter-bar {
    display: flex;
    align-items: center;
  }
}
</style>
