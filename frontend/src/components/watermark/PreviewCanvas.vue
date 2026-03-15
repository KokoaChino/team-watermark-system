<template>
  <div class="preview-container">
    <div class="preview-header" v-if="showHeader">
      <span>预览画布</span>
      <div class="preview-actions">
        <span class="size-info">{{ baseConfig.width }} × {{ baseConfig.height }} px</span>
        <el-button v-if="showRefresh" size="small" @click="refreshPreview">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>
    <div class="canvas-wrapper" ref="canvasWrapperRef">
      <div class="canvas-container" :style="canvasContainerStyle">
        <canvas 
          ref="previewCanvas" 
          class="preview-canvas"
          @mousedown="handleCanvasMouseDown"
          @wheel="handleCanvasWheel"
        ></canvas>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import type {
  BaseConfigDTO,
  WatermarkItemDTO,
  TextWatermarkConfig,
  ImageWatermarkConfig
} from '@/types'

interface Props {
  baseConfig: BaseConfigDTO
  watermarks: WatermarkItemDTO[]
  selectedWatermarkId?: string | null
  showHeader?: boolean
  showRefresh?: boolean
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedWatermarkId: null,
  showHeader: true,
  showRefresh: true,
  editable: true
})

const emit = defineEmits<{
  (e: 'update:selectedWatermarkId', id: string | null): void
  (e: 'update:watermark', watermark: WatermarkItemDTO): void
}>()

const previewCanvas = ref<HTMLCanvasElement | null>(null)
const canvasWrapperRef = ref<HTMLElement | null>(null)
const wrapperSize = ref({ width: 800, height: 600 })
const loadedImages = ref<Map<string, HTMLImageElement>>(new Map())
const loadedFonts = ref<Set<string>>(new Set())
const failedFonts = ref<Set<string>>(new Set())
const isRendering = ref(false)
const pendingRender = ref(false)

interface WatermarkBounds {
  centerX: number
  centerY: number
  rotation: number
  halfWidth: number
  halfHeight: number
  offsetX: number
  offsetY: number
}
const watermarkBounds = ref<Map<string, WatermarkBounds>>(new Map())

const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragWatermarkStartX = ref(0)
const dragWatermarkStartY = ref(0)

const canvasScale = computed(() => {
  const padding = 32
  const availableWidth = wrapperSize.value.width - padding * 2
  const availableHeight = wrapperSize.value.height - padding * 2
  return Math.min(availableWidth / props.baseConfig.width, availableHeight / props.baseConfig.height)
})

const canvasContainerStyle = computed(() => {
  return {
    width: props.baseConfig.width * canvasScale.value + 'px',
    height: props.baseConfig.height * canvasScale.value + 'px'
  }
})

function refreshPreview() {
  renderPreview()
}

function findWatermarkAtPosition(canvasX: number, canvasY: number): WatermarkItemDTO | null {
  for (let i = 0; i < props.watermarks.length; i++) {
    const watermark = props.watermarks[i]
    
    if (watermark.type === 'image' && !watermark.imageConfig?.imageUrl) {
      continue
    }
    
    const bounds = watermarkBounds.value.get(watermark.id)
    if (!bounds) continue
    
    const { centerX, centerY, rotation, halfWidth, halfHeight, offsetX, offsetY } = bounds
    
    const dx = canvasX - centerX
    const dy = canvasY - centerY
    
    const cos = Math.cos(-rotation)
    const sin = Math.sin(-rotation)
    const localX = dx * cos - dy * sin - offsetX
    const localY = dx * sin + dy * cos - offsetY
    
    const padding = 15
    if (Math.abs(localX) <= halfWidth + padding && Math.abs(localY) <= halfHeight + padding) {
      return watermark
    }
  }
  return null
}

