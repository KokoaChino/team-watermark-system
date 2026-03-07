<template>
  <div class="template-browser">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>{{ title }}</span>
            <el-tag v-if="showCountTag" type="info" size="small" class="count-tag">
              共 {{ filteredTemplates.length }} 个
            </el-tag>
          </div>
          <div v-if="$slots['header-actions']" class="header-right">
            <slot name="header-actions" />
          </div>
        </div>
      </template>

      <div class="filter-section">
        <el-input
          v-model="filterName"
          placeholder="搜索模板名称"
          clearable
          class="filter-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterCreators"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择创建者"
          clearable
          class="filter-select"
        >
          <el-option
            v-for="creator in creatorOptions"
            :key="creator.id"
            :label="creator.username"
            :value="creator.id"
          />
        </el-select>
        <el-date-picker
          v-model="filterCreatedAt"
          type="datetimerange"
          range-separator="至"
          start-placeholder="创建开始时间"
          end-placeholder="创建结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="filter-date"
        />
        <el-date-picker
          v-model="filterUpdatedAt"
          type="datetimerange"
          range-separator="至"
          start-placeholder="更新开始时间"
          end-placeholder="更新结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="filter-date"
        />
      </div>

      <div class="template-scroll-wrapper" v-loading="loading">
        <div v-if="filteredTemplates.length" class="template-grid single-row">
          <div
            v-for="item in filteredTemplates"
            :key="item.id"
            class="template-item"
            :class="{ active: selectedTemplateId === item.id }"
            @click="handleSelectTemplate(item.id)"
          >
            <div class="preview-wrapper">
              <img
                v-if="item.config?.previewImageKey"
                :src="item.config.previewImageKey"
                class="preview-image"
                @error="handlePreviewError($event, item)"
              />
              <div v-else class="preview-placeholder">
                <el-icon :size="40"><Picture /></el-icon>
                <span>暂无预览</span>
              </div>
            </div>
            <div class="item-content">
              <div class="item-header">
                <span class="item-name" :title="item.name">{{ item.name }}</span>
              </div>
              <div class="item-info-grid">
                <div class="info-col">
                  <div class="info-row">
                    <span class="info-label">创建者</span>
                    <span class="info-value">{{ item.createdByUsername }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">水印数</span>
                    <span class="info-value">{{ item.config?.watermarks?.length || 0 }}</span>
                  </div>
                </div>
                <div class="info-col">
                  <div class="info-row">
                    <span class="info-label">尺寸</span>
                    <span class="info-value">
                      {{ item.config?.baseConfig?.width }} x {{ item.config?.baseConfig?.height }}
                    </span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">版本号</span>
                    <span class="info-value">v{{ item.version }}</span>
                  </div>
                </div>
              </div>
              <div class="info-row-full">
                <span class="info-label">更新时间</span>
                <span class="info-value">{{ formatDate(item.updatedAt) }}</span>
              </div>
              <div class="info-row-full item-created-at">
                <span class="info-label">创建时间</span>
                <span class="info-value">{{ formatDate(item.createdAt) }}</span>
              </div>
              <div v-if="$slots['item-actions']" class="item-actions">
                <slot name="item-actions" :item="item" />
              </div>
            </div>
          </div>
        </div>
        <el-empty
          v-else-if="!loading"
          :description="templates.length === 0 ? emptyDescription : '暂无符合条件的模板'"
        />
      </div>
    </el-card>

    <el-card v-if="showDetail && selectedTemplate" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>
            模板详情：<span class="highlight-value">{{ selectedTemplate.name }}</span>
          </span>
        </div>
      </template>

      <div class="detail-content">
        <div class="detail-left">
          <el-card class="config-card" shadow="never">
            <template #header>
              <span class="section-title">底图配置</span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="宽度">
                <span class="highlight-value">{{ selectedTemplate.config?.baseConfig?.width }} px</span>
              </el-descriptions-item>
              <el-descriptions-item label="高度">
                <span class="highlight-value">{{ selectedTemplate.config?.baseConfig?.height }} px</span>
              </el-descriptions-item>
              <el-descriptions-item label="背景色">
                <div class="color-preview">
                  <span
                    class="color-block"
                    :style="{ backgroundColor: selectedTemplate.config?.baseConfig?.backgroundColor || '#ffffff' }"
                  ></span>
                  <span class="highlight-value">
                    {{ selectedTemplate.config?.baseConfig?.backgroundColor || '#ffffff' }}
                  </span>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="watermark-list-card" shadow="never">
            <template #header>
              <div class="section-header">
                <span class="section-title">
                  水印列表 ({{ selectedTemplate.config?.watermarks?.length || 0 }})
                </span>
              </div>
            </template>

            <div v-if="selectedTemplate.config?.watermarks?.length" class="watermark-list">
              <div
                v-for="(watermark, index) in selectedTemplate.config.watermarks"
                :key="watermark.id"
                class="watermark-item"
                :class="{ active: selectedWatermarkId === watermark.id }"
                @click="selectWatermark(watermark.id)"
              >
                <div class="item-info">
                  <span class="item-index">{{ index + 1 }}</span>
                  <span class="item-name">{{ watermark.name }}</span>
                  <el-tag size="small" :type="watermark.type === 'text' ? 'primary' : 'success'">
                    {{ watermark.type === 'text' ? '文字' : '图片' }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无水印" :image-size="60" />
          </el-card>

          <el-card v-if="selectedWatermark" class="watermark-detail-card" shadow="never">
            <template #header>
              <div class="section-header">
                <span class="section-title">
                  参数详情：<span class="highlight-value">{{ selectedWatermark.name }}</span>
                </span>
              </div>
            </template>

            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="X 坐标">
                <span class="highlight-value">{{ formatNumber(selectedWatermark.x) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="Y 坐标">
                <span class="highlight-value">{{ formatNumber(selectedWatermark.y) }}</span>
              </el-descriptions-item>
            </el-descriptions>

            <template v-if="selectedWatermark.type === 'text' && selectedWatermark.textConfig">
              <el-divider content-position="left">文字配置</el-divider>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="内容" :span="2">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.content }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="字体">
                  <span class="highlight-value">{{ getFontDisplayName(selectedWatermark.textConfig) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="字号">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.fontSize }} px</span>
                </el-descriptions-item>
                <el-descriptions-item label="颜色">
                  <div class="color-preview">
                    <span class="color-block" :style="{ backgroundColor: selectedWatermark.textConfig.color }"></span>
                    <span class="highlight-value">{{ selectedWatermark.textConfig.color }}</span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="对齐">
                  <span class="highlight-value">{{ alignMap[selectedWatermark.textConfig.align] }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="字重">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.fontWeight }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="旋转">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.rotation }}°</span>
                </el-descriptions-item>
                <el-descriptions-item label="透明度">
                  <span class="highlight-value">
                    {{ (selectedWatermark.textConfig.opacity * 100).toFixed(0) }}%
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="倾斜">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.italicAngle }}°</span>
                </el-descriptions-item>
              </el-descriptions>

              <template v-if="selectedWatermark.textConfig.strokeEnabled">
                <el-divider content-position="left">描边配置</el-divider>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="描边颜色">
                    <div class="color-preview">
                      <span class="color-block" :style="{ backgroundColor: selectedWatermark.textConfig.strokeColor }"></span>
                      <span class="highlight-value">{{ selectedWatermark.textConfig.strokeColor }}</span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="描边宽度">
                    <span class="highlight-value">{{ selectedWatermark.textConfig.strokeWidth }} px</span>
                  </el-descriptions-item>
                </el-descriptions>
              </template>

              <template v-if="selectedWatermark.textConfig.shadowEnabled">
                <el-divider content-position="left">阴影配置</el-divider>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="阴影颜色">
                    <div class="color-preview">
                      <span class="color-block" :style="{ backgroundColor: selectedWatermark.textConfig.shadowColor }"></span>
                      <span class="highlight-value">{{ selectedWatermark.textConfig.shadowColor }}</span>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="模糊半径">
                    <span class="highlight-value">{{ selectedWatermark.textConfig.shadowBlur }} px</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="X 偏移">
                    <span class="highlight-value">{{ selectedWatermark.textConfig.shadowOffsetX }} px</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="Y 偏移">
                    <span class="highlight-value">{{ selectedWatermark.textConfig.shadowOffsetY }} px</span>
                  </el-descriptions-item>
                </el-descriptions>
              </template>

              <template
                v-if="selectedWatermark.textConfig.gradientEnabled && selectedWatermark.textConfig.gradientColors?.length"
              >
                <el-divider content-position="left">渐变配置</el-divider>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="渐变角度">
                    <span class="highlight-value">{{ selectedWatermark.textConfig.gradientAngle }}°</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="渐变颜色">
                    <div class="gradient-preview">
                      <span
                        v-for="(color, idx) in selectedWatermark.textConfig.gradientColors"
                        :key="idx"
                        class="color-block"
                        :style="{ backgroundColor: color }"
                      ></span>
                    </div>
                  </el-descriptions-item>
                </el-descriptions>
              </template>
            </template>

            <template v-if="selectedWatermark.type === 'image' && selectedWatermark.imageConfig">
              <el-divider content-position="left">图片配置</el-divider>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="图片" :span="2">
                  <div class="image-preview-wrapper">
                    <img
                      v-if="selectedWatermark.imageConfig.imageUrl"
                      :src="selectedWatermark.imageConfig.imageUrl"
                      class="image-preview"
                    />
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="缩放">
                  <span class="highlight-value">{{ selectedWatermark.imageConfig.scale }}%</span>
                </el-descriptions-item>
                <el-descriptions-item label="透明度">
                  <span class="highlight-value">
                    {{ (selectedWatermark.imageConfig.opacity * 100).toFixed(0) }}%
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="适应模式">
                  <span class="highlight-value">{{ fitModeMap[selectedWatermark.imageConfig.fitMode] }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="锚点位置">
                  <span class="highlight-value">{{ anchorMap[selectedWatermark.imageConfig.anchor] }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="旋转">
                  <span class="highlight-value">{{ selectedWatermark.rotation || 0 }}°</span>
                </el-descriptions-item>
                <el-descriptions-item label="原始尺寸">
                  <span class="highlight-value">
                    {{ selectedWatermark.imageConfig.originalWidth || '-' }} x
                    {{ selectedWatermark.imageConfig.originalHeight || '-' }}
                  </span>
                </el-descriptions-item>
              </el-descriptions>
            </template>
          </el-card>
        </div>

        <div class="detail-right">
          <PreviewCanvas
            ref="previewCanvasRef"
            :base-config="selectedTemplate.config?.baseConfig || defaultBaseConfig"
            :watermarks="selectedTemplate.config?.watermarks || []"
            :selected-watermark-id="selectedWatermarkId"
            :show-header="true"
            :show-refresh="false"
            :editable="true"
            @update:selected-watermark-id="selectWatermark"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Picture, Search } from '@element-plus/icons-vue'
import PreviewCanvas from '@/components/watermark/PreviewCanvas.vue'
import { getFontList } from '@/api/font'
import { getTemplateList } from '@/api/template'
import type { FontVO, TextWatermarkConfigDTO, WatermarkTemplateVO } from '@/types'

interface Props {
  modelValue?: number | null
  title?: string
  emptyDescription?: string
  showDetail?: boolean
  showCountTag?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  title: '模板列表',
  emptyDescription: '暂无模板，请先创建水印模板',
  showDetail: true,
  showCountTag: true
})

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
  select: [template: WatermarkTemplateVO | null]
}>()

