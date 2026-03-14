import type {
  BatchTaskExecutionItem,
  BatchTaskExecutionReport,
  BatchTaskExecutionSession,
  BatchTaskExecutionWatermarkInput,
  BatchTaskImageDraft,
  BatchTaskWatermarkFallbackConfig,
  ImageWatermarkConfig,
  TextWatermarkConfig,
  WatermarkItemDTO,
  WatermarkTemplateVO
} from '@/types'
import {
  getFileExtension,
  getFileStem,
  normalizeDirectoryInput,
  normalizeSupportedOutputExtension,
  validateOutputExtension,
  validateOutputName,
  validateTargetDirectory
} from '@/utils/batchTask'
import {
  LEGACY_BATCH_TASK_WATERMARK_FALLBACK_CONFIG,
  normalizeBatchTaskWatermarkFallbackConfig
} from '@/utils/batchTaskFallback'

interface DrawableAsset {
  width: number
  height: number
  drawable: CanvasImageSource
  cleanup: () => void
}

interface RenderBatchTaskItemOptions {
  session: BatchTaskExecutionSession
  item: BatchTaskExecutionItem
  sourceFile: File
  loadWatermarkFile: (fileKey: string) => Promise<File | null>
}

interface RenderBatchTaskItemSuccess {
  success: true
  blob: Blob
  mimeType: string
  outputFileName: string
  outputExtension: string
  outputDirectory: string
  outputPath: string
  outputWidth: number
  outputHeight: number
  durationMs: number
}

interface RenderBatchTaskItemFailure {
  success: false
  errorMessage: string
  durationMs: number
}

export type RenderBatchTaskItemResult = RenderBatchTaskItemSuccess | RenderBatchTaskItemFailure

const fontLoadCache = new Map<string, Promise<boolean>>()
const remoteImageCache = new Map<string, Promise<HTMLImageElement>>()

const MIME_TYPE_MAP: Record<string, string> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  bmp: 'image/bmp'
}

function sanitizeSerializable<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

export function buildBatchTaskDescription(templateName: string, imageCount: number) {
  return `${templateName}（${imageCount}张）`
}

export function cloneExecutionSession(session: BatchTaskExecutionSession): BatchTaskExecutionSession {
  return sanitizeSerializable({
    ...session,
    templateSnapshot: cloneTemplateSnapshot(session.templateSnapshot),
    items: session.items.map((item) => ({
      ...item,
      watermarkInputs: item.watermarkInputs.map((input) => ({ ...input }))
    }))
  })
}

export function createExecutionSession(options: {
  taskId: number
  taskNo: string
  description: string
  template: WatermarkTemplateVO
  items: BatchTaskImageDraft[]
  watermarkFallbackConfig?: BatchTaskWatermarkFallbackConfig
}) {
  const sessionId = createSessionId()
  const createdAt = new Date().toISOString()

  const executionItems: BatchTaskExecutionItem[] = options.items.map((item) => ({
    id: item.id,
    imageId: item.imageId,
    sourceFileName: item.sourceFileName,
    sourceFileSize: item.size,
    sourceFileType: item.sourceFile.type,
    sourceFileKey: buildSourceFileKey(sessionId, item.id),
    sourceExtension: getFileExtension(item.sourceFileName),
    watermarkInputs: item.watermarkInputs.map((input) => buildExecutionWatermarkInput(sessionId, item.id, input)),
    targetDirectory: item.targetDirectory,
    outputFileName: item.outputFileName,
    outputExtension: item.outputExtension,
    status: 'pending',
    durationMs: 0
  }))

  const session: BatchTaskExecutionSession = {
    id: sessionId,
    taskId: options.taskId,
    taskNo: options.taskNo,
    templateId: options.template.id,
    templateName: options.template.name,
    templateVersion: options.template.version,
    templateSnapshot: cloneTemplateSnapshot(options.template),
    watermarkFallbackConfig: normalizeBatchTaskWatermarkFallbackConfig(options.watermarkFallbackConfig),
    description: options.description,
    createdAt,
    status: 'queued',
    totalCount: executionItems.length,
    processedCount: 0,
    successCount: 0,
    failedCount: 0,
    items: executionItems
  }

  return session
}

