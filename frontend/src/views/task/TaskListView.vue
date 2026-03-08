<template>
  <div class="task-execution">
    <el-skeleton v-if="loadingSession" :rows="8" animated />

    <el-empty v-else-if="!session" description="当前没有可执行的批量任务，请先前往创建页发起任务">
      <el-button type="primary" @click="router.push('/task/create')">前往创建页</el-button>
    </el-empty>

    <template v-else>
      <el-card class="status-card" shadow="never">
        <template #header>
          <div class="card-header status-header">
            <h3>批量任务执行</h3>
            <div class="header-actions">
              <el-tag :type="statusTagType" class="status-tag">{{ statusText }}</el-tag>
              <el-button
                v-if="showRetryCompletion"
                type="primary"
                :loading="retryingCompletion"
                @click="retryCompletion"
              >
                重试完成任务
              </el-button>
              <el-button
                v-if="canDownloadZip"
                :loading="downloadingZip"
                @click="downloadZipFile"
              >
                下载结果包
              </el-button>
              <el-button
                v-if="canClearTaskRecord"
                type="danger"
                plain
                :loading="clearingTaskRecord"
                @click="clearTaskRecord"
              >
                清除本次任务记录
              </el-button>
            </div>
          </div>
        </template>

        <div class="status-body">
          <div class="progress-block">
            <div class="progress-row">
              <span class="progress-label">{{ progressLabel }}</span>
              <el-progress
                :percentage="displayProgressPercent"
                :status="progressStatus"
                :show-text="false"
                :stroke-width="16"
                class="execution-progress"
                :class="{ 'is-animated': progressAnimated }"
              />
              <span class="progress-percent">{{ displayProgressPercent }}%</span>
            </div>
            <div v-if="showExecutionTrace" class="progress-trace">
              <div class="progress-trace-left">
                <span>已处理 {{ session.processedCount }} / {{ session.totalCount }}</span>
                <span>成功 {{ session.successCount }}</span>
                <span>失败 {{ session.failedCount }}</span>
              </div>
              <span class="progress-trace-time">已执行 {{ executionElapsedText }}</span>
            </div>
          </div>

          <el-table
            v-if="showSummaryTable"
            :data="summaryTableRows"
            border
            class="summary-table"
          >
            <el-table-column prop="taskNo" label="任务号" min-width="250" show-overflow-tooltip />
            <el-table-column prop="templateName" label="模板名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="totalCount" label="总数量" min-width="110" />
            <el-table-column prop="successCount" label="成功数" min-width="110" />
            <el-table-column prop="failedCount" label="失败数" min-width="110" />
            <el-table-column prop="totalDuration" label="总耗时" min-width="140" />
            <el-table-column prop="totalSize" label="总大小" min-width="140" />
            <el-table-column prop="startedAt" label="开始时间" min-width="190" />
            <el-table-column prop="finishedAt" label="结束时间" min-width="190" />
          </el-table>
        </div>

        <el-alert
          v-if="session.lastError"
          :title="session.lastError"
          type="warning"
          :closable="false"
          show-icon
          class="status-alert"
        />
      </el-card>

      <el-card class="report-card" shadow="never">
        <template #header>
          <div class="report-header">
            <div>
              <h4>执行结果</h4>
            </div>
            <div class="report-header-actions">
              <el-button
                v-if="canDeleteResultFiles"
                type="warning"
                plain
                :loading="deletingResultFiles"
                @click="deleteAllResultFiles"
              >
                删除任务结果图
              </el-button>
            </div>
          </div>
        </template>

        <el-table
          :data="pagedItems"
          border
          :row-class-name="getResultRowClassName"
          row-key="id"
          class="result-table"
          @sort-change="handleSortChange"
        >
          <el-table-column label="序号" width="80" fixed="left">
            <template #default="{ $index }">
              {{ buildRowSerial($index) }}
            </template>
          </el-table-column>

          <el-table-column
            prop="sourceFileName"
            label="文件名"
            min-width="220"
            fixed="left"
            show-overflow-tooltip
          />

          <el-table-column
            label="输出路径"
            min-width="260"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.outputPath }}
            </template>
          </el-table-column>

          <el-table-column label="分辨率" width="130">
            <template #default="{ row }">
              {{ row.resolutionText }}
            </template>
          </el-table-column>

          <el-table-column
            prop="displaySizeBytes"
            label="大小"
            width="140"
            sortable="custom"
            :sort-orders="SORT_ORDERS"
          >
            <template #default="{ row }">
              {{ formatStorageSize(row.displaySizeBytes) }}
            </template>
          </el-table-column>

          <el-table-column
            prop="durationMs"
            label="耗时"
            width="130"
            sortable="custom"
            :sort-orders="SORT_ORDERS"
          >
            <template #default="{ row }">
              {{ row.durationMs ? formatDuration(row.durationMs) : '-' }}
            </template>
          </el-table-column>

          <el-table-column
            prop="statusSortValue"
            label="状态"
            width="120"
            sortable="custom"
            :sort-orders="SORT_ORDERS"
          >
            <template #default="{ row }">
              <el-tag :type="getRowStatusType(row.status)">{{ getRowStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="失败原因" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.status === 'failed'" class="error-text">{{ row.errorMessage || '-' }}</span>
              <span v-else class="dash-text">--</span>
            </template>
          </el-table-column>

          <el-table-column v-if="showOperationColumn" label="操作" width="220">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button
                  v-if="row.status === 'success' && row.resultFileKey"
                  link
                  type="primary"
                  @click="downloadResultImage(row.id)"
                >
                  下载结果图
                </el-button>
                <el-button
                  v-if="row.status === 'failed'"
                  link
                  type="primary"
                  @click="downloadSourceImage(row.id)"
                >
                  下载原图
                </el-button>
                <el-button
                  v-if="row.status === 'failed'"
                  link
                  type="danger"
                  @click="openErrorLog(row.id)"
                >
                  查看错误日志
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="pageSizeOptions"
            :total="sortedItems.length"
            background
            layout="total, sizes, prev, pager, next, jumper"
          />
        </div>
      </el-card>

      <el-dialog v-model="logDialogVisible" title="错误日志" width="520px">
        <template v-if="selectedLogItem">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="文件名">{{ selectedLogItem.sourceFileName }}</el-descriptions-item>
            <el-descriptions-item label="图片 ID">{{ selectedLogItem.imageId }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ formatDuration(selectedLogItem.durationMs) }}</el-descriptions-item>
            <el-descriptions-item label="错误原因">
              <div class="error-log">{{ selectedLogItem.errorMessage || '-' }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import JSZip from 'jszip'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { completeBatchTask } from '@/api/batchTask'
import { useBatchTaskStore } from '@/stores/batchTask'
import { getFileStem } from '@/utils/batchTask'
import {
  consumePersistentStorageWarningMessage,
  buildStorageRuntimeFailureMessage,
  buildZipDownloadStorageFailureMessage,
  estimateExecutionStorageNeed,
  formatStorageSize,
  inspectBrowserStorage,
  isStoragePressureError,
  isUserCancelledFileSelection
} from '@/utils/browserStorage'
import type { BatchTaskExecutionItem, BatchTaskExecutionItemStatus, BatchTaskExecutionSession } from '@/types'
import {
  buildExecutionReport,
  buildResultFileKey,
  cloneExecutionSession,
  downloadFileBlob,
  formatDuration,
  getSessionZipFileName,
  isSessionRecoverable,
  renderBatchTaskItem,
  yieldToMainThread
} from '@/utils/batchTaskExecution'

interface HandledRequestError extends Error {
  __handled?: boolean
  code?: number
}

interface MutateSessionOptions {
  persist?: boolean
}

interface FilePickerAcceptType {
  description?: string
  accept: Record<string, string[]>
}

interface SaveFilePickerOptionsLike {
  suggestedName?: string
  types?: FilePickerAcceptType[]
}

interface FileSystemWritableLike {
  write: (data: Uint8Array | Blob | string) => Promise<void>
  close: () => Promise<void>
  abort?: (reason?: unknown) => Promise<void>
}

interface FileSystemFileHandleLike {
  createWritable: () => Promise<FileSystemWritableLike>
}

interface SortChangeEvent {
  prop: string | null
  order: 'ascending' | 'descending' | null
}

type SortProp = 'displaySizeBytes' | 'durationMs' | 'statusSortValue' | ''

interface ExecutionTableRow extends BatchTaskExecutionItem {
  defaultIndex: number
  outputPath: string
  resolutionText: string
  displaySizeBytes: number
  statusSortValue: number
}

const CONCURRENCY = 3
const SORT_ORDERS = ['ascending', 'descending', null] as const
const PAGE_SIZE_OPTIONS = [5, 10, 25, 50, 100] as const

const router = useRouter()
const batchTaskStore = useBatchTaskStore()

const loadingSession = ref(true)
const session = ref<BatchTaskExecutionSession | null>(null)
const zipProgressPercent = ref(0)
const downloadingZip = ref(false)
const retryingCompletion = ref(false)
const deletingResultFiles = ref(false)
const clearingTaskRecord = ref(false)
const logDialogVisible = ref(false)
const selectedLogItemId = ref('')
const runnerActive = ref(false)
const packagingActive = ref(false)
const completingActive = ref(false)
const stopRequested = ref(false)
const volatileExecutionMode = ref(false)
const terminalExecutionMessage = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(25)
const sortProp = ref<SortProp>('')
const sortOrder = ref<'ascending' | 'descending' | null>(null)
const runtimeTick = ref(Date.now())

let sessionMutationChain = Promise.resolve()
let runtimeTimer: number | null = null

const pageSizeOptions = PAGE_SIZE_OPTIONS

const selectedLogItem = computed(() => {
  return session.value?.items.find((item) => item.id === selectedLogItemId.value) || null
})

const progressPercent = computed(() => {
  if (!session.value?.totalCount) {
    return 0
  }

  return Math.round((session.value.processedCount / session.value.totalCount) * 100)
})

const hasZipProgressContext = computed(() => {
  if (!session.value) {
    return false
  }

  return downloadingZip.value
    || session.value.status === 'packaging'
    || (session.value.successCount > 0 && (Boolean(session.value.zipReadyAt) || Boolean(session.value.downloadedAt)))
})

const displayProgressPercent = computed(() => {
  if (!session.value) {
    return 0
  }

  if (hasZipProgressContext.value) {
    return session.value.status === 'packaging' || downloadingZip.value
      ? zipProgressPercent.value
      : 100
  }

  return progressPercent.value
})

const progressStatus = computed(() => {
  if (!session.value) {
    return undefined
  }

  if (session.value.status === 'completed') {
    return 'success'
  }

  if (session.value.status === 'complete_failed') {
    return 'exception'
  }

  return undefined
})

const progressAnimated = computed(() => {
  return Boolean(downloadingZip.value || (session.value && !['completed', 'complete_failed'].includes(session.value.status)))
})

const progressLabel = computed(() => {
  return hasZipProgressContext.value ? '结果包封装进度' : '任务处理进度'
})

const showExecutionTrace = computed(() => session.value?.status === 'running' && !hasZipProgressContext.value)
const statusText = computed(() => {
  if (!session.value) {
    return '无任务'
  }

  switch (session.value.status) {
    case 'queued':
      return '待开始'
    case 'running':
      return '处理中'
    case 'packaging':
      return '正在封装结果压缩包'
    case 'completing':
      return '正在提交完成回调'
    case 'completed':
      return '已完成'
    case 'complete_failed':
      return '完成回调失败'
    default:
      return '处理中'
  }
})

const statusTagType = computed(() => {
  if (!session.value) {
    return 'info'
  }

  switch (session.value.status) {
    case 'completed':
      return 'success'
    case 'complete_failed':
      return 'danger'
    case 'packaging':
    case 'completing':
      return 'warning'
    default:
      return 'primary'
  }
})

const totalDurationMs = computed(() => {
  return session.value?.items.reduce((sum, item) => sum + item.durationMs, 0) || 0
})

const totalDurationText = computed(() => {
  return totalDurationMs.value > 0 ? formatDuration(totalDurationMs.value) : '-'
})

const totalSourceSizeBytes = computed(() => {
  return session.value?.items.reduce((sum, item) => sum + item.sourceFileSize, 0) || 0
})

const totalResultSizeBytes = computed(() => {
  return session.value?.items.reduce((sum, item) => {
    if (item.status !== 'success') {
      return sum
    }

    return sum + (item.resultFileSize ?? item.sourceFileSize)
  }, 0) || 0
})

const downloadableResultCount = computed(() => {
  return session.value?.items.filter((item) => item.status === 'success' && item.resultFileKey).length || 0
})

const resultsDeleted = computed(() => {
  if (!session.value) {
    return false
  }

  return session.value.processedCount === session.value.totalCount
    && session.value.successCount > 0
    && downloadableResultCount.value === 0
})

const canDownloadZip = computed(() => {
  return Boolean(
    session.value
    && session.value.processedCount === session.value.totalCount
    && downloadableResultCount.value > 0
    && !packagingActive.value
    && !completingActive.value
  )
})

const canDeleteResultFiles = computed(() => {
  return Boolean(
    session.value
    && session.value.processedCount === session.value.totalCount
    && downloadableResultCount.value > 0
    && !packagingActive.value
    && !completingActive.value
  )
})

const canClearTaskRecord = computed(() => {
  return Boolean(
    session.value
    && session.value.processedCount === session.value.totalCount
    && !runnerActive.value
    && !packagingActive.value
    && !completingActive.value
  )
})

const showRetryCompletion = computed(() => session.value?.status === 'complete_failed')
const showSummaryTable = computed(() => Boolean(session.value && ['completed', 'complete_failed'].includes(session.value.status)))

const summaryTableRows = computed(() => {
  if (!session.value || !showSummaryTable.value) {
    return []
  }

  return [{
    taskNo: session.value.taskNo,
    templateName: session.value.templateName,
    totalCount: session.value.totalCount,
    successCount: session.value.successCount,
    failedCount: session.value.failedCount,
    totalDuration: totalDurationText.value,
    totalSize: formatStorageSize(totalResultSizeBytes.value),
    startedAt: formatDateTime(session.value.startedAt || session.value.createdAt) || '-',
    finishedAt: formatDateTime(session.value.finishedAt) || '-'
  }]
})

const executionElapsedText = computed(() => {
  if (!showExecutionTrace.value || !session.value?.startedAt) {
    return '00:00:00'
  }

  const startedAt = new Date(session.value.startedAt).getTime()
  if (Number.isNaN(startedAt)) {
    return '00:00:00'
  }

  return formatElapsedDuration(runtimeTick.value - startedAt)
})

const showOperationColumn = computed(() => {
  if (!session.value || resultsDeleted.value) {
    return false
  }

  return session.value.items.some((item) => {
    return (item.status === 'success' && item.resultFileKey) || item.status === 'failed'
  })
})

const tableRows = computed<ExecutionTableRow[]>(() => {
  if (!session.value) {
    return []
  }

  return session.value.items.map((item, index) => ({
    ...item,
    defaultIndex: index,
    outputPath: item.status === 'success' ? buildRowOutputPath(item) : '-',
    resolutionText: item.status === 'success' && item.resultWidth && item.resultHeight
      ? `${item.resultWidth} × ${item.resultHeight}`
      : '-',
    displaySizeBytes: item.status === 'success'
      ? (item.resultFileSize ?? item.sourceFileSize)
      : item.sourceFileSize,
    statusSortValue: getStatusSortValue(item.status)
  }))
})

const sortedItems = computed(() => {
  const rows = [...tableRows.value]
  const activeSortProp = sortProp.value as Exclude<SortProp, ''>
  if (!activeSortProp || !sortOrder.value) {
    return rows
  }

  const direction = sortOrder.value === 'ascending' ? 1 : -1
  rows.sort((left, right) => {
    const leftValue = Number(left[activeSortProp])
    const rightValue = Number(right[activeSortProp])

    if (leftValue === rightValue) {
      return left.defaultIndex - right.defaultIndex
    }

    return (leftValue - rightValue) * direction
  })

  return rows
})

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return sortedItems.value.slice(start, start + pageSize.value)
})