function handleCanvasMouseDown(e: MouseEvent) {
  if (!previewCanvas.value || !props.editable) return
  
  const rect = previewCanvas.value.getBoundingClientRect()
  const canvasX = (e.clientX - rect.left) / canvasScale.value
  const canvasY = (e.clientY - rect.top) / canvasScale.value
  
  const hitWatermark = findWatermarkAtPosition(canvasX, canvasY)
  
  if (hitWatermark) {
    emit('update:selectedWatermarkId', hitWatermark.id)
    
    const canDrag = hitWatermark.type === 'text' || 
      (hitWatermark.type === 'image' && hitWatermark.imageConfig?.fitMode === 'none')
    
    if (canDrag) {
      isDragging.value = true
      dragStartX.value = canvasX
      dragStartY.value = canvasY
      dragWatermarkStartX.value = hitWatermark.x
      dragWatermarkStartY.value = hitWatermark.y
      previewCanvas.value.style.cursor = 'grabbing'
      
      document.addEventListener('mousemove', handleDocumentMouseMove)
      document.addEventListener('mouseup', handleDocumentMouseUp)
    }
  } else {
    emit('update:selectedWatermarkId', null)
  }
  
  renderPreview()
}

function handleDocumentMouseMove(e: MouseEvent) {
  if (!previewCanvas.value || !isDragging.value) return
  
  const selectedWatermark = props.watermarks.find(w => w.id === props.selectedWatermarkId)
  if (!selectedWatermark) return
  
  const rect = previewCanvas.value.getBoundingClientRect()
  const canvasX = (e.clientX - rect.left) / canvasScale.value
  const canvasY = (e.clientY - rect.top) / canvasScale.value
  
  const deltaX = canvasX - dragStartX.value
  const deltaY = canvasY - dragStartY.value
  
  const updatedWatermark = {
    ...selectedWatermark,
    x: dragWatermarkStartX.value + deltaX,
    y: dragWatermarkStartY.value + deltaY
  }
  
  emit('update:watermark', updatedWatermark)
}

function handleDocumentMouseUp() {
  if (previewCanvas.value) {
    previewCanvas.value.style.cursor = 'default'
  }
  isDragging.value = false
  
  document.removeEventListener('mousemove', handleDocumentMouseMove)
  document.removeEventListener('mouseup', handleDocumentMouseUp)
}

function handleCanvasWheel(e: WheelEvent) {
  if (!props.editable) return
  
  const selectedWatermark = props.watermarks.find(w => w.id === props.selectedWatermarkId)
  if (!selectedWatermark) return
  
  if (selectedWatermark.type === 'image' && selectedWatermark.imageConfig) {
    if (selectedWatermark.imageConfig.fitMode !== 'none') {
      return
    }
  }
  
  e.preventDefault()
  
  const delta = e.deltaY > 0 ? -1 : 1
  
  if (selectedWatermark.type === 'text' && selectedWatermark.textConfig) {
    const newFontSize = Math.max(1, Math.min(10000, selectedWatermark.textConfig.fontSize + delta))
    const updatedWatermark = {
      ...selectedWatermark,
      textConfig: {
        ...selectedWatermark.textConfig,
        fontSize: newFontSize
      }
    }
    emit('update:watermark', updatedWatermark)
  } else if (selectedWatermark.type === 'image' && selectedWatermark.imageConfig) {
    const newScale = Math.max(1, Math.min(1000, selectedWatermark.imageConfig.scale + delta))
    const updatedWatermark = {
      ...selectedWatermark,
      imageConfig: {
        ...selectedWatermark.imageConfig,
        scale: newScale
      }
    }
    emit('update:watermark', updatedWatermark)
  }
  
  renderPreview()
}

function preloadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    if (loadedImages.value.has(url)) {
      resolve(loadedImages.value.get(url)!)
      return
    }
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      loadedImages.value.set(url, img)
      resolve(img)
    }
    img.onerror = reject
    img.src = url
  })
}

async function renderPreview() {
  if (!previewCanvas.value) return
  
  if (isRendering.value) {
    pendingRender.value = true
    return
  }
  
  isRendering.value = true
  pendingRender.value = false
  
  try {
    const canvas = previewCanvas.value
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    const scale = canvasScale.value
    
    canvas.width = props.baseConfig.width * scale
    canvas.height = props.baseConfig.height * scale
    
    ctx.fillStyle = props.baseConfig.backgroundColor || '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    
    watermarkBounds.value.clear()
    
    for (let i = props.watermarks.length - 1; i >= 0; i--) {
      const watermark = props.watermarks[i]
      const bounds = await drawWatermark(ctx, watermark, scale)
      if (bounds) {
        watermarkBounds.value.set(watermark.id, bounds)
      }
    }
    
    if (props.selectedWatermarkId) {
      const selected = props.watermarks.find(w => w.id === props.selectedWatermarkId)
      if (selected) {
        drawSelectionBorder(ctx, selected, scale)
      }
    }
  } finally {
    isRendering.value = false
    if (pendingRender.value) {
      pendingRender.value = false
      await renderPreview()
    }
  }
}