export function buildSourceFileKey(sessionId: string, itemId: string) {
  return `${sessionId}:source:${itemId}`
}

export function buildWatermarkFileKey(sessionId: string, itemId: string, watermarkId: string) {
  return `${sessionId}:watermark:${itemId}:${watermarkId}`
}

export function buildResultFileKey(sessionId: string, itemId: string) {
  return `${sessionId}:result:${itemId}`
}

export function buildArtifactKey(sessionId: string, artifactName: string) {
  return `${sessionId}:artifact:${artifactName}`
}

export function getSessionZipFileName(session: BatchTaskExecutionSession) {
  return `${session.taskNo || `batch-task-${session.id}`}.zip`
}

export function buildExecutionReport(session: BatchTaskExecutionSession): BatchTaskExecutionReport {
  return {
    taskId: session.taskId,
    taskNo: session.taskNo,
    templateId: session.templateId,
    templateName: session.templateName,
    templateVersion: session.templateVersion,
    totalCount: session.totalCount,
    successCount: session.successCount,
    failedCount: session.failedCount,
    startedAt: session.startedAt,
    finishedAt: session.finishedAt,
    items: session.items.map((item) => ({
      itemId: item.id,
      imageId: item.imageId,
      sourceFileName: item.sourceFileName,
      status: item.status,
      durationMs: item.durationMs,
      outputPath: buildOutputPath(item.resolvedTargetDirectory || '', item.resultFileName || ''),
      errorMessage: item.errorMessage
    }))
  }
}

export async function renderBatchTaskItem(options: RenderBatchTaskItemOptions): Promise<RenderBatchTaskItemResult> {
  const startedAt = performance.now()
  const cleanupList: Array<() => void> = []

  try {
    const sourceAsset = await loadDrawableFromFile(options.sourceFile)
    cleanupList.push(sourceAsset.cleanup)
    await yieldToMainThread()

    const canvas = document.createElement('canvas')
    canvas.width = sourceAsset.width
    canvas.height = sourceAsset.height

    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('无法创建图片渲染上下文')
    }

    ctx.drawImage(sourceAsset.drawable, 0, 0, canvas.width, canvas.height)
    await yieldToMainThread()

    const baseConfig = options.session.templateSnapshot.config.baseConfig
    const scaleX = canvas.width / baseConfig.width
    const scaleY = canvas.height / baseConfig.height
    const watermarkFallbackConfig = normalizeBatchTaskWatermarkFallbackConfig(
      options.session.watermarkFallbackConfig,
      LEGACY_BATCH_TASK_WATERMARK_FALLBACK_CONFIG
    )

    const watermarkMap = new Map(options.item.watermarkInputs.map((input) => [input.watermarkId, input]))
    const watermarks = options.session.templateSnapshot.config.watermarks

    for (let index = watermarks.length - 1; index >= 0; index -= 1) {
      const watermark = watermarks[index]
      const input = watermarkMap.get(watermark.id)
      if (!input) {
        continue
      }

      if (watermark.type === 'text') {
        await drawTextWatermarkWithFallback({
          ctx,
          watermark,
          inputValue: input.value,
          fallbackStrategy: watermarkFallbackConfig.text,
          scaleX,
          scaleY
        })
        continue
      }

      const imageAsset = await resolveImageWatermarkAssetWithFallback({
        input,
        watermark,
        fallbackStrategy: watermarkFallbackConfig.image,
        loadWatermarkFile: options.loadWatermarkFile
      })
      if (!imageAsset) {
        continue
      }

      cleanupList.push(imageAsset.cleanup)

      await drawImageWatermarkOnCanvas({
        ctx,
        watermark,
        asset: imageAsset,
        sourceWidth: canvas.width,
        sourceHeight: canvas.height,
        baseWidth: baseConfig.width,
        baseHeight: baseConfig.height,
        scaleX,
        scaleY
      })
    }

    await yieldToMainThread()

    const outputSpec = resolveOutputSpec(options.item, options.item.sourceFileName || options.sourceFile.name)
    const mimeType = MIME_TYPE_MAP[outputSpec.outputExtension]
    if (!mimeType) {
      throw new Error(`不支持的输出格式：${outputSpec.outputExtension}`)
    }

    const blob = await canvasToOutputBlob(canvas, outputSpec.outputExtension)
    await yieldToMainThread()

    canvas.width = 0
    canvas.height = 0

    const durationMs = performance.now() - startedAt
    const outputPath = buildOutputPath(outputSpec.outputDirectory, outputSpec.fileName)

    return {
      success: true,
      blob,
      mimeType,
      outputFileName: outputSpec.fileName,
      outputExtension: outputSpec.outputExtension,
      outputDirectory: outputSpec.outputDirectory,
      outputPath,
      outputWidth: sourceAsset.width,
      outputHeight: sourceAsset.height,
      durationMs
    }
  } catch (error) {
    return {
      success: false,
      errorMessage: error instanceof Error ? error.message : '图片渲染失败',
      durationMs: performance.now() - startedAt
    }
  } finally {
    cleanupList.reverse().forEach((cleanup) => {
      try {
        cleanup()
      } catch (error) {
        console.error('释放批量任务渲染资源失败:', error)
      }
    })
  }
}

