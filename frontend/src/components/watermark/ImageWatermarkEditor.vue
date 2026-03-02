<template>
  <div class="image-watermark-editor">
    <el-form :model="localConfig" label-width="70px" size="small">
      <el-form-item label="图片">
        <div class="upload-row">
          <div class="preview-container" @click="triggerUpload">
            <div v-if="previewUrl" class="preview-box">
              <img :src="previewUrl" class="preview-image" />
              <div class="clear-btn" @click.stop="handleClearImage">
                <el-icon><Close /></el-icon>
              </div>
            </div>
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <span>点击上传图片</span>
            </div>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleFileSelect"
          />
          <div v-if="localConfig.originalWidth && localConfig.originalHeight" class="size-info">
            <div class="size-row">
              <span class="size-label">原始尺寸</span>
              <span class="size-value">{{ localConfig.originalWidth }} × {{ localConfig.originalHeight }}</span>
            </div>
            <div class="size-row">
              <span class="size-label">当前尺寸</span>
              <span class="size-value">{{ currentWidth }} × {{ currentHeight }}</span>
            </div>
          </div>
        </div>
      </el-form-item>
      
      <el-form-item label="适应模式">
        <el-radio-group v-model="localConfig.fitMode" @change="handleFitModeChange">
          <el-radio-button label="none">无</el-radio-button>
          <el-radio-button label="scaleToFill">填充</el-radio-button>
          <el-radio-button label="aspectFit">适应</el-radio-button>
          <el-radio-button label="aspectFill">覆盖</el-radio-button>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item v-if="localConfig.fitMode === 'none'" label="参数">
        <div class="params-row">
          <div class="param-item">
            <span class="param-label">缩放</span>
            <el-input-number 
              v-model="scaleValue" 
              :min="1" 
              :max="1000" 
              :step="1"
              size="small"
              style="width: 100px"
              @change="handleScaleChange" 
            />
            <span class="param-unit">%</span>
          </div>
          <div class="param-item">
            <span class="param-label">锚点</span>
            <el-select v-model="localConfig.anchor" size="small" style="width: 90px" @change="handleUpdate">
              <el-option label="无" value="none" />
              <el-option label="左上" value="topLeft" />
              <el-option label="右上" value="topRight" />
              <el-option label="左下" value="bottomLeft" />
              <el-option label="右下" value="bottomRight" />
              <el-option label="中心" value="center" />
            </el-select>
          </div>
        </div>
      </el-form-item>
    </el-form>
    
    <el-alert 
      v-if="!previewUrl" 
      type="info" 
      :closable="false"
      show-icon
    >
      <template #title>
        请上传图片水印文件
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import type { ImageWatermarkConfig } from '@/types'

const props = defineProps<{
  config: ImageWatermarkConfig
  canvasWidth: number
  canvasHeight: number
}>()

const emit = defineEmits<{
  update: [config: ImageWatermarkConfig]
}>()

const localConfig = ref<ImageWatermarkConfig>({ ...props.config })
const fileInputRef = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string>('')

const scaleValue = computed({
  get: () => Math.round(localConfig.value.scale),
  set: (val: number) => {
    localConfig.value.scale = val
  }
})

const currentWidth = computed(() => {
  const imgW = localConfig.value.originalWidth
  const imgH = localConfig.value.originalHeight
  if (!imgW || !imgH) return 0
  
  const canvasW = props.canvasWidth
  const canvasH = props.canvasHeight
  const fitMode = localConfig.value.fitMode
  
  if (fitMode === 'none') {
    return Math.round(imgW * localConfig.value.scale / 100)
  }
  
  if (fitMode === 'scaleToFill') {
    return canvasW
  }
  
  const imgRatio = imgW / imgH
  const canvasRatio = canvasW / canvasH
  
  if (fitMode === 'aspectFit') {
    if (imgRatio > canvasRatio) {
      return canvasW
    } else {
      return Math.round(canvasH * imgRatio)
    }
  }
  
  if (fitMode === 'aspectFill') {
    if (imgRatio > canvasRatio) {
      return Math.round(canvasH * imgRatio)
    } else {
      return canvasW
    }
  }
  
  return Math.round(imgW * localConfig.value.scale / 100)
})