interface CreatorOption {
  id: number
  username: string
}

const defaultBaseConfig = {
  width: 800,
  height: 600,
  backgroundColor: '#ffffff'
}

const templates = ref<WatermarkTemplateVO[]>([])
const loading = ref(false)
const userFonts = ref<FontVO[]>([])
const selectedTemplateId = ref<number | null>(props.modelValue)
const selectedWatermarkId = ref<string | null>(null)
const previewCanvasRef = ref<InstanceType<typeof PreviewCanvas> | null>(null)

const filterName = ref('')
const filterCreators = ref<number[]>([])
const filterCreatedAt = ref<[string, string] | null>(null)
const filterUpdatedAt = ref<[string, string] | null>(null)

const creatorOptions = computed<CreatorOption[]>(() => {
  const creatorMap = new Map<number, string>()

  templates.value.forEach((template) => {
    if (template.createdById && template.createdByUsername) {
      creatorMap.set(template.createdById, template.createdByUsername)
    }
  })

  return Array.from(creatorMap.entries()).map(([id, username]) => ({ id, username }))
})

const filteredTemplates = computed(() => {
  let result = templates.value

  if (filterName.value.trim()) {
    const keyword = filterName.value.trim().toLowerCase()
    result = result.filter((template) => template.name.toLowerCase().includes(keyword))
  }

  if (filterCreators.value.length > 0) {
    result = result.filter((template) => filterCreators.value.includes(template.createdById))
  }

  if (filterCreatedAt.value?.length === 2) {
    const [start, end] = filterCreatedAt.value
    result = result.filter((template) => {
      if (!template.createdAt) {
        return false
      }

      return template.createdAt >= start && template.createdAt <= end
    })
  }

  if (filterUpdatedAt.value?.length === 2) {
    const [start, end] = filterUpdatedAt.value
    result = result.filter((template) => {
      if (!template.updatedAt) {
        return false
      }

      return template.updatedAt >= start && template.updatedAt <= end
    })
  }

  return result
})