export function updateSessionItem(session: BatchTaskExecutionSession, itemId: string, updater: (item: BatchTaskExecutionItem) => BatchTaskExecutionItem) {
  return {
    ...session,
    items: session.items.map((item) => (item.id === itemId ? updater(item) : item))
  }
}

export function createObjectUrl(blob: Blob) {
  return URL.createObjectURL(blob)
}

export function revokeObjectUrl(url?: string) {
  if (url && url.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}

export async function downloadFileBlob(blob: Blob, fileName: string) {
  const downloadUrl = URL.createObjectURL(blob)
  try {
    const anchor = document.createElement('a')
    anchor.href = downloadUrl
    anchor.download = fileName
    anchor.style.display = 'none'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
  } finally {
    setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000)
  }
}

export function isSessionRecoverable(status: BatchTaskExecutionSession['status']) {
  return ['queued', 'running', 'packaging', 'completing', 'complete_failed', 'completed'].includes(status)
}

export function formatDuration(durationMs: number) {
  if (durationMs < 1000) {
    return `${Math.round(durationMs)} ms`
  }

  return `${(durationMs / 1000).toFixed(2)} s`
}

export async function yieldToMainThread() {
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve())
  })
}

function buildExecutionWatermarkInput(sessionId: string, itemId: string, input: BatchTaskImageDraft['watermarkInputs'][number]): BatchTaskExecutionWatermarkInput {
  if (input.type === 'image' && input.localFile) {
    return {
      watermarkId: input.watermarkId,
      watermarkName: input.watermarkName,
      type: input.type,
      value: '',
      localFileKey: buildWatermarkFileKey(sessionId, itemId, input.watermarkId),
      localFileName: input.localFileName
    }
  }

  return {
    watermarkId: input.watermarkId,
    watermarkName: input.watermarkName,
    type: input.type,
    value: input.value
  }
}

function createSessionId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function cloneTemplateSnapshot(template: WatermarkTemplateVO): WatermarkTemplateVO {
  return sanitizeSerializable({
    ...template,
    config: {
      ...template.config,
      baseConfig: { ...template.config.baseConfig },
      watermarks: template.config.watermarks.map((watermark) => cloneWatermark(watermark))
    }
  })
}

function cloneWatermark(watermark: WatermarkItemDTO): WatermarkItemDTO {
  return sanitizeSerializable({
    ...watermark,
    textConfig: watermark.textConfig ? { ...watermark.textConfig } : undefined,
    imageConfig: watermark.imageConfig ? { ...watermark.imageConfig } : undefined
  })
}