async function drawWatermark(ctx: CanvasRenderingContext2D, watermark: WatermarkItemDTO, scale: number): Promise<WatermarkBounds | null> {
  let centerX = watermark.x
  let centerY = watermark.y
  
  if (watermark.type === 'image' && watermark.imageConfig && watermark.imageConfig.fitMode !== 'none') {
    centerX = props.baseConfig.width / 2
    centerY = props.baseConfig.height / 2
  }
  
  let rotation = 0
  if (watermark.type === 'text' && watermark.textConfig) {
    rotation = (watermark.textConfig.rotation || 0) * Math.PI / 180
  } else {
    rotation = (watermark.rotation || 0) * Math.PI / 180
  }
  
  ctx.save()
  
  const x = centerX * scale
  const y = centerY * scale
  ctx.translate(x, y)
  ctx.rotate(rotation)
  
  let bounds: WatermarkBounds | null = null
  
  if (watermark.type === 'text' && watermark.textConfig) {
    ctx.globalAlpha = watermark.textConfig.opacity ?? 1
    bounds = await drawTextWatermark(ctx, watermark.textConfig, scale, centerX, centerY, rotation)
  } else if (watermark.type === 'image' && watermark.imageConfig?.imageUrl) {
    ctx.globalAlpha = watermark.imageConfig.opacity ?? 1
    bounds = await drawImageWatermark(ctx, watermark.imageConfig, scale, centerX, centerY, rotation, watermark)
  }
  
  ctx.restore()
  return bounds
}

async function loadCustomFont(fontFamily: string, fontUrl: string): Promise<boolean> {
  if (loadedFonts.value.has(fontFamily)) return true
  if (failedFonts.value.has(fontFamily)) return false
  
  try {
    const style = document.createElement('style')
    style.id = `font-style-${fontFamily}`
    style.textContent = `
      @font-face {
        font-family: '${fontFamily}';
        src: url('${fontUrl}') format('truetype');
      }
    `
    document.head.appendChild(style)
    
    await document.fonts.load(`12px "${fontFamily}"`)
    loadedFonts.value.add(fontFamily)
    return true
  } catch (error) {
    console.error(`加载字体失败: ${fontFamily}`, error)
    if (!failedFonts.value.has(fontFamily)) {
      ElMessage.error(`字体加载失败，将使用默认字体`)
    }
    failedFonts.value.add(fontFamily)
    return false
  }
}

