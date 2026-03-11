export interface LogTagOption {
  label: string
  value: string
}

export interface PersistedColumnMeta {
  columnKey?: string
  property?: string
  label?: string
}

export function emptyToUndefined(value?: string | null) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

export function debounce<T extends (...args: never[]) => void>(fn: T, delay = 300) {
  let timer: number | undefined
  let lastArgs: Parameters<T> | undefined

  const debounced = (...args: Parameters<T>) => {
    lastArgs = args
    if (timer !== undefined) {
      window.clearTimeout(timer)
    }
    timer = window.setTimeout(() => {
      timer = undefined
      if (lastArgs) {
        fn(...lastArgs)
      }
    }, delay)
  }

  debounced.cancel = () => {
    if (timer !== undefined) {
      window.clearTimeout(timer)
      timer = undefined
    }
  }

  return debounced as T & { cancel: () => void }
}

export function buildTimeRangePayload(range?: string[]) {
  if (!range || range.length !== 2) {
    return {
      startTime: undefined,
      endTime: undefined
    }
  }

  return {
    startTime: range[0],
    endTime: range[1]
  }
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return '--'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString('zh-CN', {
    hour12: false
  })
}

export function parseJsonString(value?: string | null): unknown | null {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export function parseJsonObject(value?: string | null) {
  const parsed = parseJsonString(value)
  if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
    return parsed as Record<string, unknown>
  }
  return null
}

export function formatJson(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }

  if (typeof value === 'string') {
    const parsed = parseJsonString(value)
    return parsed === null ? value : JSON.stringify(parsed, null, 2)
  }

  if (typeof value === 'object') {
    return JSON.stringify(value, null, 2)
  }

  return String(value)
}

export function formatBytes(bytes?: number | null) {
  if (bytes === undefined || bytes === null || Number.isNaN(bytes)) {
    return '--'
  }
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

  return `${value.toFixed(value >= 10 ? 1 : 2)} ${units[unitIndex]}`
}

export function loadPersistedColumnWidths(storageKey: string) {
  if (typeof window === 'undefined' || !storageKey) {
    return {} as Record<string, number>
  }

  const rawValue = window.localStorage.getItem(`log-table-widths:${storageKey}`)
  if (!rawValue) {
    return {} as Record<string, number>
  }

  try {
    const parsed = JSON.parse(rawValue) as Record<string, number>
    return Object.entries(parsed).reduce<Record<string, number>>((acc, [key, width]) => {
      if (typeof width === 'number' && Number.isFinite(width) && width > 0) {
        acc[key] = Math.round(width)
      }
      return acc
    }, {})
  } catch {
    return {} as Record<string, number>
  }
}

export function persistColumnWidths(storageKey: string, widths: Record<string, number>) {
  if (typeof window === 'undefined' || !storageKey) {
    return
  }

  window.localStorage.setItem(`log-table-widths:${storageKey}`, JSON.stringify(widths))
}

export function resolvePersistedColumnKey(column?: PersistedColumnMeta | null) {
  if (!column) {
    return ''
  }

  const candidate = column.columnKey || column.property || column.label || ''
  return candidate.trim()
}
