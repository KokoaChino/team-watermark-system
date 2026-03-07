<template>
  <div class="task-image-config-panel">
    <el-empty
      v-if="!item"
      description="请选择左侧图片后再编辑参数"
      :image-size="110"
      class="empty-state"
    />

    <template v-else>
      <div class="panel-header">
        <div>
          <h3>参数配置</h3>
          <p>当前正在配置：{{ item.sourceFileName }}</p>
        </div>
        <el-tag type="primary" effect="light">图片 ID：{{ item.imageId }}</el-tag>
      </div>

      <div class="config-scroll">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span>源图片信息</span>
          </template>
          <div class="source-meta">
            <img :src="item.previewUrl" class="source-preview" />
            <div class="meta-list">
              <div class="meta-row">
                <span class="meta-label">文件名</span>
                <span class="meta-value">{{ item.sourceFileName }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">图片 ID</span>
                <span class="meta-value">{{ item.imageId }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">文件大小</span>
                <span class="meta-value">{{ formatFileSize(item.size) }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="section-card" shadow="never">
          <template #header>
            <span>动态水印参数</span>
          </template>

          <div class="watermark-list">
            <div
              v-for="(input, index) in item.watermarkInputs"
              :key="input.watermarkId"
              class="watermark-item"
            >
              <div class="watermark-title">
                <div class="title-left">
                  <span class="watermark-index">{{ index + 1 }}</span>
                  <span class="watermark-name">{{ input.watermarkName }}</span>
                </div>
                <el-tag size="small" :type="input.type === 'text' ? 'primary' : 'success'">
                  {{ input.type === 'text' ? '文字水印' : '图片水印' }}
                </el-tag>
              </div>

              <el-input
                v-if="input.type === 'text'"
                :model-value="input.value"
                type="textarea"
                :rows="3"
                resize="vertical"
                placeholder="请输入该水印的文字内容"
                @update:model-value="updateWatermarkValue(input.watermarkId, $event)"
              />

              <div v-else class="image-watermark-editor">
                <el-input
                  :model-value="input.value"
                  placeholder="请输入图片地址，或点击右侧按钮上传图片"
                  @update:model-value="updateWatermarkValue(input.watermarkId, $event)"
                >
                  <template #append>
                    <el-button @click="emit('pick-watermark-image', input.watermarkId)">上传图片</el-button>
                  </template>
                </el-input>

                <div v-if="input.value" class="image-preview-box">
                  <div class="preview-wrapper">
                    <img
                      v-if="!imageLoadErrors[input.watermarkId]"
                      :src="input.imagePreviewUrl || input.value"
                      class="watermark-preview"
                      @error="markImageError(input.watermarkId)"
                      @load="resetImageError(input.watermarkId)"
                    />
                    <div v-else class="preview-fallback">
                      当前值无法直接预览，请确认图片地址是否可访问。
                    </div>
                  </div>
                  <div class="preview-actions">
                    <span class="preview-value">{{ input.value }}</span>
                    <el-button link type="danger" @click="emit('clear-watermark-image', input.watermarkId)">
                      清除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="section-card" shadow="never">
          <template #header>
            <span>输出规则</span>
          </template>

          <el-form label-position="top" class="output-form">
            <el-form-item label="新的路径">
              <el-input
                :model-value="item.targetDirectory"
                placeholder="使用 / 分隔目录，例如：商品图/详情页"
                @update:model-value="updateField('targetDirectory', $event)"
                @blur="normalizeDirectory"
              />
              <div v-if="targetDirectoryError" class="field-error">{{ targetDirectoryError }}</div>
            </el-form-item>

            <el-form-item label="新的文件名">
              <el-input
                :model-value="item.outputFileName"
                placeholder="不填则沿用原文件名"
                @update:model-value="updateField('outputFileName', $event)"
              />
              <div v-if="outputNameError" class="field-error">{{ outputNameError }}</div>
            </el-form-item>

            <el-form-item label="新的拓展名">
              <el-input
                :model-value="item.outputExtension"
                placeholder="例如：jpg、png"
                @update:model-value="updateField('outputExtension', $event)"
                @blur="normalizeExtension"
              />
              <div v-if="outputExtensionError" class="field-error">{{ outputExtensionError }}</div>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import type { BatchTaskImageDraft } from '@/types'
import {
  normalizeDirectoryInput,
  sanitizeExtension,
  validateOutputExtension,
  validateOutputName,
  validateTargetDirectory
} from '@/utils/batchTask'

const props = defineProps<{
  item: BatchTaskImageDraft | null
}>()

const emit = defineEmits<{
  'update:item': [value: BatchTaskImageDraft]
  'pick-watermark-image': [watermarkId: string]
  'clear-watermark-image': [watermarkId: string]
}>()

const imageLoadErrors = reactive<Record<string, boolean>>({})

const targetDirectoryError = computed(() => {
  if (!props.item) {
    return ''
  }
  return validateTargetDirectory(props.item.targetDirectory)
})

const outputNameError = computed(() => {
  if (!props.item) {
    return ''
  }
  return validateOutputName(props.item.outputFileName)
})

const outputExtensionError = computed(() => {
  if (!props.item) {
    return ''
  }
  return validateOutputExtension(props.item.outputExtension)
})

function updateField(field: 'targetDirectory' | 'outputFileName' | 'outputExtension', value: string) {
  if (!props.item) {
    return
  }

  emit('update:item', {
    ...props.item,
    [field]: value
  })
}

function updateWatermarkValue(watermarkId: string, value: string) {
  if (!props.item) {
    return
  }

  emit('update:item', {
    ...props.item,
    watermarkInputs: props.item.watermarkInputs.map((input) => {
      if (input.watermarkId !== watermarkId) {
        return { ...input }
      }

      return {
        ...input,
        value,
        imagePreviewUrl: input.type === 'image' ? value : input.imagePreviewUrl
      }
    })
  })
}

function normalizeDirectory() {
  if (!props.item) {
    return
  }

  updateField('targetDirectory', normalizeDirectoryInput(props.item.targetDirectory))
}

function normalizeExtension() {
  if (!props.item) {
    return
  }

  updateField('outputExtension', sanitizeExtension(props.item.outputExtension))
}

function markImageError(watermarkId: string) {
  imageLoadErrors[watermarkId] = true
}

function resetImageError(watermarkId: string) {
  imageLoadErrors[watermarkId] = false
}

function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }

  return `${(size / 1024 / 1024).toFixed(2)} MB`
}
</script>

<style scoped lang="scss">
.task-image-config-panel {
  height: 100%;

  .empty-state {
    min-height: 520px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed #d7deea;
    border-radius: 16px;
    background: linear-gradient(180deg, #fbfcfe 0%, #f5f7fa 100%);
  }

  .panel-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 20px;
      color: var(--color-text-primary);
    }

    p {
      margin: 8px 0 0;
      color: var(--color-text-secondary);
      font-size: 13px;
    }
  }

  .config-scroll {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .section-card {
    border-radius: 14px;

    :deep(.el-card__header) {
      padding: 14px 18px;
      font-weight: 600;
      background: #fbfcfe;
    }
  }

  .source-meta {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  .source-preview {
    width: 96px;
    height: 96px;
    border-radius: 12px;
    object-fit: cover;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
    background: #eef3f8;
  }

  .meta-list {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .meta-row {
    display: flex;
    gap: 12px;
    font-size: 13px;
  }

  .meta-label {
    width: 64px;
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }

  .meta-value {
    color: var(--color-text-primary);
    word-break: break-all;
  }

  .watermark-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .watermark-item {
    padding: 16px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid #edf1f6;
  }

  .watermark-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }

  .title-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .watermark-index {
    width: 24px;
    height: 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 999px;
    background: #dbeafe;
    color: #2563eb;
    font-size: 12px;
    font-weight: 700;
  }

  .watermark-name {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .image-watermark-editor {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .image-preview-box {
    padding: 12px;
    border-radius: 12px;
    border: 1px solid #e6ebf2;
    background: #fff;
  }

  .preview-wrapper {
    width: 100%;
    min-height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    overflow: hidden;
    background: linear-gradient(180deg, #f9fbfd 0%, #eef3f8 100%);
  }

  .watermark-preview {
    max-width: 100%;
    max-height: 220px;
    object-fit: contain;
  }

  .preview-fallback {
    padding: 24px;
    color: var(--color-text-secondary);
    font-size: 13px;
    text-align: center;
    line-height: 1.6;
  }

  .preview-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 10px;
  }

  .preview-value {
    flex: 1;
    color: var(--color-text-secondary);
    font-size: 12px;
    word-break: break-all;
  }

  .output-form {
    .field-error {
      margin-top: 6px;
      color: #f56c6c;
      font-size: 12px;
      line-height: 1.4;
    }
  }
}

@media (max-width: 960px) {
  .task-image-config-panel {
    .panel-header,
    .source-meta,
    .preview-actions,
    .watermark-title {
      flex-direction: column;
      align-items: flex-start;
    }

    .source-preview {
      width: 100%;
      height: 180px;
    }
  }
}
</style>
