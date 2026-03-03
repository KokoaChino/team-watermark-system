<template>
  <div class="template-draft">
    <div class="draft-header">
      <div class="header-left">
        <el-input
          v-model="draftName"
          placeholder="请输入模板名称"
          style="width: 300px"
        />
        <el-tag v-if="draft?.sourceTemplateId" type="info" size="small">
          编辑自模板 #{{ draft.sourceTemplateId }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button @click="handleClearDraft">清空草稿</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmitDraft">提交模板</el-button>
      </div>
    </div>

    <div class="draft-content">
      <div class="left-panel" :style="{ width: leftPanelWidth + 'px' }">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>底图配置</span>
            </div>
          </template>
          <el-form :model="baseConfig" label-width="50px" size="small">
            <el-row :gutter="12">
              <el-col :span="6">
                <el-form-item label="宽度" label-width="40px">
                  <el-input-number v-model="baseConfig.width" :min="100" :max="5000" :step="10" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="高度" label-width="40px">
                  <el-input-number v-model="baseConfig.height" :min="100" :max="5000" :step="10" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="背景色">
                  <el-color-picker v-model="baseConfig.backgroundColor" />
                  <el-input v-model="baseConfig.backgroundColor" style="width: 80px; margin-left: 8px" size="small" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <el-card class="watermark-card">
          <template #header>
            <div class="card-header">
              <span>水印列表 ({{ watermarks.length }})</span>
              <div>
                <el-dropdown @command="handleAddWatermark">
                  <el-button type="primary" size="small">
                    <el-icon><Plus /></el-icon> 添加
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="text">文字水印</el-dropdown-item>
                      <el-dropdown-item command="image">图片水印</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </template>
          
          <div class="watermark-list" v-if="watermarks.length > 0">
            <draggable
              v-model="watermarks"
              item-key="id"
              ghost-class="dragging-item"
              animation="200"
            >
              <template #item="{ element, index }">
                <div
                  class="watermark-item"
                  :class="{ active: selectedWatermarkId === element.id }"
                  @click="selectWatermark(element.id)"
                >
                  <div class="item-info">
                    <el-icon class="drag-handle"><Rank /></el-icon>
                    <span class="item-index">{{ index + 1 }}</span>
                    <span class="item-name">{{ element.name }}</span>
                    <el-tag size="small" :type="element.type === 'text' ? 'primary' : 'success'">
                      {{ element.type === 'text' ? '文字' : '图片' }}
                    </el-tag>
                  </div>
                  <div class="item-actions">
                    <el-button type="danger" size="small" link @click.stop="removeWatermark(index)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>
            </draggable>
          </div>
          <el-empty v-else description="暂无水印，点击上方按钮添加" :image-size="60" />
        </el-card>

        <el-card class="detail-card" v-if="selectedWatermark">
          <template #header>
            <div class="card-header">
              <el-input 
                v-model="selectedWatermark.name" 
                size="small" 
                class="name-input"
              />
              <span class="header-label">参数设置</span>
            </div>
          </template>
          
          <el-form :model="selectedWatermark" label-width="70px" size="small">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="X">
                  <el-input-number 
                    v-model="selectedWatermark.x" 
                    :min="-10000" 
                    :max="10000" 
                    :precision="1"
                    :step="1"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Y">
                  <el-input-number 
                    v-model="selectedWatermark.y"
                    :min="-10000" 
                    :max="10000" 
                    :precision="1"
                    :step="1"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>


          <template v-if="selectedWatermark.type === 'text'">
            <TextWatermarkEditor
              :config="selectedWatermark.textConfig!"
              @update="handleTextConfigUpdate"
            />
          </template>
          <template v-else>
            <ImageWatermarkEditor
              :config="selectedWatermark.imageConfig!"
              :canvas-width="baseConfig.width"
              :canvas-height="baseConfig.height"
              @update="handleImageConfigUpdate"
            />
          </template>
        </el-card>
      </div>

      <div 
        class="resize-handle" 
        @mousedown="handleResizeStart"
      ></div>

      <div class="right-panel">
        <div class="preview-container">
          <div class="preview-header">
            <span>预览画布</span>
            <div class="preview-actions">
              <span class="size-info">{{ baseConfig.width }} × {{ baseConfig.height }} px</span>
              <el-button size="small" @click="refreshPreview">
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
      </div>
    </div>

    <el-dialog v-model="showConflictDialog" title="提交冲突" width="400px">
      <p>{{ conflictMessage }}</p>
      <p>是否基于当前草稿新建一个模板？</p>
      <template #footer>
        <el-button @click="showConflictDialog = false">取消</el-button>
        <el-button type="primary" @click="handleForceCreateNew">新建模板</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Refresh, Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import {
  getCurrentDraft,
  createNewDraft,
  saveDraft,
  submitDraft
} from '@/api/template'
import { uploadFile } from '@/api/file'
import type {
  DraftVO,
  WatermarkItemDTO,
  WatermarkConfigDTO,
  BaseConfigDTO,
  TextWatermarkConfig,
  ImageWatermarkConfig
} from '@/types'
import TextWatermarkEditor from '@/components/watermark/TextWatermarkEditor.vue'
import ImageWatermarkEditor from '@/components/watermark/ImageWatermarkEditor.vue'