const selectedTemplate = computed(() => {
  if (!selectedTemplateId.value) {
    return null
  }

  return templates.value.find((template) => template.id === selectedTemplateId.value) || null
})

const selectedWatermark = computed(() => {
  if (!selectedTemplate.value?.config?.watermarks) {
    return null
  }

  return selectedTemplate.value.config.watermarks.find((item) => item.id === selectedWatermarkId.value) || null
})

const alignMap: Record<string, string> = {
  left: '左对齐',
  center: '居中',
  right: '右对齐'
}

const fitModeMap: Record<string, string> = {
  none: '无',
  scaleToFill: '填充',
  aspectFit: '适应',
  aspectFill: '覆盖'
}

const anchorMap: Record<string, string> = {
  none: '无',
  topLeft: '左上角',
  topRight: '右上角',
  bottomLeft: '左下角',
  bottomRight: '右下角',
  center: '中心'
}

const systemFonts = [
  { value: 'Microsoft YaHei', label: '微软雅黑' },
  { value: 'SimSun', label: '宋体' },
  { value: 'SimHei', label: '黑体' },
  { value: 'KaiTi', label: '楷体' },
  { value: 'FangSong', label: '仿宋' },
  { value: 'Arial', label: 'Arial' },
  { value: 'Times New Roman', label: 'Times New Roman' },
  { value: 'Courier New', label: 'Courier New' },
  { value: 'Georgia', label: 'Georgia' },
  { value: 'Verdana', label: 'Verdana' }
]