async function drawTextWatermark(
  ctx: CanvasRenderingContext2D, 
  config: TextWatermarkConfig, 
  scale: number, 
  centerX: number, 
  centerY: number,
  rotation: number
): Promise<WatermarkBounds> {
  ctx.save()
  
  let fontFamily = config.fontFamily
  
  if (config.fontUrl) {
    await loadCustomFont(config.fontFamily, config.fontUrl)
  }
  
  const fontSize = config.fontSize * scale
  const fontWeight = config.fontWeight || 400
  ctx.font = `${fontWeight} ${fontSize}px "${fontFamily}"`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  
  const italicAngle = config.italicAngle ?? 0
  if (italicAngle !== 0) {
    const skewRad = italicAngle * Math.PI / 180
    ctx.transform(1, 0, Math.tan(skewRad), 1, 0, 0)
  }
  
  const letterSpacing = (config.letterSpacing || 0) * scale
  const content = config.content
  const chars = Array.from(content)
  
  const charMetrics = chars.map(char => ctx.measureText(char))
  const charWidths = charMetrics.map(m => m.width)
  const charTotalWidth = charWidths.reduce((sum, w) => sum + w, 0)
  const totalSpacing = Math.max(0, chars.length - 1) * letterSpacing
  const totalWidth = charTotalWidth + totalSpacing
  
  const ascent = charMetrics[0]?.actualBoundingBoxAscent || fontSize / 2
  const descent = charMetrics[0]?.actualBoundingBoxDescent || fontSize / 2
  
  let startX = 0
  if (config.align === 'left') {
    startX = totalWidth / 2
  } else if (config.align === 'right') {
    startX = -totalWidth / 2
  }
  
  if (config.gradientEnabled && config.gradientColors && config.gradientColors.length >= 2) {
    const angle = (config.gradientAngle ?? 0) * Math.PI / 180
    const cos = Math.cos(angle)
    const sin = Math.sin(angle)
    const halfW = totalWidth / 2
    const gradient = ctx.createLinearGradient(
      -halfW * cos, -halfW * sin,
      halfW * cos, halfW * sin
    )
    gradient.addColorStop(0, config.gradientColors[0])
    gradient.addColorStop(1, config.gradientColors[1])
    ctx.fillStyle = gradient
  } else {
    ctx.fillStyle = config.color
  }
  
  if (config.shadowEnabled && config.shadowColor) {
    ctx.shadowColor = config.shadowColor
    ctx.shadowBlur = (config.shadowBlur || 0) * scale
    ctx.shadowOffsetX = (config.shadowOffsetX || 0) * scale
    ctx.shadowOffsetY = (config.shadowOffsetY || 0) * scale
  }
  
  let currentX = -totalWidth / 2 + startX
  for (let i = 0; i < chars.length; i++) {
    const char = chars[i]
    const charWidth = charWidths[i]
    
    ctx.fillText(char, currentX + charWidth / 2, 0)
    
    currentX += charWidth + letterSpacing
  }
  
  if (config.strokeEnabled && config.strokeColor && config.strokeWidth) {
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0
    ctx.strokeStyle = config.strokeColor
    ctx.lineWidth = config.strokeWidth * scale
    
    currentX = -totalWidth / 2 + startX
    for (let i = 0; i < chars.length; i++) {
      const char = chars[i]
      const charWidth = charWidths[i]
      
      ctx.strokeText(char, currentX + charWidth / 2, 0)
      
      currentX += charWidth + letterSpacing
    }
  }
  
  let offsetX = 0
  if (config.align === 'left') {
    offsetX = totalWidth / 2
  } else if (config.align === 'right') {
    offsetX = -totalWidth / 2
  }
  
  const halfWidth = totalWidth / 2 / scale
  const halfHeight = (ascent + descent) / 2 / scale
  const boundsOffsetX = offsetX / scale
  const boundsOffsetY = (descent - ascent) / 2 / scale
  
  ctx.restore()
  
  return {
    centerX,
    centerY,
    rotation,
    halfWidth,
    halfHeight,
    offsetX: boundsOffsetX,
    offsetY: boundsOffsetY
  }
}

