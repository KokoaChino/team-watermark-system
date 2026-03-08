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
  return `\u6d4f\u89c8\u5668\u5f53\u524d\u53ef\u7528\u672c\u5730\u7a7a\u95f4\u7ea6 ${formatStorageSize(availableBytes)}\uff0c\u9884\u8ba1\u672c\u6b21\u4efb\u52a1\u81f3\u5c11\u9700\u8981 ${formatStorageSize(requiredBytes)}\u3002\u8bf7\u5148\u91ca\u653e\u78c1\u76d8\u7a7a\u95f4\u540e\u518d\u5f00\u59cb\u4efb\u52a1\u3002`
}

export function buildStorageSetupFailureMessage() {
  return '\u672c\u5730\u5b58\u50a8\u4e0d\u53ef\u7528\uff0c\u4efb\u52a1\u5c1a\u672a\u5f00\u59cb\u6267\u884c\u3002\u7cfb\u7edf\u5df2\u81ea\u52a8\u7ed3\u675f\u672c\u6b21\u4efb\u52a1\u5e76\u8fd4\u8fd8\u70b9\u6570\u3002\u8bf7\u91ca\u653e\u78c1\u76d8\u7a7a\u95f4\u6216\u68c0\u67e5\u6d4f\u89c8\u5668\u5b58\u50a8\u6743\u9650\u540e\u91cd\u8bd5\u3002'
}

export function buildStorageRuntimeFailureMessage() {
  return '\u672c\u5730\u5b58\u50a8\u7a7a\u95f4\u4e0d\u8db3\uff0c\u4efb\u52a1\u5df2\u63d0\u524d\u7ec8\u6b62\u3002\u7cfb\u7edf\u5c06\u6309\u5df2\u6210\u529f\u5904\u7406\u7684\u56fe\u7247\u6570\u91cf\u7ed3\u7b97\uff0c\u5e76\u8fd4\u8fd8\u5269\u4f59\u70b9\u6570\u3002\u8bf7\u91ca\u653e\u78c1\u76d8\u7a7a\u95f4\u540e\u91cd\u8bd5\u3002'
}

export function buildZipDownloadStorageFailureMessage() {
  return '\u7ed3\u679c\u5305\u5c01\u88c5\u6216\u4fdd\u5b58\u5931\u8d25\uff0c\u53ef\u80fd\u662f\u672c\u5730\u7a7a\u95f4\u4e0d\u8db3\u3002\u8bf7\u91ca\u653e\u78c1\u76d8\u7a7a\u95f4\u540e\u91cd\u8bd5\uff0c\u6216\u5148\u5355\u72ec\u4e0b\u8f7d\u7ed3\u679c\u56fe\u3002'
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
