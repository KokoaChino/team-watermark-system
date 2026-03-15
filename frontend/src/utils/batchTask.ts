import type {
  BatchTaskImageDraft,
  ImageConfigVO,
  PendingBatchTaskDraft,
  WatermarkTemplateVO
} from '@/types'

export type ExcelMergeMode = 'overwrite' | 'fill-empty'

const INVALID_PATH_SEGMENT_PATTERN = /[<>:"\\|?*\u0000-\u001F]/
const INVALID_FILE_NAME_PATTERN = /[<>:"/\\|?*\u0000-\u001F]/
const EXTENSION_PATTERN = /^[a-zA-Z0-9]{1,10}$/

export const SUPPORTED_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'bmp'] as const

type SupportedImageExtension = typeof SUPPORTED_IMAGE_EXTENSIONS[number]

export function createTaskImageDraft(file: File, template: WatermarkTemplateVO): BatchTaskImageDraft {
  return {
    id: createDraftId(),
    imageId: getFileStem(file.name),
    sourceFile: file,
    sourceFileName: file.name,
    previewUrl: URL.createObjectURL(file),
    size: file.size,
    watermarkInputs: template.config.watermarks.map((watermark) => ({
      watermarkId: watermark.id,
      watermarkName: watermark.name,
      type: watermark.type,
      value: '',
      imagePreviewUrl: '',
      localFile: undefined,
      localFileName: ''
    })),
    targetDirectory: '',
    outputFileName: '',
    outputExtension: ''
  }
}

export function cloneTaskImageDraft(item: BatchTaskImageDraft): BatchTaskImageDraft {
  return {
    ...item,
    watermarkInputs: item.watermarkInputs.map((input) => ({ ...input }))
  }
}

export function clonePendingBatchTaskDraft(draft: PendingBatchTaskDraft): PendingBatchTaskDraft {
  return {
    ...draft,
    items: draft.items.map(cloneTaskImageDraft)
  }
}

export function applyExcelConfigToTaskImage(options: {
  image: BatchTaskImageDraft
  excelConfig: ImageConfigVO
  mergeMode: ExcelMergeMode
}): BatchTaskImageDraft {
  const nextImage = cloneTaskImageDraft(options.image)
  const { excelConfig, mergeMode } = options

  if (mergeMode === 'overwrite') {
    nextImage.targetDirectory = ''
    nextImage.outputFileName = ''
    nextImage.outputExtension = ''
    nextImage.watermarkInputs = nextImage.watermarkInputs.map((input) => ({
      ...input,
      value: '',
      imagePreviewUrl: '',
      localFile: undefined,
      localFileName: ''
    }))
  }

  let textIndex = 0
  let imageIndex = 0

  nextImage.watermarkInputs = nextImage.watermarkInputs.map((input) => {
    if (input.type === 'text') {
      const incomingValue = normalizeCellValue(excelConfig.textWatermarks[textIndex])
      textIndex += 1
      return {
        ...input,
        value: mergeFieldValue(input.value, incomingValue, mergeMode)
      }
    }

    const incomingValue = normalizeCellValue(excelConfig.imageWatermarks[imageIndex])
    imageIndex += 1
    const shouldApplyIncomingValue = mergeMode === 'overwrite' || !isImageWatermarkConfigured(input)

    if (!shouldApplyIncomingValue) {
      return {
        ...input,
        imagePreviewUrl: input.imagePreviewUrl || input.value
      }
    }

    return {
      ...input,
      value: incomingValue,
      imagePreviewUrl: incomingValue,
      localFile: undefined,
      localFileName: ''
    }
  })

  const nextDirectory = buildTargetDirectory(excelConfig.filePaths)
  const nextFileName = normalizeCellValue(excelConfig.rename)
  const nextExtension = normalizeSupportedOutputExtension(excelConfig.extension)

  nextImage.targetDirectory = mergeFieldValue(nextImage.targetDirectory, nextDirectory, mergeMode)
  nextImage.outputFileName = mergeFieldValue(nextImage.outputFileName, nextFileName, mergeMode)
  nextImage.outputExtension = mergeFieldValue(nextImage.outputExtension, nextExtension, mergeMode)

  return nextImage
}

export function countConfiguredWatermarks(item: BatchTaskImageDraft): number {
  return item.watermarkInputs.filter((input) => {
    if (input.type === 'image') {
      return isImageWatermarkConfigured(input)
    }

    return input.value.trim().length > 0
  }).length
}

export function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }

  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

export function getFileStem(fileName: string): string {
  const lastDotIndex = fileName.lastIndexOf('.')
  if (lastDotIndex <= 0) {
    return fileName
  }

  return fileName.slice(0, lastDotIndex)
}

export function getFileExtension(fileName: string): string {
  const lastDotIndex = fileName.lastIndexOf('.')
  if (lastDotIndex <= 0) {
    return ''
  }

  return fileName.slice(lastDotIndex + 1).toLowerCase()
}

export function normalizeDirectoryInput(value: string): string {
  if (!value) {
    return ''
  }

  return value
    .trim()
    .split('/')
    .map((segment) => segment.trim())
    .filter(Boolean)
    .join('/')
}

export function validateTargetDirectory(value: string): string {
  const trimmedValue = value.trim()
  if (!trimmedValue) {
    return ''
  }

  const invalidSegment = trimmedValue
    .split('/')
    .map((segment) => segment.trim())
    .find((segment) => segment && INVALID_PATH_SEGMENT_PATTERN.test(segment))

  if (invalidSegment) {
    return '文件名包含非法字符：< > : " / \\ | ? *'
  }

  return ''
}

export function validateOutputName(value: string): string {
  const trimmedValue = value.trim()
  if (!trimmedValue) {
    return ''
  }

  if (INVALID_FILE_NAME_PATTERN.test(trimmedValue)) {
    return '文件名包含非法字符：< > : " / \\ | ? *'
  }

  return ''
}

export function sanitizeExtension(value: string | undefined): string {
  return (value || '').trim().replace(/^\./, '').toLowerCase()
}

export function normalizeSupportedOutputExtension(value: string | undefined): string {
  const normalizedValue = sanitizeExtension(value)
  if (!normalizedValue) {
    return ''
  }

  return SUPPORTED_IMAGE_EXTENSIONS.includes(normalizedValue as SupportedImageExtension)
    ? normalizedValue
    : ''
}

export function validateOutputExtension(value: string): string {
  const normalizedValue = sanitizeExtension(value)
  if (!normalizedValue) {
    return ''
  }

  if (!EXTENSION_PATTERN.test(normalizedValue)) {
    return '拓展名仅支持 1-10 位字母或数字'
  }

  if (!SUPPORTED_IMAGE_EXTENSIONS.includes(normalizedValue as SupportedImageExtension)) {
    return `拓展名仅支持：${SUPPORTED_IMAGE_EXTENSIONS.map((item) => item.toUpperCase()).join(' / ')}`
  }

  return ''
}

export function buildTargetDirectory(filePaths: string[]): string {
  return normalizeDirectoryInput(filePaths.join('/'))
}

function createDraftId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function mergeFieldValue(currentValue: string, incomingValue: string, mergeMode: ExcelMergeMode): string {
  if (mergeMode === 'overwrite') {
    return incomingValue
  }

  return currentValue.trim() ? currentValue : incomingValue
}

function normalizeCellValue(value: string | undefined): string {
  return (value || '').trim()
}

function isImageWatermarkConfigured(input: BatchTaskImageDraft['watermarkInputs'][number]): boolean {
  return Boolean(input.localFile || input.value.trim())
}