async function drawImageWatermark(
  ctx: CanvasRenderingContext2D, 
  config: ImageWatermarkConfig, 
  scale: number,
  centerX: number,
  centerY: number,
  rotation: number,
  watermark: WatermarkItemDTO
): Promise<WatermarkBounds | null> {
  try {
    if (!config.imageUrl) return null
    
    const img = await preloadImage(config.imageUrl)
    
    const imgOriginalWidth = config.originalWidth || img.width
    const imgOriginalHeight = config.originalHeight || img.height
    
    const canvasWidth = props.baseConfig.width
    const canvasHeight = props.baseConfig.height
    
    let drawWidth: number
    let drawHeight: number
    let actualCenterX = centerX
    let actualCenterY = centerY
    
    if (config.fitMode === 'none') {
      const scalePercent = config.scale / 100
      drawWidth = imgOriginalWidth * scalePercent * scale
      drawHeight = imgOriginalHeight * scalePercent * scale
      
      if (config.anchor !== 'none') {
        const halfW = drawWidth / 2
        const halfH = drawHeight / 2
        
        switch (config.anchor) {
          case 'topLeft':
            actualCenterX = halfW / scale
            actualCenterY = halfH / scale
            break
          case 'topRight':
            actualCenterX = canvasWidth - halfW / scale
            actualCenterY = halfH / scale
            break
          case 'bottomLeft':
            actualCenterX = halfW / scale
            actualCenterY = canvasHeight - halfH / scale
            break
          case 'bottomRight':
            actualCenterX = canvasWidth - halfW / scale
            actualCenterY = canvasHeight - halfH / scale
            break
          case 'center':
            actualCenterX = canvasWidth / 2
            actualCenterY = canvasHeight / 2
            break
        }
        
        ctx.restore()
        ctx.save()
        ctx.globalAlpha = config.opacity ?? 1
        ctx.translate(actualCenterX * scale, actualCenterY * scale)
        ctx.rotate(rotation)
      }
    } else if (config.fitMode === 'scaleToFill') {
      drawWidth = canvasWidth * scale
      drawHeight = canvasHeight * scale
    } else if (config.fitMode === 'aspectFit') {
      const imgRatio = imgOriginalWidth / imgOriginalHeight
      const canvasRatio = canvasWidth / canvasHeight
      
      if (imgRatio > canvasRatio) {
        drawWidth = canvasWidth * scale
        drawHeight = drawWidth / imgRatio
      } else {
        drawHeight = canvasHeight * scale
        drawWidth = drawHeight * imgRatio
      }
    } else if (config.fitMode === 'aspectFill') {
      const imgRatio = imgOriginalWidth / imgOriginalHeight
      const canvasRatio = canvasWidth / canvasHeight
      
      if (imgRatio > canvasRatio) {
        drawHeight = canvasHeight * scale
        drawWidth = drawHeight * imgRatio
      } else {
        drawWidth = canvasWidth * scale
        drawHeight = drawWidth / imgRatio
      }
    } else {
      drawWidth = imgOriginalWidth * scale
      drawHeight = imgOriginalHeight * scale
    }
    
    const halfWidth = drawWidth / 2
    const halfHeight = drawHeight / 2
    
    ctx.drawImage(img, -halfWidth, -halfHeight, drawWidth, drawHeight)
    
    return {
      centerX: actualCenterX,
      centerY: actualCenterY,
      rotation,
      halfWidth: halfWidth / scale,
      halfHeight: halfHeight / scale,
      offsetX: 0,
      offsetY: 0
    }
  } catch (error) {
    console.error('加载图片失败:', error)
    return null
  }
}