const draft = ref<DraftVO | null>(null)
const draftName = ref('')
const baseConfig = ref<BaseConfigDTO>({
  width: 800,
  height: 600,
  backgroundColor: '#ffffff'
})
const watermarks = ref<WatermarkItemDTO[]>([])
const selectedWatermarkId = ref<string | null>(null)
const saving = ref(false)
const submitting = ref(false)
const showConflictDialog = ref(false)
const conflictMessage = ref('')
const previewCanvas = ref<HTMLCanvasElement | null>(null)
const loadedImages = ref<Map<string, HTMLImageElement>>(new Map())
const loadedFonts = ref<Set<string>>(new Set())
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
const canvasWrapperRef = ref<HTMLElement | null>(null)
const wrapperSize = ref({ width: 800, height: 600 })

const LEFT_PANEL_WIDTH_KEY = 'template-draft-left-panel-width'
const leftPanelWidth = ref(parseInt(localStorage.getItem(LEFT_PANEL_WIDTH_KEY) || '400', 10))
const isResizing = ref(false)
const resizeStartX = ref(0)
const resizeStartWidth = ref(0)

const canvasScale = computed(() => {
  const padding = 32
  const availableWidth = wrapperSize.value.width - padding * 2
  const availableHeight = wrapperSize.value.height - padding * 2
  return Math.min(availableWidth / baseConfig.value.width, availableHeight / baseConfig.value.height)
})

const selectedWatermark = computed(() => {
  return watermarks.value.find(w => w.id === selectedWatermarkId.value) || null
})

const canvasContainerStyle = computed(() => {
  return {
    width: baseConfig.value.width * canvasScale.value + 'px',
    height: baseConfig.value.height * canvasScale.value + 'px'
  }
})

