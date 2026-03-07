<template>
  <div class="task-create">
    <el-card class="task-card">
      <template #header>
        <div class="card-header">
          <div class="header-intro">
            <h2>创建批量任务</h2>
          </div>
          <div class="header-steps">
            <el-steps :active="currentStep" align-center finish-status="success" class="task-steps">
              <el-step title="选择模板" />
              <el-step title="水印配置" />
            </el-steps>
          </div>
        </div>
      </template>

      <section v-show="currentStep === 0" class="step-panel">
        <TemplateBrowser
          v-model="selectedTemplateId"
          title="步骤一：选择模板"
          empty-description="暂无可用模板，请先创建水印模板"
          @select="handleTemplateSelect"
        >
          <template #header-actions>
            <div v-if="selectedTemplate" class="selected-summary">
              <span class="summary-label">当前已选</span>
              <el-tag type="primary" effect="light">{{ selectedTemplate.name }}</el-tag>
            </div>
          </template>
        </TemplateBrowser>
      </section>

      <section v-show="currentStep === 1" class="step-panel">
        <el-card class="step-two-card" shadow="never">
          <template #header>
            <div class="step-card-header">
              <div class="step-card-title">
                <span>步骤二：水印配置</span>
                <el-tag v-if="selectedTemplate" type="primary" effect="light">
                  模板：{{ selectedTemplate.name }}
                </el-tag>
              </div>
              <div class="step-card-actions">
                <el-button v-if="taskImages.length" type="primary" plain @click="openExcelImportDialog">
                  <el-icon><Document /></el-icon>
                  导入配置
                </el-button>
                <el-button type="primary" @click="triggerSourceImageSelect">
                  <el-icon><UploadFilled /></el-icon>
                  上传图片
                </el-button>
              </div>
            </div>
          </template>

          <div ref="workbenchRef" class="workbench" :style="workbenchStyle">
            <div class="fixed-pane panel-shell">
              <div class="pane-header">
                <span>待处理图片</span>
                <span class="pane-meta">已上传 {{ taskImages.length }} 张图片</span>
              </div>
              <div ref="leftPaneBodyRef" class="pane-body" @scroll.passive="handleLeftPaneScroll">
                <draggable
                  v-if="taskImages.length"
                  v-model="taskImages"
                  item-key="id"
                  ghost-class="sortable-ghost"
                  chosen-class="sortable-chosen"
                  drag-class="sortable-drag"
                  animation="120"
                  class="image-list"
                  filter=".delete-button"
                  :move="handleImageDragMove"
                  @start="handleImageDragStart"
                  @end="handleImageDragEnd"
                >
                  <template #item="{ element, index }">
                    <div class="image-row">
                      <div class="image-order">#{{ index + 1 }}</div>
                      <div class="image-thumb-box">
                        <img :src="element.previewUrl" class="image-thumb" />
                      </div>
                      <div class="image-text">
                        <div class="image-name" :title="element.sourceFileName">{{ element.sourceFileName }}</div>
                        <div class="image-size">{{ formatFileSize(element.size) }}</div>
                      </div>
                      <el-button link type="danger" class="delete-button" @click.stop="removeTaskImage(element.id)">
                        <el-icon><Delete /></el-icon>
                        删除
                      </el-button>
                    </div>
                  </template>
                </draggable>
                <div v-else class="pane-empty-state">
                  <el-empty description="请先上传待处理图片" :image-size="90" />
                </div>
              </div>
            </div>

            <div
              v-if="!isCompactLayout"
              class="pane-resizer"
              :class="{ active: isResizingPane }"
              @mousedown.prevent="handlePaneResizeStart"
            >
              <span class="resizer-line"></span>
            </div>

            <div class="config-pane panel-shell">
              <div ref="rightPaneScrollRef" class="config-scroll" @scroll.passive="handleRightPaneScroll">
                <div class="config-table" :style="{ width: `${configTableWidth}px` }">
                  <div class="config-header-row" :style="{ gridTemplateColumns: configGridTemplate }">
                    <div
                      v-for="watermark in templateWatermarks"
                      :key="watermark.id"
                      class="config-header-cell watermark-column"
                    >
                      <span class="header-name">{{ watermark.name }}</span>
                      <el-tag
                        size="small"
                        class="watermark-type-tag"
                        :type="watermark.type === 'text' ? 'primary' : 'success'"
                      >
                        {{ watermark.type === 'text' ? '文字' : '图片' }}
                      </el-tag>
                    </div>
                    <div class="config-header-cell path-column">新的路径</div>
                    <div class="config-header-cell file-name-column">新的文件名</div>
                    <div class="config-header-cell extension-column">新的拓展名</div>
                  </div>

                  <transition-group
                    v-if="displayedTaskImages.length"
                    name="config-row"
                    tag="div"
                    class="config-body"
                  >
                    <div
                      v-for="row in displayedTaskImages"
                      :key="row.id"
                      class="config-data-row"
                      :style="{ gridTemplateColumns: configGridTemplate }"
                    >
                      <div
                        v-for="input in row.watermarkInputs"
                        :key="input.watermarkId"
                        class="config-cell watermark-column"
                        :class="{ 'has-error': Boolean(getWatermarkValidationError(row.id, input)) }"
                        :data-image-id="row.id"
                        data-field="watermark"
                        :data-watermark-id="input.watermarkId"
                      >
                        <template v-if="input.type === 'text'">
                          <el-input
                            size="small"
                            :model-value="input.value"
                            placeholder="输入文字"
                            @update:model-value="updateWatermarkValue(row.id, input.watermarkId, $event)"
                          />
                        </template>
                        <template v-else>
                          <div
                            class="watermark-upload-box"
                            :class="{ 'has-value': hasWatermarkAsset(input) }"
                            tabindex="0"
                            @click="triggerWatermarkImageSelect(row.id, input.watermarkId)"
                          >
                            <img
                              v-if="getWatermarkPreviewSrc(row.id, input)"
                              :src="getWatermarkPreviewSrc(row.id, input)"
                              class="watermark-thumb"
                              @error="handleWatermarkPreviewError(row.id, input.watermarkId)"
                            />
                            <div v-else class="upload-placeholder">
                              <el-icon><UploadFilled /></el-icon>
                              <span>{{ hasWatermarkAsset(input) ? '图片不可预览' : '上传图片' }}</span>
                            </div>
                            <div v-if="hasWatermarkAsset(input)" class="watermark-upload-mask">点击更换</div>
                            <span
                              v-if="hasWatermarkAsset(input)"
                              class="watermark-remove"
                              @click.stop="clearWatermarkImage(row.id, input.watermarkId)"
                            >
                              <el-icon><Delete /></el-icon>
                            </span>
                          </div>
                        </template>
                        <div class="cell-error-placeholder">
                          {{ getWatermarkValidationError(row.id, input) }}
                        </div>
                      </div>

                      <div class="config-cell path-column" :data-image-id="row.id" data-field="targetDirectory">
                        <el-input
                          size="small"
                          :model-value="row.targetDirectory"
                          placeholder="如：商品图/详情页"
                          @update:model-value="updateOutputField(row.id, 'targetDirectory', $event)"
                        />
                        <div class="cell-error-placeholder">{{ getTargetDirectoryError(row) }}</div>
                      </div>

                      <div class="config-cell file-name-column" :data-image-id="row.id" data-field="outputFileName">
                        <el-input
                          size="small"
                          :model-value="row.outputFileName"
                          placeholder="不填则沿用原文件名"
                          @update:model-value="updateOutputField(row.id, 'outputFileName', $event)"
                        />
                        <div class="cell-error-placeholder">{{ getOutputNameError(row) }}</div>
                      </div>

                      <div class="config-cell extension-column" :data-image-id="row.id" data-field="outputExtension">
                        <el-select
                          size="small"
                          clearable
                          placeholder="选择格式"
                          :model-value="row.outputExtension || undefined"
                          @change="updateOutputExtension(row.id, $event)"
                        >
                          <el-option
                            v-for="extension in commonImageExtensions"
                            :key="extension"
                            :label="extension.toUpperCase()"
                            :value="extension"
                          />
                        </el-select>
                        <div class="cell-error-placeholder">{{ getOutputExtensionError(row) }}</div>
                      </div>
                    </div>
                  </transition-group>

                  <div v-else class="config-empty-state">
                    <el-empty description="请先上传待处理图片" :image-size="100" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </section>

      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="currentStep -= 1">上一步</el-button>
        <el-button
          v-if="currentStep === 0"
          type="primary"
          :disabled="!selectedTemplateId"
          @click="currentStep = 1"
        >
          下一步
        </el-button>
        <el-button
          v-else
          type="primary"
          :disabled="importingExcel || startingTask"
          :loading="startingTask"
          @click="startTask"
        >
          开始任务
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="excelImportVisible" title="导入 Excel 配置" width="640px" :close-on-click-modal="false">
      <el-form label-width="130px" class="excel-import-form">
        <el-form-item label="Excel 文件">
          <div class="excel-file-row">
            <el-button @click="triggerExcelFileSelect">选择文件</el-button>
            <span class="excel-file-name">{{ excelImportFile?.name || '未选择文件' }}</span>
          </div>
        </el-form-item>

        <div class="import-rule-panel">
          <div class="rule-panel-title">导入规则</div>
          <div class="rule-panel-hint">以下规则用于控制 Excel 文件的解析、匹配和覆盖方式</div>

          <el-form-item label="映射模式">
            <el-radio-group v-model="excelImportOptions.mappingMode">
              <el-radio label="id">按图片 ID 映射</el-radio>
              <el-radio label="order">按顺序映射</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="excelImportOptions.mappingMode === 'id'" label="重复 ID 处理">
            <el-radio-group v-model="excelImportOptions.duplicateHandling">
              <el-radio label="error">报错终止</el-radio>
              <el-radio label="first">保留第一个</el-radio>
              <el-radio label="last">保留最后一个</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="异常字符处理">
            <el-radio-group v-model="excelImportOptions.invalidCharHandling">
              <el-radio label="error">报错终止</el-radio>
              <el-radio label="error_folder">统一放到 ERROR 文件夹</el-radio>
              <el-radio label="underscore">用下划线替代</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="覆盖策略">
            <el-radio-group v-model="excelImportOptions.mergeMode">
              <el-radio label="overwrite">强制覆盖现有配置</el-radio>
              <el-radio label="fill-empty">仅补充空参数</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="closeExcelImportDialog">取消</el-button>
        <el-button type="primary" :loading="importingExcel" @click="importExcelConfig">开始导入</el-button>
      </template>
    </el-dialog>

    <input
      ref="sourceImageInputRef"
      type="file"
      accept="image/*"
      multiple
      hidden
      @change="handleSourceImageChange"
    />
    <input
      ref="excelInputRef"
      type="file"
      accept=".xlsx,.xls"
      hidden
      @change="handleExcelFileChange"
    />
    <input
      ref="watermarkImageInputRef"
      type="file"
      accept="image/*"
      hidden
      @change="handleWatermarkImageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document, UploadFilled } from '@element-plus/icons-vue'