function drawSelectionBorder(ctx: CanvasRenderingContext2D, watermark: WatermarkItemDTO, scale: number) {
  ctx.save()
  
  const canvasWidth = props.baseConfig.width
  const canvasHeight = props.baseConfig.height
  
  let x = watermark.x * scale
  let y = watermark.y * scale
  
  if (watermark.type === 'image' && watermark.imageConfig) {
    const config = watermark.imageConfig
    
    if (config.fitMode !== 'none') {
      x = canvasWidth * scale / 2
      y = canvasHeight * scale / 2
    } else if (config.anchor !== 'none') {
      const scalePercent = config.scale / 100
      const imgOriginalWidth = config.originalWidth || 100
      const imgOriginalHeight = config.originalHeight || 100
      const drawWidth = imgOriginalWidth * scalePercent * scale
      const drawHeight = imgOriginalHeight * scalePercent * scale
      const halfW = drawWidth / 2
      const halfH = drawHeight / 2
      
      switch (config.anchor) {
        case 'topLeft':
          x = halfW
          y = halfH
          break
        case 'topRight':
          x = canvasWidth * scale - halfW
          y = halfH
          break
        case 'bottomLeft':
          x = halfW
          y = canvasHeight * scale - halfH
          break
        case 'bottomRight':
          x = canvasWidth * scale - halfW
          y = canvasHeight * scale - halfH
          break
        case 'center':
          x = canvasWidth * scale / 2
          y = canvasHeight * scale / 2
          break
      }
    }
  }
  
  ctx.translate(x, y)
  
  let rotation = 0
  if (watermark.type === 'text' && watermark.textConfig) {
    rotation = (watermark.textConfig.rotation || 0) * Math.PI / 180
  } else {
    rotation = (watermark.rotation || 0) * Math.PI / 180
  }
  ctx.rotate(rotation)
  
  ctx.strokeStyle = '#409eff'
  ctx.lineWidth = 2
  ctx.setLineDash([5, 5])
  
  if (watermark.type === 'text' && watermark.textConfig) {
    const config = watermark.textConfig
    const fontSize = config.fontSize * scale
    const fontWeight = config.fontWeight || 400
    ctx.font = `${fontWeight} ${fontSize}px "${config.fontFamily}"`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    
    const letterSpacing = (config.letterSpacing || 0) * scale
    const chars = Array.from(config.content)
    const charWidths = chars.map(char => ctx.measureText(char).width)
    const charTotalWidth = charWidths.reduce((sum, w) => sum + w, 0)
    const totalSpacing = Math.max(0, chars.length - 1) * letterSpacing
    const totalWidth = charTotalWidth + totalSpacing
    
    const ascent = ctx.measureText(config.content).actualBoundingBoxAscent || fontSize / 2
    const descent = ctx.measureText(config.content).actualBoundingBoxDescent || fontSize / 2
    
    let offsetX = 0
    if (config.align === 'left') {
      offsetX = totalWidth / 2
    } else if (config.align === 'right') {
      offsetX = -totalWidth / 2
    }
    
    ctx.strokeRect(offsetX - totalWidth / 2 - 5, -ascent - 5, totalWidth + 10, ascent + descent + 10)
  } else if (watermark.type === 'image' && watermark.imageConfig && watermark.imageConfig.imageUrl) {
    const config = watermark.imageConfig
    const canvasWidth = props.baseConfig.width
    const canvasHeight = props.baseConfig.height
    
    let drawWidth: number
    let drawHeight: number
    
    const imgOriginalWidth = config.originalWidth || 100
    const imgOriginalHeight = config.originalHeight || 100
    
    if (config.fitMode === 'none') {
      const scalePercent = config.scale / 100
      drawWidth = imgOriginalWidth * scalePercent * scale
      drawHeight = imgOriginalHeight * scalePercent * scale
    } else if (config.fitMode === 'scaleToFill') {
      drawWidth = canvasWidth * scale
      drawHeight = canvasHeight * scale
    } else if (config.fitMode === 'aspectFit') {
      const imgRatio = imgOriginalWidth / imgOriginalHeight
      const canvasRatio = canvasWidth / canvasHeight
      
      if (imgRatio > canvasRatio) {
        drawWidth = canvasWidth * scale
        drawHeight = drawWidth / imgRatio
      } else {
        drawHeight = canvasHeight * scale
        drawWidth = drawHeight * imgRatio
      }
    } else if (config.fitMode === 'aspectFill') {
      const imgRatio = imgOriginalWidth / imgOriginalHeight
      const canvasRatio = canvasWidth / canvasHeight
      
      if (imgRatio > canvasRatio) {
        drawHeight = canvasHeight * scale
        drawWidth = drawHeight * imgRatio
      } else {
        drawWidth = canvasWidth * scale
        drawHeight = drawWidth / imgRatio
      }
    } else {
      drawWidth = imgOriginalWidth * scale
      drawHeight = imgOriginalHeight * scale
    }
    
    const halfWidth = drawWidth / 2
    const halfHeight = drawHeight / 2
    ctx.strokeRect(-halfWidth - 5, -halfHeight - 5, drawWidth + 10, drawHeight + 10)
  }
  
  ctx.restore()
}

function getCanvasElement() {
  return previewCanvas.value
}

defineExpose({
  refreshPreview,
  getCanvasElement
})

onMounted(async () => {
  await nextTick()
  renderPreview()
  
  if (canvasWrapperRef.value) {
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        wrapperSize.value = {
          width: entry.contentRect.width,
          height: entry.contentRect.height
        }
      }
      nextTick(() => renderPreview())
    })
    resizeObserver.observe(canvasWrapperRef.value)
  }
})

watch([() => props.baseConfig, () => props.watermarks, () => props.selectedWatermarkId], () => {
  renderPreview()
}, { deep: true })
</script>

<style scoped lang="scss">
.preview-container {
  flex: 1;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 16px;
    border-bottom: 1px solid #ebeef5;
    font-weight: 500;
    font-size: 14px;

    .preview-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .size-info {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .canvas-wrapper {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 16px;
    background-color: #f5f7fa;
    overflow: auto;
  }

  .canvas-container {
    position: relative;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .preview-canvas {
    display: block;
  }
}
</style>