const currentHeight = computed(() => {
  const imgW = localConfig.value.originalWidth
  const imgH = localConfig.value.originalHeight
  if (!imgW || !imgH) return 0
  
  const canvasW = props.canvasWidth
  const canvasH = props.canvasHeight
  const fitMode = localConfig.value.fitMode
  
  if (fitMode === 'none') {
    return Math.round(imgH * localConfig.value.scale / 100)
  }
  
  if (fitMode === 'scaleToFill') {
    return canvasH
  }
  
  const imgRatio = imgW / imgH
  const canvasRatio = canvasW / canvasH
  
  if (fitMode === 'aspectFit') {
    if (imgRatio > canvasRatio) {
      return Math.round(canvasW / imgRatio)
    } else {
      return canvasH
    }
  }
  
  if (fitMode === 'aspectFill') {
    if (imgRatio > canvasRatio) {
      return canvasH
    } else {
      return Math.round(canvasW / imgRatio)
    }
  }
  
  return Math.round(imgH * localConfig.value.scale / 100)
})

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
  if (newConfig.imageUrl) {
    previewUrl.value = newConfig.imageUrl
  } else if (newConfig.localFile) {
    previewUrl.value = URL.createObjectURL(newConfig.localFile)
  } else {
    previewUrl.value = ''
  }
}, { deep: true, immediate: true })

function triggerUpload() {
  fileInputRef.value?.click()
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }
  
  const url = URL.createObjectURL(file)
  previewUrl.value = url
  
  const img = new Image()
  img.onload = () => {
    localConfig.value = {
      ...localConfig.value,
      imageUrl: url,
      localFile: file,
      originalWidth: img.width,
      originalHeight: img.height
    }
    handleUpdate()
    ElMessage.success('图片已选择，保存草稿后上传')
  }
  img.src = url
  
  input.value = ''
}

function handleClearImage() {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  localConfig.value = {
    ...localConfig.value,
    imageUrl: '',
    imageKey: undefined,
    localFile: undefined,
    originalWidth: undefined,
    originalHeight: undefined
  }
  handleUpdate()
}

function handleScaleChange(val: number | undefined) {
  if (val !== undefined) {
    localConfig.value.scale = val
    handleUpdate()
  }
}

function handleFitModeChange() {
  handleUpdate()
}

function handleUpdate() {
  emit('update', { ...localConfig.value })
}
</script>

<style scoped lang="scss">
.image-watermark-editor {
  .upload-row {
    display: flex;
    gap: 12px;
    align-items: flex-start;
  }
  
  .preview-container {
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    border: 1px dashed #dcdfe6;
    border-radius: 6px;
    cursor: pointer;
    transition: border-color 0.2s;
    position: relative;
    overflow: hidden;
    background-color: #f5f7fa;
    
    &:hover {
      border-color: #409eff;
    }
  }
  
  .preview-box {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    
    .preview-image {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
    
    .clear-btn {
      position: absolute;
      top: 2px;
      right: 2px;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background-color: rgba(0, 0, 0, 0.5);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background-color 0.2s;
      
      &:hover {
        background-color: rgba(0, 0, 0, 0.7);
      }
      
      .el-icon {
        font-size: 12px;
      }
    }
  }
  
  .upload-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #909399;
    
    .el-icon {
      font-size: 24px;
      margin-bottom: 4px;
    }
    
    span {
      font-size: 12px;
    }
  }
  
  .size-info {
    flex: 1;
    min-width: 0;
    
    .size-row {
      display: flex;
      align-items: center;
      margin-bottom: 4px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .size-label {
        width: 60px;
        font-size: 12px;
        color: #909399;
        flex-shrink: 0;
      }
      
      .size-value {
        font-size: 12px;
        color: #606266;
      }
    }
  }
  
  :deep(.el-radio-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    
    .el-radio-button {
      margin: 0;
      
      .el-radio-button__inner {
        padding: 5px 10px;
        font-size: 12px;
      }
    }
  }
  
  .params-row {
    display: flex;
    gap: 16px;
    align-items: center;
    
    .param-item {
      display: flex;
      align-items: center;
      gap: 6px;
      
      .param-label {
        font-size: 12px;
        color: #606266;
        white-space: nowrap;
      }
      
      .param-unit {
        font-size: 12px;
        color: #909399;
      }
    }
  }
  
  :deep(.el-alert) {
    margin-top: 12px;
    padding: 8px 12px;
  }
}
</style>