import TemplateBrowser from '@/components/template/TemplateBrowser.vue'
import { parseExcel } from '@/api/excel'
import { uploadFile } from '@/api/file'
import { useBatchTaskStore } from '@/stores/batchTask'
import {
  applyExcelConfigToTaskImage,
  cloneTaskImageDraft,
  createTaskImageDraft,
  formatFileSize,
  SUPPORTED_IMAGE_EXTENSIONS,
  validateOutputExtension,
  validateOutputName,
  validateTargetDirectory
} from '@/utils/batchTask'
import type {
  BatchTaskImageDraft,
  BatchTaskWatermarkInput,
  ImageConfigVO,
  PendingBatchTaskDraft,
  WatermarkTemplateVO
} from '@/types'

interface DragMoveEvent {
  draggedContext: {
    element: BatchTaskImageDraft
    futureIndex?: number
  }
  relatedContext: {
    index?: number
  }
}

type ValidationField = 'watermark' | 'targetDirectory' | 'outputFileName' | 'outputExtension'

interface ValidationIssue {
  imageId: string
  field: ValidationField
  message: string
  sourceFileName: string
  watermarkId?: string
  watermarkName?: string
}

const LEFT_PANE_WIDTH_STORAGE_KEY = 'batch-task-create-left-pane-width'
const DEFAULT_LEFT_PANE_WIDTH = 340
const MIN_LEFT_PANE_WIDTH = 280
const MIN_RIGHT_PANE_WIDTH = 540
const RESIZER_WIDTH = 14
const WATERMARK_COLUMN_WIDTH = 160
const PATH_COLUMN_WIDTH = 180
const FILE_NAME_COLUMN_WIDTH = 156
const EXTENSION_COLUMN_WIDTH = 116
const EXCEL_IMPORT_OPTIONS_STORAGE_KEY = 'batch-task-create-excel-import-options'