async function drawTextWatermarkWithFallback(options: {
  ctx: CanvasRenderingContext2D
  watermark: WatermarkItemDTO
  inputValue: string
  fallbackStrategy: BatchTaskWatermarkFallbackConfig['text']
  scaleX: number
  scaleY: number
}) {
  const templateText = getTemplateTextWatermarkContent(options.watermark)
  let textValue = options.inputValue.trim()

  if (!textValue && options.fallbackStrategy === 'template') {
    textValue = templateText
  }

  if (!textValue) {
    return
  }

  try {
    await drawTextWatermarkOnCanvas({
      ctx: options.ctx,
      watermark: options.watermark,
      textValue,
      scaleX: options.scaleX,
      scaleY: options.scaleY
    })
    return
  } catch (error) {
    const canRetryWithTemplate = options.fallbackStrategy === 'template'
      && Boolean(templateText)
      && templateText !== textValue

    if (!canRetryWithTemplate) {
      console.warn(`文字水印“${options.watermark.name}”绘制失败，已按降级策略跳过:`, error)
      return
    }
  }

  try {
    await drawTextWatermarkOnCanvas({
      ctx: options.ctx,
      watermark: options.watermark,
      textValue: templateText,
      scaleX: options.scaleX,
      scaleY: options.scaleY
    })
  } catch (templateError) {
    console.warn(`文字水印“${options.watermark.name}”模板内容绘制失败，已跳过:`, templateError)
  }
}

async function resolveImageWatermarkAssetWithFallback(options: {
  input: BatchTaskExecutionWatermarkInput
  watermark: WatermarkItemDTO
  fallbackStrategy: BatchTaskWatermarkFallbackConfig['image']
  loadWatermarkFile: (fileKey: string) => Promise<File | null>
}): Promise<DrawableAsset | null> {
  const templateImageUrl = getTemplateImageWatermarkUrl(options.watermark)
  const fallbackToTemplate = options.fallbackStrategy === 'template' && Boolean(templateImageUrl)
  const templateAlreadyInUse = !options.input.localFileKey && options.input.value.trim() === templateImageUrl

  try {
    const inputAsset = await resolveWatermarkAsset(options.input, options.loadWatermarkFile)
    if (inputAsset) {
      return inputAsset
    }
  } catch (error) {
    if (!fallbackToTemplate || templateAlreadyInUse) {
      console.warn(`图片水印“${options.watermark.name}”处理失败，已按降级策略跳过:`, error)
      return null
    }

    try {
      return await loadDrawableFromUrl(templateImageUrl, `图片水印“${options.watermark.name}”模板资源加载失败`)
    } catch (templateError) {
      console.warn(`图片水印“${options.watermark.name}”模板降级失败，已跳过:`, templateError)
      return null
    }
  }

  if (!fallbackToTemplate || templateAlreadyInUse || options.input.localFileKey) {
    return null
  }

  try {
    return await loadDrawableFromUrl(templateImageUrl, `图片水印“${options.watermark.name}”模板资源加载失败`)
  } catch (templateError) {
    console.warn(`图片水印“${options.watermark.name}”模板内容加载失败，已跳过:`, templateError)
    return null
  }
}

function getTemplateTextWatermarkContent(watermark: WatermarkItemDTO) {
  return (watermark.textConfig?.content || '').trim()
}

function getTemplateImageWatermarkUrl(watermark: WatermarkItemDTO) {
  return (watermark.imageConfig?.imageUrl || '').trim()
}

async function resolveWatermarkAsset(
  input: BatchTaskExecutionWatermarkInput,
  loadWatermarkFile: (fileKey: string) => Promise<File | null>
): Promise<DrawableAsset | null> {
  if (input.type !== 'image') {
    return null
  }

  if (input.localFileKey) {
    const file = await loadWatermarkFile(input.localFileKey)
    if (!file) {
      throw new Error(`图片水印“${input.watermarkName}”文件丢失，请重新创建任务`)
    }

    return loadDrawableFromFile(file)
  }

  const imageUrl = input.value.trim()
  if (!imageUrl) {
    return null
  }

  return loadDrawableFromUrl(imageUrl, `图片水印“${input.watermarkName}”加载失败`)
}

async function loadDrawableFromFile(file: File): Promise<DrawableAsset> {
  const objectUrl = URL.createObjectURL(file)
  try {
    const image = await loadImageElement(objectUrl)
    return {
      width: image.naturalWidth || image.width,
      height: image.naturalHeight || image.height,
      drawable: image,
      cleanup: () => URL.revokeObjectURL(objectUrl)
    }
  } catch (error) {
    URL.revokeObjectURL(objectUrl)
    throw error
  }
}

