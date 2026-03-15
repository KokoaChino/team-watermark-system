import type { BatchTaskExecutionSession, BatchTaskImageDraft } from '@/types'
import { getFileExtension, normalizeSupportedOutputExtension } from '@/utils/batchTask'

const MB = 1024 * 1024
const MIN_HEADROOM_BYTES = 128 * MB
const PERSISTENT_STORAGE_WARNING_MESSAGE = '浏览器未授予持久存储，极端情况下本地结果可能被系统清理。建议在任务完成前不要清理浏览器数据'
let persistentStorageWarningShown = false

type ExtendedStorageManager = StorageManager & {
  persist?: () => Promise<boolean>
  persisted?: () => Promise<boolean>
}

export interface BrowserStorageSnapshot {
  supported: boolean
  quota?: number
  usage?: number
  available?: number
  persisted?: boolean | null
}

export function formatStorageSize(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`
  }

  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes / 1024
  let unitIndex = 0

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }

  return `${value.toFixed(value >= 100 ? 0 : value >= 10 ? 1 : 2)} ${units[unitIndex]}`
}

export async function inspectBrowserStorage(requestPersistence = false): Promise<BrowserStorageSnapshot> {
  if (typeof navigator === 'undefined' || !('storage' in navigator)) {
    return {
      supported: false,
      persisted: null
    }
  }

  const storage = navigator.storage as ExtendedStorageManager
  let persisted: boolean | null = null

  if (typeof storage.persisted === 'function') {
    persisted = await storage.persisted().catch(() => null)
  }

  if (requestPersistence && typeof storage.persist === 'function' && persisted !== true) {
    persisted = await storage.persist().catch(() => persisted)
  }

  const estimate = typeof storage.estimate === 'function'
    ? await storage.estimate().catch(() => undefined)
    : undefined

  const usage = typeof estimate?.usage === 'number' ? estimate.usage : undefined
  const quota = typeof estimate?.quota === 'number' ? estimate.quota : undefined

  return {
    supported: true,
    usage,
    quota,
    available: typeof usage === 'number' && typeof quota === 'number' ? Math.max(0, quota - usage) : undefined,
    persisted
  }
}

export function estimateDraftStorageNeed(items: BatchTaskImageDraft[]) {
  const sourceBytes = items.reduce((sum, item) => sum + item.sourceFile.size, 0)
  const watermarkBytes = items.reduce((sum, item) => {
    return sum + item.watermarkInputs.reduce((inner, input) => inner + (input.localFile?.size || 0), 0)
  }, 0)
  const resultBytes = items.reduce((sum, item) => {
    const extension = normalizeSupportedOutputExtension(item.outputExtension)
      || normalizeSupportedOutputExtension(getFileExtension(item.sourceFileName))
      || 'png'
    return sum + estimateResultBytes(item.sourceFile.size, extension)
  }, 0)

  return estimateRequiredBytes(sourceBytes + watermarkBytes + resultBytes)
}

export function estimateExecutionStorageNeed(session: BatchTaskExecutionSession) {
  const pendingItems = session.items.filter((item) => item.status === 'pending' || item.status === 'processing')
  const resultBytes = pendingItems.reduce((sum, item) => {
    const extension = normalizeSupportedOutputExtension(item.outputExtension)
      || normalizeSupportedOutputExtension(item.sourceExtension)
      || 'png'
    return sum + estimateResultBytes(item.sourceFileSize, extension)
  }, 0)

  return estimateRequiredBytes(resultBytes)
}

export function isStoragePressureError(error: unknown) {
  if (error instanceof DOMException && error.name === 'QuotaExceededError') {
    return true
  }

  const message = error instanceof Error ? error.message : String(error || '')
  return /quota|disk|space|storage|database or disk is full|no space left/i.test(message)
}

export function isUserCancelledFileSelection(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError'
}

export function buildStorageInsufficientMessage(requiredBytes: number, availableBytes: number) {
  return `浏览器当前可用本地空间约 ${formatStorageSize(availableBytes)}，预计本次任务至少需要 ${formatStorageSize(requiredBytes)}。请先释放磁盘空间后再开始任务`
}

export function buildStorageSetupFailureMessage() {
  return '本地存储不可用，任务尚未开始执行。系统已自动结束本次任务并返还点数。请释放磁盘空间或检查浏览器存储权限后重试'
}

export function buildStorageRuntimeFailureMessage() {
  return '本地存储空间不足，任务已提前终止。系统将按已成功处理的图片数量结算，并返还剩余点数。请释放磁盘空间后重试'
}

export function buildZipDownloadStorageFailureMessage() {
  return '结果包封装或保存失败，可能是本地空间不足。请释放磁盘空间后重试，或先单独下载结果图'
}

function estimateRequiredBytes(bytes: number) {
  const headroom = Math.max(MIN_HEADROOM_BYTES, Math.round(bytes * 0.25))
  return Math.max(0, Math.round(bytes + headroom))
}

function estimateResultBytes(sourceBytes: number, extension: string) {
  const normalized = extension.toLowerCase()

  switch (normalized) {
    case 'jpg':
    case 'jpeg':
    case 'webp':
      return Math.round(sourceBytes * 1.4)
    case 'png':
      return Math.round(sourceBytes * 2.4)
    case 'bmp':
      return Math.round(sourceBytes * 12)
    default:
      return Math.round(sourceBytes * 2)
  }
}

export function consumePersistentStorageWarningMessage() {
  if (persistentStorageWarningShown) {
    return ''
  }

  persistentStorageWarningShown = true
  return PERSISTENT_STORAGE_WARNING_MESSAGE
}