const defaultExcelImportOptions = {
  mappingMode: 'id' as 'id' | 'order',
  duplicateHandling: 'error' as 'error' | 'first' | 'last',
  invalidCharHandling: 'error' as 'error' | 'error_folder' | 'underscore',
  mergeMode: 'overwrite' as 'overwrite' | 'fill-empty'
}

const router = useRouter()
const batchTaskStore = useBatchTaskStore()

const currentStep = ref(0)
const selectedTemplateId = ref<number | null>(null)
const selectedTemplate = ref<WatermarkTemplateVO | null>(null)
const taskImages = ref<BatchTaskImageDraft[]>([])
const excelImportVisible = ref(false)
const excelImportFile = ref<File | null>(null)
const importingExcel = ref(false)
const startingTask = ref(false)
const allowStepTwoLeave = ref(false)
const pendingWatermarkTarget = ref<{ imageId: string; watermarkId: string } | null>(null)
const dragPreviewIds = ref<string[] | null>(null)
const syncingPane = ref<'left' | 'right' | null>(null)
const watermarkPreviewErrors = ref<Record<string, boolean>>({})
const leftPaneWidth = ref(DEFAULT_LEFT_PANE_WIDTH)
const isResizingPane = ref(false)
const isCompactLayout = ref(false)
const paneResizeStartX = ref(0)
const paneResizeStartWidth = ref(DEFAULT_LEFT_PANE_WIDTH)
const previousBodyCursor = ref('')
const previousBodyUserSelect = ref('')

const sourceImageInputRef = ref<HTMLInputElement | null>(null)
const excelInputRef = ref<HTMLInputElement | null>(null)
const watermarkImageInputRef = ref<HTMLInputElement | null>(null)
const leftPaneBodyRef = ref<HTMLElement | null>(null)
const rightPaneScrollRef = ref<HTMLElement | null>(null)
const workbenchRef = ref<HTMLElement | null>(null)

const excelImportOptions = reactive({ ...defaultExcelImportOptions })

const commonImageExtensions = [...SUPPORTED_IMAGE_EXTENSIONS]
const templateWatermarks = computed(() => selectedTemplate.value?.config.watermarks || [])
const displayedTaskImages = computed(() => {
  if (!dragPreviewIds.value) {
    return taskImages.value
  }

  const imageMap = new Map(taskImages.value.map((item) => [item.id, item]))
  return dragPreviewIds.value
    .map((id) => imageMap.get(id))
    .filter((item): item is BatchTaskImageDraft => Boolean(item))
})
const workbenchStyle = computed(() => {
  if (isCompactLayout.value) {
    return undefined
  }

  return {
    gridTemplateColumns: `${leftPaneWidth.value}px ${RESIZER_WIDTH}px minmax(0, 1fr)`
  }
})
const configTableWidth = computed(() => {
  return templateWatermarks.value.length * WATERMARK_COLUMN_WIDTH
    + PATH_COLUMN_WIDTH
    + FILE_NAME_COLUMN_WIDTH
    + EXTENSION_COLUMN_WIDTH
})
const configGridTemplate = computed(() => {
  const watermarkColumns = templateWatermarks.value.length
    ? `repeat(${templateWatermarks.value.length}, ${WATERMARK_COLUMN_WIDTH}px) `
    : ''

  return `${watermarkColumns}${PATH_COLUMN_WIDTH}px ${FILE_NAME_COLUMN_WIDTH}px ${EXTENSION_COLUMN_WIDTH}px`
})

function handleTemplateSelect(template: WatermarkTemplateVO | null) {
  const previousTemplateId = selectedTemplate.value?.id
  const nextTemplateId = template?.id
  const templateChanged = Boolean(previousTemplateId && nextTemplateId && previousTemplateId !== nextTemplateId)

  selectedTemplate.value = template

  if (templateChanged && taskImages.value.length > 0) {
    resetTaskImages()
    batchTaskStore.clearPendingDraft()
    ElMessage.warning('模板已切换，步骤二中的图片和参数配置已清空，请重新配置。')
  }
}

function triggerSourceImageSelect() {
  sourceImageInputRef.value?.click()
}

function handleSourceImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''

  if (!files.length) {
    return
  }

  if (!selectedTemplate.value) {
    ElMessage.warning('请先在步骤一选择模板')
    return
  }

  const imageFiles = files.filter((file) => file.type.startsWith('image/'))
  const skippedCount = files.length - imageFiles.length

  if (!imageFiles.length) {
    ElMessage.warning('仅支持上传图片文件')
    return
  }

  const nextItems = imageFiles.map((file) => createTaskImageDraft(file, selectedTemplate.value!))
  setTaskImages([...taskImages.value, ...nextItems])

  if (skippedCount > 0) {
    ElMessage.warning(`已跳过 ${skippedCount} 个非图片文件`)
  }

  ElMessage.success(`已加入 ${nextItems.length} 张待处理图片`)
}

function removeTaskImage(imageId: string) {
  clearWatermarkPreviewErrorsForImage(imageId)
  setTaskImages(taskImages.value.filter((item) => item.id !== imageId))
}

function updateWatermarkValue(imageId: string, watermarkId: string, value: string) {
  updateWatermarkInput(imageId, watermarkId, (input) => {
    clearWatermarkPreviewError(imageId, watermarkId)

    if (input.type === 'image') {
      revokeWatermarkLocalPreview(input)
      input.value = value
      input.imagePreviewUrl = value
      input.localFile = undefined
      input.localFileName = ''
      return
    }

    input.value = value
  })
}