async function loadDrawableFromUrl(url: string, errorMessage: string): Promise<DrawableAsset> {
  try {
    const image = await loadRemoteImage(url)
    return {
      width: image.naturalWidth || image.width,
      height: image.naturalHeight || image.height,
      drawable: image,
      cleanup: () => {}
    }
  } catch (error) {
    throw new Error(errorMessage || (error instanceof Error ? error.message : '图片资源加载失败'))
  }
}

function loadRemoteImage(url: string) {
  if (!remoteImageCache.has(url)) {
    remoteImageCache.set(url, loadImageElement(url))
  }

  return remoteImageCache.get(url)!
}

function loadImageElement(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error(`图片加载失败：${url}`))
    image.src = url
  })
}

async function drawTextWatermarkOnCanvas(options: {
  ctx: CanvasRenderingContext2D
  watermark: WatermarkItemDTO
  textValue: string
  scaleX: number
  scaleY: number
}) {
  const config = options.watermark.textConfig as TextWatermarkConfig | undefined
  if (!config) {
    return
  }

  if (config.fontUrl) {
    const fontLoaded = await ensureFontLoaded(config.fontFamily, config.fontUrl)
    if (!fontLoaded) {
      throw new Error(`文字水印“${options.watermark.name}”字体加载失败`)
    }
  }

  const ctx = options.ctx
  const centerX = options.watermark.x * options.scaleX
  const centerY = options.watermark.y * options.scaleY
  const fontSize = config.fontSize * options.scaleY
  const letterSpacing = (config.letterSpacing || 0) * options.scaleX
  const fontWeight = config.fontWeight || 400

  ctx.save()
  ctx.translate(centerX, centerY)
  ctx.rotate((config.rotation || options.watermark.rotation || 0) * Math.PI / 180)
  ctx.globalAlpha = config.opacity ?? options.watermark.opacity ?? 1
  ctx.font = `${fontWeight} ${fontSize}px "${config.fontFamily}"`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  const italicAngle = config.italicAngle ?? 0
  if (italicAngle !== 0) {
    ctx.transform(1, 0, Math.tan(italicAngle * Math.PI / 180), 1, 0, 0)
  }

  const chars = Array.from(options.textValue)
  const charWidths = chars.map((char) => ctx.measureText(char).width)
  const totalWidth = charWidths.reduce((sum, width) => sum + width, 0) + Math.max(0, chars.length - 1) * letterSpacing

  if (config.gradientEnabled && config.gradientColors && config.gradientColors.length >= 2) {
    const angle = (config.gradientAngle ?? 0) * Math.PI / 180
    const halfWidth = totalWidth / 2
    const gradient = ctx.createLinearGradient(
      -halfWidth * Math.cos(angle),
      -halfWidth * Math.sin(angle),
      halfWidth * Math.cos(angle),
      halfWidth * Math.sin(angle)
    )
    gradient.addColorStop(0, config.gradientColors[0])
    gradient.addColorStop(1, config.gradientColors[1])
    ctx.fillStyle = gradient
  } else {
    ctx.fillStyle = config.color
  }

  if (config.shadowEnabled && config.shadowColor) {
    ctx.shadowColor = config.shadowColor
    ctx.shadowBlur = (config.shadowBlur || 0) * Math.max(options.scaleX, options.scaleY)
    ctx.shadowOffsetX = (config.shadowOffsetX || 0) * options.scaleX
    ctx.shadowOffsetY = (config.shadowOffsetY || 0) * options.scaleY
  }

  let startOffset = 0
  if (config.align === 'left') {
    startOffset = totalWidth / 2
  } else if (config.align === 'right') {
    startOffset = -totalWidth / 2
  }

  let cursorX = -totalWidth / 2 + startOffset
  for (let index = 0; index < chars.length; index += 1) {
    const char = chars[index]
    const charWidth = charWidths[index]
    ctx.fillText(char, cursorX + charWidth / 2, 0)
    cursorX += charWidth + letterSpacing
  }

  if (config.strokeEnabled && config.strokeColor && config.strokeWidth) {
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0
    ctx.strokeStyle = config.strokeColor
    ctx.lineWidth = config.strokeWidth * Math.max(options.scaleX, options.scaleY)

    cursorX = -totalWidth / 2 + startOffset
    for (let index = 0; index < chars.length; index += 1) {
      const char = chars[index]
      const charWidth = charWidths[index]
      ctx.strokeText(char, cursorX + charWidth / 2, 0)
      cursorX += charWidth + letterSpacing
    }
  }

  ctx.restore()
}

