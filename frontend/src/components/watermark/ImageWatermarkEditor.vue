<template>
  <div class="image-watermark-editor">
    <el-form :model="localConfig" label-width="70px" size="small">
      <el-form-item label="图片">
        <div class="upload-area">
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            accept="image/*"
            class="uploader"
          >
            <div v-if="localConfig.imageUrl" class="preview-box">
              <el-image :src="localConfig.imageUrl" fit="contain" />
            </div>
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <span>上传图片</span>
            </div>
          </el-upload>
          <div v-if="localConfig.imageUrl" class="image-info">
            <span>{{ imageSizeText }}</span>
          </div>
        </div>
      </el-form-item>
      
      <el-form-item label="缩放">
        <el-slider 
          v-model="localConfig.scale" 
          :min="0.1" 
          :max="3" 
          :step="0.1" 
          :format-tooltip="(val: number) => `${Math.round(val * 100)}%`"
          @change="handleUpdate" 
        />
      </el-form-item>
      
      <el-form-item label="适应模式">
        <el-radio-group v-model="localConfig.fitMode" @change="handleUpdate">
          <el-radio-button label="contain">原比例</el-radio-button>
          <el-radio-button label="cover">覆盖</el-radio-button>
          <el-radio-button label="stretch">拉伸</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <el-alert 
      v-if="!localConfig.imageUrl" 
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
import { Plus } from '@element-plus/icons-vue'
import type { ImageWatermarkConfig } from '@/types'
import { uploadFile } from '@/api/file'

const props = defineProps<{
  config: ImageWatermarkConfig
}>()

const emit = defineEmits<{
  update: [config: ImageWatermarkConfig]
}>()

const localConfig = ref<ImageWatermarkConfig>({ ...props.config })
const imageSize = ref({ width: 0, height: 0 })

const imageSizeText = computed(() => {
  if (imageSize.value.width && imageSize.value.height) {
    const scaledWidth = Math.round(imageSize.value.width * localConfig.value.scale)
    const scaledHeight = Math.round(imageSize.value.height * localConfig.value.scale)
    return `原始: ${imageSize.value.width}×${imageSize.value.height} | 当前: ${scaledWidth}×${scaledHeight}`
  }
  return ''
})

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
  if (newConfig.imageUrl) {
    loadImageSize(newConfig.imageUrl)
  }
}, { deep: true, immediate: true })

function loadImageSize(url: string) {
  const img = new Image()
  img.onload = () => {
    imageSize.value = { width: img.width, height: img.height }
  }
  img.src = url
}

async function handleBeforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  
  try {
    const res = await uploadFile(file, 'watermark/')
    if (res.code === 200) {
      localConfig.value.imageUrl = res.data
      localConfig.value.imageKey = res.data
      loadImageSize(res.data)
      handleUpdate()
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    console.error('上传图片失败:', error)
  }
  
  return false
}

function handleUpdate() {
  emit('update', { ...localConfig.value })
}
</script>

<style scoped lang="scss">
.image-watermark-editor {
  .upload-area {
    width: 100%;
    
    .uploader {
      width: 100%;
      
      :deep(.el-upload) {
        width: 100%;
        border: 1px dashed #dcdfe6;
        border-radius: 6px;
        cursor: pointer;
        transition: border-color 0.2s;
        
        &:hover {
          border-color: #409eff;
        }
      }
    }
    
    .preview-box {
      width: 100%;
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 8px;
      
      :deep(.el-image) {
        max-width: 100%;
        max-height: 100%;
      }
    }
    
    .upload-placeholder {
      width: 100%;
      height: 100px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #909399;
      
      .el-icon {
        font-size: 28px;
        margin-bottom: 8px;
      }
      
      span {
        font-size: 13px;
      }
    }
    
    .image-info {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
      text-align: center;
    }
  }
  
  :deep(.el-alert) {
    margin-top: 12px;
    padding: 8px 12px;
  }
}
</style>