function updateOutputField(imageId: string, field: 'targetDirectory' | 'outputFileName', value: string) {
  const row = findTaskImage(imageId)
  if (!row) {
    return
  }

  row[field] = value
}

function updateOutputExtension(imageId: string, value: string | undefined) {
  const row = findTaskImage(imageId)
  if (!row) {
    return
  }

  row.outputExtension = value || ''
}


function openExcelImportDialog() {
  if (!taskImages.value.length) {
    ElMessage.warning('请先上传待处理图片，再导入 Excel 配置')
    return
  }

  excelImportVisible.value = true
}

function closeExcelImportDialog() {
  excelImportVisible.value = false
  excelImportFile.value = null
}

function triggerExcelFileSelect() {
  excelInputRef.value?.click()
}

function handleExcelFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  excelImportFile.value = input.files?.[0] || null
  input.value = ''
}

async function importExcelConfig() {
  if (!excelImportFile.value) {
    ElMessage.warning('请选择要导入的 Excel 文件')
    return
  }

  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择模板')
    return
  }

  importingExcel.value = true

  try {
    const response = await parseExcel({
      excelFile: excelImportFile.value,
      mappingMode: excelImportOptions.mappingMode,
      duplicateHandling: excelImportOptions.mappingMode === 'id' ? excelImportOptions.duplicateHandling : undefined,
      invalidCharHandling: excelImportOptions.invalidCharHandling
    })

    if (response.code !== 200 || !response.data) {
      return
    }

    const applyResult = applyExcelConfigs(response.data.configs)
    closeExcelImportDialog()

    ElMessage.success(
      `Excel 解析完成，后端识别 ${response.data.validRowCount} 行，已匹配 ${applyResult.matchedCount} 张图片。`
    )

    if (applyResult.unmatchedImageIds.length) {
      ElMessage.warning(
        `以下图片 ID 未匹配到上传图片：${applyResult.unmatchedImageIds.slice(0, 5).join('、')}${applyResult.unmatchedImageIds.length > 5 ? ' 等' : ''}`
      )
    }

    if (applyResult.overflowCount > 0) {
      ElMessage.warning(`有 ${applyResult.overflowCount} 行 Excel 配置超出当前图片数量，已忽略。`)
    }
  } catch (error) {
    console.error('导入 Excel 配置失败:', error)
    if (!isHandledRequestError(error)) {
      ElMessage.error(error instanceof Error ? error.message : '导入 Excel 配置失败，请检查文件内容或配置选项。')
    }
  } finally {
    importingExcel.value = false
  }
}

function applyExcelConfigs(configs: ImageConfigVO[]) {
  const template = selectedTemplate.value
  if (!template) {
    return { matchedCount: 0, unmatchedImageIds: [], overflowCount: 0 }
  }

  const nextItems = taskImages.value.map(cloneTaskImageDraft)
  let matchedCount = 0
  let overflowCount = 0
  const unmatchedImageIds: string[] = []

  if (excelImportOptions.mappingMode === 'order') {
    configs.forEach((config, index) => {
      const currentItem = nextItems[index]
      if (!currentItem) {
        overflowCount += 1
        return
      }

      nextItems[index] = applyExcelConfigToTaskImage({
        image: currentItem,
        excelConfig: config,
        mergeMode: excelImportOptions.mergeMode
      })
      matchedCount += 1
    })
  } else {
    const imageIdMap = new Map<string, number>()
    const duplicateImageIds = new Set<string>()

    nextItems.forEach((item, index) => {
      if (imageIdMap.has(item.imageId)) {
        duplicateImageIds.add(item.imageId)
        return
      }
      imageIdMap.set(item.imageId, index)
    })

    if (duplicateImageIds.size > 0) {
      throw new Error(`当前图片列表存在重复图片 ID：${Array.from(duplicateImageIds).join('、')}`)
    }

    configs.forEach((config) => {
      const targetIndex = imageIdMap.get(config.imageId.trim())
      if (targetIndex === undefined) {
        unmatchedImageIds.push(config.imageId)
        return
      }

      nextItems[targetIndex] = applyExcelConfigToTaskImage({
        image: nextItems[targetIndex],
        excelConfig: config,
        mergeMode: excelImportOptions.mergeMode
      })
      matchedCount += 1
    })
  }

  setTaskImages(nextItems)
  return { matchedCount, unmatchedImageIds, overflowCount }
}

function triggerWatermarkImageSelect(imageId: string, watermarkId: string) {
  pendingWatermarkTarget.value = { imageId, watermarkId }
  watermarkImageInputRef.value?.click()
}

function handleWatermarkImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  const target = pendingWatermarkTarget.value
  input.value = ''
  pendingWatermarkTarget.value = null

  if (!file || !target) {
    return
  }

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('仅支持上传图片水印文件')
    return
  }

  updateWatermarkInput(target.imageId, target.watermarkId, (watermarkInput) => {
    clearWatermarkPreviewError(target.imageId, target.watermarkId)
    revokeWatermarkLocalPreview(watermarkInput)
    watermarkInput.value = ''
    watermarkInput.imagePreviewUrl = URL.createObjectURL(file)
    watermarkInput.localFile = file
    watermarkInput.localFileName = file.name
  })
}

function clearWatermarkImage(imageId: string, watermarkId: string) {
  updateWatermarkInput(imageId, watermarkId, (input) => {
    clearWatermarkPreviewError(imageId, watermarkId)
    revokeWatermarkLocalPreview(input)
    input.value = ''
    input.imagePreviewUrl = ''
    input.localFile = undefined
    input.localFileName = ''
  })
}

function hasWatermarkAsset(input: BatchTaskWatermarkInput) {
  return Boolean(input.localFile || input.value)
}

function getWatermarkPreviewSrc(imageId: string, input: BatchTaskWatermarkInput) {
  if (watermarkPreviewErrors.value[getWatermarkPreviewKey(imageId, input.watermarkId)]) {
    return ''
  }

  return input.imagePreviewUrl || input.value
}

function getWatermarkValidationError(imageId: string, input: BatchTaskWatermarkInput) {
  if (input.type !== 'image' || !hasWatermarkAsset(input)) {
    return ''
  }

  if (!getWatermarkPreviewSrc(imageId, input)) {
    return '图片不可预览，请重新上传'
  }

  return ''
}