async function drawImageWatermarkOnCanvas(options: {
  ctx: CanvasRenderingContext2D
  watermark: WatermarkItemDTO
  asset: DrawableAsset
  sourceWidth: number
  sourceHeight: number
  baseWidth: number
  baseHeight: number
  scaleX: number
  scaleY: number
}) {
  const config = options.watermark.imageConfig as ImageWatermarkConfig | undefined
  if (!config) {
    return
  }

  const ctx = options.ctx
  const rotation = (options.watermark.rotation || 0) * Math.PI / 180
  const opacity = config.opacity ?? options.watermark.opacity ?? 1

  const originalWidth = config.originalWidth || options.asset.width
  const originalHeight = config.originalHeight || options.asset.height

  let drawWidth = originalWidth * (config.scale / 100) * options.scaleX
  let drawHeight = originalHeight * (config.scale / 100) * options.scaleY
  let centerX = options.watermark.x * options.scaleX
  let centerY = options.watermark.y * options.scaleY

  if (config.fitMode === 'scaleToFill') {
    drawWidth = options.sourceWidth
    drawHeight = options.sourceHeight
    centerX = options.sourceWidth / 2
    centerY = options.sourceHeight / 2
  } else if (config.fitMode === 'aspectFit') {
    const assetRatio = originalWidth / originalHeight
    const canvasRatio = options.sourceWidth / options.sourceHeight
    if (assetRatio > canvasRatio) {
      drawWidth = options.sourceWidth
      drawHeight = drawWidth / assetRatio
    } else {
      drawHeight = options.sourceHeight
      drawWidth = drawHeight * assetRatio
    }
    centerX = options.sourceWidth / 2
    centerY = options.sourceHeight / 2
  } else if (config.fitMode === 'aspectFill') {
    const assetRatio = originalWidth / originalHeight
    const canvasRatio = options.sourceWidth / options.sourceHeight
    if (assetRatio > canvasRatio) {
      drawHeight = options.sourceHeight
      drawWidth = drawHeight * assetRatio
    } else {
      drawWidth = options.sourceWidth
      drawHeight = drawWidth * assetRatio
    }
    centerX = options.sourceWidth / 2
    centerY = options.sourceHeight / 2
  }

  if (config.fitMode === 'none' && config.anchor !== 'none') {
    const halfWidth = drawWidth / 2
    const halfHeight = drawHeight / 2

    switch (config.anchor) {
      case 'topLeft':
        centerX = halfWidth
        centerY = halfHeight
        break
      case 'topRight':
        centerX = options.sourceWidth - halfWidth
        centerY = halfHeight
        break
      case 'bottomLeft':
        centerX = halfWidth
        centerY = options.sourceHeight - halfHeight
        break
      case 'bottomRight':
        centerX = options.sourceWidth - halfWidth
        centerY = options.sourceHeight - halfHeight
        break
      case 'center':
        centerX = options.sourceWidth / 2
        centerY = options.sourceHeight / 2
        break
    }
  }

  ctx.save()
  ctx.globalAlpha = opacity
  ctx.translate(centerX, centerY)
  ctx.rotate(rotation)
  ctx.drawImage(options.asset.drawable, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight)
  ctx.restore()
}

async function ensureFontLoaded(fontFamily: string, fontUrl: string) {
  const cacheKey = `${fontFamily}|${fontUrl}`
  if (!fontLoadCache.has(cacheKey)) {
    fontLoadCache.set(cacheKey, loadCustomFont(fontFamily, fontUrl))
  }

  return fontLoadCache.get(cacheKey)!
}