function generateId() {
  return 'wm_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

function getDefaultTextConfig(): TextWatermarkConfig {
  return {
    content: '请输入文本',
    fontSize: 32,
    fontFamily: 'Microsoft YaHei',
    color: '#333333',
    align: 'left',
    fontWeight: 400,
    italicAngle: 0,
    rotation: 0,
    opacity: 1,
    letterSpacing: 0,
    strokeEnabled: false,
    shadowEnabled: false,
    gradientEnabled: false
  }
}

function getDefaultImageConfig(): ImageWatermarkConfig {
  return {
    imageUrl: '',
    scale: 100,
    opacity: 1,
    fitMode: 'aspectFit',
    anchor: 'none',
    originalWidth: undefined,
    originalHeight: undefined
  }
}

async function loadDraft() {
  try {
    const res = await getCurrentDraft()
    if (res.code === 200 && res.data) {
      draft.value = res.data
      draftName.value = res.data.name || ''
      baseConfig.value = res.data.config?.baseConfig || baseConfig.value
      
      // 直接使用后端返回的水印列表，无需转换，但补充默认值
      const backendWatermarks = res.data.config?.watermarks || []
      watermarks.value = backendWatermarks.map((wm: any) => ({
        ...wm,
        name: wm.name || (wm.type === 'text' ? '文字水印' : '图片水印'),
        imageConfig: wm.imageConfig ? {
          ...wm.imageConfig,
          opacity: wm.imageConfig.opacity ?? 1  // 补充默认透明度
        } : undefined
      }))
      
      if (res.data.hasConflict) {
        conflictMessage.value = res.data.conflictMessage || '模板已被其他人修改'
        showConflictDialog.value = true
      }
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
  }
}

async function handleClearDraft() {
  try {
    await ElMessageBox.confirm('确定要清空草稿吗？所有编辑内容将丢失。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await createNewDraft(true)
    ElMessage.success('草稿已清空')
    await loadDraft()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空草稿失败:', error)
    }
  }
}

function handleAddWatermark(type: 'text' | 'image') {
  const newWatermark: WatermarkItemDTO = {
    id: generateId(),
    type,
    name: type === 'text' ? '文字水印' : '图片水印',
    x: Math.floor(baseConfig.value.width / 2),
    y: Math.floor(baseConfig.value.height / 2),
    rotation: 0,
    opacity: 1,
    textConfig: type === 'text' ? getDefaultTextConfig() : undefined,
    imageConfig: type === 'image' ? getDefaultImageConfig() : undefined
  }
  watermarks.value.push(newWatermark)
  selectedWatermarkId.value = newWatermark.id
}

function selectWatermark(id: string) {
  selectedWatermarkId.value = id
  renderPreview()
}

function removeWatermark(index: number) {
  const removed = watermarks.value.splice(index, 1)[0]
  if (selectedWatermarkId.value === removed.id) {
    selectedWatermarkId.value = watermarks.value.length > 0 ? watermarks.value[0].id : null
  }
}

function handleTextConfigUpdate(config: TextWatermarkConfig) {
  if (selectedWatermark.value && selectedWatermark.value.textConfig) {
    selectedWatermark.value.textConfig = config
  }
}

function handleImageConfigUpdate(config: ImageWatermarkConfig) {
  if (selectedWatermark.value && selectedWatermark.value.imageConfig) {
    selectedWatermark.value.imageConfig = config
    if (config.imageUrl) {
      preloadImage(config.imageUrl)
    }
  }
}

async function handleSaveDraft() {
  if (!draftName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  
  saving.value = true
  try {
    // 先上传本地图片文件
    for (const wm of watermarks.value) {
      if (wm.type === 'image' && wm.imageConfig?.localFile) {
        try {
          const res = await uploadFile(wm.imageConfig.localFile, 'watermark/')
          if (res.code === 200) {
            wm.imageConfig.imageUrl = res.data
            wm.imageConfig.imageKey = res.data
            delete wm.imageConfig.localFile
          }
        } catch (error) {
          console.error('上传图片失败:', error)
          ElMessage.error(`图片上传失败: ${wm.name}`)
          saving.value = false
          return
        }
      }
    }
    
    // 直接发送数据，无需转换
    const config: WatermarkConfigDTO = {
      baseConfig: baseConfig.value,
      watermarks: watermarks.value
    }
    const res = await saveDraft({
      sourceTemplateId: draft.value?.sourceTemplateId,
      sourceVersion: draft.value?.sourceVersion,
      name: draftName.value,
      config
    })
    
    if (res.code === 200) {
      draft.value = res.data
      ElMessage.success('草稿已保存')
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
  } finally {
    saving.value = false
  }
}

async function renderHighQualityPreview(): Promise<HTMLCanvasElement | null> {
  const tempCanvas = document.createElement('canvas')
  tempCanvas.width = baseConfig.value.width
  tempCanvas.height = baseConfig.value.height
  const tempCtx = tempCanvas.getContext('2d')
  if (!tempCtx) return null
  
  const scale = 1
  
  tempCtx.fillStyle = baseConfig.value.backgroundColor || '#ffffff'
  tempCtx.fillRect(0, 0, tempCanvas.width, tempCanvas.height)
  
  for (let i = watermarks.value.length - 1; i >= 0; i--) {
    const watermark = watermarks.value[i]
    await drawWatermark(tempCtx, watermark, scale)
  }
  
  return tempCanvas
}

function canvasToHighQualityFile(): Promise<File | null> {
  return new Promise(async (resolve) => {
    const tempCanvas = await renderHighQualityPreview()
    if (!tempCanvas) {
      resolve(null)
      return
    }
    
    tempCanvas.toBlob((blob) => {
      if (blob) {
        const file = new File([blob], `preview_${Date.now()}.png`, { type: 'image/png' })
        resolve(file)
      } else {
        resolve(null)
      }
    }, 'image/png', 1.0)
  })
}

async function uploadPreviewImage(): Promise<string | null> {
  try {
    const file = await canvasToHighQualityFile()
    if (!file) return null
    
    const res = await uploadFile(file, 'preview/')
    if (res.code === 200) {
      return res.data
    }
    return null
  } catch (error) {
    console.error('上传预览图失败:', error)
    return null
  }
}

async function handleSubmitDraft() {
  if (!draftName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  
  submitting.value = true
  try {
    const previewImageKey = await uploadPreviewImage()
    
    const config: WatermarkConfigDTO = {
      baseConfig: baseConfig.value,
      watermarks: watermarks.value,
      previewImageKey: previewImageKey || undefined
    }
    
    await saveDraft({
      sourceTemplateId: draft.value?.sourceTemplateId,
      sourceVersion: draft.value?.sourceVersion,
      name: draftName.value,
      config
    })
    
    const res = await submitDraft({ forceCreateNew: false })
    if (res.code === 200) {
      ElMessage.success('模板提交成功')
      await createNewDraft()
      await loadDraft()
    }
  } catch (error: any) {
    if (error.response?.data?.message?.includes('冲突')) {
      conflictMessage.value = error.response.data.message
      showConflictDialog.value = true
    }
    console.error('提交模板失败:', error)
  } finally {
    submitting.value = false
  }
}

async function handleForceCreateNew() {
  showConflictDialog.value = false
  submitting.value = true
  try {
    const res = await submitDraft({ forceCreateNew: true })
    if (res.code === 200) {
      ElMessage.success('已新建模板')
      await createNewDraft()
      await loadDraft()
    }
  } catch (error) {
    console.error('新建模板失败:', error)
  } finally {
    submitting.value = false
  }
}

function refreshPreview() {
  renderPreview()
}

function findWatermarkAtPosition(canvasX: number, canvasY: number): WatermarkItemDTO | null {
  // 从前往后遍历，优先选中列表中靠前（显示在上层）的水印
  for (let i = 0; i < watermarks.value.length; i++) {
    const watermark = watermarks.value[i]
    
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
  if (!previewCanvas.value) return
  
  const rect = previewCanvas.value.getBoundingClientRect()
  const canvasX = (e.clientX - rect.left) / canvasScale.value
  const canvasY = (e.clientY - rect.top) / canvasScale.value
  
  const hitWatermark = findWatermarkAtPosition(canvasX, canvasY)
  
  if (hitWatermark) {
    selectedWatermarkId.value = hitWatermark.id
    
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
    selectedWatermarkId.value = null
  }
  
  renderPreview()
}

function handleDocumentMouseMove(e: MouseEvent) {
  if (!previewCanvas.value || !isDragging.value || !selectedWatermark.value) return
  
  const rect = previewCanvas.value.getBoundingClientRect()
  const canvasX = (e.clientX - rect.left) / canvasScale.value
  const canvasY = (e.clientY - rect.top) / canvasScale.value
  
  const deltaX = canvasX - dragStartX.value
  const deltaY = canvasY - dragStartY.value
  
  selectedWatermark.value.x = dragWatermarkStartX.value + deltaX
  selectedWatermark.value.y = dragWatermarkStartY.value + deltaY
  
  renderPreview()
}

function handleDocumentMouseUp() {
  if (previewCanvas.value) {
    previewCanvas.value.style.cursor = 'default'
  }
  isDragging.value = false
  
  document.removeEventListener('mousemove', handleDocumentMouseMove)
  document.removeEventListener('mouseup', handleDocumentMouseUp)
}

function handleResizeStart(e: MouseEvent) {
  isResizing.value = true
  resizeStartX.value = e.clientX
  resizeStartWidth.value = leftPanelWidth.value
  
  document.addEventListener('mousemove', handleResizeMove)
  document.addEventListener('mouseup', handleResizeEnd)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function handleResizeMove(e: MouseEvent) {
  if (!isResizing.value) return
  
  const deltaX = e.clientX - resizeStartX.value
  const newWidth = Math.max(300, Math.min(800, resizeStartWidth.value + deltaX))
  leftPanelWidth.value = newWidth
}

function handleResizeEnd() {
  isResizing.value = false
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  
  localStorage.setItem(LEFT_PANEL_WIDTH_KEY, leftPanelWidth.value.toString())
}

function handleCanvasWheel(e: WheelEvent) {
  if (!selectedWatermark.value) return
  
  if (selectedWatermark.value.type === 'image' && selectedWatermark.value.imageConfig) {
    if (selectedWatermark.value.imageConfig.fitMode !== 'none') {
      return
    }
  }
  
  e.preventDefault()
  
  const delta = e.deltaY > 0 ? -1 : 1
  
  if (selectedWatermark.value.type === 'text' && selectedWatermark.value.textConfig) {
    const newFontSize = Math.max(1, Math.min(10000, selectedWatermark.value.textConfig.fontSize + delta))
    selectedWatermark.value.textConfig.fontSize = newFontSize
  } else if (selectedWatermark.value.type === 'image' && selectedWatermark.value.imageConfig) {
    const newScale = Math.max(1, Math.min(1000, selectedWatermark.value.imageConfig.scale + delta))
    selectedWatermark.value.imageConfig.scale = newScale
  }
  
  renderPreview()
}

function handleCanvasMouseMove(e: MouseEvent) {
  if (!previewCanvas.value) return
  
  const rect = previewCanvas.value.getBoundingClientRect()
  const canvasX = (e.clientX - rect.left) / canvasScale.value
  const canvasY = (e.clientY - rect.top) / canvasScale.value
  
  if (isDragging.value && selectedWatermark.value) {
    const deltaX = canvasX - dragStartX.value
    const deltaY = canvasY - dragStartY.value
    
    selectedWatermark.value.x = dragWatermarkStartX.value + deltaX
    selectedWatermark.value.y = dragWatermarkStartY.value + deltaY
    
    renderPreview()
  } else {
    const hitWatermark = findWatermarkAtPosition(canvasX, canvasY)
    if (hitWatermark) {
      const canDrag = hitWatermark.type === 'text' || 
        (hitWatermark.type === 'image' && hitWatermark.imageConfig?.fitMode === 'none')
      previewCanvas.value.style.cursor = canDrag ? 'grab' : 'pointer'
    } else {
      previewCanvas.value.style.cursor = 'default'
    }
  }
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
  
  // 如果正在渲染，标记待渲染并返回
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
    
    canvas.width = baseConfig.value.width * scale
    canvas.height = baseConfig.value.height * scale
    
    ctx.fillStyle = baseConfig.value.backgroundColor || '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    
    watermarkBounds.value.clear()
    
    // 反向遍历：列表下方的水印先渲染，上方的后渲染（显示在上层）
    for (let i = watermarks.value.length - 1; i >= 0; i--) {
      const watermark = watermarks.value[i]
      const bounds = await drawWatermark(ctx, watermark, scale)
      if (bounds) {
        watermarkBounds.value.set(watermark.id, bounds)
      }
    }
    
    if (selectedWatermarkId.value) {
      const selected = watermarks.value.find(w => w.id === selectedWatermarkId.value)
      if (selected) {
        drawSelectionBorder(ctx, selected, scale)
      }
    }
  } finally {
    isRendering.value = false
    // 如果有待处理的渲染请求，再次调用
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
    centerX = baseConfig.value.width / 2
    centerY = baseConfig.value.height / 2
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
    // 图片水印透明度：0-100 转换为 0-1
    ctx.globalAlpha = watermark.imageConfig.opacity ?? 1
    bounds = await drawImageWatermark(ctx, watermark.imageConfig, scale, centerX, centerY, rotation, watermark)
  }
  
  ctx.restore()
  return bounds
}

const failedFonts = ref<Set<string>>(new Set())

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
  // 保存当前 Canvas 状态，确保后续修改不会影响其他绘制操作
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
  
  // 计算每个字符的宽度和总宽度
  const charMetrics = chars.map(char => ctx.measureText(char))
  const charWidths = charMetrics.map(m => m.width)
  const charTotalWidth = charWidths.reduce((sum, w) => sum + w, 0)
  const totalSpacing = Math.max(0, chars.length - 1) * letterSpacing
  const totalWidth = charTotalWidth + totalSpacing
  
  const ascent = charMetrics[0]?.actualBoundingBoxAscent || fontSize / 2
  const descent = charMetrics[0]?.actualBoundingBoxDescent || fontSize / 2
  
  // 根据对齐方式计算起始偏移
  let startX = 0
  if (config.align === 'left') {
    startX = totalWidth / 2
  } else if (config.align === 'right') {
    startX = -totalWidth / 2
  }
  // center 时 startX = 0
  
  // 设置填充样式
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
  
  // 设置阴影
  if (config.shadowEnabled && config.shadowColor) {
    ctx.shadowColor = config.shadowColor
    ctx.shadowBlur = (config.shadowBlur || 0) * scale
    ctx.shadowOffsetX = (config.shadowOffsetX || 0) * scale
    ctx.shadowOffsetY = (config.shadowOffsetY || 0) * scale
  }
  
  // 逐字符绘制
  let currentX = -totalWidth / 2 + startX
  for (let i = 0; i < chars.length; i++) {
    const char = chars[i]
    const charWidth = charWidths[i]
    
    ctx.fillText(char, currentX + charWidth / 2, 0)
    
    currentX += charWidth + letterSpacing
  }
  
  // 描边（同样逐字符绘制）
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
  
  // 计算边界框
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
  
  // 恢复 Canvas 状态
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
    
    const canvasWidth = baseConfig.value.width
    const canvasHeight = baseConfig.value.height
    
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
        // 重新应用透明度（save/restore 会重置 globalAlpha）
        ctx.globalAlpha = config.opacity ?? 1
        ctx.translate(actualCenterX * scale, actualCenterY * scale)
        ctx.rotate(rotation)
        
        if (watermark.x !== actualCenterX || watermark.y !== actualCenterY) {
          watermark.x = actualCenterX
          watermark.y = actualCenterY
        }
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
  
  const canvasWidth = baseConfig.value.width
  const canvasHeight = baseConfig.value.height
  
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
    
    // 计算带字符间距的总宽度
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
    const canvasWidth = baseConfig.value.width
    const canvasHeight = baseConfig.value.height
    
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

onMounted(async () => {
  await loadDraft()
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

watch([baseConfig, watermarks], () => {
  renderPreview()
}, { deep: true })
</script>

<style scoped lang="scss">
.template-draft {
  height: calc(100vh - 108px);
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.draft-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .header-right {
    display: flex;
    gap: 8px;
  }
}

.draft-content {
  flex: 1;
  display: flex;
  gap: 4px;
  overflow: hidden;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  flex-shrink: 0;
  min-height: 0;
  
  .config-card,
  .watermark-card {
    flex-shrink: 0;
  }
  
  .detail-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.resize-handle {
  width: 2px;
  cursor: col-resize;
  background-color: transparent;
  transition: background-color 0.2s;
  position: relative;
  
  &:hover,
  &:active {
    background-color: #409eff;
  }
  
  &::before {
    content: '';
    position: absolute;
    left: -4px;
    right: -4px;
    top: 0;
    bottom: 0;
  }
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.config-card,
.watermark-card {
  :deep(.el-card__header) {
    padding: 10px 16px;
    border-bottom: 1px solid #ebeef5;
  }

  :deep(.el-card__body) {
    padding: 12px 16px;
  }
}

.detail-card {
  :deep(.el-card__header) {
    padding: 10px 16px;
    border-bottom: 1px solid #ebeef5;
    flex-shrink: 0;
  }

  :deep(.el-card__body) {
    padding: 12px 16px;
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    scrollbar-width: none;
    -ms-overflow-style: none;
    
    &::-webkit-scrollbar {
      display: none;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  font-size: 14px;

  .name-input {
    font-size: 15px;
    height: 30px;
    width: 240px;
  }
  
  .header-label {
    color: #909399;
    font-size: 13px;
  }
}

.watermark-list {
  max-height: 180px;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  
  &::-webkit-scrollbar {
    display: none;
  }
}

.watermark-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;
  user-select: none;

  &:hover {
    background-color: #f5f7fa;
  }

  &.active {
    background-color: #ecf5ff;
    border-color: #409eff;
  }

  &:active {
    cursor: grabbing;
  }

  .item-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .drag-handle {
      color: #909399;
      font-size: 14px;
    }

    .item-index {
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #e4e7ed;
      border-radius: 4px;
      font-size: 12px;
      color: #606266;
    }

    .item-name {
      font-size: 13px;
      max-width: 100px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.dragging-item {
  opacity: 0.5;
  background-color: #409eff !important;
  border-color: #409eff !important;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);

  .item-index,
  .item-name,
  .drag-handle {
    color: #fff !important;
  }

  .item-index {
    background-color: rgba(255, 255, 255, 0.3);
  }
}

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

.el-divider {
  margin: 16px 0 12px;
}
</style>