function handleWatermarkPreviewError(imageId: string, watermarkId: string) {
  watermarkPreviewErrors.value = {
    ...watermarkPreviewErrors.value,
    [getWatermarkPreviewKey(imageId, watermarkId)]: true
  }
}

function getTargetDirectoryError(row: BatchTaskImageDraft) {
  return validateTargetDirectory(row.targetDirectory)
}

function getOutputNameError(row: BatchTaskImageDraft) {
  return validateOutputName(row.outputFileName)
}

function getOutputExtensionError(row: BatchTaskImageDraft) {
  return validateOutputExtension(row.outputExtension)
}

function findFirstValidationIssue(): ValidationIssue | null {
  for (const image of taskImages.value) {
    const pathError = validateTargetDirectory(image.targetDirectory)
    if (pathError) {
      return {
        imageId: image.id,
        field: 'targetDirectory',
        message: pathError,
        sourceFileName: image.sourceFileName
      }
    }

    const outputNameError = validateOutputName(image.outputFileName)
    if (outputNameError) {
      return {
        imageId: image.id,
        field: 'outputFileName',
        message: outputNameError,
        sourceFileName: image.sourceFileName
      }
    }

    const outputExtensionError = validateOutputExtension(image.outputExtension)
    if (outputExtensionError) {
      return {
        imageId: image.id,
        field: 'outputExtension',
        message: outputExtensionError,
        sourceFileName: image.sourceFileName
      }
    }

    for (const input of image.watermarkInputs) {
      const watermarkError = getWatermarkValidationError(image.id, input)
      if (watermarkError) {
        return {
          imageId: image.id,
          field: 'watermark',
          message: watermarkError,
          sourceFileName: image.sourceFileName,
          watermarkId: input.watermarkId,
          watermarkName: input.watermarkName
        }
      }
    }
  }

  return null
}

function buildValidationMessage(issue: ValidationIssue) {
  if (issue.field === 'watermark') {
    return `图片“${issue.sourceFileName}”的水印“${issue.watermarkName}”无效：${issue.message}`
  }

  const fieldLabelMap: Record<Exclude<ValidationField, 'watermark'>, string> = {
    targetDirectory: '路径配置',
    outputFileName: '新文件名',
    outputExtension: '新拓展名'
  }

  return `图片“${issue.sourceFileName}”的${fieldLabelMap[issue.field]}无效：${issue.message}`
}

async function focusValidationIssue(issue: ValidationIssue) {
  await nextTick()

  const container = rightPaneScrollRef.value
  if (!container) {
    return
  }

  let selector = `.config-cell[data-image-id="${issue.imageId}"][data-field="${issue.field}"]`
  if (issue.field === 'watermark' && issue.watermarkId) {
    selector += `[data-watermark-id="${issue.watermarkId}"]`
  }

  const cell = container.querySelector<HTMLElement>(selector)
  if (!cell) {
    return
  }

  cell.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
    inline: 'center'
  })

  requestAnimationFrame(() => {
    const focusTarget = cell.querySelector<HTMLElement>('.watermark-upload-box, input, textarea, .el-select__wrapper')
    focusTarget?.focus?.()

    if (issue.field === 'outputExtension') {
      focusTarget?.click?.()
    }
  })
}

function handleImageDragStart() {
  dragPreviewIds.value = taskImages.value.map((item) => item.id)
}

function handleImageDragMove(event: DragMoveEvent) {
  const draggedId = event.draggedContext.element.id
  const currentIds = taskImages.value.map((item) => item.id)
  const currentIndex = currentIds.indexOf(draggedId)
  const futureIndex = typeof event.draggedContext.futureIndex === 'number'
    ? event.draggedContext.futureIndex
    : event.relatedContext.index

  if (currentIndex === -1 || futureIndex === undefined || futureIndex < 0) {
    return true
  }

  const nextIds = [...currentIds]
  nextIds.splice(currentIndex, 1)
  nextIds.splice(futureIndex, 0, draggedId)
  dragPreviewIds.value = nextIds

  return true
}

function handleImageDragEnd() {
  nextTick(() => {
    dragPreviewIds.value = null
  })
}

function handleLeftPaneScroll() {
  if (!leftPaneBodyRef.value || !rightPaneScrollRef.value || syncingPane.value === 'right') {
    return
  }

  syncingPane.value = 'left'
  rightPaneScrollRef.value.scrollTop = leftPaneBodyRef.value.scrollTop
  requestAnimationFrame(() => {
    syncingPane.value = null
  })
}

function handleRightPaneScroll() {
  if (!leftPaneBodyRef.value || !rightPaneScrollRef.value || syncingPane.value === 'left') {
    return
  }

  syncingPane.value = 'right'
  leftPaneBodyRef.value.scrollTop = rightPaneScrollRef.value.scrollTop
  requestAnimationFrame(() => {
    syncingPane.value = null
  })
}

function handlePaneResizeStart(event: MouseEvent) {
  if (isCompactLayout.value) {
    return
  }

  isResizingPane.value = true
  paneResizeStartX.value = event.clientX
  paneResizeStartWidth.value = leftPaneWidth.value
  previousBodyCursor.value = document.body.style.cursor
  previousBodyUserSelect.value = document.body.style.userSelect
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handlePaneResizing)
  document.addEventListener('mouseup', handlePaneResizeEnd)
}

function handlePaneResizing(event: MouseEvent) {
  const deltaX = event.clientX - paneResizeStartX.value
  leftPaneWidth.value = clampLeftPaneWidth(paneResizeStartWidth.value + deltaX)
}

function handlePaneResizeEnd() {
  if (!isResizingPane.value) {
    return
  }

  isResizingPane.value = false
  document.body.style.cursor = previousBodyCursor.value
  document.body.style.userSelect = previousBodyUserSelect.value
  document.removeEventListener('mousemove', handlePaneResizing)
  document.removeEventListener('mouseup', handlePaneResizeEnd)
  persistLeftPaneWidth()
}

