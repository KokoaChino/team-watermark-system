<template>
  <div class="template-list">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>模板列表</span>
            <el-tag type="info" size="small" class="count-tag">共 {{ filteredTemplates.length }} 个</el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleCreateNew">
              <el-icon><Plus /></el-icon>
              新建模板
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="filter-section">
        <el-input
          v-model="filterName"
          placeholder="搜索模板名称"
          clearable
          class="filter-input"
          @input="handleFilterChange"
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
          @change="handleFilterChange"
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
          @change="handleFilterChange"
        />
        <el-date-picker
          v-model="filterUpdatedAt"
          type="datetimerange"
          range-separator="至"
          start-placeholder="更新开始时间"
          end-placeholder="更新结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="filter-date"
          @change="handleFilterChange"
        />
      </div>
      
      <div class="template-scroll-wrapper" ref="scrollWrapperRef">
        <div 
          class="template-grid single-row"
          v-loading="loading"
        >
          <div 
            v-for="item in filteredTemplates" 
            :key="item.id"
            class="template-item"
            :class="{ active: selectedTemplate?.id === item.id }"
            @click="handleSelectTemplate(item)"
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
                    <span class="info-value">{{ item.config?.baseConfig?.width }} × {{ item.config?.baseConfig?.height }}</span>
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
              <div class="info-row-full" style="margin-bottom: 6px;">
                <span class="info-label">创建时间</span>
                <span class="info-value">{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="item-actions">
                <el-button type="primary" size="small" @click.stop="handleEdit(item)">
                  <el-icon><Edit /></el-icon>
                  修改
                </el-button>
                <el-button 
                  v-if="canDelete(item)"
                  type="danger" 
                  size="small" 
                  @click.stop="handleDelete(item)"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <el-empty v-if="!loading && templates.length === 0" description="暂无模板，点击上方按钮新建" />
    </el-card>

    <el-card v-if="selectedTemplate" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>模板详情：<span class="highlight-value">{{ selectedTemplate.name }}</span></span>
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
                  <span class="highlight-value">{{ selectedTemplate.config?.baseConfig?.backgroundColor || '#ffffff' }}</span>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="watermark-list-card" shadow="never">
            <template #header>
              <div class="section-header">
                <span class="section-title">水印列表 ({{ selectedTemplate.config?.watermarks?.length || 0 }})</span>
              </div>
            </template>
            
            <div class="watermark-list" v-if="selectedTemplate.config?.watermarks?.length">
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
                <span class="section-title">参数详情：<span class="highlight-value">{{ selectedWatermark.name }}</span></span>
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
                <el-descriptions-item label="基点">
                  <span class="highlight-value">{{ alignMap[selectedWatermark.textConfig.align] }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="字重">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.fontWeight }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="旋转">
                  <span class="highlight-value">{{ selectedWatermark.textConfig.rotation }}°</span>
                </el-descriptions-item>
                <el-descriptions-item label="透明度">
                  <span class="highlight-value">{{ (selectedWatermark.textConfig.opacity * 100).toFixed(0) }}%</span>
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

              <template v-if="selectedWatermark.textConfig.gradientEnabled && selectedWatermark.textConfig.gradientColors?.length">
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
                  <span class="highlight-value">{{ (selectedWatermark.imageConfig.opacity * 100).toFixed(0) }}%</span>
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
                  <span class="highlight-value">{{ selectedWatermark.imageConfig.originalWidth || '-' }} × {{ selectedWatermark.imageConfig.originalHeight || '-' }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </template>
          </el-card>
        </div>

        <div class="detail-right">
          <PreviewCanvas
            ref="previewCanvasRef"
            :base-config="selectedTemplate.config?.baseConfig || { width: 800, height: 600, backgroundColor: '#ffffff' }"
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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Picture, Search } from '@element-plus/icons-vue'
import { getTemplateList, deleteTemplate, createDraftFromTemplate, createNewDraft } from '@/api/template'
import { getFontList } from '@/api/font'
import { getTeamInfo } from '@/api/team'
import { useUserStore } from '@/stores/user'
import PreviewCanvas from '@/components/watermark/PreviewCanvas.vue'
import type { WatermarkTemplateVO, TextWatermarkConfigDTO, FontVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const templates = ref<WatermarkTemplateVO[]>([])
const loading = ref(false)
const teamInfo = ref<{ role?: string } | null>(null)
const selectedTemplate = ref<WatermarkTemplateVO | null>(null)
const selectedWatermarkId = ref<string | null>(null)
const previewCanvasRef = ref<InstanceType<typeof PreviewCanvas> | null>(null)
const scrollWrapperRef = ref<HTMLElement | null>(null)
const userFonts = ref<FontVO[]>([])

const filterName = ref('')
const filterCreators = ref<number[]>([])
const filterCreatedAt = ref<[string, string] | null>(null)
const filterUpdatedAt = ref<[string, string] | null>(null)

interface CreatorOption {
  id: number
  username: string
}

const creatorOptions = computed<CreatorOption[]>(() => {
  const creatorMap = new Map<number, string>()
  templates.value.forEach(t => {
    if (t.createdById && t.createdByUsername) {
      creatorMap.set(t.createdById, t.createdByUsername)
    }
  })
  return Array.from(creatorMap.entries()).map(([id, username]) => ({ id, username }))
})

const filteredTemplates = computed(() => {
  let result = templates.value
  
  if (filterName.value.trim()) {
    const keyword = filterName.value.trim().toLowerCase()
    result = result.filter(t => t.name.toLowerCase().includes(keyword))
  }
  
  if (filterCreators.value.length > 0) {
    result = result.filter(t => filterCreators.value.includes(t.createdById))
  }
  
  if (filterCreatedAt.value && filterCreatedAt.value.length === 2) {
    const [start, end] = filterCreatedAt.value
    result = result.filter(t => {
      if (!t.createdAt) return false
      return t.createdAt >= start && t.createdAt <= end
    })
  }
  
  if (filterUpdatedAt.value && filterUpdatedAt.value.length === 2) {
    const [start, end] = filterUpdatedAt.value
    result = result.filter(t => {
      if (!t.updatedAt) return false
      return t.updatedAt >= start && t.updatedAt <= end
    })
  }
  
  return result
})

function handleFilterChange() {
  if (selectedTemplate.value) {
    const stillExists = filteredTemplates.value.some(t => t.id === selectedTemplate.value!.id)
    if (!stillExists) {
      selectedTemplate.value = null
      selectedWatermarkId.value = null
    }
  }
}

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

const isLeader = computed(() => teamInfo.value?.role === 'leader')

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatNumber(num: number | undefined): string {
  if (num === undefined || num === null) return '0.0'
  return num.toFixed(1)
}

function getFontDisplayName(config: TextWatermarkConfigDTO): string {
  const fontFamily = config.fontFamily
  
  const systemFont = systemFonts.find(f => f.value === fontFamily)
  if (systemFont) {
    return systemFont.label
  }
  
  const userFont = userFonts.value.find(f => `custom-font-${f.id}` === fontFamily)
  if (userFont) {
    return userFont.name
  }
  
  return fontFamily || '未知字体'
}

function canDelete(row: WatermarkTemplateVO) {
  return isLeader.value || row.createdById === userStore.userInfo?.id
}

function handleSelectTemplate(item: WatermarkTemplateVO) {
  selectedTemplate.value = item
  selectedWatermarkId.value = null
}

function selectWatermark(id: string | null) {
  selectedWatermarkId.value = id
}

const selectedWatermark = computed(() => {
  if (!selectedTemplate.value?.config?.watermarks) return null
  return selectedTemplate.value.config.watermarks.find(w => w.id === selectedWatermarkId.value) || null
})

function handlePreviewError(e: Event, item: WatermarkTemplateVO) {
  const target = e.target as HTMLImageElement
  target.style.display = 'none'
  if (item.config) {
    item.config.previewImageKey = undefined
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

async function fetchTeamInfo() {
  try {
    const res = await getTeamInfo()
    if (res.code === 200) {
      teamInfo.value = res.data
    }
  } catch (error) {
    console.error('获取团队信息失败:', error)
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

async function handleCreateNew() {
  try {
    const res = await createNewDraft(false)
    if (res.code === 200) {
      if (res.data.hasConflict) {
        try {
          await ElMessageBox.confirm(res.data.conflictMessage || '当前存在未提交的草稿，继续将覆盖之前的编辑内容', '提示', {
            confirmButtonText: '继续',
            cancelButtonText: '取消',
            type: 'warning'
          })
          await createNewDraft(true)
          router.push('/template/draft')
        } catch {
          // 用户取消
        }
      } else {
        router.push('/template/draft')
      }
    }
  } catch (error) {
    console.error('创建草稿失败:', error)
  }
}

async function handleEdit(row: WatermarkTemplateVO) {
  try {
    const res = await createDraftFromTemplate(row.id, false)
    if (res.code === 200) {
      if (res.data.hasConflict) {
        try {
          await ElMessageBox.confirm(res.data.conflictMessage || '当前存在未提交的草稿，继续将覆盖之前的编辑内容', '提示', {
            confirmButtonText: '继续',
            cancelButtonText: '取消',
            type: 'warning'
          })
          await createDraftFromTemplate(row.id, true)
          router.push('/template/draft')
        } catch {
          // 用户取消
        }
      } else {
        router.push('/template/draft')
      }
    }
  } catch (error) {
    console.error('创建草稿失败:', error)
  }
}

async function handleDelete(row: WatermarkTemplateVO) {
  try {
    await ElMessageBox.confirm(`确定要删除模板 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTemplate(row.id)
    ElMessage.success('模板已删除')
    if (selectedTemplate.value?.id === row.id) {
      selectedTemplate.value = null
    }
    fetchTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模板失败:', error)
    }
  }
}

watch(selectedTemplate, async () => {
  await nextTick()
  if (previewCanvasRef.value) {
    previewCanvasRef.value.refreshPreview()
  }
})

onMounted(() => {
  fetchTemplates()
  fetchTeamInfo()
  fetchUserFonts()
})
</script>

<style scoped lang="scss">
.template-list {
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
    width: 200px;
  }
  
  .filter-date {
    width: 360px;
  }
}

.template-scroll-wrapper {
  overflow-x: auto;
  overflow-y: hidden;
}

.template-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;

  &.single-row {
    flex-wrap: nowrap;
    display: inline-flex;
    
    .template-item {
      flex-shrink: 0;
      width: 220px;
    }
  }
}

.template-item {
  display: flex;
  flex-direction: column;
  background-color: #fafafa;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
  width: calc(25% - 12px);
  min-width: 300px;
  flex-grow: 1;

  &:hover {
    background-color: #f5f7fa;
    border-color: #dcdfe6;
  }

  &.active {
    background-color: #ecf5ff;
    border-color: #409eff;
  }

  .preview-wrapper {
    width: 100%;
    height: 300px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f0f2f5;
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
    padding: 10px;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  .item-header {
    margin-bottom: 6px;
  }

  .item-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-info-grid {
    display: flex;
    gap: 8px;
  }

  .info-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 1px;
  }

  .info-row {
    display: flex;
    font-size: 12px;
    line-height: 1.4;
  }

  .info-row-full {
    display: flex;
    font-size: 12px;
    line-height: 1.4;
  }

  .info-label {
    color: #909399;
    width: 52px;
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
    padding-top: 6px;
    border-top: 1px solid #ebeef5;
    
    .el-button {
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
  height: 1000px;
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
</style>
