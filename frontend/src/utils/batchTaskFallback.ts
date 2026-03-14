import type {
  BatchTaskWatermarkFallbackConfig,
  BatchTaskWatermarkFallbackStrategy
} from '@/types'

export const DEFAULT_BATCH_TASK_WATERMARK_FALLBACK_CONFIG: BatchTaskWatermarkFallbackConfig = {
  text: 'skip',
  image: 'template'
}

export const LEGACY_BATCH_TASK_WATERMARK_FALLBACK_CONFIG: BatchTaskWatermarkFallbackConfig = {
  text: 'skip',
  image: 'skip'
}

const AVAILABLE_FALLBACK_STRATEGIES: BatchTaskWatermarkFallbackStrategy[] = ['skip', 'template']

export function normalizeBatchTaskWatermarkFallbackConfig(
  input: Partial<BatchTaskWatermarkFallbackConfig> | null | undefined,
  fallbackConfig: BatchTaskWatermarkFallbackConfig = DEFAULT_BATCH_TASK_WATERMARK_FALLBACK_CONFIG
): BatchTaskWatermarkFallbackConfig {
  const text = AVAILABLE_FALLBACK_STRATEGIES.includes(input?.text as BatchTaskWatermarkFallbackStrategy)
    ? (input?.text as BatchTaskWatermarkFallbackStrategy)
    : fallbackConfig.text
  const image = AVAILABLE_FALLBACK_STRATEGIES.includes(input?.image as BatchTaskWatermarkFallbackStrategy)
    ? (input?.image as BatchTaskWatermarkFallbackStrategy)
    : fallbackConfig.image

  return {
    text,
    image
  }
}