async function startTask() {
  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择模板')
    return
  }

  if (!taskImages.value.length) {
    ElMessage.warning('请至少上传一张待处理图片')
    return
  }

  const validationIssue = findFirstValidationIssue()
  if (validationIssue) {
    ElMessage.warning(buildValidationMessage(validationIssue))
    await focusValidationIssue(validationIssue)
    return
  }

  startingTask.value = true

  try {
    const preparedItems = await uploadPendingWatermarkImages()
    setTaskImages(preparedItems)

    const pendingDraft: PendingBatchTaskDraft = {
      templateId: selectedTemplate.value.id,
      templateName: selectedTemplate.value.name,
      templateVersion: selectedTemplate.value.version,
      templateSnapshot: selectedTemplate.value,
      createdAt: new Date().toISOString(),
      items: preparedItems.map(cloneTaskImageDraft)
    }

    batchTaskStore.setPendingDraft(pendingDraft)
    ElMessage.success('任务配置已暂存，正在进入批量任务执行页。')
    allowStepTwoLeave.value = true
    try {
      await router.push('/task/execution')
    } catch (navigationError) {
      allowStepTwoLeave.value = false
      throw navigationError
    }
  } catch (error) {
    allowStepTwoLeave.value = false
    console.error('准备批量任务失败:', error)
    if (!isHandledRequestError(error)) {
      ElMessage.error(error instanceof Error ? error.message : '准备批量任务失败，请稍后重试。')
    }
  } finally {
    startingTask.value = false
  }
}

function resetTaskImages() {
  taskImages.value.forEach((item) => cleanupTaskImage(item))
  taskImages.value = []
  dragPreviewIds.value = null
  watermarkPreviewErrors.value = {}
}

function restorePendingDraft() {
  const pendingDraft = batchTaskStore.pendingDraft
  if (!pendingDraft) {
    return
  }

  selectedTemplateId.value = pendingDraft.templateId
  selectedTemplate.value = pendingDraft.templateSnapshot
  setTaskImages(pendingDraft.items.map(cloneTaskImageDraft))
  currentStep.value = 1
}

function restoreLeftPaneWidth() {
  const storedWidth = Number(window.localStorage.getItem(LEFT_PANE_WIDTH_STORAGE_KEY))
  if (Number.isFinite(storedWidth) && storedWidth > 0) {
    leftPaneWidth.value = storedWidth
  }
}

function persistLeftPaneWidth() {
  window.localStorage.setItem(LEFT_PANE_WIDTH_STORAGE_KEY, String(leftPaneWidth.value))
}

function restoreExcelImportOptions() {
  try {
    const storedOptions = window.localStorage.getItem(EXCEL_IMPORT_OPTIONS_STORAGE_KEY)
    if (!storedOptions) {
      return
    }

    const parsedOptions = JSON.parse(storedOptions) as Partial<typeof defaultExcelImportOptions>
    excelImportOptions.mappingMode = parsedOptions.mappingMode === 'order' ? 'order' : defaultExcelImportOptions.mappingMode
    excelImportOptions.duplicateHandling = ['error', 'first', 'last'].includes(parsedOptions.duplicateHandling || '')
      ? parsedOptions.duplicateHandling as typeof defaultExcelImportOptions.duplicateHandling
      : defaultExcelImportOptions.duplicateHandling
    excelImportOptions.invalidCharHandling = ['error', 'error_folder', 'underscore'].includes(parsedOptions.invalidCharHandling || '')
      ? parsedOptions.invalidCharHandling as typeof defaultExcelImportOptions.invalidCharHandling
      : defaultExcelImportOptions.invalidCharHandling
    excelImportOptions.mergeMode = parsedOptions.mergeMode === 'fill-empty' ? 'fill-empty' : defaultExcelImportOptions.mergeMode
  } catch (error) {
    console.error('恢复导入规则失败:', error)
  }
}

function persistExcelImportOptions() {
  window.localStorage.setItem(
    EXCEL_IMPORT_OPTIONS_STORAGE_KEY,
    JSON.stringify({ ...excelImportOptions })
  )
}

function isHandledRequestError(error: unknown): error is Error & { __handled?: boolean } {
  return error instanceof Error && Boolean((error as Error & { __handled?: boolean }).__handled)
}

function hasStepTwoUnsavedState() {
  return currentStep.value === 1
}

async function confirmDiscardStepTwoChanges() {
  try {
    await ElMessageBox.confirm(
      '离开当前页面后，步骤二中的图片与水印配置不会自动保留，是否继续离开？',
      '确认离开',
      {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '留在当前页'
      }
    )
    return true
  } catch {
    return false
  }
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (allowStepTwoLeave.value || !hasStepTwoUnsavedState()) {
    return
  }

  event.preventDefault()
  event.returnValue = ''
}

function handleWindowResize() {
  isCompactLayout.value = window.innerWidth <= 960
  nextTick(() => {
    leftPaneWidth.value = clampLeftPaneWidth(leftPaneWidth.value)
  })
}

function clampLeftPaneWidth(width: number) {
  const containerWidth = workbenchRef.value?.clientWidth || 0
  if (containerWidth <= 0) {
    return Math.max(MIN_LEFT_PANE_WIDTH, width)
  }

  const maxWidth = Math.max(MIN_LEFT_PANE_WIDTH, containerWidth - MIN_RIGHT_PANE_WIDTH - RESIZER_WIDTH)
  return Math.min(Math.max(width, MIN_LEFT_PANE_WIDTH), maxWidth)
}

function setTaskImages(nextItems: BatchTaskImageDraft[]) {
  cleanupReplacedTaskImages(taskImages.value, nextItems)
  taskImages.value = nextItems

  if (dragPreviewIds.value) {
    dragPreviewIds.value = nextItems.map((item) => item.id)
  }
}

function cleanupReplacedTaskImages(previousItems: BatchTaskImageDraft[], nextItems: BatchTaskImageDraft[]) {
  const nextImageMap = new Map(nextItems.map((item) => [item.id, item]))

  previousItems.forEach((previousItem) => {
    const nextItem = nextImageMap.get(previousItem.id)
    if (!nextItem) {
      cleanupTaskImage(previousItem)
      return
    }

    const nextInputMap = new Map(nextItem.watermarkInputs.map((input) => [input.watermarkId, input]))
    previousItem.watermarkInputs.forEach((previousInput) => {
      const nextInput = nextInputMap.get(previousInput.watermarkId)
      if (!nextInput) {
        revokeWatermarkLocalPreview(previousInput)
        return
      }

      const previewChanged = previousInput.imagePreviewUrl !== nextInput.imagePreviewUrl
        || previousInput.localFile !== nextInput.localFile

      if (previewChanged) {
        revokeWatermarkLocalPreview(previousInput)
      }
    })
  })
}