async function loadCustomFont(fontFamily: string, fontUrl: string) {
  try {
    const fontFace = new FontFace(fontFamily, `url(${fontUrl})`)
    await fontFace.load()
    document.fonts.add(fontFace)
    await document.fonts.load(`12px "${fontFamily}"`)
    return true
  } catch (error) {
    console.error(`批量任务字体加载失败: ${fontFamily}`, error)
    return false
  }
}

function resolveOutputSpec(item: BatchTaskExecutionItem, sourceFileName: string) {
  const sourceStem = getFileStem(sourceFileName) || item.imageId || 'image'
  const sourceExtension = normalizeSupportedOutputExtension(item.sourceExtension)
    || normalizeSupportedOutputExtension(getFileExtension(sourceFileName))
    || 'png'

  const outputDirectory = validateTargetDirectory(item.targetDirectory)
    ? ''
    : normalizeDirectoryInput(item.targetDirectory)

  const outputFileName = validateOutputName(item.outputFileName)
    ? sourceStem
    : (item.outputFileName.trim() || sourceStem)

  const normalizedOutputExtension = normalizeSupportedOutputExtension(item.outputExtension)
  const outputExtension = validateOutputExtension(item.outputExtension)
    ? sourceExtension
    : (normalizedOutputExtension || sourceExtension)

  return {
    outputDirectory,
    outputFileName,
    outputExtension,
    fileName: `${outputFileName}.${outputExtension}`
  }
}

function buildOutputPath(outputDirectory: string, fileName: string) {
  if (!fileName) {
    return ''
  }

  return outputDirectory ? `${outputDirectory}/${fileName}` : fileName
}

async function canvasToOutputBlob(canvas: HTMLCanvasElement, extension: string) {
  if (extension === 'bmp') {
    return canvasToBmpBlob(canvas)
  }

  const mimeType = MIME_TYPE_MAP[extension]
  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('图片编码失败'))
          return
        }
        resolve(blob)
      },
      mimeType,
      extension === 'jpg' || extension === 'jpeg' || extension === 'webp' ? 0.92 : undefined
    )
  })
}

function canvasToBmpBlob(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('无法创建 BMP 编码上下文')
  }

  const width = canvas.width
  const height = canvas.height
  const imageData = ctx.getImageData(0, 0, width, height)
  const bytesPerPixel = 3
  const rowStride = Math.floor((width * bytesPerPixel + 3) / 4) * 4
  const pixelArraySize = rowStride * height
  const fileSize = 54 + pixelArraySize
  const buffer = new ArrayBuffer(fileSize)
  const view = new DataView(buffer)
  const pixelData = imageData.data

  let offset = 0
  view.setUint8(offset, 0x42)
  view.setUint8(offset + 1, 0x4d)
  offset += 2
  view.setUint32(offset, fileSize, true)
  offset += 4
  view.setUint32(offset, 0, true)
  offset += 4
  view.setUint32(offset, 54, true)
  offset += 4
  view.setUint32(offset, 40, true)
  offset += 4
  view.setInt32(offset, width, true)
  offset += 4
  view.setInt32(offset, height, true)
  offset += 4
  view.setUint16(offset, 1, true)
  offset += 2
  view.setUint16(offset, 24, true)
  offset += 2
  view.setUint32(offset, 0, true)
  offset += 4
  view.setUint32(offset, pixelArraySize, true)
  offset += 4
  view.setInt32(offset, 2835, true)
  offset += 4
  view.setInt32(offset, 2835, true)
  offset += 4
  view.setUint32(offset, 0, true)
  offset += 4
  view.setUint32(offset, 0, true)

  let byteOffset = 54
  const padding = rowStride - width * bytesPerPixel

  for (let y = height - 1; y >= 0; y -= 1) {
    for (let x = 0; x < width; x += 1) {
      const pixelOffset = (y * width + x) * 4
      view.setUint8(byteOffset, pixelData[pixelOffset + 2])
      view.setUint8(byteOffset + 1, pixelData[pixelOffset + 1])
      view.setUint8(byteOffset + 2, pixelData[pixelOffset])
      byteOffset += 3
    }

    for (let index = 0; index < padding; index += 1) {
      view.setUint8(byteOffset, 0)
      byteOffset += 1
    }
  }

  return new Blob([buffer], { type: 'image/bmp' })
}