watch(pageSize, () => {
  currentPage.value = 1
})

watch(() => sortedItems.value.length, (length) => {
  const maxPage = Math.max(1, Math.ceil(length / pageSize.value))
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
})

watch(() => [session.value?.status, session.value?.startedAt], () => {
  syncRuntimeTimer()
}, {
  immediate: true
})

onMounted(async () => {
  await restoreSession()
})

onBeforeUnmount(() => {
  stopRequested.value = true
  clearRuntimeTimer()
})

async function restoreSession() {
  loadingSession.value = true
  stopRequested.value = false
  volatileExecutionMode.value = false
  terminalExecutionMessage.value = null

  try {
    const restoredSession = await batchTaskStore.restoreActiveSession()
    session.value = restoredSession

    if (restoredSession && ['queued', 'running', 'packaging', 'completing'].includes(restoredSession.status)) {
      await prepareBrowserStorageForExecution(restoredSession)
    }

    if (restoredSession) {
      await resumeSession(restoredSession)
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '批量任务恢复失败'
    ElMessage.error(message)
    session.value = null
  } finally {
    loadingSession.value = false
  }
}

async function resumeSession(currentSession: BatchTaskExecutionSession) {
  if (!isSessionRecoverable(currentSession.status)) {
    return
  }

  if (currentSession.status === 'queued' || currentSession.status === 'running') {
    void runExecutionQueue()
    return
  }

  if (currentSession.status === 'packaging') {
    void packageResults()
    return
  }

  if (currentSession.status === 'completing') {
    void finalizeTaskCompletion()
  }
}

async function runExecutionQueue() {
  if (runnerActive.value || !session.value) {
    return
  }

  runnerActive.value = true

  try {
    await mutateSession(async (draft) => {
      draft.status = 'running'
      if (!draft.startedAt) {
        draft.startedAt = new Date().toISOString()
      }
      draft.lastError = undefined
    })

    while (!stopRequested.value) {
      const pendingItems = session.value?.items.filter((item) => item.status === 'pending').slice(0, CONCURRENCY) || []
      if (!pendingItems.length) {
        break
      }

      await Promise.allSettled(pendingItems.map((item) => processItem(item.id)))
      await yieldToMainThread()

      if (terminalExecutionMessage.value) {
        break
      }
    }

    if (stopRequested.value || !session.value) {
      return
    }

    if (terminalExecutionMessage.value) {
      await markRemainingItemsAsFailed(terminalExecutionMessage.value)
    }

    await mutateSession(async (draft) => {
      draft.currentItemId = undefined
      draft.currentFileName = undefined
      draft.finishedAt = draft.finishedAt || new Date().toISOString()
      if (terminalExecutionMessage.value) {
        draft.lastError = terminalExecutionMessage.value
      }
    }, {
      persist: !volatileExecutionMode.value
    })

    if (session.value.successCount > 0) {
      void packageResults()
    } else {
      void finalizeTaskCompletion()
    }
  } catch (error) {
    await handleTerminalExecutionFailure(error)
  } finally {
    runnerActive.value = false
  }
}

async function processItem(itemId: string) {
  try {
    const processingSnapshot = await mutateSession(async (draft) => {
      const item = draft.items.find((currentItem) => currentItem.id === itemId)
      if (!item) {
        return
      }

      item.status = 'processing'
      item.startedAt = new Date().toISOString()
      item.finishedAt = undefined
      item.errorMessage = undefined
      item.resultFileKey = undefined
      item.resultFileName = undefined
      item.resultMimeType = undefined
      item.resultFileSize = undefined
      item.resultWidth = undefined
      item.resultHeight = undefined
      item.resolvedTargetDirectory = undefined
      item.resolvedOutputFileName = undefined
      item.resolvedOutputExtension = undefined
      draft.currentItemId = item.id
      draft.currentFileName = item.sourceFileName
    })

    const item = processingSnapshot?.items.find((currentItem) => currentItem.id === itemId)
    if (!processingSnapshot || !item) {
      return
    }

    const sourceFile = await batchTaskStore.loadSourceFile(item.sourceFileKey)
    if (!sourceFile) {
      await finishItemAsFailure(itemId, '原图文件不存在，请重新创建任务')
      return
    }

    const renderResult = await renderBatchTaskItem({
      session: processingSnapshot,
      item,
      sourceFile,
      loadWatermarkFile: batchTaskStore.loadWatermarkFile
    })

    if (!renderResult.success) {
      await finishItemAsFailure(itemId, renderResult.errorMessage, renderResult.durationMs)
      return
    }

    const resultFileKey = buildResultFileKey(processingSnapshot.id, itemId)
    await batchTaskStore.saveResultFile({
      key: resultFileKey,
      sessionId: processingSnapshot.id,
      itemId,
      fileName: renderResult.outputFileName,
      mimeType: renderResult.mimeType,
      blob: renderResult.blob
    })

    await mutateSession(async (draft) => {
      const currentItem = draft.items.find((entry) => entry.id === itemId)
      if (!currentItem) {
        return
      }

      currentItem.status = 'success'
      currentItem.durationMs = renderResult.durationMs
      currentItem.finishedAt = new Date().toISOString()
      currentItem.errorMessage = undefined
      currentItem.resultFileKey = resultFileKey
      currentItem.resultFileName = renderResult.outputFileName
      currentItem.resultMimeType = renderResult.mimeType
      currentItem.resultFileSize = renderResult.blob.size
      currentItem.resultWidth = renderResult.outputWidth
      currentItem.resultHeight = renderResult.outputHeight
      currentItem.resolvedTargetDirectory = renderResult.outputDirectory
      currentItem.resolvedOutputFileName = renderResult.outputFileName.replace(/\.[^.]+$/, '')
      currentItem.resolvedOutputExtension = renderResult.outputExtension
      recalculateSession(draft)
    })
  } catch (error) {
    const storageError = isStoragePressureError(error)
    const message = storageError
      ? buildStorageRuntimeFailureMessage()
      : (error instanceof Error ? error.message : '图片处理失败')

    if (storageError) {
      volatileExecutionMode.value = true
      if (!terminalExecutionMessage.value) {
        terminalExecutionMessage.value = message
        ElMessage.error(message)
      }
    }

    await finishItemAsFailure(itemId, message)
  }
}

async function finishItemAsFailure(itemId: string, errorMessage: string, durationMs = 0) {
  const currentSession = session.value
  if (!currentSession) {
    return
  }

  const existingItem = currentSession.items.find((item) => item.id === itemId)
  if (existingItem?.resultFileKey) {
    await batchTaskStore.removeResultFile(existingItem.resultFileKey)
  }

  await mutateSession(async (draft) => {
    const currentItem = draft.items.find((entry) => entry.id === itemId)
    if (!currentItem) {
      return
    }

    currentItem.status = 'failed'
    currentItem.durationMs = durationMs
    currentItem.finishedAt = new Date().toISOString()
    currentItem.errorMessage = errorMessage
    currentItem.resultFileKey = undefined
    currentItem.resultFileName = undefined
    currentItem.resultMimeType = undefined
    currentItem.resultFileSize = undefined
    currentItem.resultWidth = undefined
    currentItem.resultHeight = undefined
    recalculateSession(draft)
    draft.lastError = errorMessage
  })
}

async function prepareBrowserStorageForExecution(currentSession: BatchTaskExecutionSession) {
  const storageSnapshot = await inspectBrowserStorage(true)
  if (!storageSnapshot.supported) {
    return
  }

  if (storageSnapshot.persisted === false) {
    const warningMessage = consumePersistentStorageWarningMessage()
    if (warningMessage) {
      ElMessage.warning(warningMessage)
    }
  }

  if (typeof storageSnapshot.available === 'number') {
    const requiredBytes = estimateExecutionStorageNeed(currentSession)
    if (storageSnapshot.available < requiredBytes) {
      ElMessage.warning(
        `浏览器当前可用本地空间约 ${formatStorageSize(storageSnapshot.available)}，预计剩余执行至少还需要 ${formatStorageSize(requiredBytes)}。如果磁盘空间不足，任务可能提前结束并返还剩余点数`
      )
    }
  }
}

async function handleTerminalExecutionFailure(error: unknown) {
  const storageError = isStoragePressureError(error)
  if (storageError) {
    volatileExecutionMode.value = true
  }

  const message = storageError
    ? buildStorageRuntimeFailureMessage()
    : (error instanceof Error ? error.message : '批量任务执行失败')

  if (!terminalExecutionMessage.value) {
    terminalExecutionMessage.value = message
  }

  await markRemainingItemsAsFailed(message)
  await mutateSession(async (draft) => {
    draft.currentItemId = undefined
    draft.currentFileName = undefined
    draft.finishedAt = draft.finishedAt || new Date().toISOString()
    draft.lastError = message
  }, {
    persist: !volatileExecutionMode.value
  })

  if (!isHandledRequestError(error)) {
    ElMessage.error(message)
  }

  if (session.value?.successCount) {
    await packageResults()
  } else {
    await finalizeTaskCompletion()
  }
}

async function markRemainingItemsAsFailed(errorMessage: string) {
  const currentSession = session.value
  if (!currentSession) {
    return
  }

  const remainingItemIds = currentSession.items
    .filter((item) => item.status === 'pending' || item.status === 'processing')
    .map((item) => item.id)

  if (!remainingItemIds.length) {
    return
  }

  const finishedAt = new Date().toISOString()
  await mutateSession(async (draft) => {
    draft.items.forEach((item) => {
      if (!remainingItemIds.includes(item.id)) {
        return
      }

      item.status = 'failed'
      item.finishedAt = finishedAt
      item.errorMessage = errorMessage
      item.resultFileKey = undefined
      item.resultFileName = undefined
      item.resultMimeType = undefined
      item.resultFileSize = undefined
      item.resultWidth = undefined
      item.resultHeight = undefined
    })
    draft.currentItemId = undefined
    draft.currentFileName = undefined
    draft.finishedAt = draft.finishedAt || finishedAt
    draft.lastError = errorMessage
    recalculateSession(draft)
  }, {
    persist: !volatileExecutionMode.value
  })
}

async function buildZipArchive(currentSession: BatchTaskExecutionSession) {
  const zip = new JSZip()
  let appendedCount = 0

  for (const item of currentSession.items) {
    if (item.status !== 'success' || !item.resultFileKey || !getResolvedResultFileName(item)) {
      continue
    }

    const resultFile = await batchTaskStore.loadResultFile(item.resultFileKey)
    if (!resultFile) {
      throw new Error(`找不到结果文件：${item.sourceFileName}`)
    }

    zip.file(buildRowOutputPath(item), resultFile)
    appendedCount += 1
    await yieldToMainThread()
  }

  if (!appendedCount) {
    throw new Error('当前没有可封装的结果图')
  }

  return zip
}

async function generateZipBlob(
  currentSession: BatchTaskExecutionSession,
  onProgress?: (percent: number) => void
) {
  const zip = await buildZipArchive(currentSession)
  return zip.generateAsync(
    {
      type: 'blob',
      streamFiles: true,
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    },
    (metadata) => {
      onProgress?.(Math.max(0, Math.min(100, Math.round(metadata.percent))))
    }
  )
}

function getSavePickerWindow() {
  return window as Window & {
    showSaveFilePicker?: (options?: SaveFilePickerOptionsLike) => Promise<FileSystemFileHandleLike>
  }
}

async function pipeZipStreamToWritable(
  zip: JSZip,
  writable: FileSystemWritableLike,
  onProgress?: (percent: number) => void
) {
  await new Promise<void>((resolve, reject) => {
    const helper = zip.generateInternalStream({
      type: 'uint8array',
      streamFiles: true,
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    })

    let settled = false

    const rejectOnce = async (error: unknown) => {
      if (settled) {
        return
      }

      settled = true
      try {
        await writable.abort?.(error)
      } catch {
        // ignore secondary abort errors
      }

      reject(error instanceof Error ? error : new Error(String(error || '结果压缩包封装失败')))
    }

    helper.on('data', (chunk, metadata) => {
      helper.pause()
      void (async () => {
        try {
          await writable.write(chunk)
          onProgress?.(Math.max(0, Math.min(100, Math.round(metadata.percent))))
          await yieldToMainThread()
          if (!settled) {
            helper.resume()
          }
        } catch (error) {
          await rejectOnce(error)
        }
      })()
    })

    helper.on('error', (error) => {
      void rejectOnce(error)
    })

    helper.on('end', () => {
      void (async () => {
        if (settled) {
          return
        }

        settled = true
        try {
          onProgress?.(100)
          await writable.close()
          resolve()
        } catch (error) {
          reject(error instanceof Error ? error : new Error(String(error || '结果压缩包保存失败')))
        }
      })()
    })

    helper.resume()
  })
}

async function downloadZipToWritableStream(
  currentSession: BatchTaskExecutionSession,
  zipFileName: string,
  onProgress?: (percent: number) => void
) {
  const pickerWindow = getSavePickerWindow()
  const showSaveFilePicker = pickerWindow.showSaveFilePicker
  if (typeof showSaveFilePicker !== 'function') {
    return false
  }

  const handle = await showSaveFilePicker({
    suggestedName: zipFileName,
    types: [{
      description: 'ZIP Archive',
      accept: {
        'application/zip': ['.zip']
      }
    }]
  })

  const writable = await handle.createWritable()
  const zip = await buildZipArchive(currentSession)
  await pipeZipStreamToWritable(zip, writable, onProgress)
  return true
}

async function downloadZipArchive(
  currentSession: BatchTaskExecutionSession,
  onProgress?: (percent: number) => void,
  preferFilePicker = false
) {
  const zipFileName = getSessionZipFileName(currentSession)
  const streamed = preferFilePicker
    ? await downloadZipToWritableStream(currentSession, zipFileName, onProgress)
    : false

  if (!streamed) {
    const zipBlob = await generateZipBlob(currentSession, onProgress)
    await downloadFileBlob(zipBlob, zipFileName)
  }

  return zipFileName
}

async function packageResults() {
  if (packagingActive.value || !session.value) {
    return
  }

  if (session.value.successCount <= 0) {
    void finalizeTaskCompletion()
    return
  }

  packagingActive.value = true
  zipProgressPercent.value = 0

  const currentSession = cloneExecutionSession(session.value)
  const zipFileName = getSessionZipFileName(currentSession)

  try {
    await mutateSession(async (draft) => {
      draft.status = 'packaging'
      draft.zipFileName = zipFileName
      draft.finishedAt = draft.finishedAt || new Date().toISOString()
    }, {
      persist: !volatileExecutionMode.value
    })

    await downloadZipArchive(currentSession, (percent) => {
      zipProgressPercent.value = percent
    }, false)

    await mutateSession(async (draft) => {
      draft.zipFileName = zipFileName
      draft.zipReadyAt = new Date().toISOString()
      draft.downloadedAt = new Date().toISOString()
    }, {
      persist: !volatileExecutionMode.value
    })
  } catch (error) {
    if (isUserCancelledFileSelection(error)) {
      ElMessage.warning('已取消结果包保存，可稍后重新下载结果包')
    } else {
      const message = isStoragePressureError(error)
        ? buildZipDownloadStorageFailureMessage()
        : (error instanceof Error ? error.message : '结果压缩包下载失败')
      if (!isHandledRequestError(error)) {
        ElMessage.error(message)
      }
      await mutateSession(async (draft) => {
        draft.zipFileName = zipFileName
        draft.lastError = message
        draft.finishedAt = draft.finishedAt || new Date().toISOString()
      }, {
        persist: !volatileExecutionMode.value
      })
    }
  } finally {
    packagingActive.value = false
    zipProgressPercent.value = 0
    void finalizeTaskCompletion()
  }
}

async function finalizeTaskCompletion() {
  if (completingActive.value || !session.value) {
    return
  }

  completingActive.value = true
  const completionTime = new Date().toISOString()

  try {
    await mutateSession(async (draft) => {
      draft.status = 'completing'
      draft.completionAttemptedAt = completionTime
      draft.finishedAt = draft.finishedAt || completionTime
    }, {
      persist: !volatileExecutionMode.value
    })

    const currentSession = cloneExecutionSession(session.value)
    const reportJson = JSON.stringify(buildExecutionReport(currentSession))
    await completeBatchTask({
      taskId: currentSession.taskId,
      successCount: currentSession.successCount,
      reportJson
    })

    await mutateSession(async (draft) => {
      draft.status = 'completed'
      draft.completionAttemptedAt = completionTime
      draft.finishedAt = draft.finishedAt || completionTime
    }, {
      persist: !volatileExecutionMode.value
    })

    terminalExecutionMessage.value = null
  } catch (error) {
    const message = error instanceof Error ? error.message : '完成任务失败'
    await mutateSession(async (draft) => {
      draft.status = 'complete_failed'
      draft.completionAttemptedAt = completionTime
      draft.finishedAt = draft.finishedAt || completionTime
      draft.lastError = message
    }, {
      persist: !volatileExecutionMode.value
    })

    if (!isHandledRequestError(error)) {
      ElMessage.error(message)
    }
  } finally {
    completingActive.value = false
  }
}

async function retryCompletion() {
  if (!session.value) {
    return
  }

  retryingCompletion.value = true
  try {
    await finalizeTaskCompletion()
    if (session.value?.status === 'completed') {
      ElMessage.success('任务完成状态已重新同步')
    }
  } finally {
    retryingCompletion.value = false
  }
}

async function downloadZipFile() {
  if (downloadingZip.value || !session.value) {
    return
  }

  downloadingZip.value = true
  zipProgressPercent.value = 0

  try {
    const currentSession = cloneExecutionSession(session.value)
    const zipFileName = await downloadZipArchive(currentSession, (percent) => {
      zipProgressPercent.value = percent
    }, true)

    await mutateSession(async (draft) => {
      draft.zipFileName = zipFileName
      draft.zipReadyAt = draft.zipReadyAt || new Date().toISOString()
      draft.downloadedAt = new Date().toISOString()
    }, {
      persist: !volatileExecutionMode.value
    })
  } catch (error) {
    if (isUserCancelledFileSelection(error)) {
      ElMessage.warning('已取消结果包保存')
    } else {
      const message = isStoragePressureError(error)
        ? buildZipDownloadStorageFailureMessage()
        : (error instanceof Error ? error.message : '结果包下载失败')
      if (!isHandledRequestError(error)) {
        ElMessage.error(message)
      }
      await mutateSession(async (draft) => {
        draft.lastError = message
      }, {
        persist: !volatileExecutionMode.value
      })
    }
  } finally {
    downloadingZip.value = false
    zipProgressPercent.value = 0
  }
}

async function downloadResultImage(itemId: string) {
  const currentItem = session.value?.items.find((item) => item.id === itemId)
  if (!currentItem?.resultFileKey) {
    ElMessage.warning('结果图已被删除，无法下载')
    return
  }

  const resultFile = await batchTaskStore.loadResultFile(currentItem.resultFileKey)
  if (!resultFile) {
    ElMessage.error('结果图不存在，请重新下载结果包或重新执行任务')
    return
  }

  await downloadFileBlob(resultFile, currentItem.resultFileName || resultFile.name)
}

async function downloadSourceImage(itemId: string) {
  const currentItem = session.value?.items.find((item) => item.id === itemId)
  if (!currentItem) {
    return
  }

  const sourceFile = await batchTaskStore.loadSourceFile(currentItem.sourceFileKey)
  if (!sourceFile) {
    ElMessage.error('原图文件不存在，无法下载')
    return
  }

  await downloadFileBlob(sourceFile, currentItem.sourceFileName)
}

function openErrorLog(itemId: string) {
  selectedLogItemId.value = itemId
  logDialogVisible.value = true
}

async function deleteAllResultFiles() {
  if (!session.value || deletingResultFiles.value || !downloadableResultCount.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '删除后将不再支持下载结果图和结果包，同时会移除结果表中的操作列。是否继续？',
      '删除任务结果图',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  deletingResultFiles.value = true
  try {
    const removableKeys = session.value.items
      .map((item) => item.resultFileKey)
      .filter((key): key is string => Boolean(key))

    for (const key of removableKeys) {
      await batchTaskStore.removeResultFile(key)
      await yieldToMainThread()
    }

    await mutateSession(async (draft) => {
      draft.items.forEach((item) => {
        item.resultFileKey = undefined
      })
      draft.zipArtifactKey = undefined
      draft.zipFileName = undefined
      draft.zipReadyAt = undefined
      draft.downloadedAt = undefined
    }, {
      persist: !volatileExecutionMode.value
    })

    ElMessage.success('已删除全部任务结果图')
  } finally {
    deletingResultFiles.value = false
  }
}

async function clearTaskRecord() {
  if (!session.value || clearingTaskRecord.value) {
    return
  }

  const warningMessage = session.value.status === 'complete_failed'
    ? '当前仅会删除本浏览器中的任务记录。由于完成回调尚未成功，服务端可能仍保留未完成任务状态，清除后也可能暂时无法立即发起新任务。是否继续？'
    : '删除后将清空本次任务的执行记录、结果报表、原图和水印缓存，页面会恢复到未执行状态。是否继续？'

  try {
    await ElMessageBox.confirm(warningMessage, '清除本次任务记录', {
      type: 'warning',
      confirmButtonText: '清除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  clearingTaskRecord.value = true
  try {
    const sessionId = session.value.id
    await batchTaskStore.clearExecutionSession(sessionId)
    session.value = null
    selectedLogItemId.value = ''
    logDialogVisible.value = false
    zipProgressPercent.value = 0
    currentPage.value = 1
    sortProp.value = ''
    sortOrder.value = null
    terminalExecutionMessage.value = null
    ElMessage.success('已清除本次任务记录')
  } finally {
    clearingTaskRecord.value = false
  }
}

function handleSortChange({ prop, order }: SortChangeEvent) {
  if (!prop || !order) {
    sortProp.value = ''
    sortOrder.value = null
    currentPage.value = 1
    return
  }

  if (!['displaySizeBytes', 'durationMs', 'statusSortValue'].includes(prop)) {
    sortProp.value = ''
    sortOrder.value = null
    currentPage.value = 1
    return
  }

  sortProp.value = prop as SortProp
  sortOrder.value = order
  currentPage.value = 1
}

function buildRowSerial(rowIndex: number) {
  return (currentPage.value - 1) * pageSize.value + rowIndex + 1
}

function getResultRowClassName({ rowIndex }: { rowIndex: number }) {
  return rowIndex % 2 === 1 ? 'result-row-striped' : ''
}

async function mutateSession(
  mutator: (draft: BatchTaskExecutionSession) => void | Promise<void>,
  options: MutateSessionOptions = {}
) {
  const run = async () => {
    if (!session.value) {
      return null
    }

    const draft = cloneExecutionSession(session.value)
    await mutator(draft)
    session.value = draft
    await batchTaskStore.saveSession(draft, options.persist !== false)
    return cloneExecutionSession(draft)
  }

  const next = sessionMutationChain.then(run, run)
  sessionMutationChain = next.then(() => undefined, () => undefined)
  return next
}

function recalculateSession(draft: BatchTaskExecutionSession) {
  draft.totalCount = draft.items.length
  draft.successCount = draft.items.filter((item) => item.status === 'success').length
  draft.failedCount = draft.items.filter((item) => item.status === 'failed').length
  draft.processedCount = draft.successCount + draft.failedCount

  const processingItem = draft.items.find((item) => item.status === 'processing')
  if (processingItem) {
    draft.currentItemId = processingItem.id
    draft.currentFileName = processingItem.sourceFileName
    return
  }

  if (draft.processedCount >= draft.totalCount) {
    draft.currentItemId = undefined
    draft.currentFileName = undefined
  }
}

function getResolvedResultFileName(item: BatchTaskExecutionItem) {
  if (item.resultFileName) {
    return item.resultFileName
  }

  const outputExtension = (item.resolvedOutputExtension || item.outputExtension || item.sourceExtension || 'png')
    .replace(/^\./, '')
    .toLowerCase()
  const outputStem = (item.resolvedOutputFileName || item.outputFileName || getFileStem(item.sourceFileName) || 'image').trim() || 'image'
  return `${outputStem}.${outputExtension || 'png'}`
}

function buildRowOutputPath(item: BatchTaskExecutionItem) {
  const fileName = getResolvedResultFileName(item)
  const directory = (item.resolvedTargetDirectory || item.targetDirectory || '')
    .trim()
    .replace(/^\/+|\/+$/g, '')

  return directory ? `${directory}/${fileName}` : fileName
}

function getStatusSortValue(status: BatchTaskExecutionItemStatus) {
  switch (status) {
    case 'pending':
      return 1
    case 'processing':
      return 2
    case 'success':
      return 3
    case 'failed':
      return 4
    default:
      return 99
  }
}

function getRowStatusText(status: BatchTaskExecutionItemStatus) {
  switch (status) {
    case 'pending':
      return '待处理'
    case 'processing':
      return '处理中'
    case 'success':
      return '成功'
    case 'failed':
      return '失败'
    default:
      return '未知'
  }
}

function getRowStatusType(status: BatchTaskExecutionItemStatus) {
  switch (status) {
    case 'success':
      return 'success'
    case 'failed':
      return 'danger'
    case 'processing':
      return 'warning'
    default:
      return 'info'
  }
}

function formatDateTime(value?: string) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date).replace(/\//g, '-')
}

function formatElapsedDuration(durationMs: number) {
  const totalSeconds = Math.max(0, Math.floor(durationMs / 1000))
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
}

function syncRuntimeTimer() {
  clearRuntimeTimer()
  runtimeTick.value = Date.now()

  if (!showExecutionTrace.value || !session.value?.startedAt) {
    return
  }

  runtimeTimer = window.setInterval(() => {
    runtimeTick.value = Date.now()
  }, 1000)
}

function clearRuntimeTimer() {
  if (runtimeTimer !== null) {
    window.clearInterval(runtimeTimer)
    runtimeTimer = null
  }
}

function isHandledRequestError(error: unknown): error is HandledRequestError {
  return Boolean(error && typeof error === 'object' && '__handled' in error && (error as HandledRequestError).__handled)
}
</script>

<style scoped>
.task-execution {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-card,
.report-card {
  border: none;
}

.status-header,
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.status-header h3,
.report-header h4 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.header-actions,
.report-header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.status-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.progress-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-label {
  flex: 0 0 auto;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}

.progress-percent {
  flex: 0 0 auto;
  min-width: 52px;
  text-align: right;
  font-size: 20px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.execution-progress {
  flex: 1 1 auto;
}

.execution-progress :deep(.el-progress-bar__outer) {
  background: var(--el-fill-color-light);
}

.execution-progress.is-animated :deep(.el-progress-bar__inner) {
  background-image: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.18) 0,
    rgba(255, 255, 255, 0.18) 18px,
    rgba(255, 255, 255, 0.38) 18px,
    rgba(255, 255, 255, 0.38) 36px,
    rgba(255, 255, 255, 0.18) 36px,
    rgba(255, 255, 255, 0.18) 54px,
    rgba(255, 255, 255, 0.38) 54px,
    rgba(255, 255, 255, 0.38) 72px
  );
  background-size: 72px 100%;
  animation: progress-stripes 2.8s linear infinite;
}

.progress-trace {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.progress-trace-left {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.progress-trace-time {
  white-space: nowrap;
}

.summary-table :deep(.el-table__cell),
.result-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.summary-table :deep(.cell),
.result-table :deep(.cell) {
  line-height: 1.5;
}

.status-alert {
  margin-top: 16px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.error-text {
  color: var(--el-color-danger);
}

.dash-text {
  color: var(--el-text-color-placeholder);
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.error-log {
  white-space: pre-wrap;
  line-height: 1.7;
}

@keyframes progress-stripes {
  from {
    background-position: 0 0;
  }

  to {
    background-position: 72px 0;
  }
}

@media (max-width: 960px) {
  .status-header,
  .report-header,
  .progress-trace {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions,
  .report-header-actions {
    justify-content: flex-start;
  }

  .table-footer {
    justify-content: center;
  }
}
</style>