function cleanupTaskImage(item: BatchTaskImageDraft) {
  revokePreviewUrl(item.previewUrl)
  item.watermarkInputs.forEach((input) => revokeWatermarkLocalPreview(input))
}

function revokePreviewUrl(url?: string) {
  if (url && url.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}

function revokeWatermarkLocalPreview(input: BatchTaskWatermarkInput) {
  if (input.localFile && input.imagePreviewUrl) {
    revokePreviewUrl(input.imagePreviewUrl)
  }
}

function findTaskImage(imageId: string) {
  return taskImages.value.find((item) => item.id === imageId)
}

function updateWatermarkInput(imageId: string, watermarkId: string, updater: (input: BatchTaskWatermarkInput) => void) {
  const row = findTaskImage(imageId)
  if (!row) {
    return
  }

  const input = row.watermarkInputs.find((item) => item.watermarkId === watermarkId)
  if (!input) {
    return
  }

  updater(input)
}

function getWatermarkPreviewKey(imageId: string, watermarkId: string) {
  return `${imageId}:${watermarkId}`
}

function clearWatermarkPreviewError(imageId: string, watermarkId: string) {
  const previewKey = getWatermarkPreviewKey(imageId, watermarkId)
  if (!watermarkPreviewErrors.value[previewKey]) {
    return
  }

  const nextErrors = { ...watermarkPreviewErrors.value }
  delete nextErrors[previewKey]
  watermarkPreviewErrors.value = nextErrors
}

function clearWatermarkPreviewErrorsForImage(imageId: string) {
  const nextErrors = { ...watermarkPreviewErrors.value }
  let hasChange = false

  Object.keys(nextErrors).forEach((key) => {
    if (key.startsWith(`${imageId}:`)) {
      delete nextErrors[key]
      hasChange = true
    }
  })

  if (hasChange) {
    watermarkPreviewErrors.value = nextErrors
  }
}

async function uploadPendingWatermarkImages() {
  const nextItems = taskImages.value.map(cloneTaskImageDraft)

  for (const image of nextItems) {
    for (const input of image.watermarkInputs) {
      if (input.type !== 'image' || !input.localFile) {
        continue
      }

      const response = await uploadFile(input.localFile, 'watermark/')
      if (response.code !== 200 || !response.data) {
        throw new Error(response.message || `图片“${image.sourceFileName}”的水印“${input.watermarkName}”上传失败`)
      }

      input.value = response.data
      input.imagePreviewUrl = response.data
      input.localFile = undefined
      input.localFileName = ''
    }
  }

  return nextItems
}

watch(currentStep, (step) => {
  if (step !== 1) {
    return
  }

  nextTick(() => {
    leftPaneWidth.value = clampLeftPaneWidth(leftPaneWidth.value)
  })
})

watch(
  excelImportOptions,
  () => {
    persistExcelImportOptions()
  },
  { deep: true }
)

onBeforeRouteLeave(async () => {
  if (allowStepTwoLeave.value || !hasStepTwoUnsavedState()) {
    return true
  }

  return confirmDiscardStepTwoChanges()
})

onMounted(() => {
  restorePendingDraft()
  restoreLeftPaneWidth()
  restoreExcelImportOptions()
  handleWindowResize()
  window.addEventListener('resize', handleWindowResize)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  handlePaneResizeEnd()
  window.removeEventListener('resize', handleWindowResize)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped lang="scss">
.task-create {
  --step-two-height: 640px;
  --row-height: 96px;

  .task-card {
    :deep(.el-card__header) {
      padding: 20px 24px 16px;
    }
  }

  .card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 32px;
  }

  .header-intro {
    flex: 1;
    min-width: 0;

    h2 {
      margin: 0;
      font-size: 22px;
      color: var(--color-text-primary);
    }
  }

  .header-steps {
    width: min(520px, 44%);
    min-width: 320px;
    margin-left: auto;
  }

  .task-steps {
    width: 100%;

    :deep(.el-step__main) {
      width: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding-top: 12px;
    }

    :deep(.el-step__title) {
      width: 100%;
      text-align: center;
      line-height: 1.4;
    }
  }

  .step-panel {
    min-height: 320px;
  }

  .selected-summary {
    display: flex;
    align-items: center;
    gap: 8px;

    .summary-label {
      color: var(--color-text-secondary);
      font-size: 13px;
    }
  }

  .step-two-card {
    border-radius: 16px;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }
  }

  .step-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    font-size: 15px;
    font-weight: 500;
  }

  .step-card-title {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .step-card-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
    margin-left: auto;
  }

  .workbench {
    display: grid;
    gap: 0;
    min-height: var(--step-two-height);
  }

  .panel-shell {
    display: flex;
    flex-direction: column;
    min-height: var(--step-two-height);
    border: 1px solid #e7edf5;
    border-radius: 16px;
    overflow: hidden;
    background: #ffffff;
  }

  .pane-resizer {
    position: relative;
    display: flex;
    align-items: stretch;
    justify-content: center;
    cursor: col-resize;
    user-select: none;

    &:hover .resizer-line,
    &.active .resizer-line {
      background: #409eff;
      box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.16);
    }
  }

  .resizer-line {
    width: 2px;
    margin: 12px 0;
    border-radius: 999px;
    background: #d7e1eb;
    transition: background-color 0.16s ease, box-shadow 0.16s ease;
  }

  .pane-header {
    height: 41px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 0 16px;
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-primary);
    background: linear-gradient(180deg, #fbfcfe 0%, #f6f9fc 100%);
    border-bottom: 1px solid #e7edf5;
    flex-shrink: 0;
  }

  .pane-meta {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-secondary);
  }

  .pane-body {
    flex: 1;
    overflow-y: auto;
    user-select: none;
  }

  .image-list {
    display: flex;
    flex-direction: column;
  }

  .image-row,
  .config-data-row {
    height: var(--row-height);
    min-height: var(--row-height);
  }

  .image-row {
    display: grid;
    grid-template-columns: 32px 44px minmax(0, 1fr) auto;
    align-items: center;
    gap: 8px;
    padding: 4px 8px;
    border-bottom: 1px solid #eef2f7;
    cursor: grab;
    user-select: none;
    transition: background-color 0.16s ease, box-shadow 0.16s ease;

    &:hover {
      background: #f8fbff;
    }

    &:active {
      cursor: grabbing;
    }
  }

  .image-order {
    width: 24px;
    height: 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 7px;
    background: #eff4fa;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
  }

  .image-thumb-box {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    overflow: hidden;
    flex-shrink: 0;
    background: linear-gradient(180deg, #f9fbfd 0%, #eef3f8 100%);
  }

  .watermark-upload-box {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2px;
    border-radius: 10px;
    overflow: hidden;
    background: linear-gradient(180deg, #fbfcfe 0%, #f2f6fb 100%);
    border: 1px dashed #c7d5e5;
    cursor: pointer;
    transition: border-color 0.12s ease, background-color 0.12s ease;

    &:hover {
      border-color: #409eff;
    }

    &:focus-visible {
      outline: none;
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.18);
    }

    &.has-value {
      border-style: solid;
      border-color: #d9e3ef;
      background: #f8fbff;
    }
  }

  .image-thumb,
  .watermark-thumb {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 2px;
    color: #6b7b8d;
    font-size: 10px;
    line-height: 1.2;
    text-align: center;
  }

  .watermark-upload-mask {
    position: absolute;
    inset: auto 0 0 0;
    padding: 3px 0;
    background: rgba(15, 23, 42, 0.58);
    color: #ffffff;
    font-size: 10px;
    text-align: center;
    opacity: 0;
    transition: opacity 0.12s ease;
  }

  .watermark-upload-box:hover .watermark-upload-mask {
    opacity: 1;
  }

  .watermark-remove {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 16px;
    height: 16px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(15, 23, 42, 0.72);
    color: #ffffff;
    opacity: 0;
    transition: opacity 0.12s ease;
  }

  .watermark-upload-box:hover .watermark-remove,
  .watermark-upload-box.has-value .watermark-remove {
    opacity: 1;
  }

  .image-text {
    min-width: 0;
  }

  .image-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .image-size {
    margin-top: 4px;
    font-size: 11px;
    color: var(--color-text-secondary);
  }

  .delete-button {
    user-select: none;
  }

  .pane-empty-state,
  .config-empty-state {
    min-height: calc(var(--step-two-height) - 44px);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .sortable-ghost {
    opacity: 0.55;
    background: #e8f3ff !important;
    box-shadow: 0 10px 24px rgba(64, 158, 255, 0.2);
  }

  .sortable-chosen,
  .sortable-drag {
    user-select: none;
  }

  .config-pane {
    min-width: 0;
  }

  .config-scroll {
    flex: 1;
    overflow: auto;
  }

  .config-table {
    min-height: 100%;
  }

  .config-header-row,
  .config-data-row {
    display: grid;
  }

  .config-header-row {
    position: sticky;
    top: 0;
    z-index: 2;
    background: #fbfcfe;
    border-bottom: 1px solid #e7edf5;
  }

  .config-header-cell {
    min-height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 8px 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--color-text-primary);
    text-align: center;
    border-right: 1px solid #eef2f7;
    background: #fbfcfe;
  }

  .config-header-cell.watermark-column {
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  .header-name {
    display: block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.2;
  }

  .watermark-type-tag {
    margin: 0;
  }

  .config-body {
    display: flex;
    flex-direction: column;
  }

  .config-data-row {
    border-bottom: 1px solid #eef2f7;
    background: #ffffff;
  }

  .config-row-move {
    transition: transform 0.12s ease;
  }

  .config-cell {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    padding: 4px 6px;
    border-right: 1px solid #eef2f7;
    background: #ffffff;
    overflow: hidden;

    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      min-height: 36px;
    }
  }

  .config-cell.watermark-column {
    position: relative;
    align-items: stretch;
    padding: 2px 4px;
  }

  .config-cell.watermark-column.has-error {
    padding-bottom: 16px;
  }

  .config-cell.watermark-column .cell-error-placeholder {
    display: none;
  }

  .config-cell.watermark-column.has-error .cell-error-placeholder {
    display: block;
    position: absolute;
    left: 4px;
    right: 4px;
    bottom: 2px;
    min-height: 12px;
    pointer-events: none;
  }

  .cell-error-placeholder {
    min-height: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 11px;
    line-height: 12px;
    color: #f56c6c;
  }

  .step-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 24px;
  }

  .excel-import-form {
    .import-rule-panel {
      margin-top: 8px;
      padding: 14px 16px 4px;
      border: 1px solid #e7edf5;
      border-radius: 14px;
      background: linear-gradient(180deg, #fbfcfe 0%, #f7faff 100%);
    }

    .rule-panel-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--color-text-primary);
    }

    .rule-panel-hint {
      margin: 4px 0 12px;
      font-size: 12px;
      color: var(--color-text-secondary);
      line-height: 1.5;
    }

    .excel-file-row {
      display: flex;
      align-items: center;
      gap: 12px;
      min-height: 32px;
    }

    .excel-file-name {
      color: var(--color-text-secondary);
      word-break: break-all;
    }
  }
}

@media (max-width: 1280px) {
  .task-create {
    .header-steps {
      width: min(480px, 46%);
      min-width: 300px;
    }
  }
}

@media (max-width: 960px) {
  .task-create {
    .card-header,
    .step-card-header {
      flex-direction: column;
      align-items: stretch;
    }

    .header-steps {
      width: 100%;
      min-width: 0;
      margin-left: 0;
    }

    .step-card-actions {
      width: 100%;
      justify-content: flex-end;
      margin-left: 0;
    }

    .workbench {
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .image-row,
    .config-data-row {
      height: auto;
      min-height: var(--row-height);
    }
  }
}
</style>