function formatDate(dateStr: string) {
  if (!dateStr) {
    return ''
  }

  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatNumber(num: number | undefined) {
  if (num === undefined || num === null) {
    return '0.0'
  }

  return num.toFixed(1)
}

function getFontDisplayName(config: TextWatermarkConfigDTO) {
  const systemFont = systemFonts.find((font) => font.value === config.fontFamily)
  if (systemFont) {
    return systemFont.label
  }

  const userFont = userFonts.value.find((font) => `custom-font-${font.id}` === config.fontFamily)
  if (userFont) {
    return userFont.name
  }

  return config.fontFamily || '未知字体'
}

function handleSelectTemplate(templateId: number) {
  selectedTemplateId.value = templateId
}

function selectWatermark(id: string | null) {
  selectedWatermarkId.value = id
}

function handlePreviewError(event: Event, template: WatermarkTemplateVO) {
  const target = event.target as HTMLImageElement
  target.style.display = 'none'

  if (template.config) {
    template.config.previewImageKey = undefined
  }
}

async function fetchTemplates() {
  loading.value = true

  try {
    const res = await getTemplateList()
    if (res.code === 200) {
      templates.value = res.data || []
    }
  } catch (error) {
    console.error('获取模板列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchUserFonts() {
  try {
    const res = await getFontList()
    if (res.code === 200) {
      userFonts.value = res.data || []
    }
  } catch (error) {
    console.error('获取字体列表失败:', error)
  }
}

watch(
  () => props.modelValue,
  (value) => {
    if (value !== selectedTemplateId.value) {
      selectedTemplateId.value = value
    }
  }
)

watch(filteredTemplates, (value) => {
  if (!selectedTemplateId.value) {
    return
  }

  const exists = value.some((template) => template.id === selectedTemplateId.value)
  if (!exists) {
    selectedTemplateId.value = null
  }
})

watch(selectedTemplateId, (value) => {
  emit('update:modelValue', value)
  emit('select', selectedTemplate.value)
})

watch(selectedTemplate, async (value) => {
  if (!value?.config?.watermarks?.some((item) => item.id === selectedWatermarkId.value)) {
    selectedWatermarkId.value = null
  }

  await nextTick()
  previewCanvasRef.value?.refreshPreview()
})

defineExpose({
  refreshTemplates: fetchTemplates
})

onMounted(() => {
  fetchTemplates()
  fetchUserFonts()
})
</script>

<style scoped lang="scss">
.template-browser {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  :deep(.el-card__header) {
    padding: 12px 16px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  font-weight: 500;
  font-size: 15px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .count-tag {
    font-weight: normal;
  }
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;

  .filter-input {
    width: 200px;
  }

  .filter-select {
    width: 220px;
  }

  .filter-date {
    width: 360px;
  }
}

.template-scroll-wrapper {
  min-height: 220px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 16px;
}

.template-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;

  &.single-row {
    display: inline-flex;
    flex-wrap: nowrap;

    .template-item {
      width: 320px;
      flex-shrink: 0;
    }
  }
}

.template-item {
  display: flex;
  flex-direction: column;
  background-color: #fafafa;
  border-radius: 10px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;

  &:hover {
    background-color: #f5f7fa;
    border-color: #dcdfe6;
    transform: translateY(-2px);
  }

  &.active {
    background-color: #ecf5ff;
    border-color: #409eff;
    box-shadow: 0 8px 24px rgba(64, 158, 255, 0.12);
  }

  .preview-wrapper {
    width: 100%;
    height: 280px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #f7f9fc 0%, #eef3f9 100%);
    overflow: hidden;

    .preview-image {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

    .preview-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #c0c4cc;
      font-size: 12px;
      gap: 8px;
    }
  }

  .item-content {
    flex: 1;
    padding: 12px;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  .item-header {
    margin-bottom: 8px;
  }

  .item-name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-info-grid {
    display: flex;
    gap: 12px;
  }

  .info-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .info-row,
  .info-row-full {
    display: flex;
    font-size: 12px;
    line-height: 1.5;
  }

  .item-created-at {
    margin-bottom: 6px;
  }

  .info-label {
    color: #909399;
    width: 56px;
    flex-shrink: 0;
  }

  .info-value {
    color: #606266;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-actions {
    display: flex;
    gap: 8px;
    padding-top: 8px;
    margin-top: auto;
    border-top: 1px solid #ebeef5;

    :deep(.el-button) {
      flex: 1;
    }
  }
}

.detail-card {
  :deep(.el-card__header) {
    padding: 12px 16px;
  }
}

.highlight-value {
  color: #409eff;
  font-weight: 500;
}

.detail-content {
  display: flex;
  gap: 16px;
  min-height: 860px;
}

.detail-left {
  width: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.detail-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;

  :deep(.preview-container) {
    height: 100%;
  }
}

.config-card,
.watermark-list-card,
.watermark-detail-card {
  flex-shrink: 0;

  :deep(.el-card__header) {
    padding: 10px 12px;
    background-color: #fafafa;
  }

  :deep(.el-card__body) {
    padding: 12px;
  }
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.color-preview {
  display: flex;
  align-items: center;
  gap: 6px;
}

.color-block {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.gradient-preview {
  display: flex;
  gap: 4px;
}

.image-preview-wrapper {
  width: 120px;
  height: 80px;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.watermark-list {
  max-height: 200px;
  overflow-y: auto;
}

.watermark-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;

  &:hover {
    background-color: #f5f7fa;
  }

  &.active {
    background-color: #ecf5ff;
    border-color: #409eff;
  }

  .item-info {
    display: flex;
    align-items: center;
    gap: 8px;
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
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.el-divider {
  margin: 12px 0;
}

@media (max-width: 1200px) {
  .detail-content {
    flex-direction: column;
    min-height: auto;
  }

  .detail-left {
    width: 100%;
    overflow: visible;
  }
}

@media (max-width: 768px) {
  .filter-section {
    .filter-input,
    .filter-select,
    .filter-date {
      width: 100%;
    }
  }

  .template-scroll-wrapper {
    overflow-x: visible;
  }

  .template-grid.single-row {
    display: flex;
    flex-direction: column;

    .template-item {
      width: 100%;
    }
  }
}
</style>
